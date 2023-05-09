package eu.wdaqua.qanary.geosparqlgenerator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.Constituent;
import edu.stanford.nlp.trees.LabeledScoredConstituentFactory;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import info.debatty.java.stringsimilarity.JaroWinkler;

import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.openrdf.query.resultio.stSPARQLQueryResultFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.NamedEntityTagAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;
import eu.earthobservatory.org.StrabonEndpoint.client.EndpointResult;
import eu.earthobservatory.org.StrabonEndpoint.client.SPARQLEndpoint;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;

@Component
/**
 * This component connected automatically to the Qanary pipeline. The Qanary
 * pipeline endpoint defined in application.properties (spring.boot.admin.url)
 *
 * @see <a href=
 *      "https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F"
 *      target="_top">Github wiki howto</a>
 */
public class GeoSparqlGenerator extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(GeoSparqlGenerator.class);
	public static ArrayList<DependencyTreeNode> myTreeNodes = new ArrayList<DependencyTreeNode>();
	public static ArrayList<DependencyTreeNode> myTreeNodes1 = new ArrayList<DependencyTreeNode>();
	public static List<List<Property>> propertiesList = new ArrayList<List<Property>>();
	public static List<String> postagListsInorderTree = new ArrayList<String>();
	public static List<List<Concept>> concpetsLists = new ArrayList<List<Concept>>();
	public static List<List<SpatialRelation>> relationsList = new ArrayList<List<SpatialRelation>>();
	public static List<List<Entity>> instancesList = new ArrayList<List<Entity>>();
	public static String questionText = "", questionTextL = "";
	public static List<String> dbpediaProperty = new ArrayList<String>();
	public static String final_DBpediaProperty = "";
	public final String yagoEndpoint = "http://pyravlos1.di.uoa.gr:9997/Strabon/Query";
	public final String dbpediaEndpoint = "https://dbpedia.org/sparql";
	public final String strabonEndpoint = "http://pyravlos1.di.uoa.gr:8080/geoqa/query";
	public final String yago2geostrabonEndpoint = "http://pyravlos1.di.uoa.gr:9999/Strabon/Query"; // update the endpoint

	public static String lemmatize(String documentText) {
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos, lemma");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		List<String> lemmas = new ArrayList<>();
		String lemmetizedQuestion = "";
		// Create an empty Annotation just with the given text
		Annotation document = new Annotation(documentText);
		// run all Annotators on this text
		pipeline.annotate(document);
		// Iterate over all of the sentences found
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			// Iterate over all tokens in a sentence
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				// Retrieve and add the lemma for each word into the
				// list of lemmas
				lemmas.add(token.get(LemmaAnnotation.class));
				lemmetizedQuestion += token.get(LemmaAnnotation.class) + " ";
			}
		}
		return lemmetizedQuestion;
	}

	public static String runSparqlOnEndpoint(String sparqlQuery, String endpointURI) {

		org.apache.jena.query.Query query = QueryFactory.create(sparqlQuery);
		System.out.println("sparql query :" + query.toString());
		QueryExecution exec = QueryExecutionFactory.sparqlService(endpointURI, query);
		ResultSet results = ResultSetFactory.copyResults(exec.execSelect());

		if (!results.hasNext()) {
			System.out.println("There is no next!");
		} else {
			while (results.hasNext()) {

				QuerySolution qs = results.next();
				ArrayList<Query> allQueriesList = new ArrayList<Query>();
				String x = qs.get("x").toString();
				System.out.println("runSparqlOnEndpoint x= " + x);
				return x;

			}
		}
		return null;
	}


	public static boolean runAskQueryOnEndpoint(String sparqlQuery, String endpointURI) {
		QueryExecution exec = QueryExecutionFactory.create(sparqlQuery);
		return exec.execAsk();
	}

	public static Boolean answerAvailable(String concept, String instance, String relation) {
		Boolean found = false;
		//concept = concept.substring(concept.lastIndexOf('/') + 1);
		System.out.println("===============Calling answer available========================");
		// parse the csv into a list of arrays
		ArrayList<String[]> ls = new ArrayList<String[]>();
		String fileName = "qanary_component-GeoSparqlGeneratorYago/src/main/resources/final_table_yago.csv";
		File file = new File(fileName);

		try {
			Scanner inputStream = new Scanner(file);
			// new
			// ClassPathResource("src/main/resources/final_table.csv").getInputStream());
			while (inputStream.hasNext()) {
				String data = inputStream.next();
				String[] arr = data.split(",");
				ls.add(arr);
			}
			inputStream.close();
		} catch (FileNotFoundException e) {
			System.out.println("Csv File not found");
		}
		System.out.println("Concept: " + concept + "\t Relation: " + relation + "\t Instance: " + instance);
		// remove from list lines without the specific concept and relation
		for (int i = 0; i < ls.size(); i++) {
			String line[] = ls.get(i);
			if (!line[0].equalsIgnoreCase(concept) || !line[1].equalsIgnoreCase(relation)) {
				ls.remove(line);
				i = i - 1;
			}
		}

		// find the type of the instance and compare with those in list
		if (!ls.isEmpty()) {
			System.out.println("Getting in table not empty==============");

			String endpoint = "http://pyravlos1.di.uoa.gr:8890/sparql";
			QueryExecution objectToExec;

			String sparqlQuery = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
					+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
					+ " SELECT  distinct ?p "
					+ " WHERE { ?x rdf:type <" + concept + ">. ?x ?p <" + instance + ">. } ";
			System.out.println("sparql query: " + sparqlQuery);
			objectToExec = QueryExecutionFactory.sparqlService(endpoint, sparqlQuery);
			ResultSet r = objectToExec.execSelect();

			while (r.hasNext()) {
				QuerySolution s = r.next();
				System.out.println("property : " + s.getResource("p").getURI());
				dbpediaProperty.add(s.getResource("p").getURI());
				found = true;
			}

//			String query = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
//					+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
//					+ "PREFIX dbo: <http://dbpedia.org/ontology/> " + " SELECT  ?type " + "WHERE { <" + instance
//					+ "> rdf:type ?type. " + "} ";
//
//			System.out.println("Query to DBpedia: " + sparqlQuery);
//			// query db
//
//			
//			while (r.hasNext()) {
//				QuerySolution s = r.next();
//				
//				System.out.print("rdf:type "+s);
			System.out.println("ls size: " + ls.size() + "\nproperty size: " + dbpediaProperty.size());
			for (int i = 0; i < dbpediaProperty.size(); i++) {
				String property = dbpediaProperty.get(i);
				boolean flgproperty = false;
				for (String[] line : ls) {
//					System.out.print(" line[2]: "+line[2]+" ++ ");
					if (property.equalsIgnoreCase(line[2])) {
						System.out.println("Found proprty in Schema Table: " + property);
						flgproperty = true;
					}
				}
				if (!flgproperty) {
					dbpediaProperty.remove(property);
					i = i - 1;
				}

			}
//			for(String property:dbpediaProperty) {
////				System.out.println("  property : "+property +" ===== \n");
//				boolean flgproperty = false;
//				for (String[] line : ls) {
////					System.out.print(" line[2]: "+line[2]+" ++ ");
//					if (property.equalsIgnoreCase(line[2])) {
//						System.out.println("Found proprty in Schema Table: "+property);
//						flgproperty = true;
//					}
//				}
//				if(!flgproperty) {
//					dbpediaProperty.remove(property);
//				}
//			}

			if (dbpediaProperty.contains("http://dbpedia.org/ontology/country")) {
				final_DBpediaProperty = "<http://dbpedia.org/ontology/country>";
			} else if (dbpediaProperty.contains("http://dbpedia.org/ontology/city")) {
				final_DBpediaProperty = "<http://dbpedia.org/ontology/city>";
			}
//			}
		}
//		if (dbpediaProperty.size()>0)
//		{
//			System.out.println("Property List : "+dbpediaProperty.toString());
//		}
		return found;
	}

	public static Boolean checkNeighbours(Concept con, Entity ent) {
		System.out.println("===============Calling checkNeighbours========================");
//		System.out.println("con: "+con.link +"\t ent: "+ent.uri);
//		System.out.println("Concept :");
//		con.print();

		for (int i = 0; i < myTreeNodes.size(); i++) {
			String treeConcept = "";
			String treeEntity = "";
			if (myTreeNodes.get(i).annotationsConcepts.size() > 0) {
				treeConcept = myTreeNodes.get(i).annotationsConcepts.get(0);
			}
			if (myTreeNodes.get(i).annotationsInstance.size() > 0) {
				treeEntity = myTreeNodes.get(i).annotationsInstance.get(0);
			}
			if (!treeConcept.equals("") && con.link.contains(treeConcept)) {
				if (i < (myTreeNodes.size() - 1) && myTreeNodes.get(i + 1).annotationsInstance.size() > 0) {
					if (ent.uri.contains(myTreeNodes.get(i + 1).annotationsInstance.get(0))) {
						System.out.println("return true");
						return true;
					}
				} else if (i > 0 && myTreeNodes.get(i - 1).annotationsInstance.size() > 0) {
					if (ent.uri.contains(myTreeNodes.get(i - 1).annotationsInstance.get(0))) {
						System.out.println("return true");
						return true;
					}
				}
			}
		}

		System.out.println("concept: " + con.link + ":" + con.begin + " : " + con.end + " ===== " + "entity : "
				+ ent.uri + " : " + ent.begin + " : " + ent.end);
		if ((con.end + 1) == ent.begin) // i.e. River Thames
			return true;
		else if ((ent.end + 1) == con.begin) // i.e. Thames River
			return true;
		else {
			if (con.begin <= ent.begin) { // i.e. Edinburgh Castle in which "Edinburgh Castle" is Instance and Castle is
				// Concept/Class
				if (con.end > ent.begin) {
					return true;
				}
			}
			if (ent.begin <= con.begin) { // i.e. River Shannon in which "Shannon" is Instance(river shannon) and River
				// is Concept/class
				if (ent.end > con.begin) {
					return true;
				}
			}

		}
		System.out.println("return false=================== ");
		return false;
	}

	public static Boolean checkTypes(Concept con, Entity ent) {
//		System.out.println("===============Calling checkTypes========================");
		String endpoint = "http://pyravlos1.di.uoa.gr:8890/sparql";
		QueryExecution objectToExec;

		// get type of instance Make sure that we query only DBpedia for DBpedia classes
		// and OSM/GADM for OSM/GADM classes.
		if (con.link.contains("http://yago-knowledge.org") && ent.uri.contains("http://yago-knowledge.org")) {
//			System.out.println("concept: " + con.link + " ===== " + "entity : " + ent.uri);
			String query = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>"
					+ "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"
					+ "PREFIX dbo: <http://dbpedia.org/ontology/>" + "SELECT  ?type " + "WHERE { "
					+ "  ?x rdfs:label \"" + ent.namedEntity + "\"@eng. " + "  ?x rdf:type ?type. " + "}";

			// must query db
			objectToExec = QueryExecutionFactory.sparqlService(endpoint, query);
			ResultSet r = objectToExec.execSelect();
			if (r.hasNext()) {
				QuerySolution s = r.next();

				return s.contains(con.link);
			}
			return false;
		} else if ((con.link.contains("gadm") && ent.uri.contains("gadm"))
				|| (con.link.contains("osm") && ent.uri.contains("osm"))) {
//			System.out.println("concept: " + con.link + " ===== " + "entity : " + ent.uri);
			String host = "pyravlos1.di.uoa.gr";
			Integer port = 8080;
			String appName = "geoqa/Query";
			String query = "select ?x where { <" + ent.uri + "> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> "
					+ con.link + "}";
			String format = "TSV";
			SPARQLEndpoint endpointosm = new SPARQLEndpoint(host, port, appName);
			try {
				EndpointResult result = endpointosm.query(query,
						(stSPARQLQueryResultFormat) stSPARQLQueryResultFormat.valueOf(format));
				String resultString[] = result.getResponse().replaceAll("\n", "\n\t").split("\n");
				if (resultString.length > 2) {
//					System.out.println("Return true====================");
					return true;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
//			System.out.println("Return true====================");
			return false;
		} else
			return false;
	}

	public static void primitiveTraversal(Tree tree, List<Concept> cons, List<Entity> ents, List<String> rels) {

//		nodeT.m_parent = tree.parent();
		for (Tree childTree : tree.children()) {
//			if (childTree.isLeaf())
			primitiveTraversal(childTree, cons, ents, rels);
		}
		boolean flg = false;
//		TreeNodeDependency nodeT = (TreeNodeDependency) tree;
		DependencyTreeNode nodeT = new DependencyTreeNode();
		nodeT.m_name = tree.nodeString();

		if (tree.isLeaf()) {

//			System.out.println("tree node: "+tree.nodeString());
//			System.out.println(
//					"cons.size(): " + cons.size() + "\trels size: " + rels.size() + "\tents.size(): " + ents.size());
			for (Concept con : cons) {

				if (con.label.toLowerCase().equals(tree.nodeString().toLowerCase())) {
//					System.out.println(" concepts : " + con.label+" : ");
					nodeT.addAnnotationConcept(con.link);
//					System.out.print("C");
					flg = true;
					break;
				}
			}
			for (Entity ent : ents) {

				String entString = ent.namedEntity;
				if (entString.contains(" ")) {
//					System.out.println("Space");

				}

				if (ent.namedEntity.toLowerCase().contains(tree.nodeString().toLowerCase())) {
//					System.out.println(" namedEntity: " + ent.namedEntity+" : ");
					nodeT.addAnnotationInstance(ent.uri);
					flg = true;
//					System.out.print("I");
					break;
				}
			}
//			if(cons.contains(tree.nodeString())) {
//				System.out.println("C");
//			}
//			if(ents.contains(tree.nodeString())) {
//				System.out.println("I");
//			}

			if (rels.contains(tree.nodeString())) {
				nodeT.addAnnotationRelation("R");
				flg = true;
//				System.out.print("R");
			}
			if (flg) {
				myTreeNodes.add(nodeT);
			}

//			dependencyTreeNodeList.add(nodeT);
//			System.out.println("tree : " + tree.nodeString());
//			System.out.println("tree parent node: "+tree.parent().nodeString());
		}
	}

	public static int wordcount(String string)
	{
		int count=0;

		char ch[]= new char[string.length()];
		for(int i=0;i<string.length();i++)
		{
			ch[i]= string.charAt(i);
			if( ((i>0)&&(ch[i]!=' ')&&(ch[i-1]==' ')) || ((ch[0]!=' ')&&(i==0)) )
				count++;
		}
		return count;
	}

	public static void mergeNodesBasedOnInstances(){

	}
	public static void mergeNodesBasedOnConcepts(Tree tree,List<List<Concept>> concepts, String question){

		int wordCount = 0;
		for(List<Concept> cons : concepts){
			//String conceptPhrashinQuestion = question.substring(question.indexOf(con.begin),question.indexOf(con.end));
			//System.out.println("concept phrash on question : "+conceptPhrashinQuestion);
			for(Concept con:cons) {
				System.out.println("concept : " + con.label);
				System.out.println("concept : " + con.link);
				System.out.println("My Question : " + question);
			}
		}

		/*for (int j = 0; j < myTreeNodes1.size() - 1; j++) {
			DependencyTreeNode tnj = myTreeNodes1.get(j);
			DependencyTreeNode tnj1 = myTreeNodes1.get(j + 1);
		}*/
	}

	public static String walkTreeAndGetPattern1() {
		StringBuilder identifiedPattern = new StringBuilder();
//		System.out.println("MyTreeNode Size : " + myTreeNodes1.size());
		for (DependencyTreeNode tn : myTreeNodes1) {

			postagListsInorderTree.add(tn.posTag);
			if (tn.relationList.size() > 0) {
				identifiedPattern.append("R");
				relationsList.add(tn.relationList);
			}
			if (tn.entityList.size() > 0) {
				identifiedPattern.append("I");
				instancesList.add(tn.entityList);
			} else if (tn.conceptList.size() > 0) {
				identifiedPattern.append("C");
				concpetsLists.add(tn.conceptList);
			} else if (tn.propertyList.size() > 0) {
				identifiedPattern.append("P");
				propertiesList.add(tn.propertyList);
			}
//			System.out.println("postag: " + tn.posTag);
		}

		return identifiedPattern.toString();
	}

	public static void walkTreeAndMergeNodes() {

		for (int j = 0; j < myTreeNodes1.size() - 1; j++) {
			DependencyTreeNode tnj = myTreeNodes1.get(j);
			DependencyTreeNode tnj1 = myTreeNodes1.get(j + 1);

			if ((tnj.posTag.equalsIgnoreCase("NN") || tnj.posTag.equalsIgnoreCase("NNP")
					|| tnj.posTag.equalsIgnoreCase("NNPS"))
					&& (tnj1.posTag.equalsIgnoreCase("NN") || tnj1.posTag.equalsIgnoreCase("NNP")
							|| tnj1.posTag.equalsIgnoreCase("NNPS"))) {
				if ((tnj.conceptList.size() > 0 || tnj.entityList.size() > 0)
						&& (tnj1.conceptList.size() > 0 || tnj1.entityList.size() > 0)) {
					tnj.m_name += " " + tnj1.m_name;
					tnj.endIndex = tnj1.endIndex;
					if (tnj1.conceptList.size() > 0) {
						tnj.conceptList.addAll(tnj1.conceptList);
					}
					if (tnj1.entityList.size() > 0) {
						tnj.entityList.addAll(tnj1.entityList);
					}
					myTreeNodes1.remove(j + 1);
					j = j - 1;
					continue;
				}
			}
			if ((tnj.posTag.equalsIgnoreCase("NN") || tnj.posTag.equalsIgnoreCase("NNP")
					|| tnj.posTag.equalsIgnoreCase("NNPS"))
					&& (tnj1.m_name.contains("'s"))) {
					tnj.m_name += " " + tnj1.m_name;
					tnj.endIndex = tnj1.endIndex;
					if (tnj1.conceptList.size() > 0) {
						tnj.conceptList.addAll(tnj1.conceptList);
					}
					if (tnj1.entityList.size() > 0) {
						tnj.entityList.addAll(tnj1.entityList);
					}
					myTreeNodes1.remove(j + 1);
					j = j - 1;

			}
//			if (tnj.relationList.size() > 0 && tnj1.entityList.size() > 0) {
//				tnj.m_name += " " + tnj1.m_name;
//				tnj.endIndex = tnj1.endIndex;
//				if (tnj1.entityList.size() > 0) {
//					tnj.entityList.addAll(tnj1.entityList);
//				}
//				myTreeNodes1.remove(j + 1);
//				j = j - 1;
//				continue;
//			}
//			if (tnj1.relationList.size() > 0 && tnj.entityList.size() > 0) {
//				tnj.m_name += " " + tnj1.m_name;
//				tnj.endIndex = tnj1.endIndex;
//				if (tnj.entityList.size() > 0) {
//					tnj1.entityList.addAll(tnj.entityList);
//				}
//				myTreeNodes1.remove(j + 1);
//				j = j - 1;
//				continue;
//			}
			if (tnj.conceptList.size() == tnj1.conceptList.size()) {
				boolean flg = false;
				for (Concept con : tnj.conceptList) {
					if (tnj1.conceptList.contains(con)) {
						System.out.println("+++++++++++++++ Inside same Concept +++++++++++++++");
						flg = true;
						break;
					}
				}
				if (flg) {
					tnj.m_name += " " + tnj1.m_name;
					tnj.endIndex = tnj1.endIndex;
					if (tnj1.entityList.size() > 0) {
						tnj.entityList.addAll(tnj1.entityList);
					}
					myTreeNodes1.remove(j + 1);
					j = j - 1;
					continue;
				}

			}
			if (tnj.entityList.size() == tnj1.entityList.size()) {
				boolean flg = false;
				for (Entity ent : tnj.entityList) {
					if (tnj1.entityList.contains(ent)) {
						System.out.println("+++++++++++++++ Inside same Entity +++++++++++++++");
						flg = true;
						break;
					}
				}
				if (flg) {
					tnj.m_name += " " + tnj1.m_name;
					tnj.endIndex = tnj1.endIndex;
					if (tnj1.conceptList.size() > 0) {
						tnj.conceptList.addAll(tnj1.conceptList);
					}
					myTreeNodes1.remove(j + 1);
					j = j - 1;
					continue;
				}
			}

//			if (tnj1.posTag.equalsIgnoreCase(",")) {
//				if (myTreeNodes1.size() >= (j + 2)) {
//
//					DependencyTreeNode tnj2 = myTreeNodes1.get(j + 2);
//					if ((tnj.posTag.equalsIgnoreCase("NN") || tnj.posTag.equalsIgnoreCase("NNP")
//							|| tnj.posTag.equalsIgnoreCase("NNPS"))
//							&& (tnj2.posTag.equalsIgnoreCase("NN") || tnj2.posTag.equalsIgnoreCase("NNP")
//									|| tnj2.posTag.equalsIgnoreCase("NNPS"))) {
//						if ((tnj.conceptList.size() > 0 || tnj.entityList.size() > 0)
//								&& (tnj2.conceptList.size() > 0 || tnj2.entityList.size() > 0)) {
//							tnj.m_name += " " + tnj2.m_name;
//							tnj.endIndex = tnj2.endIndex;
//							if (tnj2.conceptList.size() > 0) {
//								tnj.conceptList.addAll(tnj2.conceptList);
//							}
//							if (tnj2.entityList.size() > 0) {
//								tnj.entityList.addAll(tnj2.entityList);
//							}
//							myTreeNodes1.remove(j + 1);
//							myTreeNodes1.remove(j+2);
//							j = j - 1;
//						}
//					}
//				}
//			}
		}
	}

	public static void firstTraversal(Tree tree) {
		for (Tree childTree : tree.children()) {
			firstTraversal(childTree);
		}
		DependencyTreeNode nodeT = new DependencyTreeNode();
		nodeT.m_name = tree.nodeString();

		if (tree.isLeaf()) {

			int ind;
			ind = questionTextL.indexOf(nodeT.m_name);
			if (ind != -1) {
				nodeT.startIndex = ind;
				nodeT.endIndex = ind + nodeT.m_name.length() - 1;
			} else {
				ind = questionText.indexOf(nodeT.m_name);
				if (ind != -1) {
					nodeT.startIndex = ind;
					nodeT.endIndex = ind + nodeT.m_name.length() - 1;
				}
			}

			nodeT.posTag = getPosTagofWord(questionTextL, tree.nodeString());
			myTreeNodes1.add(nodeT);
		}
	}

	public static String getPosTagofWord(String documentText, String word) {
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos,lemma");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		String postags = "";
		// Create an empty Annotation just with the given text
		Annotation document = new Annotation(documentText);
		// run all Annotators on this text
		pipeline.annotate(document);
		// Iterate over all of the sentences found
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			// Iterate over all tokens in a sentence
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				String pos = token.get(PartOfSpeechAnnotation.class);
				if (token.originalText().equalsIgnoreCase(word)) {
					postags = pos;
				}
			}
		}
		return postags;
	}

	public static void printParseTree1() {
		System.out.println("parse tree annotated elements :");
		for (DependencyTreeNode tn : myTreeNodes1) {
			System.out.println("------------------------------------------------------------------");
			System.out.println("Tree node: " + tn.m_name);
			System.out.println("Start index : " + tn.startIndex + "\t end index: " + tn.endIndex);
			System.out.println("Postag : " + tn.posTag);
			System.out.println("No. of Concepts: " + tn.conceptList.size());
			System.out.println("No. of Instances: " + tn.entityList.size());
			System.out.println("No. of Relations: " + tn.relationList.size());
			System.out.println("No. of Properties: " + tn.propertyList.size());
//			System.out.println("Concepts node: " + tn.annotationsConcepts.toString());
//			System.out.println("Relations node: " + tn.annotationsRelations.toString());
//			System.out.println("Instances node: " + tn.annotationsInstance.toString());
		}
	}

	public static void annotateTreenode(Concept con) {
		System.out.println("Concept start: "+con.begin+"\t end: "+con.end+"\t con label : "+con.label +"\t con link : "+con.link);
		int wordPhrashe = wordcount(con.label);
		if(wordPhrashe>1){
			JaroWinkler jw = new JaroWinkler();
			System.out.println("size difference : "+(myTreeNodes1.size()-wordPhrashe));
			for(int i=0;i< myTreeNodes1.size()-wordPhrashe;i++){
				String treeNodeLabel = "";
				for(int j=0;j<wordPhrashe;j++){
					if(myTreeNodes1.get(i+j).entityList.size()>0)
						break;
					treeNodeLabel += myTreeNodes1.get(i+j).m_name+" ";
					System.out.println("tree node label : "+myTreeNodes1.get(i+j).m_name+"\t (i+j)"+(i+j));
				}
				System.out.println("treeNodeLabel : "+treeNodeLabel);
				treeNodeLabel = treeNodeLabel.trim();
				System.out.println("string similarity : "+ jw.similarity(treeNodeLabel,con.label));
				if(jw.similarity(treeNodeLabel, con.label)>0.99) {
					DependencyTreeNode tn = myTreeNodes1.get(i);
					for (int j = 1; j < wordPhrashe; j++) {
						tn.m_name += " " + myTreeNodes1.get(i + j).m_name;
						tn.relationList.addAll(myTreeNodes1.get(i + j).relationList);
						tn.entityList.addAll(myTreeNodes1.get(i + j).entityList);
						tn.endIndex = myTreeNodes1.get(i + j).endIndex;
					}
					int cnt = 1;
					for (int j = 1; cnt < wordPhrashe; cnt++) {
						myTreeNodes1.remove(i+j);
						System.out.println("removing node : (i+j) : "+(i+j));
					}
					for(DependencyTreeNode treenode:myTreeNodes1){
						System.out.println("node label : "+treenode.m_name);
						System.out.println("node indexes : "+treenode.startIndex +"\t end : "+treenode.endIndex);
					}
					break;
				}
			}
		}
		for (DependencyTreeNode tn : myTreeNodes1) {
//			System.out.println("tree node start: "+tn.startIndex +"\t end: "+tn.endIndex);
			if (tn.startIndex < con.end && tn.endIndex > con.begin) {

				if (con.label.contains(tn.m_name) && tn.m_name.length() > 1) { // con.label.equalsIgnoreCase(tn.m_name))
					// {
					if (!(con.label.length() > tn.m_name.length())) {
						tn.conceptList.add(con);
					}
				}
			}
		}
	}

	public static void annotateTreenode(Entity ent) {
		System.out.println("Entity start: " + ent.begin + "\t end: " + ent.end + "\t enity label : "+ent.namedEntity);
		for (DependencyTreeNode tn : myTreeNodes1) {
			System.out.println("tree node start: " + tn.startIndex + "\t end: " + tn.endIndex);
			if (tn.startIndex < ent.end && tn.endIndex > ent.begin && tn.m_name.length() > 1) {
				System.out.println("getting in index condition ");
				System.out.println("Named Entity : "+ent.namedEntity +"\t Tree Node : "+tn.m_name);
				if (ent.namedEntity.toLowerCase(Locale.ROOT).contains(tn.m_name.toLowerCase(Locale.ROOT)) && !tn.m_name.equalsIgnoreCase("and") && !tn.m_name.equalsIgnoreCase("the")&& !tn.m_name.equalsIgnoreCase("to")) { // ent.namedEntity.equalsIgnoreCase(tn.m_name)) {
//					System.out.println("annotated tree node : "+tn.m_name+"\t with entity : "+ent.uri + "::"+ent.namedEntity);
					tn.entityList.add(ent);
				}
			}
		}
	}

	public static void annotateTreenode(Property property, String myQuestion) {
		System.out.println("Property start: " + property.begin + "\t end: " + property.end+"\t proprty uri : "+property.uri);
		for (DependencyTreeNode tn : myTreeNodes1) {
			System.out.println("tree node start: " + tn.startIndex + "\t end: " + tn.endIndex);
			if((tn.posTag.equalsIgnoreCase("JJS") || tn.posTag.equalsIgnoreCase("JJR")) && (!myQuestion.toLowerCase(Locale.ROOT).contains("population") || myQuestion.toLowerCase(Locale.ROOT).contains(" county")) && !myQuestion.toLowerCase(Locale.ROOT).contains(" area")  ){
				System.out.println("inside ===========================================");
				if(property.uri.toLowerCase(Locale.ROOT).contains("length")||property.uri.toLowerCase(Locale.ROOT).contains("elevation")||property.uri.toLowerCase(Locale.ROOT).contains("area")||property.uri.contains("populationtotal")||property.uri.toLowerCase(Locale.ROOT).contains("floorcount")||property.uri.toLowerCase(Locale.ROOT).contains("createdondate")||property.uri.toLowerCase(Locale.ROOT).contains("capacity")||property.uri.toLowerCase(Locale.ROOT).contains("damheight")){
					tn.propertyList.add(property);
				}
			}else if((myQuestion.contains(" most") || myQuestion.contains(" least"))&&(property.uri.contains("http://kr.di.uoa.gr/yago2geo/ontology/hasGAG_Population")||property.uri.contains("http://dbpedia.org/ontology/numberOfVisitors")||property.uri.contains("http://yago-knowledge.org/resource/infobox/en/visitationnum")||property.uri.contains("http://yago-knowledge.org/resource/infobox/en/populationtotal")||property.uri.contains("http://yago-knowledge.org/resource/infobox/en/elevationm"))){
				if(tn.m_name.contains("populat")||tn.m_name.contains("popular") || tn.m_name.contains("populous") || tn.m_name.contains("taller") || tn.m_name.contains("height") || tn.m_name.contains("elevat")|| tn.m_name.contains("famous")) {
					tn.propertyList.add(property);
					System.out.println("getting in elevation");
				}
			}else if (tn.startIndex < property.end && tn.endIndex > property.begin && tn.m_name.length() > 1 && !tn.m_name.equalsIgnoreCase("the") && !tn.m_name.equalsIgnoreCase("in")&& !tn.m_name.equalsIgnoreCase("national")) {
				String uriLabel = property.uri;
				if(uriLabel.contains("strdf:area") && (tn.m_name.toLowerCase().contains("area") || tn.m_name.toLowerCase().contains("biggest") || tn.m_name.toLowerCase().contains("smallest")|| tn.m_name.toLowerCase().contains("largest"))){
					System.out.println("tree node label : " + tn.m_name + "\t urilabel : " + uriLabel);
					tn.propertyList.add(property);
				}else {
					if(uriLabel.contains("/")) {
						uriLabel = uriLabel.substring(uriLabel.lastIndexOf("/"));
						if (uriLabel.toLowerCase().contains(tn.m_name.toLowerCase())) { // ent.namedEntity.equalsIgnoreCase(tn.m_name))
							// {
							System.out.println("tree node label : " + tn.m_name + "\t urilabel : " + uriLabel);
							tn.propertyList.add(property);
						}
					}
				}
			}
		}
	}

	public static void annotateTreenode(SpatialRelation sr) {
		System.out.println("sr.relation : " + sr.relation);
		for (DependencyTreeNode tn : myTreeNodes1) {
			if (tn.m_name.contains(sr.relation) ||(tn.m_name.contains("cross")&&sr.relation.contains("cross")) || (tn.m_name.contains("most") && sr.relation.contains("most"))) {
				if (!(sr.relation.length() < tn.m_name.length())) {
					System.out.println("relation : " + sr.relation);
					if (sr.relation.equalsIgnoreCase("nearest") || sr.relation.equalsIgnoreCase("closest"))
						sr.relation = "distance";
					if (sr.relation.equalsIgnoreCase("on"))
						sr.relation = "crosses";
					tn.relationList.add(sr);
				}
			}else if(tn.m_name.contains("locat")&&sr.relation.equalsIgnoreCase("located")){
				tn.relationList.add(sr);
			}
		}
	}

	public static ArrayList<String> getQPConstituents(String question){
		// set up pipeline properties
		ArrayList<String> retValues = new ArrayList<>();
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse");
		// use faster shift reduce parser
//		props.setProperty("parse.model", "edu/stanford/nlp/models/srparser/englishSR.ser.gz");
//		props.setProperty("parse.maxlen", "100");
		// set up Stanford CoreNLP pipeline
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		// build annotation for a review
		Annotation annotation =
				new Annotation(question);
		// annotate
		pipeline.annotate(annotation);
		// get tree
		Tree tree =
				annotation.get(CoreAnnotations.SentencesAnnotation.class).get(0).get(TreeCoreAnnotations.TreeAnnotation.class);
//		retValues.add(tree.toString());
//		System.out.println(tree);
		Set<Constituent> treeConstituents = tree.constituents(new LabeledScoredConstituentFactory());
		for (Constituent constituent : treeConstituents) {
//			System.out.println("Constituent : "+constituent.label() + " : : "+constituent.value());
			if (constituent.label() != null &&
					( constituent.label().toString().equals("QP"))) {
//				System.out.println("found constituent: "+constituent.toString());
				retValues.add(tree.getLeaves().subList(constituent.start(), constituent.end()+1).toString());
				System.out.println(tree.getLeaves().subList(constituent.start(), constituent.end()+1));
			}
		}
		return retValues;
	}

	public static boolean isRBSMost(String documentText) {
		boolean retVal = false;
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, depparse");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		// Create an empty Annotation just with the given text
		Annotation document = new Annotation(documentText);
		// run all Annotators on this text
		pipeline.annotate(document);
		// Iterate over all of the sentences found
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			SemanticGraph dependencies = sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
			List<SemanticGraphEdge> edges = dependencies.edgeListSorted();
			for (SemanticGraphEdge edge : edges) {
				if ((edge.getSource().toString().contains("JJ")||edge.getSource().toString().contains("NNS")) && edge.getDependent().toString().contains("RBS")) {
					retVal = true;
				} else if (edge.getSource().toString().contains("NN")
						&& edge.getDependent().toString().contains("JJS")) {
					retVal = true;
				}
			}
		}
		return retVal;
	}
	public static String isCDNNS(String documentText) {
		String retVal = "";
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, depparse");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		// Create an empty Annotation just with the given text
		Annotation document = new Annotation(documentText);
		// run all Annotators on this text
		pipeline.annotate(document);
		// Iterate over all of the sentences found
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			SemanticGraph dependencies = sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
			List<SemanticGraphEdge> edges = dependencies.edgeListSorted();
			for (SemanticGraphEdge edge : edges) {
				System.out.println("edge is : "+edge.toString());
				if ((edge.getSource().toString().contains("CD")) && (edge.getDependent().toString().contains("NNS")||edge.getDependent().toString().contains("NN"))) {
					retVal = edge.getSource().toString();
					System.out.println("CD value : "+retVal);
				} else if ((edge.getSource().toString().contains("NN") || edge.getSource().toString().contains("NNS"))
						&& edge.getDependent().toString().contains("CD")) {
					retVal = edge.getDependent().toString();
				}
			}
		}
		return retVal;
	}

	public static double getCDValue(String documentText) {
		double retVal = 0.0;
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, depparse");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		// Create an empty Annotation just with the given text
		Annotation document = new Annotation(documentText);
		// run all Annotators on this text
		pipeline.annotate(document);
		// Iterate over all of the sentences found
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			SemanticGraph dependencies = sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
			List<SemanticGraphEdge> edges = dependencies.edgeListSorted();
			for (SemanticGraphEdge edge : edges) {
				System.out.println("edge is : "+edge.toString());
				if ((edge.getSource().toString().contains("CD"))) {
					String tempVal = edge.getSource().toString();
					tempVal = tempVal.substring(0,tempVal.indexOf("/"));
					//retVal = Double.parseDouble(tempVal);
					String testString = tempVal.replaceAll("[^-\\d]+", "");
					System.out.println("test string: "+testString);
					if(testString.length()>0){
						retVal = Double.parseDouble(testString);
						System.out.println("======= propValue : "+retVal);
					}
					System.out.println("CD value : "+tempVal);
				} else if ( edge.getDependent().toString().contains("CD")) {
					String tempVal = edge.getDependent().toString();
					tempVal = tempVal.substring(0,tempVal.indexOf("/"));
					String testString = tempVal.replaceAll("[^-\\d]+", "");
					System.out.println("test string: "+testString);
					if(testString.length()>0){
						retVal = Double.parseDouble(testString);
						System.out.println("======= propValue : "+retVal);
					}
					System.out.println("CD value : "+tempVal);
				}
			}
		}
		return retVal;
	}

	public static boolean isJJSNN(String documentText) {
		boolean retVal = false;
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, depparse");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		// Create an empty Annotation just with the given text
		Annotation document = new Annotation(documentText);
		// run all Annotators on this text
		pipeline.annotate(document);
		// Iterate over all of the sentences found
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			SemanticGraph dependencies = sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
			List<SemanticGraphEdge> edges = dependencies.edgeListSorted();
			for (SemanticGraphEdge edge : edges) {
				if (edge.getSource().toString().contains("JJS") && edge.getDependent().toString().contains("NN")) {
					retVal = true;
				} else if (edge.getSource().toString().contains("NN")
						&& edge.getDependent().toString().contains("JJS")) {
					retVal = true;
				}
			}
		}
		return retVal;
	}


	public static String getMostRBS(String documentText) {
		String retVal = "";
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, depparse");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		// Create an empty Annotation just with the given text
		Annotation document = new Annotation(documentText);
		// run all Annotators on this text
		pipeline.annotate(document);
		// Iterate over all of the sentences found
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			SemanticGraph dependencies = sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
			List<SemanticGraphEdge> edges = dependencies.edgeListSorted();
			for (SemanticGraphEdge edge : edges) {
//				System.out.println("edge source : "+edge.getSource().toString());
//				System.out.println("edge dependent : "+edge.getDependent().toString());
				if((edge.getSource().toString().contains("most/"))){
					if (edge.getSource().toString().contains("RBS") || edge.getSource().toString().contains("JJS")) {
						String retVals = edge.getDependent().toString();
						retVal = retVals.split("/")[0];
//						System.out.println("retVal : "+retVal);
					}
				}else if(edge.getDependent().toString().toLowerCase(Locale.ROOT).contains("most/")){
					if (edge.getDependent().toString().contains("RBS") || edge.getDependent().toString().contains("JJS")) {
						String retVals = edge.getSource().toString();
						retVal = retVals.split("/")[0];
//						System.out.println("retVal : "+retVal);
					}
				}
			}
		}
		return retVal;
	}

	public static ArrayList<String> getCCConstituents(String question){
		// set up pipeline properties
		ArrayList<String> retValues = new ArrayList<>();
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse");
		// use faster shift reduce parser
//		props.setProperty("parse.model", "edu/stanford/nlp/models/srparser/englishSR.ser.gz");
//		props.setProperty("parse.maxlen", "100");
		// set up Stanford CoreNLP pipeline
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		// build annotation for a review
		Annotation annotation =
				new Annotation(question);
		// annotate
		pipeline.annotate(annotation);
		// get tree
		Tree tree =
				annotation.get(CoreAnnotations.SentencesAnnotation.class).get(0).get(TreeCoreAnnotations.TreeAnnotation.class);
//		retValues.add(tree.toString());
		System.out.println(tree);
		Set<Constituent> treeConstituents = tree.constituents(new LabeledScoredConstituentFactory());
		for (Constituent constituent : treeConstituents) {
//			System.out.println("Constituent : "+constituent.label() + " : : "+constituent.value());
			if (constituent.label() != null &&
					( constituent.label().toString().equals("CC"))) {
				System.out.println("found constituent: "+constituent.toString());
				retValues.add(tree.getLeaves().subList(constituent.start(), constituent.end()+1).toString());
				System.out.println(tree.getLeaves().subList(constituent.start(), constituent.end()+1));
			}
		}
		return retValues;
	}

	public static String getCCPostag(String documentText) {
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos,lemma");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		String postags = "";
		// Create an empty Annotation just with the given text
		Annotation document = new Annotation(documentText);
		// run all Annotators on this text
		pipeline.annotate(document);
		// Iterate over all of the sentences found
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			// Iterate over all tokens in a sentence
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				String pos = token.get(PartOfSpeechAnnotation.class);
				if (pos.contains("CC")) {
					System.out.println("CC postag : "+token.originalText());
					postags = token.originalText();
				}
			}
		}
		return postags;
	}

	public static ArrayList<String> getNPConstituents(String question){
		// set up pipeline properties
		ArrayList<String> retValues = new ArrayList<>();
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse");
		// use faster shift reduce parser
//		props.setProperty("parse.model", "edu/stanford/nlp/models/srparser/englishSR.ser.gz");
//		props.setProperty("parse.maxlen", "100");
		// set up Stanford CoreNLP pipeline
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		// build annotation for a review
		Annotation annotation =
				new Annotation(question);
		// annotate
		pipeline.annotate(annotation);
		// get tree
		Tree tree =
				annotation.get(CoreAnnotations.SentencesAnnotation.class).get(0).get(TreeCoreAnnotations.TreeAnnotation.class);
//		retValues.add(tree.toString());
		System.out.println(tree);
		Set<Constituent> treeConstituents = tree.constituents(new LabeledScoredConstituentFactory());
		for (Constituent constituent : treeConstituents) {
//			System.out.println("Constituent : "+constituent.label() + " : : "+constituent.value());
			if (constituent.label() != null &&
					( constituent.label().toString().equals("NP"))) {
//				System.out.println("found constituent: "+constituent.toString());
				retValues.add(tree.getLeaves().subList(constituent.start(), constituent.end()+1).toString());
//				System.out.println(tree.getLeaves().subList(constituent.start(), constituent.end()+1));
			}
		}
		return retValues;
	}

	public static ArrayList<String> getADJPConstituents(String question){
		// set up pipeline properties
		ArrayList<String> retValues = new ArrayList<>();
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,parse");
		// use faster shift reduce parser
//		props.setProperty("parse.model", "edu/stanford/nlp/models/srparser/englishSR.ser.gz");
//		props.setProperty("parse.maxlen", "100");
		// set up Stanford CoreNLP pipeline
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		// build annotation for a review
		Annotation annotation =
				new Annotation(question);
		// annotate
		pipeline.annotate(annotation);
		// get tree
		Tree tree =
				annotation.get(CoreAnnotations.SentencesAnnotation.class).get(0).get(TreeCoreAnnotations.TreeAnnotation.class);
//		retValues.add(tree.toString());
		System.out.println(tree);
		Set<Constituent> treeConstituents = tree.constituents(new LabeledScoredConstituentFactory());
		for (Constituent constituent : treeConstituents) {
//			System.out.println("Constituent : "+constituent.label() + " : : "+constituent.value());
			if (constituent.label() != null &&
					( constituent.label().toString().equals("ADJP"))) {
				System.out.println("found constituent: "+constituent.toString());
				retValues.add(tree.getLeaves().subList(constituent.start(), constituent.end()+1).toString());
				System.out.println(tree.getLeaves().subList(constituent.start(), constituent.end()+1));
			}
		}
		return retValues;
	}

	public static void checkNPphrashe(List<String> nps){
		for(String npphrs:nps){
			String splittedNp[] = npphrs.split(",");
			if(splittedNp.length>1 && splittedNp.length<5) {
				System.out.println("NP : "+npphrs.toString());
				for (int i = 0; i < splittedNp.length; i++) {

				}
			}
		}
	}

	public static boolean isJJSClosestOrNearest(String documentText) {
		boolean retVal = false;
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos,lemma");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		// Create an empty Annotation just with the given text
		Annotation document = new Annotation(documentText);
		// run all Annotators on this text
		pipeline.annotate(document);
		// Iterate over all of the sentences found
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			// Iterate over all tokens in a sentence
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				String pos = token.get(PartOfSpeechAnnotation.class);
				if (pos.contains("JJS")) {
					if (token.originalText().equalsIgnoreCase("nearest")
							|| token.originalText().equalsIgnoreCase("closest")) {
						retVal = true;
					}
				}
			}
		}
		return retVal;
	}

	public static List<String> getW(String documentText) {
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos,lemma");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		List<String> postags = new ArrayList<>();
		// Create an empty Annotation just with the given text
		Annotation document = new Annotation(documentText);
		// run all Annotators on this text
		pipeline.annotate(document);
		// Iterate over all of the sentences found
		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			// Iterate over all tokens in a sentence
			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
				String pos = token.get(PartOfSpeechAnnotation.class);
				if (pos.contains("WRB")) {
					postags.add(token.get(LemmaAnnotation.class));
				}
			}
		}
		return postags;
	}

	/**
	 * implement this method encapsulating the functionality of your Qanary
	 * component
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		logger.info("process: {}", myQanaryMessage);
		// TODO: implement processing of question

		String detectedPattern = "";
		ArrayList<Query> allQueriesList = new ArrayList<Query>();
		List<String> properties = new ArrayList<String>();
		List<String> propertiesValue = new ArrayList<String>();
		List<Integer> indexOfConcepts = new ArrayList<Integer>();
		List<Integer> indexOfInstances = new ArrayList<Integer>();
		List<Property> propertiesList = new ArrayList<Property>();
		Map<String, List<Integer>> mapOfRelationIdex = new HashMap<String, List<Integer>>();
		Map<Integer, String> patternForQueryGeneration = new HashMap<Integer, String>();
		Map<Integer, String> mapOfGeoRelation = new TreeMap<Integer, String>();
		Map<Integer, List<Concept>> allConcepts = new HashMap<Integer, List<Concept>>();
		Map<Integer, List<Entity>> sameInstances = new HashMap<Integer, List<Entity>>();

		try {
			logger.info("store data in graph {}",
					myQanaryMessage.getValues().get(new URL(myQanaryMessage.getEndpoint().toString())));
			// TODO: insert data in QanaryMessage.outgraph

			QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
			QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);
			String myQuestion = myQanaryQuestion.getTextualRepresentation();
			String myQuestionNL = myQuestion;
			myQuestion = lemmatize(myQuestion);
			questionText = myQuestionNL;
			questionTextL = myQuestion;
			logger.info("Question: {}", myQuestion);

			// myQuestion = myQuestion.replaceAll(" one ", " 1 ");
			// myQuestion = myQuestion.replaceAll(" two ", " 2 ");
			// myQuestion = myQuestion.replaceAll(" three ", " 3 ");	
			// myQuestion = myQuestion.replaceAll(" four ", " 4 ");
			// myQuestion = myQuestion.replaceAll(" five ", " 5 ");
			// myQuestion = myQuestion.replaceAll(" six ", " 6 ");
			// myQuestion = myQuestion.replaceAll(" seven ", " 7 ");
			// myQuestion = myQuestion.replaceAll(" eight ", " 8 ");
			// myQuestion = myQuestion.replaceAll(" nine ", " 9 ");
			// myQuestion = myQuestion.replaceAll(" ten ", " 10 ");

			// System.out.println("Modified String...");
			// logger.info("Question: {}", myQuestion);

			Properties props = new Properties();
			props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, parse");
			StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
			Annotation document = new Annotation(myQuestion);
			pipeline.annotate(document);
			Tree tree = null;
			List<CoreMap> sentences = document.get(SentencesAnnotation.class);
			for (CoreMap sentence : sentences) {
				// traversing the words in the current sentence
				// a CoreLabel is a CoreMap with additional token-specific methods
				for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
					// this is the text of the token
					String word = token.get(TextAnnotation.class); // this is the POS tag of the token
					String pos = token.get(PartOfSpeechAnnotation.class); // this is the NER label of the token
					String ne = token.get(NamedEntityTagAnnotation.class);
				}
				// this is the parse tree of the current sentence
				tree = sentence.get(TreeAnnotation.class);

				tree.pennPrint();

				// this is the Stanford dependency graph of the current sentence
				SemanticGraph dependencies = sentence
						.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);

				dependencies.prettyPrint();

			}

			firstTraversal(tree);
			String sparql;
			boolean dbpediaPropertyFlag = false;
			boolean dbpediaPropertyValueFlag = false;
			Entity ent = new Entity();
			Concept concept = new Concept();
			List<Concept> concepts = new ArrayList<>();
			List<String> geoSPATIALRelations = new ArrayList<String>();
			List<Entity> entities = new ArrayList<Entity>();
			List<String> relationKeywords = new ArrayList<String>();
			ResultSet r;
			String geoRelation = null;
			String thresholdDistance = "";
			String unitDistance = "";
			List<String> distanceUnits = new ArrayList<String>();
			distanceUnits.add("kilometer");
			distanceUnits.add("km");
			distanceUnits.add("metre");
			distanceUnits.add("meter");
			String geoSparqlQuery = "";// prefixs + selectClause;
			boolean thresholdFlag = false;

			relationKeywords.add("in");
			relationKeywords.add("within");
			relationKeywords.add("of");
			relationKeywords.add("inside");
			relationKeywords.add("contains");
			relationKeywords.add("includes");
			relationKeywords.add("have");
			relationKeywords.add("above");
			relationKeywords.add("north");
			relationKeywords.add("below");
			relationKeywords.add("south");
			relationKeywords.add("right");
			relationKeywords.add("east");
			relationKeywords.add("west");
			relationKeywords.add("left");
			relationKeywords.add("near");
			relationKeywords.add("nearby");
			relationKeywords.add("close");
			relationKeywords.add("at most");
			relationKeywords.add("around");
			relationKeywords.add("less than");
			relationKeywords.add("at least");
			relationKeywords.add("center");
			relationKeywords.add("middle");
			relationKeywords.add("border");
			relationKeywords.add("outskirts");
			relationKeywords.add("boundary");
			relationKeywords.add("surround");
			relationKeywords.add("adjacent");
			relationKeywords.add("crosses");
			relationKeywords.add("cross");
			relationKeywords.add("intersect");
			relationKeywords.add("flows");
			relationKeywords.add("flow");

			// Identify distance threshold
			thresholdDistance = myQuestion.replaceAll("[^-\\d]+", "");
			logger.info("Question without numbers: {}", myQuestion.replaceAll("[^-\\d]+", ""));
			if (!thresholdDistance.equals("")) {
				for (String tempUnit : distanceUnits) {

					Pattern p = Pattern.compile("\\b" + tempUnit + "\\b", Pattern.CASE_INSENSITIVE);
					Matcher m = p.matcher(myQuestion.replaceAll(thresholdDistance, ""));
					if (m.find()) {
						unitDistance = tempUnit;
						break;
					}
				}

				if (unitDistance.equalsIgnoreCase("km") || unitDistance.equalsIgnoreCase("kilometer")
						|| unitDistance.equalsIgnoreCase("kms")) {

					thresholdDistance = thresholdDistance + "000";
					thresholdFlag = true;
				}
				if (unitDistance.contains("meter") || unitDistance.contains("metre")|| unitDistance.contains("metres")) {
					thresholdFlag = true;
				}
			}

			// property
			sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> "
					+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "//
					+ "SELECT  ?uri ?start ?end ?pConcept " + "FROM <" + myQanaryQuestion.getInGraph() + "> " //
					+ "WHERE { " //
					+ "  ?a a qa:AnnotationOfRelation . " + "  ?a oa:hasTarget [ " + " a    oa:SpecificResource; "
					+ "           oa:hasSource    ?q; " + "				oa:hasSelector  [ " //
					+ "			         a        oa:TextPositionSelector ; " //
					+ "			         oa:start ?start ; " //
					+ "			         oa:end   ?end " //
					+ "		     ] " //
					+ "  ]; " + "     oa:hasValue ?uri ; oa:hasConcept ?pConcept ;oa:AnnotatedAt ?time} order by(?time)";

			r = myQanaryUtils.selectFromTripleStore(sparql);

			while (r.hasNext()) {
				QuerySolution s = r.next();
				Property property = new Property();
				properties.add(s.getResource("uri").getURI());
				property.begin = s.getLiteral("start").getInt();
				property.end = s.getLiteral("end").getInt();
//				property.label = myQuestionNL.substring(property.begin,property.end);
				property.uri = s.getResource("uri").getURI();
				property.conceptUri = s.getResource("pConcept").getURI();


				if (property.end > 0) {
					propertiesList.add(property);
					dbpediaPropertyFlag = true;
					annotateTreenode(property,myQuestionNL);
				}
				logger.info("Yago (property) uri info {} label {} coceptUri{}", s.getResource("uri").getURI(), property.label, property.conceptUri);
			}

			System.out.println("total properties found : " + propertiesList.size());


			// instance property
			sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> "
					+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "//
					+ "SELECT  ?uri ?start ?end " + "FROM <" + myQanaryQuestion.getInGraph() + "> " //
					+ "WHERE { " //
					+ "  ?a a qa:AnnotationOfRelationInstance . " + "  ?a oa:hasTarget [ " + " a    oa:SpecificResource; "
					+ "           oa:hasSource    ?q; " + "				oa:hasSelector  [ " //
					+ "			         a        oa:TextPositionSelector ; " //
					+ "			         oa:start ?start ; " //
					+ "			         oa:end   ?end " //
					+ "		     ] " //
					+ "  ]; " + "     oa:hasValue ?uri ;oa:AnnotatedAt ?time} order by(?time)";

			r = myQanaryUtils.selectFromTripleStore(sparql);

			while (r.hasNext()) {
				QuerySolution s = r.next();
				Property property = new Property();
				properties.add(s.getResource("uri").getURI());
				property.begin = s.getLiteral("start").getInt();
				property.end = s.getLiteral("end").getInt();
//				property.label = myQuestionNL.substring(property.begin,property.end);
				property.uri = s.getResource("uri").getURI();

				if (property.end > 0) {
					propertiesList.add(property);
					dbpediaPropertyFlag = true;
					annotateTreenode(property,myQuestionNL);
				}
				logger.info("Yago (instance property) uri info {} label {}", s.getResource("uri").getURI(), property.label);
			}

			System.out.println("total properties found : " + propertiesList.size());

			// TODO: refactor this to an enum or config file
			Map<String, String> mappingOfGeospatialRelationsToGeosparqlFunctions = new HashMap<>();
			mappingOfGeospatialRelationsToGeosparqlFunctions.put("geof:sfWithin", "within");
			mappingOfGeospatialRelationsToGeosparqlFunctions.put("geof:sfCrosses", "crosses");
			mappingOfGeospatialRelationsToGeosparqlFunctions.put("geof:distance", "near");
			mappingOfGeospatialRelationsToGeosparqlFunctions.put("strdf:above", "north");
			mappingOfGeospatialRelationsToGeosparqlFunctions.put("strdf:above_left", "north_west");
			mappingOfGeospatialRelationsToGeosparqlFunctions.put("strdf:above_right", "north_east");
			mappingOfGeospatialRelationsToGeosparqlFunctions.put("strdf:below", "south");
			mappingOfGeospatialRelationsToGeosparqlFunctions.put("strdf:below_right", "south_east");
			mappingOfGeospatialRelationsToGeosparqlFunctions.put("strdf:below_left", "south_west");
			mappingOfGeospatialRelationsToGeosparqlFunctions.put("strdf:right", "east");
			mappingOfGeospatialRelationsToGeosparqlFunctions.put("strdf:left", "west");
			mappingOfGeospatialRelationsToGeosparqlFunctions.put("postgis:ST_Centroid", "center");
			mappingOfGeospatialRelationsToGeosparqlFunctions.put("geof:boundary", "boundary");
			// implement the CRI pattern

			// 1. concepts: Retrieve via SPARQL the concepts identified for the
			// given question

			// 2. relation in the question: Retrieves the spatial function
			// supported by the GeoSPARQL from the graph for e.g.
			// fetch the geospatial relation identifier
			sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> "
					+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "//
					+ "SELECT ?geoRelation ?start ?relString " + "FROM <" + myQanaryQuestion.getInGraph() + "> " //
					+ "WHERE { " //
					+ "    ?a a qa:AnnotationOfRelation . " + "?a oa:hasTarget [ "
					+ "		     a               oa:SpecificResource; " //
					+ "		     oa:hasSource    ?q; " //
					+ "	         oa:hasRelation  [ " //
					+ "			         a        oa:GeoRelation ; " //
					+ "			         oa:geoRelation ?geoRelation ; " //
					+ "	         		 oa:hasSelector  [ " //
					+ "			         		a        oa:TextPositionSelector ; " //
					+ "			         		oa:start ?start ; " //
					+ "                          oa:relString ?relString ;" + "		     ] " //
					+ "		     ] " //
					+ "    ] ; " //
					+ "} " //
					+ "ORDER BY ?start ";

			r = myQanaryUtils.selectFromTripleStore(sparql);
			int previous_relation_index = -1;
			while (r.hasNext()) {
				QuerySolution s = r.next();
				logger.info("found relation : {} at {}", s.getResource("geoRelation").getURI().toString(),
						s.getLiteral("start").getInt());
				String geoSpatialRelation = s.getResource("geoRelation").getURI().toString();
				int geoSpatialRelationIndex = s.getLiteral("start").getInt();
				String relStringQuestion = s.getLiteral("relString").getString();
				geoSPATIALRelations.add(geoSpatialRelation);
				System.out.println("geoSpatialRelation : " + geoSpatialRelation);
				if (mapOfRelationIdex.size() == 0) {
					List<Integer> indexes = new ArrayList<Integer>();
					indexes.add(geoSpatialRelationIndex);
					mapOfRelationIdex.put(geoSpatialRelation, indexes);

					SpatialRelation sr = new SpatialRelation();
					sr.relation = relStringQuestion;
					sr.index = geoSpatialRelationIndex;
					sr.relationFunction = geoSpatialRelation;
					previous_relation_index = geoSpatialRelationIndex;
					annotateTreenode(sr);
				} else {
					int difference_in_index = previous_relation_index - geoSpatialRelationIndex;
					if(difference_in_index<0){
						difference_in_index *= -1;
					}
					System.out.println("previous index : "+previous_relation_index+"\t current relation index : "+geoSpatialRelationIndex+"\t difference : "+difference_in_index);
					if (mapOfRelationIdex.keySet().contains(geoSpatialRelation)) {
						if(!(difference_in_index >=10)) {
							if (geoSpatialRelation.contains("geof:sfWithin")) {
								if ((mapOfRelationIdex.keySet().contains("strdf:left")
										|| mapOfRelationIdex.keySet().contains("strdf:right")
										|| mapOfRelationIdex.keySet().contains("strdf:above")
										|| mapOfRelationIdex.keySet().contains("strdf:below"))) {
									continue;
								}
							}
							if (mapOfRelationIdex.keySet().contains("geof:sfContains")) {
								if (geoSpatialRelation.contains("geof:sfWithin")) {
									continue;
								}
							}
						}
						List<Integer> indexes = mapOfRelationIdex.remove(geoSpatialRelation);
						indexes.add(geoSpatialRelationIndex);
						mapOfRelationIdex.put(geoSpatialRelation, indexes);
						SpatialRelation sr = new SpatialRelation();
						sr.relation = relStringQuestion;
						sr.index = geoSpatialRelationIndex;
						sr.relationFunction = geoSpatialRelation;
						previous_relation_index = geoSpatialRelationIndex;
						annotateTreenode(sr);
					} else {
						if(!(difference_in_index >=10)) {
							if (geoSpatialRelation.contains("geof:sfWithin")) {
								if (mapOfRelationIdex.keySet().contains("strdf:left")
										|| mapOfRelationIdex.keySet().contains("strdf:right")
										|| mapOfRelationIdex.keySet().contains("strdf:above")
										|| mapOfRelationIdex.keySet().contains("strdf:below")) {
									continue;
								}
							}
							if (mapOfRelationIdex.keySet().contains("geof:sfContains")) {
								if (geoSpatialRelation.contains("geof:sfWithin")) {
									continue;
								}
							}
						}
						System.out.println("adding relation : "+geoSpatialRelation);
						List<Integer> indexes = new ArrayList<Integer>();
						indexes.add(geoSpatialRelationIndex);
						mapOfRelationIdex.put(geoSpatialRelation, indexes);
						SpatialRelation sr = new SpatialRelation();
						sr.relation = relStringQuestion;
						sr.index = geoSpatialRelationIndex;
						sr.relationFunction = geoSpatialRelation;
						previous_relation_index = geoSpatialRelationIndex;
						annotateTreenode(sr);
					}
				}
			}

			// map the given relation identifier to a GeoSPARQL function
			String geosparqlFunction = mappingOfGeospatialRelationsToGeosparqlFunctions.get(geoRelation);

			// STEP 3.0 Retrieve concepts from Triplestore

			sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> "
					+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "//
					+ "SELECT ?start ?end ?uri " + "FROM <" + myQanaryQuestion.getInGraph() + "> " //
					+ "WHERE { " //
					+ "    ?a a qa:AnnotationOfConcepts . " + "?a oa:hasTarget [ "
					+ "		     a               oa:SpecificResource; " //
					+ "		     oa:hasSource    ?q; " //
					+ "	         oa:hasSelector  [ " //
					+ "			         a        oa:TextPositionSelector ; " //
					+ "			         oa:start ?start ; " //
					+ "			         oa:end   ?end " //
					+ "		     ] " //
					+ "    ] . " //
					+ " ?a oa:hasBody ?uri ; " + "    oa:annotatedBy ?annotator " //
					+ "} " + "ORDER BY ?start ";

			r = myQanaryUtils.selectFromTripleStore(sparql);

			while (r.hasNext()) {
				QuerySolution s = r.next();

				Concept conceptTemp = new Concept();
				conceptTemp.begin = s.getLiteral("start").getInt();

				conceptTemp.end = s.getLiteral("end").getInt();

				conceptTemp.link = s.getResource("uri").getURI();

				conceptTemp.label = myQuestion.substring(conceptTemp.begin, conceptTemp.end);
				// geoSparqlQuery += "" + conceptTemplate.replace("poiURI",
				// conceptTemp.link).replaceAll("poi",
				// "poi" + conceptTemp.begin);
				// newGeoSparqlQuery += "" + conceptTemplate.replace("poiURI",
				// conceptTemp.link).replaceAll("poi",
				// "poi" + conceptTemp.begin);
				indexOfConcepts.add(conceptTemp.begin);
				concepts.add(conceptTemp);
				annotateTreenode(conceptTemp);
				logger.info("Concept start {}, end {}, URI {}", conceptTemp.begin, conceptTemp.end, conceptTemp.link);

			}

			// 3.1 Instance: Retrieve Starting and ending Index of the Instance
			// (Point of Interest) as well as URI
			sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> "
					+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "//
					+ "SELECT ?start ?end ?y2glcount ?y2lcount ?uri " + "FROM <" + myQanaryQuestion.getInGraph() + "> " //
					+ "WHERE { " //
					+ "    ?a a qa:AnnotationOfInstance . " + "?a oa:hasTarget [ "
					+ "		     a               oa:SpecificResource; " //
					+ "		     oa:hasSource    ?q; " //
					+ "	         oa:hasSelector  [ " //
					+ "			         a        oa:TextPositionSelector ; " //
					+ "			         oa:start ?start ; " //
					+ "			         oa:end   ?end ; " //
					+ "			         oa:y2glinkcount   ?y2glcount; "
					+ "                  oa:y2linkcount ?y2lcount "
					+ "		     ] " //
					+ "    ] . " //
					+ " ?a oa:hasBody ?uri ; " + "    oa:annotatedBy ?annotator " //
					+ "} " + "ORDER BY ?start ";

			r = myQanaryUtils.selectFromTripleStore(sparql);
			System.out.println("If r.hasnext() : "+r.hasNext());
			while (r.hasNext()) {
				QuerySolution s = r.next();

				Entity entityTemp = new Entity();
				entityTemp.begin = s.getLiteral("start").getInt();

				entityTemp.end = s.getLiteral("end").getInt();

				entityTemp.uri = s.getResource("uri").getURI();

				System.out.println(
						"uri: " + entityTemp.uri + "\t start: " + entityTemp.begin + "\tend: " + entityTemp.end);

				entityTemp.namedEntity = myQuestionNL.substring(entityTemp.begin, entityTemp.end);

				entityTemp.y2glinkCount = s.getLiteral("y2glcount").getInt();

				entityTemp.y2Linkcount = s.getLiteral("y2lcount").getInt();
				indexOfInstances.add(entityTemp.begin);
				entities.add(entityTemp);
				annotateTreenode(entityTemp);
//				logger.info("Instance start {}, end {}, instance {}, URI{}", entityTemp.begin, entityTemp.end,
//						entityTemp.namedEntity, entityTemp.uri);

			}
//			System.out.println("=============Before merging nodes===========================");
//			printParseTree1();
			System.out.println("============================================================");
			walkTreeAndMergeNodes();
			System.out.println("============================================================");
			printParseTree1();
			System.out.println("============================================================");
			String detectedPatternNew = walkTreeAndGetPattern1();
			System.out.println("++++++++++++++++++++++  Identified Pattern : " + walkTreeAndGetPattern1()
					+ "  ++++++++++++++++++++++");
			System.out.println("Postag sequance: " + postagListsInorderTree.toString());

			geoSPATIALRelations.clear();
			List<String> allSparqlQueries = new ArrayList<String>();
			boolean countFlag = false;
			boolean nearestFlag = isJJSClosestOrNearest(myQuestionNL);
			boolean sortingFlag = isJJSNN(myQuestion);
			boolean qauntifierPhrashFlag = false;
			boolean adjpFlag = false;
			boolean cdFlag = false;
			double cDValue = 0.0;
			boolean ccAndFlag = false;
			boolean ccOrFlag = false;
			String mostValue = "";
			List<String> adjpConstituents = getADJPConstituents(myQuestion);
			if(adjpConstituents.size()>0){
				System.out.println("ADJP Constituents : "+ adjpConstituents);
				adjpFlag = true;
			}
			cDValue = getCDValue(myQuestion);
			if(cDValue!=0.0){
				cdFlag = true;
			}
			mostValue = getMostRBS(myQuestion);
			System.out.println("most value : "+mostValue);
			int propValue = 0;
			String qpPhrasheValue = null;
			List<String> qpConstituents = getQPConstituents(myQuestion);
//			System.out.println("QP constitutes size : "+qpConstituents.size());
			if(qpConstituents.size()>0){

				String splitted[] = qpConstituents.get(0).split(",");
				qpPhrasheValue = qpConstituents.get(0);
				qpPhrasheValue = qpPhrasheValue.replace(",","");
				if(splitted.length>2 && !thresholdFlag){
//					System.out.println("have QP phrashe and need to add Group By");
					qauntifierPhrashFlag = true;
					String testString = splitted[2].replaceAll("[^-\\d]+", "");
					System.out.println("test string: "+testString);
					if(testString.length()>0){
						propValue = Integer.parseInt(testString);
						System.out.println("======= propValue : "+propValue);
					}
				}
			} else {
				String cdValue = isCDNNS(myQuestion);
				if(cdValue.length()>0){
					System.out.println("Found count "+cdValue);
					String splitted[] = cdValue.split("/");
					System.out.println("splitted[0]: "+splitted[0]+"\t splitted[1]: "+splitted[1]);
					String testString = cdValue.replaceAll("[^-\\d]+", "");
					if(testString.length()>0){
						propValue = Integer.parseInt(testString);
						System.out.println("======= propValue inside cd nns : "+propValue);
					}
				}
			}

			int cSize = 0, rSize = 0, iSize = 0, pSize = 0;
			char patterenChar[] = detectedPatternNew.toCharArray();
			for (char ch : patterenChar) {
				if (ch == 'C') {
					cSize++;
				}
				if (ch == 'R') {
					rSize++;
				}
				if (ch == 'I') {
					iSize++;
				}
				if (ch == 'P') {
					pSize++;
				}
			}
			if (postagListsInorderTree.get(0).contains("WRB") && postagListsInorderTree.get(1).contains("JJ")) {
				countFlag = true;

			}
			getCCConstituents(myQuestion);
			String ccTextValue = getCCPostag(myQuestion);
			if(ccTextValue.equalsIgnoreCase("and")){
				ccAndFlag = true;
			}
			if(ccTextValue.equalsIgnoreCase("or")){
				ccOrFlag = true;
			}
//			mergeNodesBasedOnConcepts(tree,concpetsLists,myQuestionNL);
			System.out.println("cSize : " + cSize + "\trSize : " + rSize + "\tiSize : " + iSize + "\tpSize : " + pSize
					+ "\n" + "CountFlag = " + countFlag +"\t Thresholdflag = "+thresholdFlag +"\t sorting flag= "+sortingFlag+"\t GroupByFlag = "+qauntifierPhrashFlag +"\t adjpflag = "+adjpFlag);

			System.out.println("Nearest flag : "+nearestFlag);
			// add code that would check for nearest flag and generate the closest/nearest
			// queries

			List<String> allNPs = getNPConstituents(myQuestionNL);
//			System.out.println("NP Phrashe : "+getNPConstituents(myQuestionNL));
			checkNPphrashe(allNPs);
			// if POSTags contains JJS generate queries with ORDER BY()

			if (cSize == 0 && rSize == 1 && iSize == 1 && pSize== 0) {
				//if (getW(myQuestionNL).size() > 0) {
					System.out.println("***************** RI ( Category 1) identified *****************");
					String spatialRelation = relationsList.get(0).get(0).relationFunction.toLowerCase();
					String sparqlQ = "";
					for (Entity ents : instancesList.get(0)) {
						if(ents.y2glinkCount>0){
							sparqlQ = "select ?iWKT where { <" + ents.uri
									+ "> geo:hasGeometry ?geom. ?geom geo:asWKT ?iWKT. }";
						}else {
							sparqlQ = "select ?y where { <" + ents.uri
									+ "> yago:isLocatedIn ?y. }";
						}

						Query q = new Query();
						q.query = sparqlQ;
						q.score = ents.y2glinkCount+ ents.y2Linkcount;
						allQueriesList.add(q);
						allSparqlQueries.add(sparqlQ);
					}
				//}
			}


			if (cSize == 1 && (rSize == 1 || rSize == 0 || rSize==2 )&& iSize == 0 && pSize == 1) {
//				System.out.println("Inside IP/PRI");
				System.out.println("***************** CP identified *****************");
				String propertyUri = propertiesList.get(0).uri;
				String sparqlQ = "";
				for(Concept con:concpetsLists.get(0)){
					if(propertyUri.contains("strdf:area")){
						if(myQuestion.contains(" sum of ") || myQuestion.contains(" total area ")){
							sparqlQ = "select distinct (SUM(strdf:area(?cWKT)) as ?area) where { ?x rdf:type <"+con.link+">; geo:hasGeometry ?cGeom. ?cGeom geo:asWKT ?cWKT. }";
						} else if(myQuestion.contains(" average ")){
							sparqlQ = "select distinct (AVERAGE(strdf:area(?cWKT)) as ?area) where { ?x rdf:type <"+con.link+">; geo:hasGeometry ?cGeom. ?cGeom geo:asWKT ?cWKT. }";
						} else if(myQuestion.toLowerCase(Locale.ROOT).contains(" maximum ") || myQuestion.toLowerCase(Locale.ROOT).contains(" max ")){
							sparqlQ = "select distinct (MAX(strdf:area(?cWKT)) as ?area) where { ?x rdf:type <"+con.link+">; geo:hasGeometry ?cGeom. ?cGeom geo:asWKT ?cWKT. }";
						}
						else {
							sparqlQ = "select distinct (strdf:area(?cWKT) as ?area) where { ?x rdf:type <" + con.link + ">; geo:hasGeometry ?cGeom. ?cGeom geo:asWKT ?cWKT. }";
							if(myQuestionNL.contains("smallest")){
								sparqlQ += " Order By(?area) Limit 1";
							} else if (myQuestionNL.contains("biggest")){
								sparqlQ += " Order By DESC(?area) Limit 1";
							}
						}
					} else{
						sparqlQ = "select distinct ?property where { ?x rdf:type <"+con.link+">; <"+propertyUri+"> ?property. } ";
						if(myQuestionNL.contains("smallest")){
							sparqlQ += " Order By(?property) Limit 1";
						} else if (myQuestionNL.contains("biggest")){
							sparqlQ += " Order By DESC(?property) Limit 1";
						}
					}

					Query q = new Query();
					q.query = sparqlQ;
					q.score = 0;
					allQueriesList.add(q);
					allSparqlQueries.add(sparqlQ);
				}
			}

			if (cSize == 0 && (rSize == 1 || rSize == 0 )&& iSize == 1 && pSize == 1) {
//				System.out.println("Inside IP/PRI");
				System.out.println("***************** IP identified *****************");
				String propertyUri = propertiesList.get(0).uri;
				String sparqlQ = "";
				for (Entity ents : instancesList.get(0)) {
					if(propertyUri.contains("strdf:area") && (ents.y2glinkCount>0)){
						sparqlQ = "select distinct (strdf:area(?iWKT) as ?area) where {   <" + ents.uri
								+ "> geo:hasGeometry ?geom. ?geom geo:asWKT ?iWKT. }  ";
					} else{
						if(ents.y2glinkCount>0){
							sparqlQ = "select ?property where { <" + ents.uri
									+ "> <"+propertyUri+"> ?property. } ";
						}else {
							sparqlQ = "select ?property where { <" + ents.uri
									+ "> <"+propertyUri+"> ?property. } ";
						}

					}

						/*if(sortingFlag){
							sparqlQ += " ORDER BY DESC(?property) LIMIT 1";
						}*/
						Query q = new Query();
						q.query = sparqlQ;
						q.score = ents.y2glinkCount+ ents.y2Linkcount;
						allQueriesList.add(q);
						allSparqlQueries.add(sparqlQ);
				}
			}

			if (cSize == 1 && (rSize == 2 || rSize == 1 || rSize == 3) && iSize == 1 && pSize == 1) {
				if (rSize == 1) {
					System.out.println("***************** CRIP identified *****************");
				} else {
					System.out.println("***************** CRIRP identified *****************");
				}

				String spatialRelation = relationsList.get(0).get(0).relationFunction.toLowerCase();

				String propertyUri = propertiesList.get(0).uri;
				for (Concept con : concpetsLists.get(0)) {
					List<String> propertyListforConcept = new ArrayList<>();
					for(Property prop:propertiesList){
						if(prop.conceptUri.equalsIgnoreCase(con.link)){
							propertyListforConcept.add(prop.uri);
						}
					}
					for(String prop: propertyListforConcept){
						for (Entity ents : instancesList.get(0)) {
							String sparqlQ = "";

							if((con.link.contains("wordnet_")||con.link.contains("wikicat_")) && prop.contains("yago-knowledge.org/resource/")){

								sparqlQ = "select distinct ?x ?property where { ?type rdfs:subClassOf+ <"+con.link+"> . ?x a ?type ; <"+prop+"> ?property.";
								if(spatialRelation.contains("within") || spatialRelation.contains("contains") || spatialRelation.contains("cross")){
									sparqlQ +=  " ?x yago:isLocatedIn+ <"+ents.uri+">. }";
								}
							}
							else{
								sparqlQ = "select distinct ?x ?property where { ?x rdf:type <"+con.link+">; geo:hasGeometry ?cGeom; <"+prop+"> ?property. ?cGeom geo:asWKT ?cWKT. <"+ents.uri+"> geo:hasGeometry ?iGeom. ?iGeom geo:asWKT ?iWKT. ";
								if(propertyUri.contains("strdf:area")){

									if(myQuestionNL.contains(" sum of ") || myQuestionNL.contains(" total area ")){
										sparqlQ = "select distinct ?x (SUM(strdf:area(?cWKT)) as ?totalArea) where { ?x rdf:type <"+con.link+">; geo:hasGeometry ?cGeom. ?cGeom geo:asWKT ?cWKT. <"+ents.uri+"> geo:hasGeometry ?iGeom. ?iGeom geo:asWKT ?iWKT. ";
									} else if(myQuestionNL.contains(" average ") ){
										sparqlQ = "select distinct ?x (AVERAGE(strdf:area(?cWKT)) as ?totalArea) where { ?x rdf:type <"+con.link+">; geo:hasGeometry ?cGeom. ?cGeom geo:asWKT ?cWKT. <"+ents.uri+"> geo:hasGeometry ?iGeom. ?iGeom geo:asWKT ?iWKT. ";
									} else {
										sparqlQ = "select distinct ?x ((strdf:area(?cWKT)) as ?totalArea) where { ?x rdf:type <"+con.link+">; geo:hasGeometry ?cGeom. ?cGeom geo:asWKT ?cWKT. <"+ents.uri+"> geo:hasGeometry ?iGeom. ?iGeom geo:asWKT ?iWKT. ";
									}

								} else if(propertyUri.contains("_Area")){

									if(myQuestionNL.contains(" sum of ") || myQuestionNL.contains(" total area ")){
										sparqlQ = "select distinct ?x (SUM(?property) as ?totalArea) where { ?x rdf:type <"+con.link+">; geo:hasGeometry ?cGeom; <"+propertyUri+"> ?property. ?cGeom geo:asWKT ?cWKT. <"+ents.uri+"> geo:hasGeometry ?iGeom. ?iGeom geo:asWKT ?iWKT. ";
									} else if(myQuestionNL.contains(" average ") ){
										sparqlQ = "select distinct ?x (AVERAGE(?property) as ?totalArea) where { ?x rdf:type <"+con.link+">; geo:hasGeometry ?cGeom; <"+propertyUri+"> ?property. ?cGeom geo:asWKT ?cWKT. <"+ents.uri+"> geo:hasGeometry ?iGeom. ?iGeom geo:asWKT ?iWKT. ";
									}
								}
								if (spatialRelation.contains("within")) {
									if(!countFlag && thresholdFlag){
										sparqlQ += " FILTER(geof:distance(?cWKT,?iWKT,uom:metre) <= 1000) }";
										sparqlQ = sparqlQ.replace("1000", thresholdDistance);
									} else {
										sparqlQ += " FILTER(geof:sfWithin(?cWKT,?iWKT)) }";
									}
								}
								if (spatialRelation.contains("contains")) {
									sparqlQ += " FILTER(geof:sfWithin(?iWKT,?cWKT)) }";
								}
								if (spatialRelation.contains("distance")) {
									sparqlQ +=  "FILTER(geof:distance(?cWKT,?iWKT,uom:metre) <= 1000) }";
									if (nearestFlag) {
										sparqlQ = sparqlQ.replace(
												"FILTER(geof:distance(?cWKT,?iWKT,uom:metre) <= 1000) }",
												" } ORDER BY(geof:distance(?cWKT,?iWKT,uom:metre)) LIMIT 1");
									} else if (thresholdFlag) {
										sparqlQ = sparqlQ.replace("1000", thresholdDistance);
									} else {
										if (con.link.contains("Restaurant") || con.link.contains("Park")) {
											sparqlQ = sparqlQ.replace("1000", "500");
										}
										if (con.link.contains("City")) {
											sparqlQ = sparqlQ.replace("1000", "5000");
										}
									}
								}
								if (spatialRelation.contains("intersect")) {
									sparqlQ += " FILTER(geof:sfIntersect(?cWKT,?iWKT))} ";
								}
								if (spatialRelation.contains("crosses")) {
									sparqlQ += " FILTER(geof:sfCrosses(?cWKT,?iWKT)) } ";
								}
								if (spatialRelation.contains("boundary")) {
									sparqlQ += " FILTER(geof:sfTouches(?cWKT,?iWKT)) } ";
								}
								if (spatialRelation.contains("right")) {
									sparqlQ += " FILTER(strdf:right(?cWKT,?iWKT)) } ";
								}
								if (spatialRelation.contains("left")) {
									sparqlQ += " FILTER(strdf:left(?cWKT,?iWKT)) } ";
								}
								if (spatialRelation.contains("above")) {
									sparqlQ += " FILTER(strdf:above(?cWKT,?iWKT)) } ";
								}
								if (spatialRelation.contains("below")) {
									sparqlQ += " FILTER(strdf:below(?cWKT,?iWKT)) } ";
								}
							}


							if(sortingFlag || adjpFlag){
								System.out.println("Inside condition for Sorting ");

								if(propertyUri.contains("strdf:area")){
									if(myQuestion.contains(" area") && cdFlag) {
										System.out.println("inside area bigger than :" + cDValue);
										if(myQuestion.contains("bigger than") ||myQuestion.contains("more than") ||myQuestion.contains("higher than") ){
											sparqlQ += " having(?area > "+cDValue+" )";
										} else if(myQuestion.contains("lesser than")|| myQuestion.contains("smaller than")){
											sparqlQ += " having(?area < "+cDValue+" )";
										} else if(myQuestion.contains("at least")){
											sparqlQ += " having(?area >= "+cDValue+" )";
										} else {
											sparqlQ += "ORDER BY DESC(?totalArea) LIMIT 1 ";
										}
									}else {
										sparqlQ += "ORDER BY DESC(?totalArea) LIMIT 1 ";
									}
								}else{
									if(propValue !=0){
										System.out.println("getting inside the propvalue!=0 ");
										if(myQuestion.contains("over") || myQuestion.contains("more than") || myQuestion.contains("higher than") || myQuestion.contains("taller than")){
											sparqlQ = sparqlQ.replace("}"," FILTER(xsd:float(?property) > "+propValue+" ) }");
//										sparqlQ += " having( xsd:float(?property) > "+propValue+" )";
										} else if(myQuestion.contains("below") || myQuestion.contains("less than")||myQuestion.contains("smaller than")){
											sparqlQ = sparqlQ.replace("}"," FILTER(xsd:float(?property) < "+propValue+" ) }");
//										sparqlQ += " having( xsd:float(?property) < "+propValue+" )";
										} else if(myQuestion.contains("least")){
											sparqlQ = sparqlQ.replace("}"," FILTER(xsd:float(?property) >= "+cDValue+" ) }");
//										sparqlQ += " having(xsd:float(?property) >= "+cDValue+" )";
										} else{
											sparqlQ += " ORDER BY DESC(xsd:float(?property)) LIMIT 1 ";
										}
									}else{
										sparqlQ += " ORDER BY DESC(xsd:float(?property)) LIMIT 1 ";
									}
								}
								if(myQuestion.contains("small") || myQuestion.contains("least") ||myQuestion.contains("lowest")||myQuestion.contains("fewest")){
									sparqlQ = sparqlQ.replace("DESC(","ASC(");
								}
								if(propValue >0 && !qauntifierPhrashFlag){
									sparqlQ = sparqlQ.replace("LIMIT 1","LIMIT "+propValue);
								}
							} else if(myQuestion.contains("bigger than") || myQuestion.contains("lesser than")|| myQuestion.contains("smaller than")|| myQuestion.contains("more than")||myQuestion.contains("higher than")||myQuestion.contains("at least")){
								if(myQuestion.contains(" area")) {
									double propval = getCDValue(myQuestion);
									System.out.println("inside area bigger than :" + propval);
									if(myQuestion.contains("bigger than") ||myQuestion.contains("more than") ||myQuestion.contains("higher than") ){
										sparqlQ += " having(?area > "+propval+" )";
									} else if(myQuestion.contains("lesser than")|| myQuestion.contains("smaller than")){
										sparqlQ += " having(?area < "+propval+" )";
									} else if(myQuestion.contains("at least")){
										sparqlQ += " having(?area >= "+propval+" )";
									}
								} else{
									if(propValue !=0){
										System.out.println("getting inside ELSE the propvalue!=0 ");
										if(myQuestion.contains("over") || myQuestion.contains("more than") || myQuestion.contains("higher than") || myQuestion.contains("taller than")){
											sparqlQ = sparqlQ.replace("}"," FILTER(xsd:float(?property) > "+propValue+" ) }");
//										sparqlQ += " having( xsd:float(?property) > "+propValue+" )";
										} else if(myQuestion.contains("below") || myQuestion.contains("less than")||myQuestion.contains("smaller than")){
											sparqlQ = sparqlQ.replace("}"," FILTER(xsd:float(?property) < "+propValue+" ) }");
//										sparqlQ += " having( xsd:float(?property) < "+propValue+" )";
										} else if(myQuestion.contains("least")){
											sparqlQ = sparqlQ.replace("}"," FILTER(xsd:float(?property) >= "+cDValue+" ) }");
//										sparqlQ += " having(xsd:float(?property) >= "+cDValue+" )";
										} else{
											sparqlQ += " ORDER BY DESC(xsd:float(?property)) LIMIT 1 ";
										}
									}
								}
							}else if(cdFlag){
								System.out.println("inside cdFalg");
								if(myQuestion.contains("over") || myQuestion.contains("more than") || myQuestion.contains("higher than") || myQuestion.contains("taller than")){
									sparqlQ = sparqlQ.replace("}"," FILTER(xsd:float(?property) > "+cDValue+" ) }");
//								sparqlQ += " having( xsd:float(?property) > "+cDValue+" )";
								} else if(myQuestion.contains("below") || myQuestion.contains("less than")||myQuestion.contains("smaller than")||myQuestion.contains("shorter than")){
									sparqlQ = sparqlQ.replace("}"," FILTER(xsd:float(?property) < "+cDValue+" ) }");
//								sparqlQ += " having( xsd:float(?property) < "+cDValue+" )";
								} else if(myQuestion.contains("at least")){
//								sparqlQ += " having(xsd:float(?property) >= "+cDValue+" )";
									sparqlQ = sparqlQ.replace("}"," FILTER(xsd:float(?property) >= "+cDValue+" ) }");
								} else if(mostValue.length()>1){
									sparqlQ += " ORDER BY DESC(?property) LIMIT  "+cDValue;
								}
							}

							if(countFlag){
								sparqlQ = sparqlQ.replace("select distinct ?x","select (count(distinct ?x) as ?totalX) ");
							}

						/*if(qauntifierPhrashFlag){
							System.out.println("getting inside the qauntifierPhrashFlag : "+qauntifierPhrashFlag);
							if(myQuestion.contains("over") || myQuestion.contains("more than") || myQuestion.contains("higher than") || myQuestion.contains("taller than")){
								sparqlQ = sparqlQ.replace("}"," FILTER(xsd:float(?property) > "+propValue+" ) }");
//										sparqlQ += " having( xsd:float(?property) > "+propValue+" )";
							} else if(myQuestion.contains("below") || myQuestion.contains("less than")||myQuestion.contains("smaller than")){
								sparqlQ = sparqlQ.replace("}"," FILTER(xsd:float(?property) < "+propValue+" ) }");
//										sparqlQ += " having( xsd:float(?property) < "+propValue+" )";
							} else if(myQuestion.contains("least")){
								sparqlQ = sparqlQ.replace("}"," FILTER(xsd:float(?property) >= "+cDValue+" ) }");
//										sparqlQ += " having(xsd:float(?property) >= "+cDValue+" )";
							}
							*//*sparqlQ += " Group By(?x) having (?total >= thresholdValue) }";
							sparqlQ = sparqlQ.replace("select distinct ?x","ASK { SELECT (count(distinct ?x) as ?total) ");
							if(propValue>0){
								System.out.println("Got in condition");
								if(qpPhrasheValue.contains("at least") || qpPhrasheValue.contains("more than")) {
									sparqlQ = sparqlQ.replace("thresholdValue)", ""+propValue+" )");
								}
								else{
									sparqlQ = sparqlQ.replace(">= thresholdValue)", "< "+propValue+" )");
								}
							}*//*
						}*/
							// add code for count
							Query q = new Query();
							q.query = sparqlQ;
							q.score = ents.y2glinkCount+ ents.y2Linkcount;
							allQueriesList.add(q);
						}
					}

				}
			}
			// Materialized relations:
			// geo:sfWithin
			//geo:sfContains
			//geo:sfTouches
			//geo:sfCrosses
			//geo:sfIntersects


			// CRI

			if (cSize == 1 && (rSize == 1 ||rSize == 2) && iSize == 1 && pSize == 0) {
				System.out.println("***************** CRI identified *****************");
				System.out.println("relation is: " + relationsList.get(0).get(0).relationFunction.toLowerCase());
				String spatialRelation = relationsList.get(0).get(0).relationFunction.toLowerCase();
				String secondSpatialRelation = "";
				if(rSize == 2){
					secondSpatialRelation = relationsList.get(1).get(0).relationFunction.toLowerCase();
					System.out.println("Second Spatial Relation is : "+secondSpatialRelation);
					if((spatialRelation.toLowerCase(Locale.ROOT).contains("geof:sfwithin")&&secondSpatialRelation.toLowerCase(Locale.ROOT).contains("geof:boundary")) || (spatialRelation.toLowerCase(Locale.ROOT).contains("geof:sfcontains")&&secondSpatialRelation.contains("geof:boundary"))){
						spatialRelation = secondSpatialRelation;
					}
					if(spatialRelation.toLowerCase(Locale.ROOT).contains("geof:sfwithin")&&(secondSpatialRelation.toLowerCase(Locale.ROOT).contains("strdf:above") || secondSpatialRelation.toLowerCase(Locale.ROOT).contains("strdf:below") || secondSpatialRelation.toLowerCase(Locale.ROOT).contains("strdf:right") || secondSpatialRelation.toLowerCase(Locale.ROOT).contains("strdf:left"))){
						spatialRelation = secondSpatialRelation;
					}
				}
				System.out.println("spatial relation after change : "+spatialRelation);
				for (Concept con : concpetsLists.get(0)) {
					for (Entity ents : instancesList.get(0)) {
						String sparqlQ = "";
						String sparqlQM= "";
						if(con.link.contains("wordnet") || con.link.contains("wikicat_")){

							if(spatialRelation.contains("within") || spatialRelation.contains("cross") || spatialRelation.contains("contain")){
								if(myQuestion.toLowerCase(Locale.ROOT).startsWith("in which")  || myQuestion.toLowerCase(Locale.ROOT).startsWith("in what")){
									sparqlQ = " select distinct ?x where { ?type rdfs:subClassOf+ <"+con.link+"> . ?x a ?type .  <"+ents.uri+"> _relation_ ?x} ";
								} else {
									sparqlQ = " select distinct ?x where { ?type rdfs:subClassOf+ <"+con.link+"> . ?x a ?type ; _relation_ <"+ents.uri+">. } ";
								}
								sparqlQ = sparqlQ.replace("_relation_","yago:isLocatedIn+");
							}
						}
						else {
							sparqlQ = "select distinct ?x where { ?x rdf:type <" + con.link + ">; geo:hasGeometry ?cGeom. ?cGeom geo:asWKT ?cWKT. <" + ents.uri + "> geo:hasGeometry ?iGeom. ?iGeom geo:asWKT ?iWKT. ";
							sparqlQM = "select distinct ?x where { ?x rdf:type <" + con.link + ">; _mat_rel_ <" + ents.uri + "> . }";
							if (spatialRelation.contains("within")) {
								String askSparql = "PREFIX geo: <http://www.opengis.net/ont/geosparql#> ASK where {?x rdf:type <" + con.link + ">; geo:sfWithin <" + ents.uri + "> . }";
								if (true == false) {
									sparqlQM = sparqlQM.replace("_mat_rel_","geo:sfWithin");
								} else {
									if (!countFlag && thresholdFlag) {
										sparqlQ += " FILTER(geof:distance(?cWKT,?iWKT,uom:metre) <= 1000) }";
										sparqlQ = sparqlQ.replace("1000", thresholdDistance);
									} else {
										if (myQuestion.toLowerCase(Locale.ROOT).startsWith("in which") || myQuestion.toLowerCase(Locale.ROOT).startsWith("in what")) {
											sparqlQ += " FILTER(geof:sfWithin(?iWKT,?CWKT)) }";
										} else {
											sparqlQ += " FILTER(geof:sfWithin(?cWKT,?iWKT)) }";
										}
									}
								}
							}
							if (spatialRelation.contains("contain")) {
								String askSparql = "PREFIX geo: <http://www.opengis.net/ont/geosparql#> ASK where {?x rdf:type <" + con.link + ">; geo:sfContains <" + ents.uri + "> . }";
								if (true == false) {
									sparqlQM = sparqlQM.replace("_mat_rel_","geo:sfContains");
								} else {
									sparqlQ += " FILTER(geof:sfContains(?iWKT,?cWKT)) }";
								}
							}
							if (spatialRelation.contains("distance")) {
								sparqlQ += "FILTER(geof:distance(?cWKT,?iWKT,uom:metre) <= 1000) }";
								if (nearestFlag) {
									sparqlQ = sparqlQ.replace(
											"FILTER(geof:distance(?cWKT,?iWKT,uom:metre) <= 1000) }",
											" } ORDER BY(geof:distance(?cWKT,?iWKT,uom:metre)) LIMIT 1");
								} else if (thresholdFlag) {
									sparqlQ = sparqlQ.replace("1000", thresholdDistance);
								} else {
									if (con.link.contains("Restaurant") || con.link.contains("Park")) {
										sparqlQ = sparqlQ.replace("1000", "500");
									}
									if (con.link.contains("City")) {
										sparqlQ = sparqlQ.replace("1000", "5000");
									}
								}
							}

							if (spatialRelation.contains("crosses")) {
								String askSparql = "PREFIX geo: <http://www.opengis.net/ont/geosparql#> ASK where {?x rdf:type <" + con.link + ">; geo:sfCrosses <" + ents.uri + "> . }";
								if (true == false) {
									sparqlQM = sparqlQM.replace("_mat_rel_","geo:sfCrosses");
								} else {
									sparqlQ += " FILTER(geof:sfCrosses(?cWKT,?iWKT))}";
								}
							}
							if (spatialRelation.contains("intersect")) {
								String askSparql = "PREFIX geo: <http://www.opengis.net/ont/geosparql#> ASK where {?x rdf:type <" + con.link + ">; geo:sfIntersects <" + ents.uri + "> . }";
								if (true == false) {
									sparqlQM = sparqlQM.replace("_mat_rel_","geo:sfIntersects");
								} else {
									sparqlQ += " FILTER(geof:sfIntersects(?cWKT,?iWKT))}";
								}
							}
							if (spatialRelation.contains("boundary")) {
								String askSparql = "PREFIX geo: <http://www.opengis.net/ont/geosparql#> ASK where {?x rdf:type <" + con.link + ">; geo:sfTouches <" + ents.uri + "> . }";
								if (true == false) {
									sparqlQM = sparqlQM.replace("_mat_rel_","geo:sfTouches");
								} else {
									sparqlQ += " FILTER(geof:sfTouches(?cWKT,?iWKT)) }";
								}
							}
							if (spatialRelation.contains("right")) {
								sparqlQ += " FILTER(strdf:right(?cWKT,?iWKT)) }";
							}
							if (spatialRelation.contains("left")) {
								sparqlQ += " FILTER(strdf:left(?cWKT,?iWKT)) }";
							}
							if (spatialRelation.contains("above")) {
								sparqlQ += " FILTER(strdf:above(?cWKT,?iWKT)) }";
							}
							if (spatialRelation.contains("below")) {
								sparqlQ += " FILTER(strdf:below(?cWKT,?iWKT)) }";
							}
						}
						if(countFlag){
							sparqlQ = sparqlQ.replace("select distinct ?x","select (count(distinct ?x) as ?totalX) ");
							if(!sparqlQM.isEmpty() && !sparqlQM.contains("_mat_rel_") ){
								sparqlQM = sparqlQM.replace("select distinct ?x","select (count(distinct ?x) as ?totalX) ");
							}
						}
						if(mostValue.length()>1 && !thresholdFlag && !qauntifierPhrashFlag){
							sparqlQ += " GROUP BY(?x) ORDER BY DESC(count(?y)) ";
							sparqlQ = sparqlQ.replace("select distinct ?x"," select distinct ?x (count(distinct ?y) as ?total) ");
							if(propValue > 0){
								sparqlQ += " LIMIT "+propValue;
							}
							if(!sparqlQM.isEmpty() && !sparqlQM.contains("_mat_rel_") ){
								sparqlQM += " GROUP BY(?x) ORDER BY DESC(count(?y)) ";
								sparqlQM = sparqlQM.replace("select distinct ?x"," select distinct ?x (count(distinct ?y) as ?total) ");
								if(propValue > 0){
									sparqlQM += " LIMIT "+propValue;
								}
							}
						}
						if(qauntifierPhrashFlag){
							sparqlQ += " Group By(?x) having (?total >= thresholdValue) ";
							sparqlQ = sparqlQ.replace("select distinct ?x","SELECT ?x (count(distinct ?x) as ?total) ");
							if(propValue>0){
								System.out.println("Got in condition");
								if(qpPhrasheValue.contains("at least") || qpPhrasheValue.contains("more than")) {
									sparqlQ = sparqlQ.replace("thresholdValue)", ""+propValue+" )");
								}
								else{
									sparqlQ = sparqlQ.replace(">= thresholdValue)", "< "+propValue+" )");
								}
							}
							if(!sparqlQM.isEmpty() && !sparqlQM.contains("_mat_rel_") ){
								sparqlQM += " Group By(?x) having (?total >= thresholdValue) ";
								sparqlQM = sparqlQM.replace("select distinct ?x","SELECT ?x (count(distinct ?x) as ?total) ");
								if(propValue>0){
									System.out.println("Got in condition");
									if(qpPhrasheValue.contains("at least") || qpPhrasheValue.contains("more than")) {
										sparqlQM = sparqlQM.replace("thresholdValue)", ""+propValue+" )");
									}
									else{
										sparqlQM = sparqlQM.replace(">= thresholdValue)", "< "+propValue+" )");
									}
								}
							}
						}
						// add code for count
						Query q = new Query();
						q.query = sparqlQ;
						q.score = ents.y2glinkCount+ ents.y2Linkcount;
						q.queryM = sparqlQM;
						allQueriesList.add(q);

					}
				}

			}

			// CCRI and
			if (cSize == 2 && (rSize == 1 ||rSize == 2) && iSize == 1 && pSize == 0 && ccAndFlag) {
				System.out.println("***************** CCRI(And) identified *****************");
				System.out.println("relation is: " + relationsList.get(0).get(0).relation);
				String spatialRelation = relationsList.get(0).get(0).relationFunction.toLowerCase();

				for (Concept con : concpetsLists.get(0)) {
					for (Concept con2 : concpetsLists.get(1)) {
						for (Entity ents : instancesList.get(0)) {
							String sparqlQ = "";
							sparqlQ = "select distinct ?x ?y where { ?x rdf:type <" + con.link + ">; geo:hasGeometry ?cGeom. ?cGeom geo:asWKT ?cWKT. ?y rdf:type <" + con2.link + ">; geo:hasGeometry ?cGeom2. ?cGeom2 geo:asWKT ?cWKT2. <" + ents.uri + "> geo:hasGeometry ?iGeom. ?iGeom geo:asWKT ?iWKT. ";
							if (spatialRelation.contains("within")) {
								if (!countFlag && thresholdFlag) {
									sparqlQ += " FILTER((geof:distance(?cWKT,?iWKT,uom:metre) <= 1000) && geof:distance(?cWKT2,?iWKT,uom:metre) <= 1000) }";
									sparqlQ = sparqlQ.replace("1000", thresholdDistance);
								} else {
									sparqlQ += " FILTER(geof:sfWithin(?cWKT,?iWKT) && geof:sfWithin(?cWKT2,?iWKT)) }";
								}
							}
							if (spatialRelation.contains("contain")) {
								sparqlQ += " FILTER(geof:sfContains(?iWKT,?cWKT) && geof:sfContains(?iWKT,?cWKT2)) }";
							}
							if (spatialRelation.contains("distance")) {
								sparqlQ += "FILTER((geof:distance(?cWKT,?iWKT,uom:metre) <= 1000) && geof:distance(?cWKT2,?iWKT,uom:metre) <= 1000) }";
								if (nearestFlag) {
									sparqlQ = sparqlQ.replace(
											"FILTER(geof:distance(?cWKT,?iWKT,uom:metre) <= 1000) }",
											" } ORDER BY(geof:distance(?cWKT,?iWKT,uom:metre)) LIMIT 1");
								} else if (thresholdFlag) {
									sparqlQ = sparqlQ.replace("1000", thresholdDistance);
								} else {
									if (con.link.contains("Restaurant") || con.link.contains("Park")) {
										sparqlQ = sparqlQ.replace("1000", "500");
									}
									if (con.link.contains("City")) {
										sparqlQ = sparqlQ.replace("1000", "5000");
									}
								}
							}

							if (spatialRelation.contains("crosses")) {
								sparqlQ += " FILTER(geof:sfCrosses(?cWKT,?iWKT) && geof:sfCrosses(?cWKT2,?iWKT))}";
							}
							if (spatialRelation.contains("intersect")) {
								sparqlQ += " FILTER(geof:sfIntersect(?cWKT,?iWKT) && geof:sfIntersect(?cWKT2,?iWKT))}";
							}
							if (spatialRelation.contains("boundary")) {
								sparqlQ += " FILTER(geof:sfTouches(?cWKT,?iWKT) && geof:sfTouches(?cWKT2,?iWKT)) }";
							}
							if (spatialRelation.contains("right")) {
								sparqlQ += " FILTER(strdf:right(?cWKT,?iWKT) && strdf:right(?cWKT2,?iWKT)) }";
							}
							if (spatialRelation.contains("left")) {
								sparqlQ += " FILTER(strdf:left(?cWKT,?iWKT) && strdf:left(?cWKT2,?iWKT)) }";
							}
							if (spatialRelation.contains("above")) {
								sparqlQ += " FILTER(strdf:above(?cWKT,?iWKT) && strdf:above(?cWKT2,?iWKT)) }";
							}
							if (spatialRelation.contains("below")) {
								sparqlQ += " FILTER(strdf:below(?cWKT,?iWKT) && strdf:below(?cWKT2,?iWKT)) }";
							}
							if (countFlag) {
								sparqlQ = sparqlQ.replace("select distinct ?x ?y", "select (count(distinct ?x) as ?totalX) (count(distinct ?y) as ?totalY) ");
							}
							if (qauntifierPhrashFlag) {
								sparqlQ += " Group By(?x) having (?total >= thresholdValue) ";
								sparqlQ = sparqlQ.replace("select distinct ?x ?y", "SELECT (count(distinct ?x) as ?total) ?y ");
								if (propValue > 0) {
									System.out.println("Got in condition");
									if (qpPhrasheValue.contains("at least") || qpPhrasheValue.contains("more than")) {
										sparqlQ = sparqlQ.replace("thresholdValue)", "" + propValue + " )");
									} else {
										sparqlQ = sparqlQ.replace(">= thresholdValue)", "< " + propValue + " )");
									}
								}
							}
							// add code for count
							Query q = new Query();
							q.query = sparqlQ;
							q.score = ents.y2glinkCount+ ents.y2Linkcount;
							allQueriesList.add(q);

						}
					}
				}

			}

			// CRII and
			if (cSize == 1 && (rSize == 1 ||rSize == 2) && iSize == 2 && pSize == 0 && ccAndFlag) {
				System.out.println("***************** CRII(and) identified *****************");
				System.out.println("relation is: " + relationsList.get(0).get(0).relation);
				String spatialRelation = relationsList.get(0).get(0).relationFunction.toLowerCase();

				for (Concept con : concpetsLists.get(0)) {
					for (Entity ents : instancesList.get(0)) {
						for (Entity ent2 : instancesList.get(1)) {
							String sparqlQ = "";
							sparqlQ = "select distinct ?x where { ?x rdf:type <" + con.link + ">; geo:hasGeometry ?cGeom. ?cGeom geo:asWKT ?cWKT.  <" + ents.uri + "> geo:hasGeometry ?iGeom. ?iGeom geo:asWKT ?iWKT. <" + ent2.uri + "> geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2.";
							if (spatialRelation.contains("within")) {
								if (!countFlag && thresholdFlag) {
									sparqlQ += " FILTER((geof:distance(?cWKT,?iWKT,uom:metre) <= 1000) && geof:distance(?cWKT,?iWKT2,uom:metre) <= 1000) }";
									sparqlQ = sparqlQ.replace("1000", thresholdDistance);
								} else {
									sparqlQ += " FILTER(geof:sfWithin(?cWKT,?iWKT) && geof:sfWithin(?cWKT,?iWKT2)) }";
								}
							}
							if (spatialRelation.contains("contain")) {
								sparqlQ += " FILTER(geof:sfContains(?iWKT,?cWKT) && geof:sfContains(?iWKT2,?cWKT)) }";
							}
							if (spatialRelation.contains("distance")) {
								sparqlQ += "FILTER((geof:distance(?cWKT,?iWKT,uom:metre) <= 1000) && geof:distance(?cWKT,?iWKT2,uom:metre) <= 1000) }";
								if (nearestFlag) {
									sparqlQ = sparqlQ.replace(
											"FILTER(geof:distance(?cWKT,?iWKT,uom:metre) <= 1000) }",
											" } ORDER BY(geof:distance(?cWKT,?iWKT,uom:metre)) LIMIT 1");
								} else if (thresholdFlag) {
									sparqlQ = sparqlQ.replace("1000", thresholdDistance);
								} else {
									if (con.link.contains("Restaurant") || con.link.contains("Park")) {
										sparqlQ = sparqlQ.replace("1000", "500");
									}
									if (con.link.contains("City")) {
										sparqlQ = sparqlQ.replace("1000", "5000");
									}
								}
							}

							if (spatialRelation.contains("crosses")) {
								sparqlQ += " FILTER(geof:sfCrosses(?cWKT,?iWKT) && geof:sfCrosses(?cWKT,?iWKT2))}";
							}
							if (spatialRelation.contains("intersect")) {
								sparqlQ += " FILTER(geof:sfIntersect(?cWKT,?iWKT) && geof:sfIntersect(?cWKT,?iWKT2))}";
							}
							if (spatialRelation.contains("boundary")) {
								sparqlQ += " FILTER(geof:sfTouches(?cWKT,?iWKT) && geof:sfTouches(?cWKT,?iWKT2)) }";
							}
							if (spatialRelation.contains("right")) {
								sparqlQ += " FILTER(strdf:right(?cWKT,?iWKT) && strdf:right(?cWKT,?iWKT2)) }";
							}
							if (spatialRelation.contains("left")) {
								sparqlQ += " FILTER(strdf:left(?cWKT,?iWKT) && strdf:left(?cWKT,?iWKT2)) }";
							}
							if (spatialRelation.contains("above")) {
								sparqlQ += " FILTER(strdf:above(?cWKT,?iWKT) && strdf:above(?cWKT,?iWKT2)) }";
							}
							if (spatialRelation.contains("below")) {
								sparqlQ += " FILTER(strdf:below(?cWKT,?iWKT) && strdf:below(?cWKT,?iWKT2)) }";
							}
							if (countFlag) {
								sparqlQ = sparqlQ.replace("select distinct ?x", "select (count(distinct ?x) as ?totalX) ");
							}
							if (qauntifierPhrashFlag) {
								sparqlQ += " Group By(?x) having (?total >= thresholdValue) ";
								sparqlQ = sparqlQ.replace("select distinct ?x", "SELECT (count(distinct ?x) as ?total) ");
								if (propValue > 0) {
									System.out.println("Got in condition");
									if (qpPhrasheValue.contains("at least") || qpPhrasheValue.contains("more than")) {
										sparqlQ = sparqlQ.replace("thresholdValue)", "" + propValue + " )");
									} else {
										sparqlQ = sparqlQ.replace(">= thresholdValue)", "< " + propValue + " )");
									}
								}
							}
							// add code for count
							Query q = new Query();
							q.query = sparqlQ;
							q.score = ents.y2glinkCount+ ents.y2Linkcount;
							allQueriesList.add(q);

						}
					}
				}

			}

			if (cSize == 2 && (rSize == 1 || rSize==2) && iSize == 0 && pSize == 0) {
				System.out.println("***************** CRC identified *****************");

				String spatialRelation = relationsList.get(0).get(0).relationFunction.toLowerCase();
				System.out.println("Spatial Relation : "+spatialRelation);
				for (Concept con1 : concpetsLists.get(0)) {
					for (Concept con2 : concpetsLists.get(1)) {


						String sparqlQ = "select distinct ?x where { ?x rdf:type <" + con1.link
								+ ">; geo:hasGeometry ?cGeom1. ?cGeom1 geo:asWKT ?cWKT1. ?y rdf:type <"
								+ con2.link + ">; geo:hasGeometry ?cGeom2. ?cGeom2 geo:asWKT ?cWKT2. ";
						if (spatialRelation.contains("within")) {
							if( thresholdFlag){
								sparqlQ += " FILTER(geof:distance(?cWKT1,?cWKT2,uom:metre) <= 1000) }";
								sparqlQ = sparqlQ.replace("1000", thresholdDistance);
							} else {
								sparqlQ += " FILTER(geof:sfWithin(?cWKT2,?cWKT1)) }";
							}
						}
						if (spatialRelation.contains("distance")) {

							sparqlQ += " FILTER(geof:distance(?cWKT1,?cWKT2,uom:metre) <= 1000)}";
							if (nearestFlag) {
								sparqlQ = sparqlQ.replace("FILTER(geof:distance(?cWKT1,?cWKT2,uom:metre) <= 1000)}",
										" } ORDER BY(geof:distance(?cWKT1,?cWKT2,uom:metre)) LIMIT 1");
							} else if (thresholdFlag) {
								sparqlQ = sparqlQ.replace("1000", thresholdDistance);
							} else {
								if (con2.link.contains("Restaurant") || con2.link.contains("Park")) {
									sparqlQ = sparqlQ.replace("1000", "500");
								}
								if (con2.link.contains("City")) {
									sparqlQ = sparqlQ.replace("1000", "5000");
								}
							}
						}
						if (spatialRelation.contains("boundary")) {
							sparqlQ += " FILTER(geof:sfTouches(?cWKT1,?cWKT2)) }";
						}
						if (spatialRelation.contains("cross")) {
							sparqlQ += " FILTER(geof:sfCrosses(?cWKT1,?cWKT2)) }";
						}
						if (spatialRelation.contains("contain")) {
							sparqlQ += " FILTER(geof:sfContains(?cWKT1,?cWKT2)) }";
						}
						if (spatialRelation.contains("intersect")) {
							sparqlQ += " FILTER(geof:sfIntersects(?cWKT1,?cWKT2)) }";
						}
						if (spatialRelation.contains("above")) {
							sparqlQ += " FILTER(strdf:above(?cWKT1,?cWKT2)) }";
						}
						if (spatialRelation.contains("below")) {
							sparqlQ += " FILTER(strdf:below(?cWKT1,?cWKT2)) }";
						}
						if (spatialRelation.contains("right")) {
							sparqlQ += " FILTER(strdf:right(?cWKT1,?cWKT2)) }";
						}
						if (spatialRelation.contains("left")) {
							sparqlQ += " FILTER(strdf:left(?cWKT1,?cWKT2)) }";
						}

						if(countFlag){
							sparqlQ = sparqlQ.replace("select distinct ?x","select (count(distinct ?x) as ?totalX) ");
						}

						if(mostValue.length()>1){
							sparqlQ += " GROUP BY(?x) ORDER BY DESC(count(?y)) ";
							sparqlQ = sparqlQ.replace("select distinct ?x"," select distinct ?x (count(distinct ?y) as ?total) ");
							if(propValue > 0){
								sparqlQ += " LIMIT "+propValue;
							}
						}

						if(qauntifierPhrashFlag){
							sparqlQ += " Group by(?x) having (?total >= thresholdValue) ";
							sparqlQ = sparqlQ.replace("select distinct ?x"," SELECT ?x (count(distinct ?y) as ?total) ");
							if(propValue>0){
								System.out.println("Got in condition");
								if(qpPhrasheValue.contains("at least") || qpPhrasheValue.contains("more than")) {
									sparqlQ = sparqlQ.replace("thresholdValue)", ""+propValue+" )");
								}
								else{
									sparqlQ = sparqlQ.replace(">= thresholdValue)", "< "+propValue+" )");
								}
							}
						}


						Query q = new Query();
						q.query = sparqlQ;
						allQueriesList.add(q);
					}
				}

			}

			if (cSize == 1 && (rSize == 2 || rSize == 3) && iSize == 2 && pSize == 0) {
				System.out.println("***************** CRIRI identified *****************");
				System.out.println("Relations : "+relationsList.get(0).get(0).relationFunction+"\t "+relationsList.get(1).get(0).relationFunction);
				boolean flg = false;

				for (Concept con : concpetsLists.get(0)) {

					for (Entity ent1 : instancesList.get(0)) {
						for (Entity ent2 : instancesList.get(1)) {
							String sparqlQ = "";
							sparqlQ = "select distinct ?x where { ?x rdf:type <"+con.link+">; geo:hasGeometry ?cGeom. ?cGeom geo:asWKT ?cWKT. <"+ent1.uri+"> geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1. <"+ent2.uri+"> geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. ";
							//first relation within
							if (relationsList.get(0).get(0).relationFunction.toLowerCase()
									.contains("within")) {
								System.out.println("getting inside relation within =============");
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("within")) {
									sparqlQ += " FILTER(geof:sfWithin(?cWKT,?iWKT1) && geof:sfWithin(?cWKT,?iWKT2)) }";
									if(qauntifierPhrashFlag){
										sparqlQ +=" GROUP BY(?x) ORDER BY DESC(?total) HAVING(?total >= thresholdValue)";
										sparqlQ = sparqlQ.replace("select distinct ?x {","select distinct ?x (count(distinct ?y) as ?total) {");
										if(propValue>0){
											if(qpPhrasheValue.contains("at least") || qpPhrasheValue.contains("more than")) {
												sparqlQ = sparqlQ.replace("thresholdValue)", ""+propValue+" )");
											}
											else{
												sparqlQ = sparqlQ.replace(">= thresholdValue)", "< "+propValue+" )");
											}
										}
									}
								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("distance")) {
									sparqlQ += " FILTER(geof:sfWithin(?cWKT,?iWKT1) && geof:distance(?cWKT,?iWKT2,uom:metre) <= 1000) }";
									if (nearestFlag) {
										sparqlQ = sparqlQ.replace(
												"FILTER(geof:sfWithin(?cWKT,?iWKT1) && geof:distance(?cWKT,?iWKT2,uom:metre) <= 1000) }",
												" FILTER(geof:sfWithin(?cWKT,?iWKT1) ) } ORDER BY(geof:distance(?cWKT,?iWKT2,uom:metre)) LIMIT 1");
									} else if (thresholdFlag) {
										sparqlQ = sparqlQ.replace("1000", thresholdDistance);
									}

									else {
										if (con.link.contains("Restaurant")
												|| con.link.contains("Park")) {
											sparqlQ = sparqlQ.replace("1000", "500");
										}
										if (con.link.contains("City")) {
											sparqlQ = sparqlQ.replace("1000", "5000");
										}
									}
									if(qauntifierPhrashFlag){
										sparqlQ +=" GROUP BY(?x) ORDER BY DESC(?total) HAVING(?total >= thresholdValue)";
										sparqlQ = sparqlQ.replace("select distinct ?x {","select distinct ?x (count(distinct ?y) as ?total) {");
										if(propValue>0){
											if(qpPhrasheValue.contains("at least") || qpPhrasheValue.contains("more than")) {
												sparqlQ = sparqlQ.replace("thresholdValue)", ""+propValue+" )");
											}
											else{
												sparqlQ = sparqlQ.replace(">= thresholdValue)", "< "+propValue+" )");
											}
										}
									}
								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("boundary")) {
									sparqlQ += " FILTER(geof:sfWithin(?cWKT,?iWKT1) && geof:sfTouches(?cWKT,?iWKT2)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("cross")) {
									sparqlQ += " FILTER(geof:sfWithin(?cWKT,?iWKT1) && geof:sfCrosses(?iWKT1,?iWKT2)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("intersect")) {
									sparqlQ += " FILTER(geof:sfWithin(?cWKT,?iWKT1) && geof:sfIntersects(?iWKT1,?iWKT2)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("right")) {
									sparqlQ += " FILTER(geof:sfWithin(?cWKT,?iWKT1) && strdf:right(?cWKT,?iWKT2)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("left")) {
									sparqlQ += " FILTER(geof:sfWithin(?cWKT,?iWKT1) && strdf:left(?cWKT,?iWKT2)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("above")) {
									sparqlQ += " FILTER(geof:sfWithin(?cWKT,?iWKT1) && strdf:above(?cWKT,?iWKT2)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("below")) {
									sparqlQ += " FILTER(geof:sfWithin(?cWKT,?iWKT1) && strdf:below(?cWKT,?iWKT2)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("contain")) {
									sparqlQ += " FILTER(geof:sfWithin(?cWKT,?iWKT1) && geof:sfContains(?iWKT1,?iWKT2)) }";

								}
							}
							// add code as above for rest of the relations using some example questions
//							if(mostValue.length()>1 && !thresholdFlag && !qauntifierPhrashFlag){
//								sparqlQ += " GROUP BY(?x) ORDER BY DESC(count(?y)) ";
//								sparqlQ = sparqlQ.replace("select distinct ?x"," select distinct ?x (count(distinct ?y) as ?total) ");
//								if(propValue > 0){
//									sparqlQ += " LIMIT "+propValue;
//								}
//							}
							Query q = new Query();
							q.query = sparqlQ;
							q.score = ent1.y2glinkCount+ ent1.y2Linkcount + ent2.y2glinkCount+ ent2.y2Linkcount;
							allQueriesList.add(q);

						}
					}
				}
			}

			if (cSize == 1 && (rSize == 2 || rSize == 3|| rSize == 4) && iSize == 2 && pSize >= 1) {
				System.out.println("***************** PCRIRI identified *****************");
				System.out.println("Relations : "+relationsList.get(0).get(0).relationFunction+"\t "+relationsList.get(1).get(0).relationFunction);
				boolean flg = false;
				String propertyUri = propertiesList.get(0).uri;
				List<String> propertyUris = new ArrayList<>();
				for(Property property:propertiesList){
					propertyUris.add(property.uri);
				}
				for (Concept con : concpetsLists.get(0)) {

					for (Entity ent1 : instancesList.get(0)) {
						for (Entity ent2 : instancesList.get(1)) {
							String sparqlQ = "";
							sparqlQ = "select distinct ?x ?property where { ?x rdf:type <"+con.link+">; geo:hasGeometry ?cGeom. <"+propertyUri+"> ?property. ?cGeom geo:asWKT ?cWKT. <"+ent1.uri+"> geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1. <"+ent2.uri+"> geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. ";
//							sparqlQ = "select distinct ";
							//add code for multiple properties
							/*if(propertyUris.size()==2){
								for(String propUri:propertyUris){
									if(propUri.contains("strdf:area")){
										sparqlQ += " (strdf:area(?cWKT) as ?area) ";
									}
									else {

									}

								}
							}*/
							if(propertyUri.contains("strdf:area")){
								if(myQuestion.contains(" total area ") || myQuestion.contains(" sum ")){
									sparqlQ = "select distinct ?x (SUM(strdf:area(?cWKT)) as ?area) where { ?x rdf:type <"+con.link+">; geo:hasGeometry ?cGeom. ?cGeom geo:asWKT ?cWKT. <"+ent1.uri+"> geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1. <"+ent2.uri+"> geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. ";
								} else if(myQuestion.contains(" average ")){
									sparqlQ = "select distinct ?x (AVERAGE(strdf:area(?cWKT)) as ?area) where { ?x rdf:type <"+con.link+">; geo:hasGeometry ?cGeom. ?cGeom geo:asWKT ?cWKT. <"+ent1.uri+"> geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1. <"+ent2.uri+"> geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. ";
								} else {
									sparqlQ = "select distinct ?x (strdf:area(?cWKT) as ?area) where { ?x rdf:type <" + con.link + ">; geo:hasGeometry ?cGeom. ?cGeom geo:asWKT ?cWKT. <" + ent1.uri + "> geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1. <" + ent2.uri + "> geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. ";
								}
							}
							//first relation within
							if (relationsList.get(0).get(0).relationFunction.toLowerCase()
									.contains("within")) {
								System.out.println("getting inside relation within =============");
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("within")) {
									sparqlQ += " FILTER(geof:sfWithin(?cWKT,?iWKT1) && geof:sfWithin(?cWKT,?iWKT2)) }";
									if(qauntifierPhrashFlag){
										sparqlQ +=" GROUP BY(?x) ORDER BY DESC(?total) HAVING(?total >= thresholdValue)";
										sparqlQ = sparqlQ.replace("select distinct ?x {","select distinct ?x (count(distinct ?y) as ?total) {");
										if(propValue>0){
											if(qpPhrasheValue.contains("at least") || qpPhrasheValue.contains("more than")) {
												sparqlQ = sparqlQ.replace("thresholdValue)", ""+propValue+" )");
											}
											else{
												sparqlQ = sparqlQ.replace(">= thresholdValue)", "< "+propValue+" )");
											}
										}
									}
									if(sortingFlag || adjpFlag){
										System.out.println("Inside condition for Sorting ");

										if(propertyUri.contains("strdf:area")){
											sparqlQ += "ORDER BY DESC(?totalArea) LIMIT 1 ";
										}else{
											sparqlQ += " ORDER BY DESC(xsd:float(?property)) LIMIT 1 ";
										}
										if(myQuestion.contains("small") || myQuestion.contains("least")){
											sparqlQ = sparqlQ.replace("DESC(","ASC(");
										}
										if(propValue >0 && !qauntifierPhrashFlag){
											sparqlQ = sparqlQ.replace("LIMIT 1","LIMIT "+propValue);
										}
									}
								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("distance")) {
									sparqlQ += " FILTER(geof:sfWithin(?cWKT,?iWKT1) && geof:distance(?cWKT,?iWKT2,uom:metre) <= 1000) }";
									if (nearestFlag) {
										sparqlQ = sparqlQ.replace(
												"FILTER(geof:sfWithin(?cWKT,?iWKT1) && geof:distance(?cWKT,?iWKT2,uom:metre) <= 1000) }",
												" FILTER(geof:sfWithin(?cWKT,?iWKT1) ) } ORDER BY(geof:distance(?cWKT,?iWKT2,uom:metre)) LIMIT 1");
									} else if (thresholdFlag) {
										sparqlQ = sparqlQ.replace("1000", thresholdDistance);
									}

									else {
										if (con.link.contains("Restaurant")
												|| con.link.contains("Park")) {
											sparqlQ = sparqlQ.replace("1000", "500");
										}
										if (con.link.contains("City")) {
											sparqlQ = sparqlQ.replace("1000", "5000");
										}
									}
									if(qauntifierPhrashFlag){
										sparqlQ +=" GROUP BY(?x) ORDER BY DESC(?total) HAVING(?total >= thresholdValue)";
										sparqlQ = sparqlQ.replace("select distinct ?x {","select distinct ?x (count(distinct ?y) as ?total) {");
										if(propValue>0){
											if(qpPhrasheValue.contains("at least") || qpPhrasheValue.contains("more than")) {
												sparqlQ = sparqlQ.replace("thresholdValue)", ""+propValue+" )");
											}
											else{
												sparqlQ = sparqlQ.replace(">= thresholdValue)", "< "+propValue+" )");
											}
										}
									}
								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("boundary")) {
									sparqlQ += " FILTER(geof:sfWithin(?cWKT,?iWKT1) && geof:sfTouches(?cWKT,?iWKT2)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("cross")) {
									sparqlQ += " FILTER(geof:sfWithin(?cWKT,?iWKT1) && geof:sfCrosses(?iWKT1,?iWKT2)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("intersect")) {
									sparqlQ += " FILTER(geof:sfWithin(?cWKT,?iWKT1) && geof:sfIntersects(?iWKT1,?iWKT2)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("right")) {
									sparqlQ += " FILTER(geof:sfWithin(?cWKT,?iWKT1) && strdf:right(?cWKT,?iWKT2)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("left")) {
									sparqlQ += " FILTER(geof:sfWithin(?cWKT,?iWKT1) && strdf:left(?cWKT,?iWKT2)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("above")) {
									sparqlQ += " FILTER(geof:sfWithin(?cWKT,?iWKT1) && strdf:above(?cWKT,?iWKT2)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("below")) {
									sparqlQ += " FILTER(geof:sfWithin(?cWKT,?iWKT1) && strdf:below(?cWKT,?iWKT2)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("contain")) {
									sparqlQ += " FILTER(geof:sfWithin(?cWKT,?iWKT1) && geof:sfContains(?iWKT1,?iWKT2)) }";

								}
							}

							if(myQuestion.contains("bigger than") || myQuestion.contains("lesser than")|| myQuestion.contains("smaller than")|| myQuestion.contains("more than")||myQuestion.contains("higher than")||myQuestion.contains("at least")){
								if(myQuestion.contains(" area")) {
									double propval = getCDValue(myQuestion);
									System.out.println("inside area bigger than :" + propval);
									if(myQuestion.contains("bigger than") ||myQuestion.contains("more than") ||myQuestion.contains("higher than") ){
										sparqlQ += " having(?area > "+propval+" )";
									} else if(myQuestion.contains("lesser than")|| myQuestion.contains("smaller than")){
										sparqlQ += " having(?area < "+propval+" )";
									} else if(myQuestion.contains("at least")){
										sparqlQ += " having(?area >= "+propval+" )";
									}
								}
								if(myQuestion.contains("population")){
									double propval = getCDValue(myQuestion);
									System.out.println("inside population bigger than :" + propval);
									if(myQuestion.contains("bigger than") ||myQuestion.contains("more than") ||myQuestion.contains("higher than") ){
										sparqlQ += " having(?property > "+propval+" )";
									} else if(myQuestion.contains("property than")|| myQuestion.contains("smaller than")){
										sparqlQ += " having(?property < "+propval+" )";
									} else if(myQuestion.contains("at least")){
										sparqlQ += " having(?property >= "+propval+" )";
									}
								}
							}
							// add code as above for rest of the relations using some example questions

							Query q = new Query();
							q.query = sparqlQ;
							q.score = ent1.y2glinkCount+ ent1.y2Linkcount + ent2.y2glinkCount+ ent2.y2Linkcount;
							allQueriesList.add(q);

						}
					}
				}
			}


			if (cSize == 2 && (rSize == 2 || rSize == 3) && iSize == 1 && pSize == 1) {
				System.out.println("***************** PCRCRI identified *****************");
				System.out.println("Relations : "+relationsList.get(0).get(0).relationFunction+"\t "+relationsList.get(1).get(0).relationFunction);
				String propertyUri = propertiesList.get(0).uri;
				for (Concept con1 : concpetsLists.get(0)) {
					for (Concept con2 : concpetsLists.get(1)) {
						List<Property> propertyListforConcept = new ArrayList<>();
						for(Property prop:propertiesList){
							if(prop.conceptUri.equalsIgnoreCase(con1.link) || prop.conceptUri.equalsIgnoreCase(con2.link)){
								propertyListforConcept.add(prop);
							}
						}
						for(Property prop: propertyListforConcept){
							for (Entity ents : instancesList.get(0)) {
								String sparqlQ = "";
								sparqlQ = "select distinct ?x where { ?x rdf:type <"+con1.link+">; <"+propertyUri+"> ?property; geo:hasGeometry ?cGeom1. ?cGeom1 geo:asWKT ?cWKT1. ?y rdf:type <"+con2.link+">; geo:hasGeometry ?cGeom2. ?cGeom2 geo:asWKT ?cWKT2. <"+ents.uri+"> geo:hasGeometry ?iGeom. ?iGeom geo:asWKT ?iWKT. ";
								if(propertyUri.contains("strdf:area")){
									if(myQuestion.contains(" total area ")|| myQuestion.contains(" sum ")){
										sparqlQ = "select distinct ?x (SUM(strdf:area(?cWKT1)) as ?area) where { ?x rdf:type <" + con1.link + ">; geo:hasGeometry ?cGeom1. ?cGeom1 geo:asWKT ?cWKT1. ?y rdf:type <" + con2.link + ">; geo:hasGeometry ?cGeom2. ?cGeom2 geo:asWKT ?cWKT2. <" + ents.uri + "> geo:hasGeometry ?iGeom. ?iGeom geo:asWKT ?iWKT. ";
									} else if(myQuestion.contains(" average ")){
										sparqlQ = "select distinct ?x (AVERAGE(strdf:area(?cWKT1)) as ?area) where { ?x rdf:type <" + con1.link + ">; geo:hasGeometry ?cGeom1. ?cGeom1 geo:asWKT ?cWKT1. ?y rdf:type <" + con2.link + ">; geo:hasGeometry ?cGeom2. ?cGeom2 geo:asWKT ?cWKT2. <" + ents.uri + "> geo:hasGeometry ?iGeom. ?iGeom geo:asWKT ?iWKT. ";
									}else {
										sparqlQ = "select distinct ?x (strdf:area(?cWKT1) as ?area) where { ?x rdf:type <" + con1.link + ">; geo:hasGeometry ?cGeom1. ?cGeom1 geo:asWKT ?cWKT1. ?y rdf:type <" + con2.link + ">; geo:hasGeometry ?cGeom2. ?cGeom2 geo:asWKT ?cWKT2. <" + ents.uri + "> geo:hasGeometry ?iGeom. ?iGeom geo:asWKT ?iWKT. ";
									}
								}
								//first relation within
								if (relationsList.get(0).get(0).relationFunction.toLowerCase()
										.contains("within")) {
									System.out.println("getting inside relation within =============");
									if (relationsList.get(1).get(0).relationFunction.toLowerCase()
											.contains("within")) {
										System.out.println("con1.link : "+con1.link+":: con2.link : "+con2.link+" :: prop :"+prop);
										if(((con1.link.contains("wordnet_")||con1.link.contains("wikicat_")) && (con2.link.contains("wordnet_")||con2.link.contains("wikicat_"))) && prop.conceptUri.contains("yago-knowledge.org/resource/") ) {
											if(prop.conceptUri.equalsIgnoreCase(con1.link) ){
												System.out.println("going inside yago2 Query==================================");
												sparqlQ = " select distinct ?x ?property where { ?type rdfs:subClassOf+ <"+con1.link+"> . ?x a ?type ; <"+prop.uri+"> ?property . ?y rdf:type <"+con2.link+">. ?x yago:isLocatedIn+ <"+ents.uri+"> . ?x yago:isLocatedIn+ ?y. } ";
											} else if(prop.conceptUri.equalsIgnoreCase(con2.link)){
												sparqlQ = " select distinct ?x ?property where { ?type rdfs:subClassOf+ <"+con1.link+"> . ?x a ?type . ?y rdf:type <"+con2.link+">'; <"+prop.uri+"> ?property . ?x yago:isLocatedIn+ <"+ents.uri+"> . ?x yago:isLocatedIn+ ?y. } ";
											}
										}else{
											System.out.println("geeting inside else =================== ");
											sparqlQ += " FILTER(geof:sfWithin(?cWKT1,?iWKT) && geof:sfWithin(?cWKT1,?cWKT2)) }";
											if(qauntifierPhrashFlag){
												sparqlQ +=" GROUP BY(?x) ORDER BY DESC(?total) HAVING(?total >= thresholdValue)";
												sparqlQ = sparqlQ.replace("select distinct ?x {","select ?x (count(?y) as ?total) {");
												if(propValue>0){
													if(qpPhrasheValue.contains("at least") || qpPhrasheValue.contains("more than")) {
														sparqlQ = sparqlQ.replace("thresholdValue)", ""+propValue+" )");
													}
													else{
														sparqlQ = sparqlQ.replace(">= thresholdValue)", "< "+propValue+" )");
													}
												}
											}
										}

									}
									if (relationsList.get(1).get(0).relationFunction.toLowerCase()
											.contains("distance")) {
										sparqlQ += " FILTER(geof:sfWithin(?cWKT1,?iWKT) && geof:distance(?cWKT1,?cWKT2,uom:metre) <= 1000) }";
										if (nearestFlag) {
											sparqlQ = sparqlQ.replace(
													"FILTER(geof:sfWithin(?cWKT1,?iWKT) && geof:distance(?cWKT1,?cWKT2,uom:metre) <= 1000) }",
													" FILTER(geof:sfWithin(?cWKT1, ?cWKT2) ) } ORDER BY(geof:distance(?cWKT2,?iWKT,uom:metre)) LIMIT 1");
										} else if (thresholdFlag) {
											sparqlQ = sparqlQ.replace("1000", thresholdDistance);
										}

										else {
											if (con2.link.contains("Restaurant")
													|| con2.link.contains("Park")) {
												sparqlQ = sparqlQ.replace("1000", "500");
											}
											if (con2.link.contains("City")) {
												sparqlQ = sparqlQ.replace("1000", "5000");
											}
										}
										if(qauntifierPhrashFlag){
											sparqlQ +=" GROUP BY(?x) ORDER BY DESC(?total) HAVING(?total >= thresholdValue)";
											sparqlQ = sparqlQ.replace("select distinct ?x {","select ?x (count(?y) as ?total) {");
											if(propValue>0){
												if(qpPhrasheValue.contains("at least") || qpPhrasheValue.contains("more than")) {
													sparqlQ = sparqlQ.replace("thresholdValue)", ""+propValue+" )");
												}
												else{
													sparqlQ = sparqlQ.replace(">= thresholdValue)", "< "+propValue+" )");
												}
											}
										}
									}
									if (relationsList.get(1).get(0).relationFunction.toLowerCase()
											.contains("boundary")) {
										sparqlQ += " FILTER(geof:sfWithin(?cWKT1,?iWKT) && geof:sfTouches(?cWKT1,?cWKT2)) }";

									}
									if (relationsList.get(1).get(0).relationFunction.toLowerCase()
											.contains("cross")) {
										if(((con1.link.contains("wordnet_")||con1.link.contains("wikicat_")) && prop.conceptUri.contains("yago-knowledge.org/resource/")) && ((con2.link.contains("wordnet_")||con2.link.contains("wikicat_")) && prop.conceptUri.contains("yago-knowledge.org/resource/"))){
											if(prop.conceptUri.equalsIgnoreCase(con1.link) ){
												sparqlQ = " select distinct ?x ?property where { ?type rdfs:subClassOf+ <"+con1.link+"> . ?x a ?type ; <"+prop.uri+"> ?property . ?y rdf:type <"+con2.link+">. ?x yago:isLocatedIn+ <"+ents.uri+"> . ?x yago:isLocatedIn+ ?y. } ";
											} else if(prop.conceptUri.equalsIgnoreCase(con2.link)){
												sparqlQ = " select distinct ?x ?property where { ?type rdfs:subClassOf+ <"+con1.link+"> . ?x a ?type . ?y rdf:type <"+con2.link+">; <"+prop.uri+"> ?property . ?x yago:isLocatedIn+ <"+ents.uri+"> . ?x yago:isLocatedIn+ ?y. } ";
											}
										}else{
											sparqlQ += " FILTER(geof:sfWithin(?cWKT1,?iWKT) && geof:sfCrosses(?cWKT1,?cWKT2)) }";
										}
									}
									if (relationsList.get(1).get(0).relationFunction.toLowerCase()
											.contains("intersect")) {
										sparqlQ += " FILTER(geof:sfWithin(?cWKT1,?cWKT2) && geof:sfIntersects(?cWKT2,?iWKT)) }";

									}
									if (relationsList.get(1).get(0).relationFunction.toLowerCase()
											.contains("right")) {
										sparqlQ += " FILTER(geof:sfWithin(?cWKT1,?iWKT) && strdf:right(?cWKT1,?cWKT2)) }";

									}
									if (relationsList.get(1).get(0).relationFunction.toLowerCase()
											.contains("left")) {
										sparqlQ += " FILTER(geof:sfWithin(?cWKT1,?iWKT) && strdf:left(?cWKT1,?cWKT2)) }";

									}
									if (relationsList.get(1).get(0).relationFunction.toLowerCase()
											.contains("above")) {
										sparqlQ += " FILTER(geof:sfWithin(?cWKT1,?iWKT) && strdf:above(?cWKT1,?cWKT2)) }";

									}
									if (relationsList.get(1).get(0).relationFunction.toLowerCase()
											.contains("below")) {
										sparqlQ += " FILTER(geof:sfWithin(?cWKT1,?iWKT) && strdf:below(?cWKT1,?cWKT2)) }";

									}
									if (relationsList.get(1).get(0).relationFunction.toLowerCase()
											.contains("contain")) {
										if(((con1.link.contains("wordnet_")||con1.link.contains("wikicat_")) && prop.conceptUri.contains("yago-knowledge.org/resource/")) && ((con2.link.contains("wordnet_")||con2.link.contains("wikicat_")) && prop.conceptUri.contains("yago-knowledge.org/resource/"))){
											if(prop.conceptUri.equalsIgnoreCase(con1.link) ){
												sparqlQ = " select distinct ?x ?property where { ?type rdfs:subClassOf+ <"+con1.link+"> . ?x a ?type ; <"+prop.uri+"> ?property . ?y rdf:type <"+con2.link+">. ?x yago:isLocatedIn+ <"+ents.uri+"> . ?y yago:isLocatedIn+ ?x. } ";
											} else if(prop.conceptUri.equalsIgnoreCase(con2.link)){
												sparqlQ = " select distinct ?x ?property where { ?type rdfs:subClassOf+ <"+con1.link+"> . ?x a ?type . ?y rdf:type <"+con2.link+">; <"+prop.uri+"> ?property . ?x yago:isLocatedIn+ <"+ents.uri+"> . ?y yago:isLocatedIn+ ?x. } ";
											}
										}else{
											sparqlQ += " FILTER(geof:sfWithin(?cWKT1,?iWKT) && geof:sfContains(?cWKT1,?cWKT2)) }";
										}


									}
								}
								if (relationsList.get(0).get(0).relationFunction.toLowerCase()
										.contains("contain")) {
									if (relationsList.get(1).get(0).relationFunction.toLowerCase()
											.contains("within")) {
										if(((con1.link.contains("wordnet_")||con1.link.contains("wikicat_")) && prop.conceptUri.contains("yago-knowledge.org/resource/")) && ((con2.link.contains("wordnet_")||con2.link.contains("wikicat_")) && prop.conceptUri.contains("yago-knowledge.org/resource/"))){
											if(prop.conceptUri.equalsIgnoreCase(con1.link) ){
												sparqlQ = " select distinct ?x ?property where { ?type rdfs:subClassOf+ <"+con1.link+"> . ?x a ?type ; <"+prop.uri+"> ?property . ?y rdf:type <"+con2.link+">. ?x yago:isLocatedIn+ <"+ents.uri+"> . ?y yago:isLocatedIn+ ?x. } ";
											} else if(prop.conceptUri.equalsIgnoreCase(con2.link)){
												sparqlQ = " select distinct ?x ?property where { ?type rdfs:subClassOf+ <"+con1.link+"> . ?x a ?type . ?y rdf:type <"+con2.link+">; <"+prop.uri+"> ?property . ?x yago:isLocatedIn+ <"+ents.uri+"> . ?y yago:isLocatedIn+ ?x. } ";
											}
										}else{
											sparqlQ += " FILTER(geof:sfWithin(?cWKT1,?iWKT) && geof:sfContains(?cWKT1,?cWKT2)) }";
										}
									}
								}
								if(sortingFlag || adjpFlag){
									System.out.println("Inside condition for Sorting ");

									if(propertyUri.contains("strdf:area")){
										sparqlQ += "ORDER BY DESC(?area) LIMIT 1 ";
									}else{
										sparqlQ += " ORDER BY DESC(xsd:float(?property)) LIMIT 1 ";
									}
									if(myQuestion.contains("small") || myQuestion.contains("least")){
										sparqlQ = sparqlQ.replace("DESC(","ASC(");
									}
									if(propValue >0 && !qauntifierPhrashFlag){
										sparqlQ = sparqlQ.replace("LIMIT 1","LIMIT "+propValue);
									}
								}

								Query q = new Query();
								q.query = sparqlQ;
								q.score = ent.y2glinkCount+ent.y2Linkcount;
								allQueriesList.add(q);

							}
						}

					}
				}
			}

			if (cSize == 2 && (rSize == 2 || rSize == 3) && iSize == 1 && pSize == 0) {
				System.out.println("***************** CRCRI identified *****************");
				System.out.println("Relations : "+relationsList.get(0).get(0).relationFunction+"\t "+relationsList.get(1).get(0).relationFunction);
				for (Concept con1 : concpetsLists.get(0)) {
					for (Concept con2 : concpetsLists.get(1)) {
						for (Entity ents : instancesList.get(0)) {
							String sparqlQ = "";
							sparqlQ = "select distinct ?x where { ?x rdf:type <"+con1.link+">; geo:hasGeometry ?cGeom1. ?cGeom1 geo:asWKT ?cWKT1. ?y rdf:type <"+con2.link+">; geo:hasGeometry ?cGeom2. ?cGeom2 geo:asWKT ?cWKT2. <"+ents.uri+"> geo:hasGeometry ?iGeom. ?iGeom geo:asWKT ?iWKT. ";
							//first relation within
							if (relationsList.get(0).get(0).relationFunction.toLowerCase()
									.contains("within")) {
								System.out.println("getting inside relation within =============");

								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("within")) {
									if((con1.link.contains("wordnet_") && con2.link.contains("wordnet_")) || (con1.link.contains("wordnet_") && con2.link.contains("wikicat")) ||(con1.link.contains("wikicat") && con2.link.contains("wordnet_"))){
										sparqlQ = "select distinct ?x where { ?type rdfs:subClassOf+ <"+con1.link+"> . ?x a ?type . ?y rdf:type <"+con2.link+"> . ?x yago:isLocatedIn+ <"+ents.uri+"> . ?x yago:isLocatedIn+ ?y. } ";
									} else{
										sparqlQ += " FILTER(geof:sfWithin(?cWKT1,?iWKT) && geof:sfWithin(?cWKT1,?cWKT2)) }";
										if (!countFlag && thresholdFlag) {
											sparqlQ = sparqlQ.replace("FILTER(geof:sfWithin(?cWKT1,?iWKT) && geof:sfWithin(?cWKT1,?cWKT2)) }"," FILTER(geof:sfWithin(?cWKT1,?iWKT) && geof:distance(?cWKT1,?cWKT2,uom:metre) <= 1000) }");
											sparqlQ = sparqlQ.replace("1000", thresholdDistance);
										} else if(qauntifierPhrashFlag){
											sparqlQ +=" GROUP BY(?x) ORDER BY DESC(?total) HAVING(?total >= thresholdValue)";
											sparqlQ = sparqlQ.replace("select distinct ?x {","select ?x (count(?y) as ?total) {");
											if(propValue>0){
												if(qpPhrasheValue.contains("at least") || qpPhrasheValue.contains("more than")) {
													sparqlQ = sparqlQ.replace("thresholdValue)", ""+propValue+" )");
												}
												else{
													sparqlQ = sparqlQ.replace(">= thresholdValue)", "< "+propValue+" )");
												}
											}
										}
									}

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("distance")) {
									sparqlQ += " FILTER(geof:sfWithin(?cWKT1,?iWKT) && geof:distance(?cWKT1,?cWKT2,uom:metre) <= 1000) }";
									if (nearestFlag) {
										sparqlQ = sparqlQ.replace(
												"FILTER(geof:sfWithin(?cWKT1,?iWKT) && geof:distance(?cWKT1,?cWKT2,uom:metre) <= 1000) }",
												" FILTER(geof:sfWithin(?cWKT1, ?cWKT2) ) } ORDER BY(geof:distance(?cWKT2,?iWKT,uom:metre)) LIMIT 1");
									} else if (thresholdFlag) {
										sparqlQ = sparqlQ.replace("1000", thresholdDistance);
									}

									else {
										if (con2.link.contains("Restaurant")
												|| con2.link.contains("Park")) {
											sparqlQ = sparqlQ.replace("1000", "500");
										}
										if (con2.link.contains("City")) {
											sparqlQ = sparqlQ.replace("1000", "5000");
										}
									}
									if(qauntifierPhrashFlag){
										sparqlQ +=" GROUP BY(?x) ORDER BY DESC(?total) HAVING(?total >= thresholdValue)";
										sparqlQ = sparqlQ.replace("select distinct ?x {","select ?x (count(?y) as ?total) {");
										if(propValue>0){
											if(qpPhrasheValue.contains("at least") || qpPhrasheValue.contains("more than")) {
												sparqlQ = sparqlQ.replace("thresholdValue)", ""+propValue+" )");
											}
											else{
												sparqlQ = sparqlQ.replace(">= thresholdValue)", "< "+propValue+" )");
											}
										}
									}
								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("boundary")) {
									sparqlQ += " FILTER(geof:sfWithin(?cWKT1,?iWKT) && geof:sfTouches(?cWKT1,?cWKT2)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("cross")) {
									sparqlQ += " FILTER(geof:sfWithin(?cWKT1,?iWKT) && geof:sfCrosses(?cWKT1,?cWKT2)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("intersect")) {
									sparqlQ += " FILTER(geof:sfWithin(?cWKT1,?cWKT2) && geof:sfIntersects(?cWKT2,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("right")) {
									sparqlQ += " FILTER(geof:sfWithin(?cWKT1,?iWKT) && strdf:right(?cWKT1,?cWKT2)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("left")) {
									sparqlQ += " FILTER(geof:sfWithin(?cWKT1,?iWKT) && strdf:left(?cWKT1,?cWKT2)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("above")) {
									sparqlQ += " FILTER(geof:sfWithin(?cWKT1,?iWKT) && strdf:above(?cWKT1,?cWKT2)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("below")) {
									sparqlQ += " FILTER(geof:sfWithin(?cWKT1,?cWKT2) && strdf:below(?cWKT1,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("contain")) {
									if((con1.link.contains("wordnet_") && con2.link.contains("wordnet_")) || (con1.link.contains("wordnet_") && con2.link.contains("wikicat")) ||(con1.link.contains("wikicat") && con2.link.contains("wordnet_"))){
										sparqlQ = "select distinct ?x where { ?type rdfs:subClassOf+ <"+con1.link+"> . ?x a ?type . ?y rdf:type <"+con2.link+"> . ?x yago:isLocatedIn+ <"+ents.uri+"> . ?y yago:isLocatedIn+ ?x. } ";
									}else{
										sparqlQ += " FILTER(geof:sfWithin(?cWKT1,?iWKT) && geof:sfContains(?cWKT1,?cWKT2)) }";
										if (!countFlag && thresholdFlag) {
											sparqlQ = sparqlQ.replace("FILTER(geof:sfWithin(?cWKT1,?iWKT) && geof:sfContains(?cWKT1,?cWKT2)) }"," FILTER(geof:sfWithin(?cWKT1,?iWKT) && geof:distance(?cWKT1,?cWKT2,uom:metre) <= 1000) }");
											sparqlQ = sparqlQ.replace("1000", thresholdDistance);
										}
									}
								}
							}
							//First relation distance/nearby with rest
							if (relationsList.get(0).get(0).relationFunction.toLowerCase()
									.contains("distance")) {
								System.out.println("getting inside relation distance =============");
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("within")) {
									sparqlQ += " FILTER((geof:distance(?cWKT1, ?cWKT2,uom:metre) < 1000)  && geof:sfWithin(?cWKT2, ?iWKT) ) }";
									if (nearestFlag) {
										sparqlQ = sparqlQ.replace(
												"FILTER((geof:distance(?cWKT1, ?cWKT2,uom:metre) < 1000)  && geof:sfWithin(?cWKT2, ?iWKT) ) }",
												" FILTER(geof:sfWithin(?cWKT2, ?iWKT) ) } ORDER BY(geof:distance(?cWKT1,?cWKT2,uom:metre)) LIMIT 1");
									} else if (thresholdFlag) {
										sparqlQ = sparqlQ.replace("1000", thresholdDistance);
									}

									else {
										if (con2.link.contains("Restaurant")
												|| con2.link.contains("Park")) {
											sparqlQ = sparqlQ.replace("1000", "500");
										}
										if (con2.link.contains("City")) {
											sparqlQ = sparqlQ.replace("1000", "5000");
										}
									}
								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("distance")) {
									sparqlQ += " FILTER((geof:distance(?cWKT1, ?cWKT2,uom:metre) < 1000) && geof:distance(?cWKT1,?cWKT2,uom:metre)) }";
								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("boundary")) {
									sparqlQ += " FILTER((geof:distance(?cWKT1, ?cWKT2,uom:metre) < 1000) && geof:sfTouches(?cWKT1,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("cross")) {
									sparqlQ += " FILTER((geof:distance(?cWKT1, ?cWKT2,uom:metre) < 1000) && geof:sfCrosses(?cWKT1,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("intersect")) {
									sparqlQ += " FILTER((geof:distance(?cWKT1, ?cWKT2,uom:metre) < 1000) && geof:sfIntersects(?cWKT1,?iWKT)) }";
									if (nearestFlag) {
										sparqlQ = sparqlQ.replace(
												"FILTER((geof:distance(?cWKT1, ?cWKT2,uom:metre) < 1000) && geof:sfIntersects(?cWKT1,?iWKT)) }",
												" FILTER(geof:sfIntersects(?cWKT1,?iWKT) ) } ORDER BY(geof:distance(?cWKT1,?cWKT2,uom:metre)) LIMIT 1");
									} else if (thresholdFlag) {
										sparqlQ = sparqlQ.replace("1000", thresholdDistance);
									} else {
										if (con2.link.contains("Restaurant")
												|| con2.link.contains("Park")) {
											sparqlQ = sparqlQ.replace("1000", "500");
										}
										if (con2.link.contains("City")) {
											sparqlQ = sparqlQ.replace("1000", "5000");
										}
									}
								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("right")) {
									sparqlQ += " FILTER((geof:distance(?cWKT1, ?cWKT2,uom:metre) < 1000) && strdf:right(?cWKT1,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("left")) {
									sparqlQ += " FILTER((geof:distance(?cWKT1, ?cWKT2,uom:metre) < 1000) && strdf:left(?cWKT1,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("above")) {
									sparqlQ += " FILTER((geof:distance(?cWKT1, ?cWKT2,uom:metre) < 1000) && strdf:above(?cWKT1,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("below")) {
									sparqlQ += " FILTER((geof:distance(?cWKT1, ?cWKT2,uom:metre) < 1000) && strdf:below(?cWKT1,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("contain")) {
									sparqlQ += " FILTER((geof:distance(?cWKT1, ?cWKT2,uom:metre) < 1000) && geof:sfContains(?cWKT1,?iWKT)) }";
								}
							}
							//First relation intersect with rest
							if (relationsList.get(0).get(0).relationFunction.toLowerCase()
									.contains("intersect")) {
								System.out.println("getting inside relation intersect =============");
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("within")) {
									sparqlQ += " FILTER(geof:sfIntersects(?cWKT1,?cWKT2) && geof:sfWithin(?cWKT2,?iWKT)) }";
								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("distance")) {
									sparqlQ += " FILTER(geof:sfIntersects(?cWKT1,?iWKT) && geof:distance(?cWKT1,?cWKT2,uom:metre)) }";
								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("boundary")) {
									sparqlQ += " FILTER(geof:sfIntersects(?cWKT1,?cWKT2) && geof:sfTouches(?cWKT2,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("cross")) {
									sparqlQ += " FILTER(geof:sfIntersects(?cWKT1,?iWKT) && geof:sfCrosses(?cWKT1,?cWKT2)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("intersect")) {
									sparqlQ += " FILTER(geof:sfIntersects(?cWKT1,?iWKT) && geof:sfIntersects(?cWKT1,?cWKT2)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("right")) {
									sparqlQ += " FILTER(geof:sfIntersects(?cWKT1,?iWKT) && strdf:right(?cWKT1,?cWKT2)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("left")) {
									sparqlQ += " FILTER(geof:sfIntersects(?cWKT1,?iWKT) && strdf:left(?cWKT1,?cWKT2)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("above")) {
									sparqlQ += " FILTER(geof:sfIntersects(?cWKT1,?iWKT) && strdf:above(?cWKT1,?cWKT2)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("below")) {
									sparqlQ += " FILTER(geof:sfIntersects(?cWKT1,?iWKT) && strdf:below(?cWKT1,?cWKT2)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("contain")) {
									sparqlQ += " FILTER(geof:sfIntersects(?cWKT1,?iWKT) && geof:sfContains(?cWKT1,?cWKT2)) }";

								}
							}
							//First relation cross with rest
							if (relationsList.get(0).get(0).relationFunction.toLowerCase()
									.contains("cross")) {
								System.out.println("getting inside relation cross =============");
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("within")) {
									sparqlQ += " FILTER(geof:sfCrosses(?cWKT1,?cWKT2) && geof:sfWithin(?cWKT2,?iWKT)) }";
								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("distance")) {
									sparqlQ += " FILTER(geof:sfCrosses(?cWKT1,?cWKT2) && geof:distance(?cWKT2,?iWKT,uom:metre)) }";
								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("boundary")) {
									sparqlQ += " FILTER(geof:sfCrosses(?cWKT1,?cWKT2) && geof:sfTouches(?cWKT2,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("cross")) {
									sparqlQ += " FILTER(geof:sfCrosses(?cWKT1,?cWKT2) && geof:sfCrosses(?cWKT2,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("intersect")) {
									sparqlQ += " FILTER(geof:sfCrosses(?cWKT1,?cWKT2) && geof:sfIntersects(?cWKT2,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("right")) {
									sparqlQ += " FILTER(geof:sfCrosses(?cWKT1,?cWKT2) && strdf:right(?cWKT2,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("left")) {
									sparqlQ += " FILTER(geof:sfCrosses(?cWKT1,?cWKT2) && strdf:left(?cWKT2,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("above")) {
									sparqlQ += " FILTER(geof:sfCrosses(?cWKT1,?cWKT2) && strdf:above(?cWKT2,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("below")) {
									sparqlQ += " FILTER(geof:sfCrosses(?cWKT1,?cWKT2) && strdf:below(?cWKT2,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("contain")) {
									sparqlQ += " FILTER(geof:sfCrosses(?cWKT1,?cWKT2) && geof:sfContains(?cWKT2,?iWKT)) }";

								}
							}

							//first relation contains
							if (relationsList.get(0).get(0).relationFunction.toLowerCase()
									.contains("contain")) {
								System.out.println("getting inside relation contains =============");
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("within")) {
									sparqlQ += " FILTER(geof:sfWithin(?cWKT1,?iWKT) && geof:sfWithin(?cWKT1,?cWKT2)) }";
								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("distance")) {
									sparqlQ += " FILTER(geof:sfContains(?cWKT1,?cWKT2) && geof:distance(?cWKT1,?iWKT,uom:metre) < 1000) }";
									if (nearestFlag) {
										sparqlQ = sparqlQ.replace(
												"FILTER(geof:sfContains(?cWKT1,?cWKT2) && geof:distance(?cWKT1,?iWKT,uom:metre) < 1000) }",
												" FILTER(geof:sfContains(?cWKT1,?cWKT2) ) } ORDER BY(geof:distance(?cWKT1,?iWKT,uom:metre)) LIMIT 1");
									} else if (thresholdFlag) {
										sparqlQ = sparqlQ.replace("1000", thresholdDistance);
									}

									else {
										if (con2.link.contains("Restaurant")
												|| con2.link.contains("Park")) {
											sparqlQ = sparqlQ.replace("1000", "500");
										}
										if (con2.link.contains("City")) {
											sparqlQ = sparqlQ.replace("1000", "5000");
										}
									}
								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("boundary")) {
									sparqlQ += " FILTER(geof:sfContains(?cWKT1,?iWKT) && geof:sfTouches(?cWKT1,?cWKT2)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("cross")) {
									sparqlQ += " FILTER(geof:sfContains(?cWKT1,?iWKT) && geof:sfCrosses(?cWKT1,?cWKT2)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("intersect")) {
									sparqlQ += " FILTER(geof:sfContains(?cWKT1,?iWKT) && geof:sfIntersects(?cWKT1,?cWKT2)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("right")) {
									sparqlQ += " FILTER(geof:sfContains(?cWKT1,?cWKT2) && strdf:right(?cWKT2,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("left")) {
									sparqlQ += " FILTER(geof:sfContains(?cWKT1,?cWKT2) && strdf:left(?cWKT2,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("above")) {
									sparqlQ += " FILTER(geof:sfContains(?cWKT1,?cWKT2) && strdf:above(?cWKT2,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("below")) {
									sparqlQ += " FILTER(geof:sfContains(?cWKT1,?cWKT2) && strdf:below(?cWKT2,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("contain")) {
									sparqlQ += " FILTER(geof:sfContains(?cWKT1,?iWKT) && geof:sfContains(?cWKT1,?cWKT2)) }";

								}
							}

							//first relation boundary
							if (relationsList.get(0).get(0).relationFunction.toLowerCase()
									.contains("boundary")) {
								System.out.println("getting inside relation contains =============");
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("within")) {
									sparqlQ += " FILTER(geof:sfTouches(?cWKT1,?iWKT) && geof:sfWithin(?cWKT1,?cWKT2)) }";
								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("distance")) {
									sparqlQ += " FILTER(geof:sfTouches(?cWKT1,?cWKT2) && geof:distance(?cWKT1,?iWKT,uom:metre) < 1000) }";
									if (nearestFlag) {
										sparqlQ = sparqlQ.replace(
												"FILTER(geof:sfTouches(?cWKT1,?cWKT2) && geof:distance(?cWKT1,?iWKT,uom:metre) < 1000) }",
												" FILTER(geof:sfContains(?cWKT1,?cWKT2) ) } ORDER BY(geof:distance(?cWKT1,?iWKT,uom:metre)) LIMIT 1");
									} else if (thresholdFlag) {
										sparqlQ = sparqlQ.replace("1000", thresholdDistance);
									}

									else {
										if (con2.link.contains("Restaurant")
												|| con2.link.contains("Park")) {
											sparqlQ = sparqlQ.replace("1000", "500");
										}
										if (con2.link.contains("City")) {
											sparqlQ = sparqlQ.replace("1000", "5000");
										}
									}
								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("boundary")) {
									sparqlQ += " FILTER(geof:sfTouches(?cWKT1,?iWKT) && geof:sfTouches(?cWKT1,?cWKT2)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("cross")) {
									sparqlQ += " FILTER(geof:sfTouches(?cWKT1,?iWKT) && geof:sfCrosses(?cWKT1,?cWKT2)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("intersect")) {
									sparqlQ += " FILTER(geof:sfTouches(?cWKT1,?iWKT) && geof:sfIntersects(?cWKT1,?cWKT2)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("right")) {
									sparqlQ += " FILTER(geof:sfTouches(?cWKT1,?iWKT) && strdf:right(?cWKT1,?cWKT2)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("left")) {
									sparqlQ += " FILTER(geof:sfTouches(?cWKT1,?iWKT) && strdf:left(?cWKT1,?cWKT2)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("above")) {
									sparqlQ += " FILTER(geof:sfTouches(?cWKT1,?iWKT) && strdf:above(?cWKT1,?cWKT2)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("below")) {
									sparqlQ += " FILTER(geof:sfTouches(?cWKT1,?iWKT) && strdf:below(?cWKT1,?cWKT2)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("contain")) {
									sparqlQ += " FILTER(geof:sfTouches(?cWKT1,?iWKT) && geof:sfContains(?cWKT1,?cWKT2)) }";

								}
							}

							//first relation right
							if (relationsList.get(0).get(0).relationFunction.toLowerCase()
									.contains("right")) {
								System.out.println("getting inside relation right =============");
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("within")) {
									sparqlQ += " FILTER(strdf:right(?cWKT1,?cWKT2) && geof:sfWithin(?cWKT1,?iWKT)) }";
								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("distance")) {
									sparqlQ += " FILTER(strdf:right(?cWKT1,?cWKT2) && geof:distance(?cWKT1,?iWKT,uom:metre) < 1000) }";
									if (nearestFlag) {
										sparqlQ = sparqlQ.replace(
												"FILTER(strdf:right(?cWKT1,?cWKT2) && geof:distance(?cWKT1,?iWKT,uom:metre) < 1000) }",
												" FILTER(strdf:right(?cWKT1,?cWKT2) ) } ORDER BY(geof:distance(?cWKT1,?iWKT,uom:metre)) LIMIT 1");
									} else if (thresholdFlag) {
										sparqlQ = sparqlQ.replace("1000", thresholdDistance);
									}

									else {
										if (con2.link.contains("Restaurant")
												|| con2.link.contains("Park")) {
											sparqlQ = sparqlQ.replace("1000", "500");
										}
										if (con2.link.contains("City")) {
											sparqlQ = sparqlQ.replace("1000", "5000");
										}
									}

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("boundary")) {
									sparqlQ += " FILTER(strdf:right(?cWKT1,?iWKT) && geof:sfTouches(?cWKT1,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("cross")) {
									sparqlQ += " FILTER(strdf:right(?cWKT1,?iWKT) && geof:sfCrosses(?cWKT1,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("intersect")) {
									sparqlQ += " FILTER(strdf:right(?cWKT1,?iWKT) && geof:sfIntersects(?cWKT1,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("right")) {
									sparqlQ += " FILTER(strdf:right(?cWKT1,?iWKT) && strdf:right(?cWKT1,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("left")) {
									sparqlQ += " FILTER(strdf:right(?cWKT1,?iWKT) && strdf:left(?cWKT1,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("above")) {
									sparqlQ += " FILTER(strdf:right(?cWKT1,?iWKT) && strdf:above(?cWKT1,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("below")) {
									sparqlQ += " FILTER(strdf:right(?cWKT1,?iWKT) && strdf:below(?cWKT1,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("contain")) {
									sparqlQ += " FILTER(strdf:right(?cWKT1,?iWKT) && geof:sfContains(?cWKT1,?iWKT)) }";

								}
							}

							//first relation left
							if (relationsList.get(0).get(0).relationFunction.toLowerCase()
									.contains("left")) {
								System.out.println("getting inside relation left =============");
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("within")) {
									sparqlQ += " FILTER(strdf:left(?cWKT1,?cWKT2) && geof:sfWithin(?cWKT1,?iWKT)) }";
								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("distance")) {
									sparqlQ += " FILTER(strdf:left(?cWKT1,?cWKT2) && geof:distance(?cWKT1,?iWKT,uom:metre) < 1000) }";
									if (nearestFlag) {
										sparqlQ = sparqlQ.replace(
												"FILTER(strdf:left(?cWKT1,?cWKT2) && geof:distance(?cWKT1,?iWKT,uom:metre) < 1000) }",
												" FILTER(strdf:left(?cWKT1,?cWKT2) ) } ORDER BY(geof:distance(?cWKT1,?iWKT,uom:metre)) LIMIT 1");
									} else if (thresholdFlag) {
										sparqlQ = sparqlQ.replace("1000", thresholdDistance);
									}

									else {
										if (con2.link.contains("Restaurant")
												|| con2.link.contains("Park")) {
											sparqlQ = sparqlQ.replace("1000", "500");
										}
										if (con2.link.contains("City")) {
											sparqlQ = sparqlQ.replace("1000", "5000");
										}
									}
								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("boundary")) {
									sparqlQ += " FILTER(strdf:left(?cWKT1,?iWKT) && geof:sfTouches(?cWKT1,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("cross")) {
									sparqlQ += " FILTER(strdf:left(?cWKT1,?iWKT) && geof:sfCrosses(?cWKT1,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("intersect")) {
									sparqlQ += " FILTER(strdf:left(?cWKT1,?iWKT) && geof:sfIntersects(?cWKT1,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("right")) {
									sparqlQ += " FILTER(strdf:left(?cWKT1,?iWKT) && strdf:right(?cWKT1,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("left")) {
									sparqlQ += " FILTER(strdf:left(?cWKT1,?iWKT) && strdf:left(?cWKT1,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("above")) {
									sparqlQ += " FILTER(strdf:left(?cWKT1,?iWKT) && strdf:above(?cWKT1,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("below")) {
									sparqlQ += " FILTER(strdf:left(?cWKT1,?iWKT) && strdf:below(?cWKT1,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("contain")) {
									sparqlQ += " FILTER(strdf:left(?cWKT1,?iWKT) && geof:sfContains(?cWKT1,?iWKT)) }";

								}
							}

							//first relation above
							if (relationsList.get(0).get(0).relationFunction.toLowerCase()
									.contains("above")) {
								System.out.println("getting inside relation above =============");
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("within")) {
									sparqlQ += " FILTER(strdf:above(?cWKT1,?cWKT2) && geof:sfWithin(?cWKT1,?iWKT)) }";
								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("distance")) {
									sparqlQ += " FILTER(strdf:above(?cWKT1,?cWKT2) && geof:distance(?cWKT1,?iWKT,uom:metre) < 1000) }";
									if (nearestFlag) {
										sparqlQ = sparqlQ.replace(
												"FILTER(strdf:above(?cWKT1,?cWKT2) && geof:distance(?cWKT1,?iWKT,uom:metre) < 1000) }",
												" FILTER(strdf:above(?cWKT1,?cWKT2) ) } ORDER BY(geof:distance(?cWKT1,?iWKT,uom:metre)) LIMIT 1");
									} else if (thresholdFlag) {
										sparqlQ = sparqlQ.replace("1000", thresholdDistance);
									}

									else {
										if (con2.link.contains("Restaurant")
												|| con2.link.contains("Park")) {
											sparqlQ = sparqlQ.replace("1000", "500");
										}
										if (con2.link.contains("City")) {
											sparqlQ = sparqlQ.replace("1000", "5000");
										}
									}
								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("boundary")) {
									sparqlQ += " FILTER(strdf:above(?cWKT1,?iWKT) && geof:sfTouches(?cWKT1,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("cross")) {
									sparqlQ += " FILTER(strdf:above(?cWKT1,?iWKT) && geof:sfCrosses(?cWKT1,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("intersect")) {
									sparqlQ += " FILTER(strdf:above(?cWKT1,?iWKT) && geof:sfIntersects(?cWKT1,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("right")) {
									sparqlQ += " FILTER(strdf:above(?cWKT1,?iWKT) && strdf:right(?cWKT1,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("left")) {
									sparqlQ += " FILTER(strdf:above(?cWKT1,?iWKT) && strdf:left(?cWKT1,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("above")) {
									sparqlQ += " FILTER(strdf:above(?cWKT1,?iWKT) && strdf:above(?cWKT1,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("below")) {
									sparqlQ += " FILTER(strdf:above(?cWKT1,?iWKT) && strdf:below(?cWKT1,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("contain")) {
									sparqlQ += " FILTER(strdf:above(?cWKT1,?iWKT) && geof:sfContains(?cWKT1,?iWKT)) }";

								}
							}

							//first relation below
							if (relationsList.get(0).get(0).relationFunction.toLowerCase()
									.contains("below")) {
								System.out.println("getting inside relation below =============");
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("within")) {
									sparqlQ += " FILTER(strdf:below(?cWKT1,?cWKT2) && geof:sfWithin(?cWKT1,?iWKT)) }";
								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("distance")) {
									sparqlQ += " FILTER(strdf:below(?cWKT1,?cWKT2) && geof:distance(?cWKT1,?iWKT,uom:metre) < 1000) }";
									if (nearestFlag) {
										sparqlQ = sparqlQ.replace(
												"FILTER(strdf:below(?cWKT1,?cWKT2) && geof:distance(?cWKT1,?iWKT,uom:metre) < 1000) }",
												" FILTER(strdf:below(?cWKT1,?cWKT2) ) } ORDER BY(geof:distance(?cWKT1,?iWKT,uom:metre)) LIMIT 1");
									} else if (thresholdFlag) {
										sparqlQ = sparqlQ.replace("1000", thresholdDistance);
									}

									else {
										if (con2.link.contains("Restaurant")
												|| con2.link.contains("Park")) {
											sparqlQ = sparqlQ.replace("1000", "500");
										}
										if (con2.link.contains("City")) {
											sparqlQ = sparqlQ.replace("1000", "5000");
										}
									}
								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("boundary")) {
									sparqlQ += " FILTER(strdf:below(?cWKT1,?iWKT) && geof:sfTouches(?cWKT1,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("cross")) {
									sparqlQ += " FILTER(strdf:below(?cWKT1,?iWKT) && geof:sfCrosses(?cWKT1,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("intersect")) {
									sparqlQ += " FILTER(strdf:below(?cWKT1,?iWKT) && geof:sfIntersects(?cWKT1,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("right")) {
									sparqlQ += " FILTER(strdf:below(?cWKT1,?iWKT) && strdf:right(?cWKT1,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("left")) {
									sparqlQ += " FILTER(strdf:below(?cWKT1,?iWKT) && strdf:left(?cWKT1,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("above")) {
									sparqlQ += " FILTER(strdf:below(?cWKT1,?iWKT) && strdf:above(?cWKT1,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("below")) {
									sparqlQ += " FILTER(strdf:below(?cWKT1,?iWKT) && strdf:below(?cWKT1,?iWKT)) }";

								}
								if (relationsList.get(1).get(0).relationFunction.toLowerCase()
										.contains("contain")) {
									sparqlQ += " FILTER(strdf:below(?cWKT1,?iWKT) && geof:sfContains(?cWKT1,?cWKT2)) }";
								}
							}

							if(mostValue.length()>1 && !thresholdFlag && !qauntifierPhrashFlag){
								sparqlQ += " GROUP BY(?x) ORDER BY DESC(count(?y)) LIMIT 1";
								sparqlQ = sparqlQ.replace("select distinct ?x"," select distinct ?x (count(distinct ?y) as ?total) ");
								if(propValue > 0){
									sparqlQ = sparqlQ.replace("LIMIT 1","LIMIT "+propValue);
								}
							}
							if(qauntifierPhrashFlag){
								sparqlQ +=" GROUP BY(?x) HAVING(count(?y) > thresholdValue)";
								sparqlQ = sparqlQ.replace("select distinct ?x"," select distinct ?x (count(distinct ?y) as ?total) ");
								if(propValue>0){
									if(qpPhrasheValue.contains("at least") ) {
										sparqlQ = sparqlQ.replace("> thresholdValue)", ">= "+propValue+" )");
									} else if(qpPhrasheValue.contains("more than") ) {
										sparqlQ = sparqlQ.replace("> thresholdValue)", " "+propValue+" )");
									}
									else if (qpPhrasheValue.contains("less than") ){
										sparqlQ = sparqlQ.replace("> thresholdValue)", "< "+propValue+" )");
									}
								}
							}
							//need to think about the code for below
							/*if(sortingFlag || adjpFlag){
								System.out.println("Inside condition for Sorting ");

								sparqlQ += " ORDER BY DESC(xsd:float(?property)) LIMIT 1 ";
								if(myQuestion.contains("small") || myQuestion.contains("least")){
									sparqlQ = sparqlQ.replace("DESC(","ASC(");
								}
								if(propValue >0 && !qauntifierPhrashFlag){
									sparqlQ = sparqlQ.replace("LIMIT 1","LIMIT "+propValue);
								}
							}*/

							Query q = new Query();
							q.query = sparqlQ;
							q.score = ent.y2glinkCount+ ent.y2Linkcount ;
							allQueriesList.add(q);
						}
					}
				}
			}
			if (cSize == 0 && (rSize == 1 || rSize == 2) && iSize == 2 && pSize == 0) {
				System.out.println("***************** IRI identified *****************");

				String spatialRelation = relationsList.get(0).get(0).relationFunction.toLowerCase();
				String secondSpatialRelation = "";
				System.out.println("First Spatial Relation is : "+spatialRelation);
				if(rSize == 2){
					secondSpatialRelation = relationsList.get(1).get(0).relationFunction.toLowerCase();
					System.out.println("Second Spatial Relation is : "+secondSpatialRelation);
					if(spatialRelation.contains("geof:sfwithin")&&secondSpatialRelation.contains("geof:boundary")){
						spatialRelation = secondSpatialRelation;
					}
					if(spatialRelation.contains("geof:sfwithin")&&(secondSpatialRelation.contains("strdf:above") || secondSpatialRelation.contains("strdf:below") || secondSpatialRelation.contains("strdf:right") || secondSpatialRelation.contains("strdf:left"))){
						spatialRelation = secondSpatialRelation;
					}

				}
				for (Entity ent1 : instancesList.get(0)) {
					if(ent1.y2glinkCount >0){
						for (Entity ent2 : instancesList.get(1)) {
							if(ent2.y2glinkCount >0){
								String sparqlQ = "";
								sparqlQ = "ASK {  <" + ent1.uri + "> geo:hasGeometry ?iGeometry1. ?iGeometry1 geo:asWKT ?iWKT1. <"
										+ ent2.uri + "> geo:hasGeometry ?iGeometry2. ?iGeometry2 geo:asWKT ?iWKT2. ";
								if (spatialRelation.contains("within")) {
									if(thresholdFlag){
										sparqlQ += "FILTER(geof:distance(?iWKT1, ?iWKT2,uom:metre) < 10000) }";
										sparqlQ = sparqlQ.replace("10000",""+thresholdDistance+"");
									} else{
										sparqlQ += " FILTER(geof:sfWithin(?iWKT1, ?iWKT2)) }";
									}
								}
								if (spatialRelation.contains("contains")) {
									sparqlQ += " FILTER(geof:sfContains(?iWKT1, ?iWKT2)) }";
								}
								if (spatialRelation.contains("distance")) {
									if(myQuestionNL.contains("How close") || myQuestionNL.contains("What is") || myQuestionNL.contains("How far")){
										sparqlQ = sparqlQ.replace("ASK ","SELECT (geof:distance(?iWKT1, ?iWKT2, uom:metre) AS ?distance) ");
										sparqlQ += " }";
									} else if(myQuestionNL.toLowerCase(Locale.ROOT).contains("near") || myQuestionNL.toLowerCase(Locale.ROOT).contains(" close ")|| myQuestionNL.toLowerCase(Locale.ROOT).contains(" far ")){
										sparqlQ += "FILTER(geof:distance(?iWKT1, ?iWKT2,uom:metre) < 10000) }";
									}else if(myQuestionNL.toLowerCase(Locale.ROOT).contains("distance") && !thresholdFlag){
										sparqlQ += "FILTER(geof:distance(?iWKT1, ?iWKT2,uom:metre)) }";
									} else if(thresholdFlag){
										sparqlQ += "FILTER(geof:distance(?iWKT1, ?iWKT2,uom:metre) < 10000) }";
										if(myQuestion.contains("more than")){
											sparqlQ = sparqlQ.replace(") <",") >");
										}
										sparqlQ = sparqlQ.replace("10000",""+thresholdDistance+"");
									}
								}
								if (spatialRelation.contains("cross")) {
									sparqlQ += " FILTER(geof:sfCrosses(?iWKT1, ?iWKT2)) }";
								}
								if (spatialRelation.contains("intersect")) {
									sparqlQ += " FILTER(geof:sfIntersects(?iWKT1, ?iWKT2)) }";
								}
								if (spatialRelation.contains("boundary")) {
									sparqlQ += " FILTER(geof:sfTouches(?iWKT1, ?iWKT2)) }";
								}
								if (spatialRelation.contains("right")) {
									sparqlQ += " FILTER(strdf:right(?iWKT1, ?iWKT2)) }";
								}
								if (spatialRelation.contains("left")) {
									sparqlQ += " FILTER(strdf:left(?iWKT1, ?iWKT2)) }";
								}
								if (spatialRelation.contains("above")) {
									sparqlQ += " FILTER(strdf:above(?iWKT1, ?iWKT2)) }";
								}
								if (spatialRelation.contains("below")) {
									sparqlQ += " FILTER(strdf:below(?iWKT1, ?iWKT2)) }";
								}
								Query q = new Query();
								q.query = sparqlQ;
								q.score = ent1.y2glinkCount + ent2.y2glinkCount;
								allQueriesList.add(q);
							}
						}
					}else {
						for (Entity ent2 : instancesList.get(1)) {
							String sparqlQ = "";
							sparqlQ = "ASK {  <" + ent1.uri + "> _relation_ <"+ ent2.uri + "> . ";
							if (spatialRelation.contains("within") || spatialRelation.contains("contains") ||spatialRelation.contains("cross")) {
								sparqlQ = sparqlQ.replace("_relation_","yago:isLoactedIn");
							}
						}
					}

				}
			}

			if (cSize == 1 && rSize == 1 && iSize == 2 && pSize == 0) {
				System.out.println("***************** CIRI identified *****************");

				String spatialRelation = relationsList.get(0).get(0).relationFunction.toLowerCase();
				for (Entity ent1 : instancesList.get(0)) {
					for (Entity ent2 : instancesList.get(1)) {
						if (ent1.uri.contains("dbpedia.org")) {
							if (ent2.uri.contains("dbpedia.org")) {
								if (spatialRelation.contains("within")) {
									String sparqlQ = "ASK { SERVICE <"+yagoEndpoint+"> { <" + ent1.uri
											+ "> ?y <" + ent2.uri + ">. } }";
									Query q = new Query();
									q.query = sparqlQ;
									q.score = ent1.y2glinkCount+ ent1.y2Linkcount + ent2.y2glinkCount+ ent2.y2Linkcount;
									allQueriesList.add(q);
									allSparqlQueries.add(sparqlQ);
								}
								if (spatialRelation.contains("crosses")) {
									String sparqlQ = "ASK { SERVICE <http://dbpedia.org/sparql> { <" + ent1.uri
											+ "> dbo:crosses <" + ent2.uri + ">. } }";
									Query q = new Query();
									q.query = sparqlQ;
									q.score = ent1.y2glinkCount+ ent1.y2Linkcount + ent2.y2glinkCount+ ent2.y2Linkcount;
									allQueriesList.add(q);
									allSparqlQueries.add(sparqlQ);
								}
								if (spatialRelation.contains("east")) {
									String sparqlQ = "ASK { SERVICE <http://dbpedia.org/sparql> { <" + ent1.uri
											+ "> dbp:east <" + ent2.uri + ">. } }";
									Query q = new Query();
									q.query = sparqlQ;
									q.score = ent1.y2glinkCount+ ent1.y2Linkcount + ent2.y2glinkCount+ ent2.y2Linkcount;
									allQueriesList.add(q);
									allSparqlQueries.add(sparqlQ);
								}
								if (spatialRelation.contains("west")) {
									String sparqlQ = "ASK { SERVICE <http://dbpedia.org/sparql> { <" + ent1.uri
											+ "> dbp:west <" + ent2.uri + ">. } }";
									Query q = new Query();
									q.query = sparqlQ;
									q.score = ent1.y2glinkCount+ ent1.y2Linkcount + ent2.y2glinkCount+ ent2.y2Linkcount;
									allQueriesList.add(q);
									allSparqlQueries.add(sparqlQ);
								}
								if (spatialRelation.contains("north")) {
									String sparqlQ = "ASK { SERVICE <http://dbpedia.org/sparql> { <" + ent1.uri
											+ "> dbp:north <" + ent2.uri + ">. } }";
									Query q = new Query();
									q.query = sparqlQ;
									q.score = ent1.y2glinkCount+ ent1.y2Linkcount + ent2.y2glinkCount+ ent2.y2Linkcount;
									allQueriesList.add(q);
									allSparqlQueries.add(sparqlQ);
								}
								if (spatialRelation.contains("south")) {
									String sparqlQ = "ASK { SERVICE <http://dbpedia.org/sparql> { <" + ent1.uri
											+ "> dbp:south <" + ent2.uri + ">. } }";
									Query q = new Query();
									q.query = sparqlQ;
									q.score = ent1.y2glinkCount+ ent1.y2Linkcount + ent2.y2glinkCount+ ent2.y2Linkcount;
									allQueriesList.add(q);
									allSparqlQueries.add(sparqlQ);
								}
							}
						} else if (!ent2.uri.contains("dbpedia.org")) {
							if (spatialRelation.contains("within")) {
								String sparqlQ = "ASK {  <" + ent1.uri
										+ "> geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1. <" + ent2.uri
										+ "> geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER(geof:sfWithin(?iWKT1, ?iWKT2)) }";
								Query q = new Query();
								q.query = sparqlQ;
								q.score = ent1.y2glinkCount+ ent1.y2Linkcount + ent2.y2glinkCount+ ent2.y2Linkcount;
								allQueriesList.add(q);
								allSparqlQueries.add(sparqlQ);
							}
							if (spatialRelation.contains("distance")) {
								String sparqlQ = "ASK {  <" + ent1.uri
										+ "> geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1. <" + ent2.uri
										+ "> geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER(geof:distance(?iWKT1, ?iWKT2) < 10000) }";
								if (nearestFlag) {
									sparqlQ = sparqlQ.replace("FILTER(geof:distance(?iWKT1, ?iWKT2) < 10000) }",
											" } ORDER BY(geof:distance(?iWKT1,?iWKT2,uom:metre)) LIMIT 1");
								}
								Query q = new Query();
								q.query = sparqlQ;
								q.score = ent1.y2glinkCount+ ent1.y2Linkcount + ent2.y2glinkCount+ ent2.y2Linkcount;
								allQueriesList.add(q);
								allSparqlQueries.add(sparqlQ);
							}

							if (spatialRelation.contains("east")) {
								String sparqlQ = "ASK {  <" + ent1.uri
										+ "> geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1. <" + ent2.uri
										+ "> geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER(strdf:right(?iWKT1, ?iWKT2)) }";
								Query q = new Query();
								q.query = sparqlQ;
								q.score = ent1.y2glinkCount+ ent1.y2Linkcount + ent2.y2glinkCount+ ent2.y2Linkcount;
								allQueriesList.add(q);
								allSparqlQueries.add(sparqlQ);
							}
							if (spatialRelation.contains("west")) {
								String sparqlQ = "ASK {  <" + ent1.uri
										+ "> geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1. <" + ent2.uri
										+ "> geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER(strdf:left(?iWKT1, ?iWKT2)) }";
								Query q = new Query();
								q.query = sparqlQ;
								q.score = ent1.y2glinkCount+ ent1.y2Linkcount + ent2.y2glinkCount+ ent2.y2Linkcount;
								allQueriesList.add(q);
								allSparqlQueries.add(sparqlQ);
							}
							if (spatialRelation.contains("north")) {
								String sparqlQ = "ASK {  <" + ent1.uri
										+ "> geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1. <" + ent2.uri
										+ "> geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER(strdf:above(?iWKT1, ?iWKT2)) }";
								Query q = new Query();
								q.query = sparqlQ;
								q.score = ent1.y2glinkCount+ ent1.y2Linkcount + ent2.y2glinkCount+ ent2.y2Linkcount;
								allQueriesList.add(q);
								allSparqlQueries.add(sparqlQ);
							}
							if (spatialRelation.contains("south")) {
								String sparqlQ = "ASK {  <" + ent1.uri
										+ "> geo:hasGeometry ?iGeom1. ?iGeom1 geo:asWKT ?iWKT1. <" + ent2.uri
										+ "> geo:hasGeometry ?iGeom2. ?iGeom2 geo:asWKT ?iWKT2. FILTER(strdf:below(?iWKT1, ?iWKT2)) }";
								Query q = new Query();
								q.query = sparqlQ;
								q.score = ent1.y2glinkCount+ ent1.y2Linkcount + ent2.y2glinkCount+ ent2.y2Linkcount;
								allQueriesList.add(q);
								allSparqlQueries.add(sparqlQ);
							}
						}
					}
				}
			}

//			if (cSize == 2 && rSize == 1 && iSize == 1 && pSize == 0) {
//				System.out.println("***************** CRCI identified *****************");
//				String spatialRelation = relationsList.get(0).get(0).relationFunction.toLowerCase();
//
//				List<Concept> concept1 = concpetsLists.get(0);
//				List<Concept> concept2 = concpetsLists.get(1);
//				List<Concept> finalConcept = concpetsLists.get(0);
//
//				Concept con1 = concpetsLists.get(0).get(0);
//				Concept con2 = concpetsLists.get(1).get(0);
////				Entity ent1 = instancesList.get(0);
//				Boolean flag = false;
//
//				for (Entity ent1 : instancesList.get(0)) {
//					for (int i = 0; i < concept1.size(); i++) {
//						if (checkNeighbours(concept1.get(i), ent1)) {
//							flag = true;
//							finalConcept = concept2;
//						}
//					}
//
//					for (int i = 0; i < concept2.size(); i++) {
//						if (checkNeighbours(concept2.get(i), ent1)) {
//							flag = true;
//							finalConcept = concept1;
//						}
//					}
//
//					if (flag) {
//						for (Concept con : finalConcept) {
//							if (con.link.contains("http://yago-knowledge.org")) {
//								if (ent1.uri.contains("http://yago-knowledge.org")) {
//									// check if the combination of this concept - relation - typeofinstance exist
//									if (answerAvailable(con.link, ent1.uri, spatialRelation)) {
//										if (spatialRelation.contains("within")) { // these code block is to be
//
//											String sparqlQ = "select ?x where { SERVICE <http://pyravlos1.di.uoa.gr:8890/sparql> { ?x rdf:type <"
//													+ con.link
//													+ ">. ?x <http://yago-knowledge.org/resource/isLocatedIn> <"
//													+ ent1.uri + ">.} }";
//											Query q = new Query();
//											q.query = sparqlQ;
//											q.score = ent1.linkCount;
//											allQueriesList.add(q);
//											allSparqlQueries.add(sparqlQ);
//										}
//
//										// We can't answer other relationships if the concept is YAGO class
//
//									}
//
//								}
//							} else {
//
//								if (ent1.uri.contains("http://yago-knowledge.org")) {
//
//									// CONCEPT = OSM, INSTANCE = YAGO
//									boolean yagoEntityThatIsNotInEndpoint = false;
//									String answer = null;
//									// If I is from yago, we first check if we have polygon for yago entity in
//									// pyravlos
//									String Query = "SELECT ?x where { <" + ent1.uri + "> ?p ?x . }";
//									// If at least one result is returned, it means we have the polygon in pyravlos
//									// and we don't need to do anything else
//									answer = runSparqlOnEndpoint(Query, "http://pyravlos1.di.uoa.gr:8080/geoqa/Query");
//									if (answer == null) {
//										yagoEntityThatIsNotInEndpoint = true;
//									}
//
//									String sparqlQ = "select ?x where { ?x rdf:type <" + con.link
//											+ ">; geo:hasGeometry ?cGeom. ?cGeom geo:asWKT ?cWKT. ";
//
//									if (yagoEntityThatIsNotInEndpoint)
//										sparqlQ += "?instance owl:sameAs <" + ent1.uri
//												+ ">; geo:hasGeometry ?geom. ?geom geo:asWKT ?iWKT.";
//									else
//										sparqlQ += "<" + ent1.uri + "> geo:hasGeometry ?geom. ?geom geo:asWKT ?iWKT.";
//
//									if (spatialRelation.contains("within")) {
//										sparqlQ += "FILTER(geof:sfWithin(?cWKT,?iWKT))}";
//
//									}
//									if (spatialRelation.contains("near")) {
//										sparqlQ += "FILTER(geof:distance(?cWKT,?iWKT,uom:metre) <= 1000) }";
//										if (nearestFlag) {
//											sparqlQ = sparqlQ.replace(
//													"FILTER(geof:distance(?cWKT,?iWKT,uom:metre) <= 1000) }",
//													" } ORDER BY(geof:distance(?cWKT,?iWKT,uom:metre)) LIMIT 1");
//										} else if (thresholdFlag) {
//											sparqlQ = sparqlQ.replace("1000", thresholdDistance);
//										}
//
//										else {
//											if (con.link.contains("Restaurant") || con.link.contains("Park")) {
//												sparqlQ = sparqlQ.replace("1000", "500");
//											}
//											if (con.link.contains("City")) {
//												sparqlQ = sparqlQ.replace("1000", "5000");
//											}
//										}
//									}
//									if (spatialRelation.contains("crosses")) {
//										sparqlQ += "FILTER(geof:sfCrosses(?cWKT,?iWKT))}";
//									}
//									if (spatialRelation.contains("boundry")) {
//										sparqlQ += "FILTER(geof:sfTouches(?cWKT,?iWKT))}";
//									}
//									Query q = new Query();
//									q.query = sparqlQ;
//									q.score = ent1.linkCount;
//									allQueriesList.add(q);
//									allSparqlQueries.add(sparqlQ);
//								} else {
//									// CONCEPT = OSM, INSTANCE = OSM
//									String sparqlQ = "select ?x where { ?x rdf:type <" + con.link
//											+ ">; geo:hasGeometry ?cGeom. ?cGeom geo:asWKT ?cWKT. <" + ent1.uri
//											+ "> geo:hasGeometry ?geom. ?geom geo:asWKT ?iWKT. ";
//
//									if (spatialRelation.contains("within")) {
//										sparqlQ += "FILTER(geof:sfWithin(?cWKT,?iWKT))}";
//
//									}
//									if (spatialRelation.contains("near")) {
//										sparqlQ += "FILTER(geof:distance(?cWKT,?iWKT,uom:metre) <= 1000) }";
//										if (nearestFlag) {
//											sparqlQ = sparqlQ.replace(
//													"FILTER(geof:distance(?cWKT,?iWKT,uom:metre) <= 1000) }",
//													" } ORDER BY(geof:distance(?cWKT,?iWKT,uom:metre)) LIMIT 1");
//										} else if (thresholdFlag) {
//											sparqlQ = sparqlQ.replace("1000", thresholdDistance);
//										}
//
//										else {
//											if (con.link.contains("Restaurant") || con.link.contains("Park")) {
//												sparqlQ = sparqlQ.replace("1000", "500");
//											}
//											if (con.link.contains("City")) {
//												sparqlQ = sparqlQ.replace("1000", "5000");
//											}
//										}
//									}
//									if (spatialRelation.contains("crosses")) {
//										sparqlQ += "FILTER(geof:sfCrosses(?cWKT,?iWKT))}";
//									}
//									if (spatialRelation.contains("boundry")) {
//										sparqlQ += "FILTER(geof:sfTouches(?cWKT,?iWKT))}";
//									}
//
//									Query q = new Query();
//									q.query = sparqlQ;
//									q.score = ent1.linkCount;
//									allQueriesList.add(q);
//									allSparqlQueries.add(sparqlQ);
//								}
//							}
//						}
//					}
//				}
//			}

			concpetsLists.clear();
			relationsList.clear();
			instancesList.clear();
			postagListsInorderTree.clear();
			geoSPATIALRelations.clear();

			// -----------------------------------------------------------------------------------------------------------------------------------------------

			ArrayList<Integer> rankScoreofQuery = new ArrayList<Integer>();

			for (String qry : allSparqlQueries) {
				rankScoreofQuery.add(0);
			}

			if (allSparqlQueries.isEmpty()) {
				System.out.println("Can not Answer ");
			}
			String finalQuery = "";

			// add link count for ranking

			String countQuery1 = "SELECT (count(?p) as ?nOfLinks where { ";
			String countQuery2 = " ?p ?o. }";

			if (allSparqlQueries.size() == 1) {
				finalQuery = allSparqlQueries.get(0);
			} else {
//				int index = -1;
//				for (int i = 0; i < allSparqlQueries.size(); i++) {
//					if (identifiedPattern.equals("IRI")) {
//						// System.out.println("getting Inside:===========");
//						if (allSparqlQueries.get(i).contains("http://yago-knowledge.org/resource")) {
//							// System.out.println("Selecting query====");
//							index = i;
////							break;
//						}
//					} else if (allSparqlQueries.get(i).contains("http://yago-knowledge.org/resource")) {
//						index = i;
////						break;
//					}
//				}
//				if (index != -1) {
//					finalQuery = allSparqlQueries.get(index);
//					// System.out.println("Selected Index: " + index);
//				} else {
//					if (finalQuery.equals("")) {
//						for (int i = 0; i < allSparqlQueries.size(); i++) {
//							if (allSparqlQueries.get(i).contains("http://www.app-lab.eu/gadm/AdministrativeUnit"))
//								index = i;
//						}
//
//					}
//					if (index != -1) {
//						finalQuery = allSparqlQueries.get(index);
//					} else {
//						if (finalQuery.equals("")) {
//							for (int i = 0; i < allSparqlQueries.size(); i++) {
//								if (allSparqlQueries.get(i).contains("http://www.app-lab.eu/osm/england")
//										|| allSparqlQueries.get(i).contains("http://www.app-lab.eu/osm/scotland")
//										|| allSparqlQueries.get(i).contains("http://www.app-lab.eu/osm/wales")
//										|| allSparqlQueries.get(i)
//												.contains("http://www.app-lab.eu/osm/irelandandnorthernireland"))
//									index = i;
//							}
//
//						}
//						if (index != -1) {
//							finalQuery = allSparqlQueries.get(index);
//						}
//					}
//				}
			}

			Collections.sort(allQueriesList, new Comparator<Query>() {

				public int compare(Query o1, Query o2) {
					// TODO Auto-generated method stub
					int com = 0;
					if (o1.score > o2.score)
						com = -1;
					else if (o1.score < o2.score)
						com = 1;
					return com;
				}
			});

			myTreeNodes1.clear();
			System.out.println("myquestionNl: "+myQuestionNL);
			if (myQuestionNL.toLowerCase(Locale.ROOT).startsWith("is there") || myQuestionNL.toLowerCase(Locale.ROOT).startsWith("does") || myQuestionNL.startsWith("do")
					|| myQuestionNL.toLowerCase(Locale.ROOT).startsWith("are there") || myQuestionNL.toLowerCase(Locale.ROOT).startsWith("is")) {
				for (Query queries : allQueriesList) {
//					System.out.println("Generated Query: " + queries.query + "\nScore: " + queries.score);
					queries.query = queries.query.replace("select distinct ?x where", "ASK ");
				}
			}

			if (allQueriesList.size() > 0) {
				finalQuery = allQueriesList.get(0).query;
			}
			for (Query queries : allQueriesList) {
				System.out.println("Generated Query: " + queries.query + "\nScore: " + queries.score);
			}
			System.out.println("Total number of Generated queries: " + allQueriesList.size());
			if (!finalQuery.equals("")) {
				if (countFlag || myQuestionNL.contains(" number of ")) {
					if (finalQuery.contains("select distinct ?x")) {
						finalQuery = finalQuery.replace("select distinct ?x", "select (count(distinct ?x) as ?total) ");
					}
					countFlag = false;
				}
				System.out.println("Selected query : " + finalQuery);
			}

			/*BufferedWriter bw = new BufferedWriter(new FileWriter("/home/dharmenp/generatedQueries2021julyYago2geo_b_type.txt",true));
			bw.newLine();
			bw.write(myQuestionNL+" :: "+finalQuery);
			bw.close();*/

			String filename= "results.txt";
			FileWriter fw = new FileWriter(filename,true); //the true will append the new data
			fw.write(myQanaryQuestion.getTextualRepresentation() + "\n" + finalQuery + "\n"); //appends the string to the file
			fw.close();

			String filename2= "query.txt";
			FileWriter fw2 = new FileWriter(filename2);
			fw2.write(finalQuery + "\n");
			fw2.close();

			logger.debug("store the generated GeoSPARQL query in triplestore: {}", finalQuery);
			// STEP 3: Push the GeoSPARQL query to the triplestore
//			for (Query generatedQuery : allQueriesList) {
//				sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
//						+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
//						+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
//						+ "INSERT { " //
//						+ "GRAPH <" + myQanaryUtils.getInGraph() + "> { " //
//						+ " ?a a qa:AnnotationOfAnswerSPARQL . " //
//						+ " ?a oa:hasTarget <URIAnswer> . " //
//						+ " ?a oa:hasBody \"" + generatedQuery.query.replaceAll("\n", " ") + "\" ;" //
//						+ " oa:score \"" + generatedQuery.score + "\"^^xsd:nonNegativeInteger ;"
//						+ " oa:annotatedBy <urn:qanary:geosparqlgenerator> ; " //
//						+ " oa:AnnotatedAt ?time . " //
//						+ "}} " //
//						+ "WHERE { " //
//						+ " BIND (IRI(str(RAND())) AS ?a) ." //
//						+ " BIND (now() as ?time) " //
//						+ "}";
//				myQanaryUtils.updateTripleStore(sparql, myQanaryMessage.getEndpoint().toString());
//			}

		} catch (MalformedURLException e) {
			e.printStackTrace();
		}

		return myQanaryMessage;
	}

	/**
	 *
	 *
	 * @param sparqlQuery
	 * @return
	 */
	public static String bindVariablesInSparqlQuery(String sparqlQuery, Map<String, String> variablesToBind) {

		String variableBindings = "";
		String uri;
		String name;

		// for each used variable create a bind statement
		for (Map.Entry<String, String> variable : variablesToBind.entrySet()) {
			name = variable.getKey();
			uri = variable.getValue();
			variableBindings += "\tBIND(<" + uri + "> AS ?" + name + ").\n";
		}

		// insert new bindings block at the end of the SPARQL query
		StringBuffer concreteSparqlQueryWithBindings = new StringBuffer(sparqlQuery);
		int position = concreteSparqlQueryWithBindings.lastIndexOf("}");
		concreteSparqlQueryWithBindings.insert(position, variableBindings);

		return concreteSparqlQueryWithBindings.toString();

	}

	public class Concept {
		public int begin;
		public int end;
		public String link;
		public String label;
		public Property property;
	}

	public class SpatialRelation {
		public int index;
		public String relation;
		public String relationFunction;
	}

	public class Query {
		public int score = 0;
		public String query = "";
		public String queryM = "";
	}

	public class Entity {

		public int begin;
		public int end;
		public String namedEntity;
		public String uri;
		public int y2glinkCount;
		public int y2Linkcount;
		public Property property;

		public void print() {
			System.out.println("Start: " + begin + "\t End: " + end + "\t Entity: " + namedEntity);
		}
	}

	public class TimeAnnotation {

		public String type = ""; // TIMEX type
		public String value = ""; // TIMEX value
		public String text = ""; // Annotated Text
		public int startIndex = -1;
		public int endIndex = -1;
	}

	public class Property {
		public int begin;
		public int end;
		public String label;
		public String uri;
		public String conceptUri;
	}
}
