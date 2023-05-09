package eu.wdaqua.qanary.propertyidentifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.robrua.nlp.bert.Bert;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.trees.Constituent;
import edu.stanford.nlp.trees.LabeledScoredConstituentFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetFactory;
import org.checkerframework.checker.units.qual.C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;
//import net.ricecode.similarity.JaroWinklerStrategy;
//import net.ricecode.similarity.SimilarityStrategy;
//import net.ricecode.similarity.StringSimilarityService;
//import net.ricecode.similarity.StringSimilarityServiceImpl;

@Component
/**
 * This component connected automatically to the Qanary pipeline. The Qanary
 * pipeline endpoint defined in application.properties (spring.boot.admin.url)
 * 
 * @see <a href=
 *      "https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F"
 *      target="_top">Github wiki howto</a>
 */
public class PropertyIdentifier extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(PropertyIdentifier.class);

	public static List<String> getVerbsNouns(String documentText) {
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos,lemma");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		List<String> postags = new ArrayList<>();
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
				String pos = token.get(PartOfSpeechAnnotation.class);
				if (pos.contains("VB") || pos.contains("IN") || pos.contains("NN") || pos.contains("JJ")) {
					postags.add(token.get(LemmaAnnotation.class));
				}
			}
		}
		return postags;
	}

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
		List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
		for (CoreMap sentence : sentences) {
			// Iterate over all tokens in a sentence
			for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
				// Retrieve and add the lemma for each word into the
				// list of lemmas
				lemmas.add(token.get(CoreAnnotations.LemmaAnnotation.class));
				lemmetizedQuestion += token.get(CoreAnnotations.LemmaAnnotation.class) + " ";
			}
		}
		return lemmetizedQuestion;
	}

	public static List<String> getNouns(String documentText) {
		Properties props = new Properties();
		props.put("annotators", "tokenize, ssplit, pos,lemma");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		List<String> postags = new ArrayList<>();
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
				String pos = token.get(PartOfSpeechAnnotation.class);
				if (pos.contains("NN") || pos.contains("JJ") || pos.contains("NP") || pos.contains("NNP")) {
					postags.add(token.get(LemmaAnnotation.class));
				}
			}
		}
		return postags;
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
			// Iterate over all tokens in a sentence
			SemanticGraph dependencies = sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
//			dependencies.prettyPrint();
			List<SemanticGraphEdge> edges = dependencies.edgeListSorted();

			for (SemanticGraphEdge edge : edges) {

				if (edge.getSource().toString().contains("JJS") && edge.getDependent().toString().contains("NN")) {
					retVal = true;
//					System.out.println(" Source ================================================= Dest ");
//					System.out.println("edge : "+edge.toString());
//					System.out.println("source: "+edge.getSource());
//					System.out.println("relation: "+edge.getRelation());
//					System.out.println("dependent :"+edge.getDependent());
				} else if (edge.getSource().toString().contains("NN")
						&& edge.getDependent().toString().contains("JJS")) {
					retVal = true;
//					System.out.println("Dest ================================================= Source");
//					System.out.println("edge : "+edge.toString());
//					System.out.println("source: "+edge.getSource());
//					System.out.println("relation: "+edge.getRelation());
//					System.out.println("dependent :"+edge.getDependent());
				} else if (edge.getSource().toString().contains("NNS")
						&& edge.getDependent().toString().contains("JJR")) {
					retVal = true;
				}
			}
		}
		return retVal;
	}

	static float computeCosSimilarity(float[] a, float[] b) {
		//todo: you might want to check they are the same size before proceeding

		float dotProduct = 0;
		float normASum = 0;
		float normBSum = 0;

		for(int i = 0; i < a.length; i ++) {
			dotProduct += a[i] * b[i];
			normASum += a[i] * a[i];
			normBSum += b[i] * b[i];
		}

		float eucledianDist = (float) (Math.sqrt(normASum) * Math.sqrt(normBSum));
		return dotProduct / eucledianDist;
	}

	public static boolean isRBSMost(String documentText) {
		boolean retVal = false;
		List<String> strRetVal = new ArrayList<String>();
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
//					strRetVal.add("true");
					String val = edge.getSource().toString();
//					strRetVal.add("");
					retVal = true;
				} else if (edge.getSource().toString().contains("NN")
						&& edge.getDependent().toString().contains("JJS")) {
					retVal = true;
//					strRetVal.add("true");
//					strRetVal.add("");
				}
			}
		}
		return retVal;
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
//		System.out.println(tree);
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

	public static String getJJS(String documentText) {
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
			// Iterate over all tokens in a sentence
			SemanticGraph dependencies = sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
//			dependencies.prettyPrint();
			List<SemanticGraphEdge> edges = dependencies.edgeListSorted();

			for (SemanticGraphEdge edge : edges) {
				if (edge.getSource().toString().contains("JJS") && edge.getDependent().toString().contains("NN")) {
					retVal = edge.getSource().toString();
					retVal = retVal.substring(0, retVal.indexOf('/') - 1);
				} else if (edge.getSource().toString().contains("NN")
						&& edge.getDependent().toString().contains("JJS")) {
					retVal = edge.getDependent().toString();
					retVal = retVal.substring(0, retVal.indexOf('/') );
				}
			}
		}
		System.out.println("ret value : "+retVal);
		return retVal;
	}

	public static boolean isAlphaNumeric(String s) {
		return s != null && s.matches("^[a-zA-Z0-9]*$");
	}

	/**
	 * implement this method encapsulating the functionality of your Qanary
	 * component
	 * @throws Exception
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		logger.info("process: {}", myQanaryMessage);
		// TODO: implement processing of question

		String yago2geoOnlyEndpoint = "http://pyravlos1.di.uoa.gr:9999/Strabon/Query";
		String yago2endpoint = "http://pyravlos1.di.uoa.gr:9997/Strabon/Query";
		QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
		QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);
		String myQuestion = myQanaryQuestion.getTextualRepresentation();
		String lemQuestion = lemmatize(myQuestion);
		WordNetAnalyzer wordNet = new WordNetAnalyzer("qanary_component-PropertyIdentifierYago/src/main/resources/WordNet-3.0/dict");
		logger.info("Question: {}", myQuestion);
		// TODO: implement processing of question

		List<String> allVerbs = getVerbsNouns(myQuestion);
		List<String> relationList = new ArrayList<String>();
		List<Property> properties = new ArrayList<Property>();
		List<Property> instProperties = new ArrayList<Property>();
		List<String> instanceProperties = new ArrayList<String>();
		List<String> valuePropertyList = new ArrayList<String>();
		List<String> allClassesList = new ArrayList<String>();
		List<String> dbpediaClassList = new ArrayList<String>();
		boolean valueFlag = false;
		Set<String> coonceptsUri = new HashSet<String>();
		ResultSet r;
		Set<Entity> entities = new HashSet<Entity>();
		Set<String> entitiesUri = new HashSet<String>();
		HashMap<String,String> mapProperty = new HashMap<String,String>();
		Set<Concept> concepts = new HashSet<Concept>();

//		mapProperty.put("mountain","http://dbpedia.org/ontology/elevation");
//		mapProperty.put("river","http://dbpedia.org/property/length");
//		mapProperty.put("stadium","http://dbpedia.org/property/capacity");
//		mapProperty.put("city","http://dbpedia.org/ontology/populationTotal");
//		mapProperty.put("lake","http://dbpedia.org/ontology/areaTotal");
//		mapProperty.put("bridge","http://dbpedia.org/ontology/length");
//		mapProperty.put("mountain","http://dbpedia.org/ontology/elevation");
//		mapProperty.put("mountain","http://dbpedia.org/ontology/elevation");

		String sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> "
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

			// geoSparqlQuery += "" + conceptTemplate.replace("poiURI",
			// conceptTemp.link).replaceAll("poi",
			// "poi" + conceptTemp.begin);
			// newGeoSparqlQuery += "" + conceptTemplate.replace("poiURI",
			// conceptTemp.link).replaceAll("poi",
			// "poi" + conceptTemp.begin);
			//if (conceptTemp.link.contains("yago-knowledge.org")) {
				concepts.add(conceptTemp);
				coonceptsUri.add(conceptTemp.link);
				dbpediaClassList.add(conceptTemp.link);
//				logger.info("Concept start {}, end {} concept {} link {}", conceptTemp.begin, conceptTemp.end,
//						myQuestion.substring(conceptTemp.begin, conceptTemp.end), conceptTemp.link);
			//}
			allClassesList.add(conceptTemp.link);
		}

		for (int i = 0; i < allVerbs.size(); i++) {
			for (String classlUri : allClassesList) {
				System.out.println("Class URI : "+classlUri +"    allverbs :"+allVerbs.get(i));
				if (classlUri.toLowerCase().contains(allVerbs.get(i).toLowerCase())) {
					allVerbs.remove(i);
					i--;
					break;
				}
			}
		}

		sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> " + "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
				+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "//
				+ "SELECT ?start ?end ?y2glcount ?y2lcount ?uri " + "FROM <" + myQanaryQuestion.getInGraph() + "> " //
				+ "WHERE { " //
				+ "    ?a a qa:AnnotationOfInstance . " + "?a oa:hasTarget [ "
				+ "		     a               oa:SpecificResource; " //
				+ "		     oa:hasSource    ?q; " //
				+ "	         oa:hasSelector  [ " //
				+ "			         a        oa:TextPositionSelector ; " //
				+ "			         oa:start ?start ; " //
				+ "			         oa:end   ?end ;" //
				+ "			         oa:y2glinkcount   ?y2glcount; "
				+ "                  oa:y2linkcount ?y2lcount"
				+ "		     ] " //
				+ "    ] . " //
				+ " ?a oa:hasBody ?uri ; " + "    oa:annotatedBy ?annotator " //
				+ "} " + "ORDER BY ?start ";

		r = myQanaryUtils.selectFromTripleStore(sparql, myQanaryQuestion.getEndpoint().toString());

		while (r.hasNext()) {
			QuerySolution s = r.next();

			Entity entityTemp = new Entity();
			entityTemp.begin = s.getLiteral("start").getInt();

			entityTemp.end = s.getLiteral("end").getInt();

			entityTemp.uri = s.getResource("uri").getURI();

			System.out.println("uri: " + entityTemp.uri + "\t start: " + entityTemp.begin + "\tend: " + entityTemp.end);

			entityTemp.namedEntity = myQuestion.substring(entityTemp.begin, entityTemp.end);

			entityTemp.y2glinkCount = s.getLiteral("y2glcount").getInt();
			entityTemp.y2LinkCount = s.getLiteral("y2lcount").getInt();
			entities.add(entityTemp);
			entitiesUri.add(entityTemp.uri);
		}

		System.out.println("all verbs: " + allVerbs);
		for(Concept concept:concepts){
			String classLabel = concept.link.substring(concept.link.lastIndexOf("/")+1);
			System.out.println("classLabel : " + classLabel);
			File file = new File("qanary_component-PropertyIdentifierYago/src/main/resources/yago2geoproperties/" + classLabel + ".txt");
			//File file1 = new File("D:\\GeoQA code\\intelij\\GeoQAUpdated2021\\qanary_component-PropertyIdentifierYago\\src\\main\\resources\\yagoproperties\\" + classLabel + "_yago_label_of_properties.txt");
			Map<String, String> valuePropertyD = new HashMap<String, String>();
			Map<String, String> labelPropertyD = new HashMap<String, String>();

			System.out.println("File name : "+ file.getAbsolutePath());
			if (file.exists()) {
				valuePropertyD.clear();
				labelPropertyD.clear();
				System.out.println("opening file : " + file.getName());
				BufferedReader br = new BufferedReader(new FileReader(file));

				String line = "";
				while ((line = br.readLine()) != null) {
					String splitedLine[] = line.split(",");
					if (splitedLine.length > 1) {
						labelPropertyD.put(splitedLine[0], splitedLine[1]);
					}
				}
				br.close();
				/*if(file1.exists()) {
					BufferedReader br1 = new BufferedReader(new FileReader(file1));

					line = "";
					while ((line = br1.readLine()) != null) {
						String splitedLine[] = line.split(",");
						if (splitedLine.length > 1) {
							valuePropertyD.put(splitedLine[1], splitedLine[0]);
						}
					}
					br1.close();
				}*/

//				System.out.println("size is : " + valuePropertyD.size());

				System.out.println("size is : " + labelPropertyD.size());

				if (labelPropertyD.size() > 0) {
					double score = 0.0;
					System.out.println("Inside label property:");
//					SimilarityStrategy strategy = new JaroWinklerStrategy();
//
//					StringSimilarityService service = new StringSimilarityServiceImpl(strategy);
//					BufferedWriter bw1 = new BufferedWriter(
//							new FileWriter("/home/dharmen/justtestproperties.txt", true));
					for (Entry<String, String> entry : labelPropertyD.entrySet()) {
						System.out.println("111111111111");
						for (String verb : allVerbs) {
							System.out.println("222222222");
							if(verb.equalsIgnoreCase("of")||verb.equalsIgnoreCase("in")||verb.equalsIgnoreCase("be"))//||verb.equalsIgnoreCase("name")
								continue;
							ArrayList<String> wordNetSynonyms = wordNet.getSynonyms(verb);
							for (String synonym : wordNetSynonyms) {
//								System.out.println("Verb : "+verb+" :::: "+"Synonym : "+synonym);
								Pattern p = Pattern.compile("\\b" + synonym  + "\\b", Pattern.CASE_INSENSITIVE);
//							if (verb.contains("height") && entry.getKey().contains("height")) {
//								bw1.write(entry.getKey());
//								bw1.newLine();
//							}
//						System.out.print("::Keyword: "+verb+"=================================="+entry.getKey()+"::");
								System.out.println("HEEEEEEEREEEEEEEEEE" + entry.getValue());
								Matcher m = p.matcher(entry.getValue());
//							if (!concept.contains(verb)) {
//								System.out.println("before matching if condition =============== "+ verb);
								if (m.find() && !entry.getValue().equalsIgnoreCase("crosses") && !entry.getKey().contains("Wikipage")
										&& !entry.getKey().equalsIgnoreCase("runs")
										&& !entry.getKey().equalsIgnoreCase("south")
										&& !entry.getKey().equalsIgnoreCase("number") && !synonym.equalsIgnoreCase("km") && !synonym.equalsIgnoreCase("of")
										&& !synonym.equalsIgnoreCase("number") && !synonym.equalsIgnoreCase("range")
										&& !synonym.equalsIgnoreCase("be") && !synonym.equalsIgnoreCase("in")
										&& entry.getKey().length() > 2) {
									valueFlag = true;
									Property property = new Property();
									if (relationList.size() == 0 || !relationList.contains(entry.getKey())) {
										relationList.add(entry.getKey());
										valuePropertyList.add(entry.getKey());
										property.begin = lemQuestion.toLowerCase().indexOf(synonym);
										property.end = property.begin + synonym.length();
										property.label = entry.getValue();
										property.uri = entry.getKey();
                                        property.concept = concept;
										/*property.concept.link = concept.link;
										property.concept.begin = concept.begin;
										property.concept.end = concept.end;*/
										if(classLabel.equals("Mountain")  ){
											if(property.uri.equals("http://yago-knowledge.org/resource/infobox/en/highest") || property.uri.equals("http://dbpedia.org/property/height"))
												property.uri = "http://yago-knowledge.org/resource/infobox/en/elevationm";
										}
										if(!entry.getKey().contains("geosparql#sfWithin")&&!entry.getKey().contains("geosparql#sfTouches") && !entry.getKey().contains("geosparql#sfIntersects")){
											properties.add(property);
											System.out.println("For class : " + classLabel + "   Found Value: " + entry.getKey()
													+ " :" + entry.getValue() + " : Begin : " + property.begin + "  : end "
													+ property.end);
										}
									}
									System.out.println("For class : " + classLabel + "   Found Value: " + entry.getKey()
											+ " :" + entry.getValue());
								}
//								Bert bert = Bert.load("bert-cased-L-12-H-768-A-12");
//								float[] emb1 = bert.embedSequence(entry.getValue());
//								float[] emb2 = bert.embedSequence(synonym);
//								if ((computeCosSimilarity(emb1,emb2)>0.95) && !entry.getValue().equalsIgnoreCase("crosses") && !entry.getKey().contains("Wikipage")
//										&& !entry.getKey().equalsIgnoreCase("runs")
//										&& !entry.getKey().equalsIgnoreCase("south")
//										&& !entry.getKey().equalsIgnoreCase("number") && !synonym.equalsIgnoreCase("km") && !synonym.equalsIgnoreCase("of")
//										&& !synonym.equalsIgnoreCase("number") && !synonym.equalsIgnoreCase("range")
//										&& !synonym.equalsIgnoreCase("be") && !synonym.equalsIgnoreCase("in")
//										&& entry.getKey().length() > 2) {
//									valueFlag = true;
//									Property property = new Property();
//									if (relationList.size() == 0 || !relationList.contains(entry.getKey())) {
//										relationList.add(entry.getKey());
//										valuePropertyList.add(entry.getKey());
//										property.begin = lemQuestion.toLowerCase().indexOf(synonym);
//										property.end = property.begin + synonym.length();
//										property.label = entry.getValue();
//										property.uri = entry.getKey();
//										property.concept = concept;
//										/*property.concept.link = concept.link;
//										property.concept.begin = concept.begin;
//										property.concept.end = concept.end;*/
//										if(classLabel.equals("Mountain")  ){
//											if(property.uri.equals("http://yago-knowledge.org/resource/infobox/en/highest") || property.uri.equals("http://dbpedia.org/property/height"))
//												property.uri = "http://yago-knowledge.org/resource/infobox/en/elevationm";
//										}
//										if(!entry.getKey().contains("geosparql#sfWithin")&&!entry.getKey().contains("geosparql#sfTouches") && !entry.getKey().contains("geosparql#sfIntersects")){
//											properties.add(property);
//											System.out.println("For class : " + classLabel + "   Found Value: " + entry.getKey()
//													+ " :" + entry.getValue() + " : Begin : " + property.begin + "  : end "
//													+ property.end);
//										}
//									}
//									System.out.println("For class : " + classLabel + "   Found Value: " + entry.getKey()
//											+ " :" + entry.getValue());
//								}
							}
						}
					}
				}
			}
			valuePropertyD.clear();
			labelPropertyD.clear();
		}
		/*for (String concept : coonceptsUri) {

			String classLabel = concept.substring(concept.lastIndexOf("/") + 1);
			System.out.println("classLabel : " + classLabel);
			File file = new File("D:\\GeoQA code\\intelij\\GeoQAUpdated2021\\qanary_component-PropertyIdentifierYago\\src\\main\\resources\\yago2geoproperties\\" + classLabel + ".txt");
			//File file1 = new File("D:\\GeoQA code\\intelij\\GeoQAUpdated2021\\qanary_component-PropertyIdentifierYago\\src\\main\\resources\\yagoproperties\\" + classLabel + "_yago_label_of_properties.txt");
			Map<String, String> valuePropertyD = new HashMap<String, String>();
			Map<String, String> labelPropertyD = new HashMap<String, String>();

			System.out.println("File name : "+ file.getAbsolutePath());
			if (file.exists()) {
				valuePropertyD.clear();
				labelPropertyD.clear();
				System.out.println("opening file : " + file.getName());
				BufferedReader br = new BufferedReader(new FileReader(file));

				String line = "";
				while ((line = br.readLine()) != null) {
					String splitedLine[] = line.split(",");
					if (splitedLine.length > 1) {
						labelPropertyD.put(splitedLine[0], splitedLine[1]);
					}
				}
				br.close();
				*//*if(file1.exists()) {
					BufferedReader br1 = new BufferedReader(new FileReader(file1));

					line = "";
					while ((line = br1.readLine()) != null) {
						String splitedLine[] = line.split(",");
						if (splitedLine.length > 1) {
							valuePropertyD.put(splitedLine[1], splitedLine[0]);
						}
					}
					br1.close();
				}*//*

//				System.out.println("size is : " + valuePropertyD.size());

				System.out.println("size is : " + labelPropertyD.size());

				if (labelPropertyD.size() > 0) {
					double score = 0.0;
					System.out.println("Inside label property:");
//					SimilarityStrategy strategy = new JaroWinklerStrategy();
//
//					StringSimilarityService service = new StringSimilarityServiceImpl(strategy);
//					BufferedWriter bw1 = new BufferedWriter(
//							new FileWriter("/home/dharmen/justtestproperties.txt", true));
					for (Entry<String, String> entry : labelPropertyD.entrySet()) {
						for (String verb : allVerbs) {
							if(verb.equalsIgnoreCase("of")||verb.equalsIgnoreCase("in")||verb.equalsIgnoreCase("be"))//||verb.equalsIgnoreCase("name")
								continue;
							ArrayList<String> wordNetSynonyms = wordNet.getSynonyms(verb);
							for (String synonym : wordNetSynonyms) {
								System.out.println("Verb : "+verb+" :::: "+"Synonym : "+synonym);
								Pattern p = Pattern.compile("\\b" + synonym  + "\\b", Pattern.CASE_INSENSITIVE);
//							if (verb.contains("height") && entry.getKey().contains("height")) {
//								bw1.write(entry.getKey());
//								bw1.newLine();
//							}
//						System.out.print("::Keyword: "+verb+"=================================="+entry.getKey()+"::");
								Matcher m = p.matcher(entry.getValue());
//							if (!concept.contains(verb)) {
//								System.out.println("before matching if condition =============== "+ verb);
								if (m.find() && !entry.getValue().equalsIgnoreCase("crosses") && !entry.getKey().contains("Wikipage")
										&& !entry.getKey().equalsIgnoreCase("runs")
										&& !entry.getKey().equalsIgnoreCase("south")
										&& !entry.getKey().equalsIgnoreCase("number") && !synonym.equalsIgnoreCase("km") && !synonym.equalsIgnoreCase("of")
										&& !synonym.equalsIgnoreCase("number") && !synonym.equalsIgnoreCase("range")
										&& !synonym.equalsIgnoreCase("be") && !synonym.equalsIgnoreCase("in")
										&& entry.getKey().length() > 2) {
									valueFlag = true;
									Property property = new Property();
									if (relationList.size() == 0 || !relationList.contains(entry.getKey())) {
										relationList.add(entry.getKey());
										valuePropertyList.add(entry.getKey());
										property.begin = lemQuestion.toLowerCase().indexOf(synonym);
										property.end = property.begin + synonym.length();
										property.label = entry.getValue();
										property.uri = entry.getKey();
										if(classLabel.equals("Mountain")  ){
											if(property.uri.equals("http://yago-knowledge.org/resource/infobox/en/highest") || property.uri.equals("http://dbpedia.org/property/height"))
												property.uri = "http://yago-knowledge.org/resource/infobox/en/elevationm";
										}
										if(!entry.getKey().contains("geosparql#sfWithin")&&!entry.getKey().contains("geosparql#sfTouches") && !entry.getKey().contains("geosparql#sfIntersects")){
											properties.add(property);
											System.out.println("For class : " + classLabel + "   Found Value: " + entry.getKey()
													+ " :" + entry.getValue() + " : Begin : " + property.begin + "  : end "
													+ property.end);
										}
									}
									System.out.println("For class : " + classLabel + "   Found Value: " + entry.getKey()
											+ " :" + entry.getValue());
								}
							}
						}
					}
				}
			}
			valuePropertyD.clear();
			labelPropertyD.clear();
		}*/

		//yago2 class properties
		for (Concept concept : concepts) {

			String classLabel = concept.link.substring(concept.link.lastIndexOf("/") + 1);
			System.out.println("classLabel : " + classLabel);
			//File file = new File("D:\\GeoQA code\\intelij\\GeoQAUpdated2021\\qanary_component-PropertyIdentifierYago\\src\\main\\resources\\yago2geoproperties\\" + classLabel + ".txt");
			File file = new File("qanary_component-PropertyIdentifierYago/src/main/resources/yagoproperties/" + classLabel + "_yago_label_of_properties.txt");
			Map<String, String> valuePropertyD = new HashMap<String, String>();
			Map<String, String> labelPropertyD = new HashMap<String, String>();
			System.out.println("File name : "+ file.getAbsolutePath());
			if (file.exists()) {
				valuePropertyD.clear();
				labelPropertyD.clear();
				System.out.println("opening file : " + file.getName());
				BufferedReader br = new BufferedReader(new FileReader(file));

				String line = "";
				while ((line = br.readLine()) != null) {
					String splitedLine[] = line.split(",");
					if (splitedLine.length > 1) {
						labelPropertyD.put(splitedLine[1], splitedLine[0]);
					}
				}
				br.close();
				/*if(file1.exists()) {
					BufferedReader br1 = new BufferedReader(new FileReader(file1));

					line = "";
					while ((line = br1.readLine()) != null) {
						String splitedLine[] = line.split(",");
						if (splitedLine.length > 1) {
							valuePropertyD.put(splitedLine[1], splitedLine[0]);
						}
					}
					br1.close();
				}*/

//				System.out.println("size is : " + valuePropertyD.size());

				System.out.println("size is : " + labelPropertyD.size());

				if (labelPropertyD.size() > 0) {
					double score = 0.0;
					System.out.println("Inside yago2 label property:");
//					SimilarityStrategy strategy = new JaroWinklerStrategy();
//
//					StringSimilarityService service = new StringSimilarityServiceImpl(strategy);
//					BufferedWriter bw1 = new BufferedWriter(
//							new FileWriter("/home/dharmen/justtestproperties.txt", true));
					for (Entry<String, String> entry : labelPropertyD.entrySet()) {
						for (String verb : allVerbs) {
							if(verb.equalsIgnoreCase("of")||verb.equalsIgnoreCase("in")||verb.equalsIgnoreCase("be"))//||verb.equalsIgnoreCase("name")
								continue;
//							System.out.println("Verb : "+verb);
							ArrayList<String> wordNetSynonyms = wordNet.getSynonyms(verb);
							for (String synonym : wordNetSynonyms) {
//								System.out.println("Verb : "+verb+" :::: "+"Synonym : "+synonym);
								Pattern p = Pattern.compile("\\b" + synonym  + "\\b", Pattern.CASE_INSENSITIVE);
//							if (verb.contains("height") && entry.getKey().contains("height")) {
//								bw1.write(entry.getKey());
//								bw1.newLine();
//							}
//						System.out.print("::Keyword: "+verb+"=================================="+entry.getKey()+"::");
								Matcher m = p.matcher(entry.getValue());
//							if (!concept.contains(verb)) {
//								System.out.println("before matching if condition =============== "+ verb);
								if (m.find() && !entry.getValue().equalsIgnoreCase("crosses") && !entry.getKey().contains("Wikipage")
										&& !entry.getKey().equalsIgnoreCase("runs")
										&& !entry.getKey().equalsIgnoreCase("south")
										&& !entry.getKey().equalsIgnoreCase("number") && !synonym.equalsIgnoreCase("km")
										&& !synonym.equalsIgnoreCase("of") && !synonym.equalsIgnoreCase("number")
										&& !synonym.equalsIgnoreCase("range") && !synonym.equalsIgnoreCase("be")
										&& !synonym.equalsIgnoreCase("in")&& entry.getKey().length() > 2) {
									valueFlag = true;
									Property property = new Property();
									if (relationList.size() == 0 || !relationList.contains(entry.getKey())) {
										relationList.add(entry.getKey());
										valuePropertyList.add(entry.getKey());
										property.begin = lemQuestion.toLowerCase().indexOf(synonym);
										property.end = property.begin + synonym.length();
										property.label = entry.getValue();
										property.uri = entry.getKey();
                                        property.concept = concept;
										/*property.concept.link = concept.link;
										property.concept.begin = concept.begin;
										property.concept.end = concept.end;*/
//										if (property.begin > 0 && property.end > property.begin) {
										if(classLabel.toLowerCase(Locale.ROOT).contains("mountain")  ){
											if(property.uri.contains("highest") || property.uri.contains("height")){
												property.uri = "http://yago-knowledge.org/resource/infobox/en/elevationm";
												System.out.println("ffff    insied===================================================");
											}
										}

										properties.add(property);
										System.out.println("For class : " + classLabel + "   Found Value: " + property.uri
												+ " :" + entry.getValue() + " : Begin : " + property.begin + "  : end "
												+ property.end);

//									}
									}
									System.out.println("For class : " + classLabel + "   Found Value: " + entry.getKey()
											+ " :" + entry.getValue());
								}
							}

//							}
						}
					}
//					bw1.close();
				}
			}

			valuePropertyD.clear();
			labelPropertyD.clear();
		}

		//check for implicit properties in question
		System.out.println("isJJN : "+isJJSNN(myQuestion));
		if(properties.size()<1) {
			List<String> adjpText= null;
			if (isJJSNN(myQuestion)) {
				String questionProperty = getJJS(myQuestion);
				System.out.println("isJJS: TRUE");
				//for(Property prop: properties){
				//	if(!prop.label.toLowerCase().contains(questionProperty)||!prop.uri.toLowerCase().contains(questionProperty)){
				Property property = new Property();
				System.out.println("question property : "+questionProperty);
				switch (questionProperty) {
					// Update the code based on class property mapping

					case "smallest":
					case "longest":
					case "biggest":
					case "bigges":
					case "largest":
					case "tallest":
					case "highest":
					case "least":
					case "fewest":
						System.out.println("inside switch case with : "+questionProperty);
						if(lemQuestion.contains("population") || lemQuestion.contains("populat")){
						property.uri = "http://kr.di.uoa.gr/yago2geo/ontology/hasGAG_Population";
						property.label = "";
						if(myQuestion.contains("population")){
							property.label = "population";
							property.begin = lemQuestion.indexOf("population");
							property.end = property.begin + "population".length();
						}else if(myQuestion.contains("populated")){
							property.label = "populated";
							property.begin = lemQuestion.indexOf("populated");
							property.end = property.begin + "populated".length();
						} else if(myQuestion.contains("populous")){
							property.label = "populated";
							property.begin = lemQuestion.indexOf("populated");
							property.end = property.begin + "populated".length();
						}
						for(Concept con: concepts){
							if(con.link.contains("wordnet_city") || con.link.contains("wordnet_county")|| con.link.contains("wordnet_village")||con.link.contains("wordnet_town") ||con.link.contains("wordnet_settlement")){
								property.concept = new Concept();
								property.concept.link = con.link;
							}else {
								property.concept = new Concept();
								property.concept.link = "test";
							}
						}

							System.out.println("Adding populationTotal");
						}
						else if(lemQuestion.contains("river") || lemQuestion.contains("bridge")){
							property.uri = "http://yago-knowledge.org/resource/infobox/en/length";
							System.out.println("property URI : "+property.uri);
							if(questionProperty.equalsIgnoreCase("longest")){
								property.label = "longest";
								property.begin = lemQuestion.indexOf("longest");
								property.end = property.begin + "longest".length();
							} else if(questionProperty.equalsIgnoreCase("largest")){
								property.label = "largest";
								property.begin = lemQuestion.indexOf("largest");
								property.end = property.begin + "largest".length();
							}
							else if(questionProperty.equalsIgnoreCase("smallest")){
								property.label = "smallest";
								property.begin = lemQuestion.indexOf("smallest");
								property.end = property.begin + "smallest".length();
							}
							for(Concept con: concepts){
								if(con.link.contains("river") || con.link.contains("bridge")){
									property.concept = new Concept();
									property.concept.link = con.link;
									break;
								} else {
									property.concept = new Concept();
									property.concept.link = "test";
								}
							}
						}else if(lemQuestion.contains("stadium") || lemQuestion.contains("hotel") || lemQuestion.contains("hospital") || lemQuestion.contains("wordnet_park") ){

							if(questionProperty.equalsIgnoreCase("biggest")){
								property.uri = "http://yago-knowledge.org/resource/infobox/en/capacity";
								property.label = "biggest";
								property.begin = lemQuestion.indexOf("biggest");
								property.end = property.begin + "biggest".length()-1;
							} else if(questionProperty.equalsIgnoreCase("largest") ){
								property.uri = "http://yago-knowledge.org/resource/infobox/en/area";
								property.label = "largest";
								property.begin = lemQuestion.indexOf("largest");
								property.end = property.begin + "largest".length()-1;
							}
							System.out.println("property URI : "+property.uri);
							for(Concept con: concepts){
								System.out.println("concept: "+con.link);
								if(con.link.contains("stadium") || con.link.contains("hotel") || con.link.contains("hospital") || con.link.contains("wordnet_park")){
									property.concept = new Concept();
									property.concept.link = con.link;
									break;
								} else {
									property.concept = new Concept();
									property.concept.link = "test";
								}
							}
						}else if(lemQuestion.contains("dam") ){

							if(questionProperty.equalsIgnoreCase("tallest")){
								property.uri = "http://yago-knowledge.org/resource/infobox/en/damheight";
								property.label = "tallest";
								property.begin = lemQuestion.indexOf("tallest");
								property.end = property.begin + "tallest".length()-1;
							} else if(questionProperty.equalsIgnoreCase("largest")  ){
								property.uri = "http://yago-knowledge.org/resource/infobox/en/area";
								property.label = "largest";
								property.begin = lemQuestion.indexOf("largest");
								property.end = property.begin + "largest".length()-1;
							}
							System.out.println("property URI : "+property.uri);
							for(Concept con: concepts){
								if(con.link.contains("dam") ){
									property.concept = new Concept();
									property.concept.link = con.link;
									break;
								} else {
									property.concept = new Concept();
									property.concept.link = "test";
								}
							}
						}else if(lemQuestion.contains("mountain") ){
							if(questionProperty.equalsIgnoreCase("highest")){
								property.uri = "http://yago-knowledge.org/resource/infobox/en/elevationm";
								property.label = "highest";
								property.begin = lemQuestion.indexOf("highest");
								property.end = property.begin + "highest".length()-1;
							} else if(questionProperty.equalsIgnoreCase("tallest")){
								property.uri = "http://yago-knowledge.org/resource/infobox/en/elevationm";
								property.label = "tallest";
								property.begin = lemQuestion.indexOf("tallest");
								property.end = property.begin + "tallest".length()-1;
							}
							System.out.println("property URI : "+property.uri);
							for(Concept con: concepts){
								System.out.println("concept: "+con.link);
								if(con.link.contains("mountain")){
									property.concept = new Concept();
									property.concept.link = con.link;
									break;
								} else {
									property.concept = new Concept();
									property.concept.link = "test";
								}
							}
						} else if(lemQuestion.contains("building") ){
							if(questionProperty.equalsIgnoreCase("highest")){
								property.uri = "http://yago-knowledge.org/resource/infobox/en/height";
								property.label = "highest";
								property.begin = lemQuestion.indexOf("highest");
								property.end = property.begin + "highest".length()-1;
							} else if(questionProperty.equalsIgnoreCase("tallest")){
								property.uri = "http://yago-knowledge.org/resource/infobox/en/height";
								property.label = "tallest";
								property.begin = lemQuestion.indexOf("tallest");
								property.end = property.begin + "tallest".length()-1;
							}
							System.out.println("property URI : "+property.uri);
							for(Concept con: concepts){
								System.out.println("concept: "+con.link);
								if(con.link.contains("building")){
									property.concept = new Concept();
									property.concept.link = con.link;
									break;
								} else {
									property.concept = new Concept();
									property.concept.link = "test";
								}
							}
						} else if(lemQuestion.contains("lake") ){
							if(questionProperty.equalsIgnoreCase("largest")){
								property.uri = "strdf:area";
								property.label = "largest";
								property.begin = lemQuestion.indexOf("largest");
								property.end = property.begin + "largest".length()-1;
							}
							if(questionProperty.equalsIgnoreCase("smallest")){
								property.uri = "strdf:area";
								property.label = "smallest";
								property.begin = lemQuestion.indexOf("smallest");
								property.end = property.begin + "smallest".length()-1;
							}
							System.out.println("property URI : "+property.uri);
							for(Concept con: concepts){
								System.out.println("concept: "+con.link);
								if(con.link.contains("lake")){
									property.concept = new Concept();
									property.concept.link = con.link;
									break;
								} else {
									property.concept = new Concept();
									property.concept.link = "test";
								}
							}
						} else if(lemQuestion.contains("county") || lemQuestion.contains("city") || lemQuestion.contains("town") || lemQuestion.contains("village") || lemQuestion.contains("settlement")){
							System.out.println("inside ============= county city town village largest");
							if(questionProperty.equalsIgnoreCase("biggest")||questionProperty.equalsIgnoreCase("bigges") ){
								property.uri = "strdf:area";
								property.label = "biggest";
								property.begin = lemQuestion.indexOf("biggest");
								property.end = property.begin + "biggest".length()-1;
							} else if(questionProperty.equalsIgnoreCase("largest") ){
								property.uri = "strdf:area";
								property.label = "largest";
								property.begin = lemQuestion.indexOf("largest");
								property.end = property.begin + "largest".length()-1;
							}
							System.out.println("property URI : "+property.uri);
							for(Concept con: concepts){
								System.out.println("concept: "+con.link);
								if(con.link.contains("county") || con.link.contains("city") || con.link.contains("town") || con.link.contains("village") || con.link.contains("settlement")){
									property.concept = new Concept();
									property.concept.link = con.link;
									break;
								} else {
									property.concept = new Concept();
									property.concept.link = "test";
								}
							}
						}
						/*else if(!questionProperty.contains("fewset")){
							property.label = "area";
							if(lemQuestion.toLowerCase(Locale.ROOT).contains(" area")){
								property.begin = lemQuestion.toLowerCase(Locale.ROOT).indexOf("area");
								property.end = property.begin+4;
							}else{
								property.begin = lemQuestion.indexOf(questionProperty);
								property.end = property.begin + questionProperty.length();
							}
							property.uri = "strdf:area";
							instProperties.add(property);
							System.out.println("Adding strdf:area");
						}*/
						break;
					default:
						break;
				}
				if (property.uri != null ) {
					System.out.println("adding property :"+property.uri);
					properties.add(property);
				}
				//}
				//}
			}
			else {
				if ((adjpText = getADJPConstituents(myQuestion)) != null && properties.size()<1) {
					Property property = new Property();
					if (adjpText.size() > 0) {
						String adjpVal = adjpText.get(0);
						adjpVal = adjpVal.replaceAll(",", "");
						adjpVal = adjpVal.replace("most", "");
						adjpVal = adjpVal.replaceAll("\\[","");
						adjpVal = adjpVal.replaceAll("\\]","");
						adjpVal = adjpVal.trim();
						System.out.println("adjp value : "+adjpVal);
						if (adjpVal.contains("densely populated")) {
							adjpVal = adjpVal.split(" ")[1];
							for(Concept con: concepts){
								if(con.link.contains("wordnet_city") || con.link.contains("wordnet_county")||con.link.contains("wordnet_village")||con.link.contains("wordnet_town")){
									property.concept = new Concept();
									property.concept.link = con.link;
									property.uri = "http://yago-knowledge.org/resource/infobox/en/populationtotal";
									property.label = adjpVal;
									property.begin = myQuestion.indexOf(adjpVal);
									property.end = property.begin + adjpVal.length();
									System.out.println("Adding populationDensity");
									System.out.println("myquestion : "+myQuestion);
									break;
								}
							}
						} else if (adjpVal.contains("popular")) {
							property.uri = "http://dbpedia.org/ontology/numberOfVisitors";
							property.label = adjpVal;
							property.begin = myQuestion.indexOf(adjpVal);
							property.end = property.begin + adjpVal.length();
							System.out.println("Adding numberOfVisitors");
							for(Concept con: concepts){
								if(con.link.contains("river") || con.link.contains("bridge")){
									property.concept = new Concept();
									property.concept.link = con.link;
									break;
								} else {
									property.concept = new Concept();
									property.concept.link = "test";
								}
							}
						} else if (adjpVal.contains("famous")) {
							property.uri = "http://yago-knowledge.org/resource/infobox/en/visitationnum";
							property.label = "famous";
							property.begin = myQuestion.indexOf("famous");
							property.end = property.begin + "famous".length()-1;
							property.concept = new Concept();
							property.concept.link = "test";
							System.out.println("Adding visitationnum");
							for(Concept con: concepts){
								if(con.link.contains("river") || con.link.contains("bridge")){
									property.concept = new Concept();
									property.concept.link = con.link;
									break;
								} else {
									property.concept = new Concept();
									property.concept.link = "test";
								}
							}
						} else if (adjpVal.contains("populated")||adjpVal.contains("populous")) {
							System.out.println(" inside populated");
							property.uri = "http://yago-knowledge.org/resource/infobox/en/populationtotal";
							if(adjpVal.contains("populated")){
								property.label = "populated";
								property.begin = myQuestion.indexOf("populated");
								property.end = property.begin + "populated".length()-1;
							}else if(adjpVal.contains("populous")){
								property.label = "populous";
								property.begin = myQuestion.indexOf("populous");
								property.end = property.begin + "populous".length()-1;
							}
							for(String con: allClassesList){
								System.out.println("calling concept : "+con);
								if(con.contains("city") || con.contains("county")|| con.contains("village")||con.contains("town")){
									property.concept = new Concept();
									property.concept.link = con;
									break;
								}else {
									property.concept = new Concept();
									property.concept.link = "test";
								}
							}
							System.out.println("Adding populationTotal");
						} else if (adjpVal.contains("elevated") ) {
							property.uri = "http://yago-knowledge.org/resource/infobox/en/elevationm";
							property.label = adjpVal;
							property.begin = myQuestion.indexOf(adjpVal);
							property.end = property.begin + adjpVal.length();
							System.out.println("Adding elevation");
							for(Concept con: concepts){
								if(con.link.contains("city") || con.link.contains("county")|| con.link.contains("village")||con.link.contains("town")){
									property.concept = new Concept();
									property.concept.link = con.link;
									break;
								}else {
									property.concept = new Concept();
									property.concept.link = "test";
								}
							}
						} else if(adjpVal.contains("smallest")||adjpVal.contains("biggest")||adjpVal.contains("largest")){
							property.label = "area";
							if(lemQuestion.toLowerCase(Locale.ROOT).contains(" area")){
								property.begin = lemQuestion.toLowerCase(Locale.ROOT).indexOf("area");
								property.end = property.begin+4;
							}else if(lemQuestion.contains("smallest")){
								property.begin = myQuestion.indexOf("smallest");
								property.end = property.begin + "smallest".length();
							}else if(lemQuestion.contains("biggest")){
								property.begin = lemQuestion.indexOf("biggest");
								property.end = property.begin + "biggest".length();
							}else if(lemQuestion.contains("largest")){
								property.begin = lemQuestion.indexOf("largest");
								property.end = property.begin + "largest".length();
							}
							property.uri = "strdf:area";
							property.concept = new Concept();
							property.concept.link = "test";

//							instProperties.add(property);
							System.out.println("Adding strdf:area");
						} else if (adjpVal.contains("taller") || adjpVal.contains("shorter")) {
							property.uri = "http://yago-knowledge.org/resource/infobox/en/elevationm";


							if(adjpVal.contains("taller")){
								property.label = "taller";
								property.begin = myQuestion.indexOf("taller");
								property.end = property.begin + "taller".length()-1;
							}
							else if(adjpVal.contains("shorter")){
								property.label = "shorter";
								property.begin = myQuestion.indexOf("shorter");
								property.end = property.begin + "shorter".length() -1;
							}
							for(Concept con: concepts){
								if(con.link.contains("mountain") || con.link.contains("building")|| con.link.contains("tower")){
									property.concept = new Concept();
									property.concept.link = con.link;
									break;
								}else {
									property.concept = new Concept();
									property.concept.link = "test";
								}
							}
							System.out.println("Adding elevation");
						}
					}
					if (property.uri != null) {
						properties.add(property);
					}

				}
			}
		}

		for (Property YAGOProperty : properties) {
			System.out.println("property :"+YAGOProperty.uri);
			sparql = "prefix qa: <http://www.wdaqua.eu/qa#> "
					+ "prefix oa: <http://www.w3.org/ns/openannotation/core/> "
					+ "prefix xsd: <http://www.w3.org/2001/XMLSchema#> "
					+ "prefix dbp: <http://dbpedia.org/property/> " + "INSERT { " + "GRAPH <"
					+ myQanaryQuestion.getOutGraph() + "> { " + "  ?a a qa:AnnotationOfRelation . "
					+ "  ?a oa:hasTarget [ " + "           a    oa:SpecificResource; "
					+ "           oa:hasSource    <" + myQanaryQuestion.getUri() + ">; "
					+ "			  oa:hasSelector  [ " //
					+ "			         a        oa:TextPositionSelector ; " //
					+ "			         oa:start " + YAGOProperty.begin + " ; " //
					+ "			         oa:end   " + YAGOProperty.end + " " //
					+ "		     ] " //
					+ "  ] ; " + "     oa:hasValue <" + YAGOProperty.uri.trim() + ">;"
					+" oa:hasConcept <"+YAGOProperty.concept.link+">;"
//						+ "     oa:annotatedBy <http:DBpedia-RelationExtractor.com> ; "
					+ "	    oa:AnnotatedAt ?time  " + "}} " + "WHERE { " + "BIND (IRI(str(RAND())) AS ?a) ."
					+ "BIND (now() as ?time) " + "}";
			logger.info("Sparql query {}", sparql);
			myQanaryUtils.updateTripleStore(sparql, myQanaryMessage.getEndpoint().toString());
		}
		// Instance property
		if(properties.size()<1) {
			List<String> allNouns = getNouns(myQuestion);
			System.out.println("Nouns : " + allNouns);
			for (String entity : entitiesUri) {

				String entityLabel = entity.substring(entity.lastIndexOf("/")+1);
				String sparqlQuery = "prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> "
						+ "prefix geo:<http://www.w3.org/2003/01/geo/wgs84_pos#> "
						+ "prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> "
						+ "prefix dbo: <http://dbpedia.org/ontology/> " + " select DISTINCT ?p " + " where {" + "<" + entity
						+ "> " + " ?p ?o. }  ";

				System.out.println("Sparql Query : " + sparqlQuery + "\n Instance: " + entity);
				Query query = QueryFactory.create(sparqlQuery);

				QueryExecution exec = QueryExecutionFactory.sparqlService(yago2geoOnlyEndpoint, query);

				ResultSet results = ResultSetFactory.copyResults(exec.execSelect());
//			System.out.println("result set : "+results.toString());
				if (!results.hasNext() && properties.size() > 0) {
					System.out.println("Property size : " + properties.size() + "Getting out of loop");
					break;
				} else {
					while (results.hasNext()) {
						QuerySolution qs = results.next();
						String dbpediaProperty = qs.get("p").toString();
						String dbpediaPropertyLabel = dbpediaProperty.substring(dbpediaProperty.lastIndexOf('/') + 1);
//						System.out.println("YAGO property label : " + dbpediaPropertyLabel);
//					SimilarityStrategy strategy = new JaroWinklerStrategy();
//
//					StringSimilarityService service = new StringSimilarityServiceImpl(strategy);
						for (String verb : allNouns) {
//									Pattern p = Pattern.compile("\\b" + dbpediaPropertyLabel + "\\b", Pattern.CASE_INSENSITIVE);
//									Matcher m = p.matcher(verb);
							if (verb.toLowerCase().contains("south") || verb.toLowerCase().contains("cross")
									|| verb.toLowerCase().contains("north") || verb.toLowerCase().contains("west")
									|| verb.toLowerCase().contains("east") || verb.toLowerCase().contains("of")
									|| verb.toLowerCase().contains("county")|| verb.toLowerCase().contains("distance") || verb.toLowerCase().contains(entityLabel))
								continue;
							Property property = new Property();
//									if (m.find() && !dbpediaPropertyLabel.contains("cross")) {
							if (dbpediaPropertyLabel.toLowerCase().contains(verb.toLowerCase()) && verb.length() > 2) {
								if (relationList.size() == 0 || !relationList.contains(dbpediaPropertyLabel)
										&& !dbpediaProperty.toLowerCase().contains("rank")&& !dbpediaProperty.toLowerCase().contains("distance")&& !dbpediaProperty.toLowerCase().contains(entityLabel)) {
									relationList.add(dbpediaPropertyLabel);
									instanceProperties.add(dbpediaProperty);
//											valuePropertyList.add(enrty.getValue());
									property.label = dbpediaPropertyLabel;
//											dbpediaPropertyLabel = dbpediaPropertyLabel.substring(dbpediaPropertyLabel.indexOf(' '));
									property.begin = lemQuestion.toLowerCase().indexOf(verb.toLowerCase());
									property.end = property.begin + verb.length();
									property.uri = dbpediaProperty;
									property.entity = new Entity();
									property.entity.uri = entity;
									if (property.begin > 0 && property.end > property.begin
											&& !property.uri.toLowerCase().contains("mouth") && !property.uri.contains("distance")&& !property.uri.contains("geosparql#sfWithin")&& !property.uri.contains("geosparql#sfTouches") && !property.uri.contains("geosparql#sfIntersects"))
										instProperties.add(property);
//											break;
								}
							}
//								}
						}

					}
				}
				exec = QueryExecutionFactory.sparqlService(yago2endpoint, query);
				results = ResultSetFactory.copyResults(exec.execSelect());
				if (!results.hasNext() && properties.size() > 0) {
					System.out.println("Property size : " + properties.size() + "Getting out of loop");
					break;
				} else {
					while (results.hasNext()) {
						QuerySolution qs = results.next();
						String dbpediaProperty = qs.get("p").toString();
						String dbpediaPropertyLabel = dbpediaProperty.substring(dbpediaProperty.lastIndexOf('/') + 1);
//						System.out.println("YAGO property label : " + dbpediaPropertyLabel);
//					SimilarityStrategy strategy = new JaroWinklerStrategy();
//
//					StringSimilarityService service = new StringSimilarityServiceImpl(strategy);
						for (String verb : allNouns) {
//									Pattern p = Pattern.compile("\\b" + dbpediaPropertyLabel + "\\b", Pattern.CASE_INSENSITIVE);
//									Matcher m = p.matcher(verb);
							if (verb.toLowerCase().contains("south") || verb.toLowerCase().contains("cross")
									|| verb.toLowerCase().contains("north") || verb.toLowerCase().contains("west")
									|| verb.toLowerCase().contains("east") || verb.toLowerCase().contains("of")
									|| verb.toLowerCase().contains("county")|| verb.toLowerCase().contains("distance") || verb.toLowerCase().contains(entityLabel))
								continue;
							Property property = new Property();
//									if (m.find() && !dbpediaPropertyLabel.contains("cross")) {
							if (dbpediaPropertyLabel.toLowerCase().contains(verb.toLowerCase()) && verb.length() > 2) {
								if (relationList.size() == 0 || !relationList.contains(dbpediaPropertyLabel)
										&& !dbpediaProperty.toLowerCase().contains("rank")&& !dbpediaProperty.toLowerCase().contains("distance")&& !dbpediaProperty.toLowerCase().contains(entityLabel)) {
									relationList.add(dbpediaPropertyLabel);
									instanceProperties.add(dbpediaProperty);
//											valuePropertyList.add(enrty.getValue());
									property.label = dbpediaPropertyLabel;
//											dbpediaPropertyLabel = dbpediaPropertyLabel.substring(dbpediaPropertyLabel.indexOf(' '));
									property.begin = lemQuestion.toLowerCase().indexOf(verb.toLowerCase());
									property.end = property.begin + verb.length();
									property.uri = dbpediaProperty;
									property.entity = new Entity();
									property.entity.uri = entity;
									if (property.begin > 0 && property.end > property.begin
											&& !property.uri.toLowerCase().contains("mouth") && !property.uri.contains("distance") && !property.uri.contains("geosparql#sfWithin")&& !property.uri.contains("geosparql#sfTouches") && !property.uri.contains("geosparql#sfIntersects")) {
										System.out.println("property : " + property.uri+" entityLabel : "+entityLabel);
										instProperties.add(property);
									}
//											break;
								}
							}
//								}
						}

					}
				}
			}
			if(properties.size()<1){
				if(myQuestion.toLowerCase(Locale.ROOT).contains(" area") || myQuestion.toLowerCase(Locale.ROOT).contains(" totalarea") ){
					Property property = new Property();
					property.label = "area";
					property.begin = myQuestion.indexOf(" area")+1;
					property.end = property.begin+4;
					property.uri = "strdf:area";
					property.entity = new Entity();
					property.entity.uri = entitiesUri.iterator().next();
					instProperties.add(property);
				}
				if(myQuestion.toLowerCase().contains("height")){
					Property property = new Property();
					property.label = "height";
					property.begin = myQuestion.indexOf(" height")+1;
					property.end = property.begin+"height".length()-1;
					property.uri = "http://yago-knowledge.org/resource/infobox/en/elevationm";
					property.entity = new Entity();
					property.entity.uri = entitiesUri.iterator().next();
					instProperties.add(property);
				}
			}
		}

		allVerbs.clear();
		for (Property DBpediaProperty : instProperties) {
			sparql = "prefix qa: <http://www.wdaqua.eu/qa#> "
					+ "prefix oa: <http://www.w3.org/ns/openannotation/core/> "
					+ "prefix xsd: <http://www.w3.org/2001/XMLSchema#> " + "prefix dbp: <http://dbpedia.org/property/> "
					+ "INSERT { " + "GRAPH <" + myQanaryQuestion.getOutGraph() + "> { "
					+ "  ?a a qa:AnnotationOfRelationInstance . " + "  ?a oa:hasTarget [ "
					+ "           a    oa:SpecificResource; " + "           oa:hasSource    <"
					+ myQanaryQuestion.getUri() + ">; " + "			  oa:hasSelector  [ " //
					+ "			         a        oa:TextPositionSelector ; " //
					+ "			         oa:start " + DBpediaProperty.begin + " ; " //
					+ "			         oa:end   " + DBpediaProperty.end + " " //
					+ "		     ] " //
					+ "  ] ; " + "     oa:hasValue <" + DBpediaProperty.uri + ">;"
					+" oa:hasEntity <"+DBpediaProperty.entity.uri+">;"
//								+ "     oa:annotatedBy <http:DBpedia-RelationExtractor.com> ; "
					+ "	    oa:AnnotatedAt ?time  " + "}} " + "WHERE { " + "BIND (IRI(str(RAND())) AS ?a) ."
					+ "BIND (now() as ?time) " + "}";
			logger.info("Sparql query {}", sparql);
			myQanaryUtils.updateTripleStore(sparql, myQanaryMessage.getEndpoint().toString());
		}
		logger.info("store data in graph {}", myQanaryMessage.getValues().get(myQanaryMessage.getEndpoint()));
		// TODO: insert data in QanaryMessage.outgraph

		logger.info("apply vocabulary alignment on outgraph");

		return myQanaryMessage;
	}

	class Concept {
		public int begin;
		public int end;
		public String link;
	}

	public class Property {
		public int begin;
		public int end;
		public String label;
		public String uri;
		public Concept concept;
		public Entity entity;
	}

	public class Entity {

		public int begin;
		public int end;
		public String namedEntity;
		public String uri;
		public int y2glinkCount;
		public int y2LinkCount;

		public void print() {
			System.out.println("Start: " + begin + "\t End: " + end + "\t Entity: " + namedEntity);
		}
	}
}
