package eu.wdaqua.qanary.tagmedisambiguate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.jena.query.*;
/*import org.apache.jena.rdfxml.xmlinput.impl.RDFXMLParser;
import org.apache.jena.vocabulary.RDF;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.Rio;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.openrdf.query.resultio.stSPARQLQueryResultFormat;
import org.openrdf.rio.RDFParser;*/
import org.openrdf.query.resultio.stSPARQLQueryResultFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import eu.earthobservatory.org.StrabonEndpoint.client.EndpointResult;
import eu.earthobservatory.org.StrabonEndpoint.client.SPARQLEndpoint;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;
import eu.wdaqua.qanary.exceptions.SparqlQueryFailed;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


import javax.servlet.http.HttpUtils;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


@Component
/**
 * This component connected automatically to the Qanary pipeline.
 * The Qanary pipeline endpoint defined in application.properties (spring.boot.admin.url)
 * @see <a href="https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F" target="_top">Github wiki howto</a>
 */
public class TagMeDisambiguate extends QanaryComponent {
	private final String tagMeKey = "150907b3-f257-4d8f-b22b-6d0e6c72f53d-843339462";
	private final String wikipediaLink = "https://en.wikipedia.org/wiki/";
	private final String yagoLink = "http://yago-knowledge.org/resource/";
	public final String yagoEndpoint = "http://pyravlos1.di.uoa.gr:9997/Strabon/Query";
	public final String dbpediaEndpoint = "https://dbpedia.org/sparql";
	public final String dbpediaLink = "http://dbpedia.org/resource/";
	public final String yago2geoOnlyEndpoint = "http://pyravlos1.di.uoa.gr:9999/Strabon/Query";
	private static final Logger logger = LoggerFactory.getLogger(TagMeDisambiguate.class);

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

	public static int getNoOfLinks(String sparqlQuery, String endpointURI) {
		int count = 0;

		System.out.println("Sparql Query : "+sparqlQuery);
		Query query = QueryFactory.create(sparqlQuery);
		System.out.println("sparql query :" + query.toString());
		QueryExecution exec = QueryExecutionFactory.sparqlService(endpointURI, query);
		ResultSet results = ResultSetFactory.copyResults(exec.execSelect());

		if (!results.hasNext()) {

		} else {
			while (results.hasNext()) {
				QuerySolution qs = results.next();
				count = qs.getLiteral("total").getInt();
//				System.out.println("total: " + count);
			}
		}

		return count;
	}

	private String getArguments(String text) {
		HashMap<String, String> parameters = new HashMap<String,String>();
		StringBuilder stb = new StringBuilder();

		for(char c: text.toCharArray()) {
			if(c != ' ')
				stb.append(c);
			else
				stb.append("%20");
		}

		parameters.put("text", stb.toString());

		return "lang="+parameters.get("lang")+"&gcube-token="+parameters.get("gcube-token")+"&text="+parameters.get("text");
	}


	public static List<String> getInstances(String sparqlQuery, String endpointURI) {

		List<String> retValues = new ArrayList<String>();
		Query query = QueryFactory.create(sparqlQuery);
		System.out.println("sparql query :" + query.toString());
		QueryExecution exec = QueryExecutionFactory.sparqlService(endpointURI, query);

		ResultSet results = ResultSetFactory.copyResults(exec.execSelect());

		if (!results.hasNext()) {
			System.out.println("There is no next!");
		} else {
			while (results.hasNext()) {
				QuerySolution qs = results.next();
				retValues.add(qs.getResource("instance").toString());
			}
		}
		return retValues;
	}

	public static String runSparqlOnEndpoint(String sparqlQuery, String endpointURI) {

		Query query = QueryFactory.create(sparqlQuery);
		System.out.println("sparql query :" + query.toString());
		QueryExecution exec = QueryExecutionFactory.sparqlService(endpointURI, query);

		ResultSet results = ResultSetFactory.copyResults(exec.execSelect());

		if (!results.hasNext()) {
			System.out.println("There is no next!");
		} else {
			while (results.hasNext()) {

				QuerySolution qs = results.next();

				String uria = qs.get("x").toString();
				System.out.println("uria: " + uria);
				return uria;

			}
		}
		return null;
	}
	public static List<String> getEntitesWithName(String sparqlQuery, String endpointURI) {

		List<String> retValues = new ArrayList<String>();
		Query query = QueryFactory.create(sparqlQuery);
		System.out.println("sparql query :" + query.toString());
		QueryExecution exec = QueryExecutionFactory.sparqlService(endpointURI, query);
		ResultSet results = ResultSetFactory.copyResults(exec.execSelect());

		if (!results.hasNext()) {
			System.out.println("There is no next!");

		} else {
			while (results.hasNext()){
				QuerySolution qs = results.next();
				String predicate = qs.getResource("x").toString();
				retValues.add(predicate);
			}
		}
		return retValues;
	}

	/**
	 * implement this method encapsulating the functionality of your Qanary
	 * component, some helping notes w.r.t. the typical 3 steps of implementing a
	 * Qanary component are included in the method (you might remove all of them)
	 *
	 * @throws SparqlQueryFailed
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {

		try {
			long startTime = System.currentTimeMillis();
			logger.info("process: {}", myQanaryMessage);

			List<String> entitiesList = new ArrayList<String>();
			List<String> namePredicates = new ArrayList<>();
			// STEP 1: Retrieve the information needed for the question

			// the class QanaryUtils provides some helpers for standard tasks
			QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
			QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);

			// Retrieves the question string
			String myQuestion = myQanaryQuestion.getTextualRepresentation();

			String countQuery1 = "SELECT (count(?p) as ?total) where { ";
			String countQuery2 = " ?p ?o. }";
			namePredicates.add("http://kr.di.uoa.gr/yago2geo/ontology/hasOSM_Name");
			namePredicates.add("http://kr.di.uoa.gr/yago2geo/ontology/hasGADM_Name");
			namePredicates.add("http://kr.di.uoa.gr/yago2geo/ontology/hasOS_Name");
			namePredicates.add("http://kr.di.uoa.gr/yago2geo/ontology/hasGAG_Name");
			namePredicates.add("http://kr.di.uoa.gr/yago2geo/ontology/hasOSNI_Name");
			namePredicates.add("http://kr.di.uoa.gr/yago2geo/ontology/hasOSI_Name");
			// Step 2: Call the TagMe service
			// Information about the service can be found here
			// https://services.d4science.org/web/tagme/wat-api
			String input = myQuestion;// lemmatize(myQuestion);

			logger.info("Input to TagMe: " + input);

			// http request to the TagMe service
			TagMeRequest tagMeRequest = new TagMeRequest(tagMeKey);

			TagMeResponse response = tagMeRequest.doRequest(input);
			TagMeResponse responseT = tagMeRequest.doRequestTagMeAPI(input);
			ArrayList<NedAnnotation> annotations = response.getAnnotations();
			/*if(annotations.size()<2) {
				annotations.addAll(responseT.getAnnotations());
			}*/

			// Extract entities
			ArrayList<Link> links = new ArrayList<Link>();

			for (NedAnnotation ann : annotations) {
				if (ann.getTitle() != null && !ann.getTitle().contains("(")) {

					System.out.println("title : "+ann.getTitle());
					int cnt = 0;
					Link l = new Link();
					l.link = this.yagoLink + ann.getTitle();
//					l.link = l.link.replaceAll("&amp;","&");
//					l.link = l.link.replaceAll(" ","_");


					if(l.link.contains("&amp;")){
						l.link = l.link.replaceAll("&amp;","&");
					}
					if(l.link.contains(" ")){
						l.link = l.link.replaceAll(" ","_");
					}

					System.out.println("l.link : "+l.link);
					l.y2glinkCount = getNoOfLinks(countQuery1 + " <" + l.link + "> " + countQuery2,
							yago2geoOnlyEndpoint);
					l.y2LinkCount = getNoOfLinks(countQuery1 + " <" + l.link + "> " + countQuery2,
								yagoEndpoint);

					l.begin = ann.getStart();
					l.end = ann.getEnd();
					System.out.println("count of :"+ l.link +" = "+l.y2glinkCount + " :: "+l.y2LinkCount);
					if(ann.getTitle().contains("_GAA"))
						l.y2glinkCount=0;
					if(l.y2glinkCount<1){
						String ent_title = ann.getTitle();
						for(String predicate:namePredicates){
							if(ent_title.contains("_")){
								ent_title = ent_title.replaceAll("_"," ");
								if(ent_title.contains("GAA")){
									ent_title = ent_title.replaceAll("GAA","");
								}
								if(ent_title.contains(",")){
									ent_title = ent_title.substring(0,ent_title.indexOf(","));
								}
							}
							String langTag = "@en";
							List<String> entitiesfromy2g = getEntitesWithName("select distinct ?x where { ?x <"+predicate+"> \""+ent_title+"\""+langTag+"  } ",yago2geoOnlyEndpoint);
							if(entitiesfromy2g.size()<1){
								entitiesfromy2g = getEntitesWithName("select distinct ?x where { ?x <"+predicate+"> \""+ent_title+"\"  } ",yago2geoOnlyEndpoint);
							}
							if(entitiesfromy2g.size()<1){
								String ent_name = myQuestion.substring(ann.getStart(), ann.getEnd());
								System.out.println("ent name : "+ent_name);
								entitiesfromy2g = getEntitesWithName("select distinct ?x where { ?x <"+predicate+"> \""+ent_name+"\""+langTag+"  } ",yago2geoOnlyEndpoint);
								if(entitiesfromy2g.size()<1){
									entitiesfromy2g = getEntitesWithName("select distinct ?x where { ?x <"+predicate+"> \""+ent_name+"\"  } ",yago2geoOnlyEndpoint);
								}
							}
							if(entitiesfromy2g.size()>0){
								for(String ent: entitiesfromy2g){
									Link ll = new Link();
									ll.link = ent;
									ll.y2glinkCount = getNoOfLinks(countQuery1 + " <" + ll.link + "> " + countQuery2,
											yago2geoOnlyEndpoint);

									ll.y2LinkCount = l.y2LinkCount;
									ll.begin = ann.getStart();
									ll.end = ann.getEnd();
									links.add(ll);

									System.out.println("ADDING LINK for (" + input.substring(l.begin, l.end) + "):" + l.begin + " "
											+ l.end + " " + l.link);
									System.out.println("count of :"+ l.link +" = "+l.y2glinkCount+"  :: "+l.y2LinkCount);
									cnt++;
								}
							}
						}
					}
					links.add(l);
					System.out.println("ADDING LINK for (" + input.substring(l.begin, l.end) + "):" + l.begin + " "
								+ l.end + " " + l.link);

					cnt = 0;
				}

			}
			for(Link ll: links){
				System.out.println("link : "+ll.link);
			}
			int cnt = 0;

			// STEP4: Push the result of the component to the triplestore
			String sparql;
			logger.info("Apply vocabulary alignment on outgraph");
			System.out.println("");
			for (Link l : links) {
				sparql = "prefix qa: <http://www.wdaqua.eu/qa#> "
						+ "prefix oa: <http://www.w3.org/ns/openannotation/core/> "
						+ "prefix xsd: <http://www.w3.org/2001/XMLSchema#> " + "INSERT { " + "GRAPH <"
						+ myQanaryQuestion.getOutGraph() + "> { " + "  ?a a qa:AnnotationOfInstance . "
						+ "  ?a oa:hasTarget [ " + "           a    oa:SpecificResource; "
						+ "           oa:hasSource    <" + myQanaryQuestion.getUri() + ">; "
						+ "           oa:hasSelector  [ " + "                    a oa:TextPositionSelector ; "
						+ "                    oa:start \"" + l.begin + "\"^^xsd:nonNegativeInteger ; "
						+ "                    oa:end  \"" + l.end + "\"^^xsd:nonNegativeInteger;  "
						+ "					   oa:y2glinkcount \"" + l.y2glinkCount + "\"^^xsd:nonNegativeInteger; "
						+ "                    oa:y2linkcount \"" + l.y2LinkCount +"\"^^xsd:nonNegativeInteger  "
						+ "           ] " + "  ] . " + "  ?a oa:hasBody <" + l.link + "> ;"
						+ "     oa:annotatedBy <http://TagMeDisambiguate> ; "
						+ "	    oa:AnnotatedAt ?time  " + "}} " + "WHERE { " + "BIND (IRI(str(RAND())) AS ?a) ."
						+ "BIND (now() as ?time) " + "}";
				logger.info("Sparql query {}", sparql);
				System.out.println("Sparql : "+ sparql);
				myQanaryUtils.updateTripleStore(sparql, myQanaryQuestion.getEndpoint().toString());
			}
			long estimatedTime = System.currentTimeMillis() - startTime;
			logger.info("Time {}", estimatedTime);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return myQanaryMessage;
	}
	class Spot {
		public int begin;
		public int end;
	}

	class Link {
		public int begin;
		public int end;
		public String link;
		public int y2glinkCount;
		public int y2LinkCount;
	}
}
