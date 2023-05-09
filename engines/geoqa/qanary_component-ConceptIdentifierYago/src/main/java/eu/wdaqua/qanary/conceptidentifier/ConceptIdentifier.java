package eu.wdaqua.qanary.conceptidentifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.robrua.nlp.bert.Bert;
import info.debatty.java.stringsimilarity.JaroWinkler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import eu.wdaqua.qanary.commons.QanaryMessage;
import eu.wdaqua.qanary.commons.QanaryQuestion;
import eu.wdaqua.qanary.commons.QanaryUtils;
import eu.wdaqua.qanary.component.QanaryComponent;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

@Component
/**
 * This component connected automatically to the Qanary pipeline. The Qanary
 * pipeline endpoint defined in application.properties (spring.boot.admin.url)
 *
 * @see <a href=
 *      "https://github.com/WDAqua/Qanary/wiki/How-do-I-integrate-a-new-component-in-Qanary%3F"
 *      target="_top">Github wiki howto</a>
 */
public class ConceptIdentifier extends QanaryComponent {
	private static final Logger logger = LoggerFactory.getLogger(ConceptIdentifier.class);

	static List<String> allConceptWordUri = new ArrayList<String>();
	static List<String> osmClass = new ArrayList<String>();
	static List<String> commonClasses = new ArrayList<String>();
	static Map<String, String> osmUriMap = new HashMap<String, String>();
	static Map<String, String> yago2classesmap = new HashMap<String, String>();
	static Map<String, String> yago2geoclassesmap = new HashMap<>();
	public static void getCommonClass(Set<String> dbpediaConcepts) {

		for (String lab : osmClass) {

			if (dbpediaConcepts.contains(lab)) {
				if (!commonClasses.contains(lab))
					commonClasses.add(lab);
			}
		}

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

	public static void loadlistOfClasses(String fname){
		try{
			BufferedReader br = new BufferedReader(new FileReader(fname));
			String line = "";
			while((line = br.readLine())!=null){
				String splittedLine[] = line.split(",");
//				System.out.println("0: "+splittedLine[0]+"\t 1:"+splittedLine[1]);
				yago2geoclassesmap.put(splittedLine[0].trim(),splittedLine[1].trim());
			}
			br.close();
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	public static void loadlistOfyago2Classes(String fname){
		try{
			BufferedReader br = new BufferedReader(new FileReader(fname));
			String line = "";
			while((line = br.readLine())!=null){
				String splittedLine[] = line.split(",");
//				System.out.println("0: "+splittedLine[0]+"\t 1:"+splittedLine[1]);
				yago2classesmap.put(splittedLine[0].trim(),splittedLine[1].trim());
			}
			br.close();
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	public static void getXML(String fname) {
		try {
			File fXmlFile = new File(fname);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);

			doc.getDocumentElement().normalize();

			NodeList nList = doc.getElementsByTagName("owl:Class");
			for (int temp = 0; temp < nList.getLength(); temp++) {

				Node nNode = nList.item(temp);
				String uri, cEntity;

				if (nNode.getNodeType() == Node.ELEMENT_NODE) {

					Element eElement = (Element) nNode;
					uri = eElement.getAttribute("rdf:about");
					osmUriMap.put(uri.substring(uri.indexOf('#') + 1), uri);
					uri = uri.substring(uri.indexOf('#') + 1);
					osmClass.add(uri);
				}

			}

		} catch (Exception e) {
			e.printStackTrace();
		}

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
				if (pos.contains("NN")) {
					postags.add(token.get(LemmaAnnotation.class));
				}
			}
		}
		return postags;
	}

	public static ArrayList<String> ngrams(int n, String str) {
		ArrayList<String> ngrams = new ArrayList<String>();
		String[] words = str.split(" ");
		for (int i = 0; i < words.length - n + 1; i++)
			ngrams.add(concat(words, i, i+n));
		return ngrams;
	}
	public static String concat(String[] words, int start, int end) {
		StringBuilder sb = new StringBuilder();
		for (int i = start; i < end; i++)
			sb.append((i > start ? " " : "") + words[i]);
		return sb.toString();
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

	static int wordcount(String string)
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

	/**
	 * implement this method encapsulating the functionality of your Qanary
	 * component
	 *
	 * @throws Exception
	 */
	@Override
	public QanaryMessage process(QanaryMessage myQanaryMessage) throws Exception {
		logger.info("process: {}", myQanaryMessage);

		// Map<String, String> allMapConceptWord =
		// DBpediaConceptsAndURIs.getDBpediaConceptsAndURIs();
//		Map<String, ArrayList<String>> allMapConceptWord = YagoConceptsAndURIs.getYagoConceptsAndURIs();
//		getXML("qanary_component-ConceptIdentifierYago/src/main/resources/osm.owl");

		// getCommonClass(allMapConceptWord.keySet());
		QanaryUtils myQanaryUtils = this.getUtils(myQanaryMessage);
		QanaryQuestion<String> myQanaryQuestion = this.getQanaryQuestion(myQanaryMessage);
		List<Concept> mappedConcepts = new ArrayList<Concept>();
		List<Concept> yago2mappedConcepts = new ArrayList<Concept>();
		List<Concept> osmConcepts = new ArrayList<Concept>();
		List<Concept> yago2geoConcepts = new ArrayList<>();
		List<String> allNouns = getNouns(myQanaryQuestion.getTextualRepresentation());
		loadlistOfClasses("qanary_component-ConceptIdentifierYago/src/main/resources/YAGO2geoClasses.txt");
		loadlistOfyago2Classes("qanary_component-ConceptIdentifierYago/src/main/resources/yagoclasslist.txt");
		//osmUriMap.remove("county");
//		Bert bert = Bert.load("bert-cased-L-12-H-768-A-12");
		String myQuestion = lemmatize(myQanaryQuestion.getTextualRepresentation());

		String myQuestionNl = myQanaryQuestion.getTextualRepresentation();

		logger.info("Lemmatize Question: {}", myQuestion);
		logger.info("store data in graph {}",
				myQanaryMessage.getValues().get(new URL(myQanaryMessage.getEndpoint().toString())));
		WordNetAnalyzer wordNet = new WordNetAnalyzer("qanary_component-ConceptIdentifierYago/src/main/resources/WordNet-3.0/dict");
		osmUriMap.remove("county");


		//sliding window for string similarity
		boolean falgFound = false;
		for (String conceptLabel : yago2geoclassesmap.keySet()) {
			int wordCount = wordcount(conceptLabel);
//			System.out.println("total words :"+wordCount+"\t in : "+conceptLabel);
//			String wordsOdSentence[] = myQuestionNl.split(" ");
			List<String> ngramsOfquestion = ngrams(wordCount,myQuestion);
			JaroWinkler jw = new JaroWinkler();
			double similarityScore = 0.0;
			for(String ngramwords: ngramsOfquestion){
				similarityScore = jw.similarity(ngramwords.toLowerCase(Locale.ROOT),conceptLabel.toLowerCase(Locale.ROOT));
				System.out.println("got similarity for  ngram :"+ngramwords+"\t and concept label : "+conceptLabel+"\t is = "+similarityScore);
				if(similarityScore>0.99){
					System.out.println("====================got similarity more than 95 for  ngram :"+ngramwords+"\t and concept label : "+conceptLabel);
					falgFound = true;
					Concept concept = new Concept();
					int begin = myQuestion.toLowerCase().indexOf(ngramwords.toLowerCase());
					concept.setBegin(begin);
					concept.setEnd(begin + ngramwords.length());
					concept.setURI(yago2geoclassesmap.get(conceptLabel));
					mappedConcepts.add(concept);
					System.out.println("Identified Concepts: yago2geo:" + conceptLabel + " ============================"
							+ "ngram inside question is: " + ngramwords + " ===================");
					logger.info("identified concept: concept={} : {} : {}", concept.toString(), myQuestion,
							concept.getURI());
					break;
				}
			}

//			for(String ngramwords: ngramsOfquestion){
//				float[] emb1 = bert.embedSequence(conceptLabel.toLowerCase(Locale.ROOT));
//				float[] emb2 = bert.embedSequence(ngramwords.toLowerCase(Locale.ROOT));
//				float similarityEmbSeq = computeCosSimilarity(emb1, emb2);
//				System.out.println("got similarity for  ngram :"+ngramwords+"\t and concept label : "+conceptLabel+"\t is = "+similarityEmbSeq);
//				if(similarityEmbSeq>0.95){
//					System.out.println("====================got similarity more than 95 for  ngram :"+ngramwords+"\t and concept label : "+conceptLabel);
//					falgFound = true;
//					Concept concept = new Concept();
//					int begin = myQuestion.toLowerCase().indexOf(ngramwords.toLowerCase());
//					concept.setBegin(begin);
//					concept.setEnd(begin + ngramwords.length());
//					concept.setURI(yago2geoclassesmap.get(conceptLabel));
//					mappedConcepts.add(concept);
//					System.out.println("Identified Concepts: yago2geo:" + conceptLabel + " ============================"
//							+ "ngram inside question is: " + ngramwords + " ===================");
//					logger.info("identified concept: concept={} : {} : {}", concept.toString(), myQuestion,
//							concept.getURI());
//					break;
//				}
//			}
			/*if(falgFound){
				break;
			}*/
		}
		for (String conceptLabel : yago2classesmap.keySet()) {
			int wordCount = wordcount(conceptLabel);
//			System.out.println("total words :"+wordCount+"\t in : "+conceptLabel);
//			String wordsOdSentence[] = myQuestionNl.split(" ");
			List<String> ngramsOfquestion = ngrams(wordCount,myQuestion);
			JaroWinkler jw = new JaroWinkler();
			double similarityScore = 0.0;
			for(String ngramwords: ngramsOfquestion){
				similarityScore = jw.similarity(ngramwords.toLowerCase(Locale.ROOT),conceptLabel.toLowerCase(Locale.ROOT));
				System.out.println("got similarity for  ngram :"+ngramwords+"\t and concept label : "+conceptLabel+"\t is = "+similarityScore);
				if(similarityScore>0.99){
					System.out.println("====================got similarity more than 95 for  ngram :"+ngramwords+"\t and concept label : "+conceptLabel);
					falgFound = true;
					Concept concept = new Concept();
					int begin = myQuestion.toLowerCase().indexOf(ngramwords.toLowerCase());
					concept.setBegin(begin);
					concept.setEnd(begin + ngramwords.length());
					concept.setURI(yago2classesmap.get(conceptLabel));
					yago2mappedConcepts.add(concept);
					System.out.println("Identified Concepts: yago2:" + conceptLabel + " ============================"
							+ "ngram inside question is: " + ngramwords + " ===================");
					logger.info("identified concept: concept={} : {} : {}", concept.toString(), myQuestion,
							concept.getURI());
					break;
				}
			}
//			for(String ngramwords: ngramsOfquestion){
//				float[] emb1 = bert.embedSequence(conceptLabel.toLowerCase(Locale.ROOT));
//				float[] emb2 = bert.embedSequence(ngramwords.toLowerCase(Locale.ROOT));
//				float similarityEmbSeq = computeCosSimilarity(emb1, emb2);
//				System.out.println("got similarity for  ngram :"+ngramwords+"\t and concept label : "+conceptLabel+"\t is = "+similarityEmbSeq);
//				if(similarityEmbSeq>0.95){
//					System.out.println("====================got similarity more than 95 for  ngram :"+ngramwords+"\t and concept label : "+conceptLabel);
//					falgFound = true;
//					Concept concept = new Concept();
//					int begin = myQuestion.toLowerCase().indexOf(ngramwords.toLowerCase());
//					concept.setBegin(begin);
//					concept.setEnd(begin + ngramwords.length());
//					concept.setURI(yago2classesmap.get(conceptLabel));
//					yago2mappedConcepts.add(concept);
//					System.out.println("EmbdSeq Cosine simi Identified Concepts: yago2:" + conceptLabel + " ============================"
//							+ "ngram inside question is: " + ngramwords + " ===================");
//					logger.info("identified concept: concept={} : {} : {}", concept.toString(), myQuestion,
//							concept.getURI());
//					break;
//				}
//			}
			/*if(falgFound){
				break;
			}*/
		}
		if(!falgFound){

			for (String conceptLabel : yago2geoclassesmap.keySet()) {
//			System.out.println("============Got Inside==============");
				ArrayList<String> wordNetSynonyms = wordNet.getSynonyms(conceptLabel);
				for (String synonym : wordNetSynonyms) {
					for (String nounWord : allNouns) {
						Pattern p = Pattern.compile("\\b" + synonym + "\\b", Pattern.CASE_INSENSITIVE);
						Matcher m = p.matcher(nounWord);
//					System.out.println("for synonym : "+synonym+"\t noun word : "+nounWord);
						if (m.find()) {
							Concept concept = new Concept();
							int begin = myQuestionNl.toLowerCase().indexOf(synonym.toLowerCase());
							concept.setBegin(begin);
							concept.setEnd(begin + synonym.length());
							concept.setURI(yago2geoclassesmap.get(conceptLabel));
							mappedConcepts.add(concept);
							System.out.println("Identified Concepts: yago2geo:" + conceptLabel + " ============================"
									+ "Synonym inside question is: " + synonym + " ===================");
							logger.info("identified concept: concept={} : {} : {}", concept.toString(), myQuestion,
									concept.getURI());
							break;
						}
					}
				}
			}

//			for (String conceptLabel : yago2geoclassesmap.keySet()) {
////			System.out.println("============Got Inside==============");
//				ArrayList<String> wordNetSynonyms = wordNet.getSynonyms(conceptLabel);
//				for (String synonym : wordNetSynonyms) {
//					for (String nounWord : allNouns) {
//						float[] emb1 = bert.embedSequence(nounWord.toLowerCase(Locale.ROOT));
//						float[] emb2 = bert.embedSequence(synonym.toLowerCase(Locale.ROOT));
////						Pattern p = Pattern.compile("\\b" + synonym + "\\b", Pattern.CASE_INSENSITIVE);
////						Matcher m = p.matcher(nounWord);
////					System.out.println("for synonym : "+synonym+"\t noun word : "+nounWord);
//						float similarityEmbSeq = computeCosSimilarity(emb1, emb2);
//						if (similarityEmbSeq>0.95) {
//							Concept concept = new Concept();
//							int begin = myQuestionNl.toLowerCase().indexOf(synonym.toLowerCase());
//							concept.setBegin(begin);
//							concept.setEnd(begin + synonym.length());
//							concept.setURI(yago2geoclassesmap.get(conceptLabel));
//							mappedConcepts.add(concept);
//							System.out.println(" EmbdSeq Cosine simi Identified Concepts: yago2geo:" + conceptLabel + " ============================"
//									+ "Synonym inside question is: " + synonym + " ===================");
//							logger.info("identified concept: concept={} : {} : {}", concept.toString(), myQuestion,
//									concept.getURI());
//							break;
//						}
//					}
//				}
//			}

		}


		ArrayList<Concept> removalList = new ArrayList<Concept>();

//		for (Concept tempConcept : mappedConcepts) {
//			String conUri = tempConcept.getURI();
//			if (conUri != null) {
//				if (conUri.contains("Parking")) {
//					System.out.println("Getting in parking with question : " + myQuestionNl);
//					if (!myQuestionNl.toLowerCase().contains("parking") && !myQuestionNl.contains(" car ")) {
//						System.out.println("getting in car parking :" + myQuestion);
//						removalList.add(tempConcept);
//					}
//				}else if (conUri.contains("Park")) {
//					System.out.println("Getting in park with question : " + myQuestionNl);
//					if (myQuestionNl.toLowerCase().contains(" car park")) {
//						System.out.println("getting in car parking :" + myQuestion);
//						if(tempConcept.getURI().contains("dbpedia"))
//						tempConcept.setURI("http://dbpedia.org/ontology/Parking");
//						if(tempConcept.getURI().contains("app-lab"))
//							tempConcept.setURI("http://www.app-lab.eu/osm/ontology#Parking");
//					}
//				}
//				if(conUri.contains("http://dbpedia.org/ontology/Area")){
//					removalList.add(tempConcept);
//				}
//				if (conUri.contains("Gondola") || conUri.contains("http://dbpedia.org/ontology/List")
//						|| conUri.contains("http://dbpedia.org/ontology/Automobile")
//						|| conUri.contains("http://dbpedia.org/ontology/Altitude")
//						|| conUri.contains("http://dbpedia.org/ontology/Name")
//						|| conUri.contains("http://dbpedia.org/ontology/Population")
//						|| (conUri.contains("http://www.app-lab.eu/osm/ontology#Peak") || (conUri.contains("http://dbpedia.org/ontology/Area"))
//								&& myQuestion.toLowerCase().contains("height"))) {
//					removalList.add(tempConcept);
//				}
//			}
////			System.out.println("Concept: " + conUri);
//		}
//
		for (Concept removalC : removalList) {
			mappedConcepts.remove(removalC);
		}

		for (Concept mappedConcept : mappedConcepts) {
			// insert data in QanaryMessage.outgraph
			logger.info("apply vocabulary alignment on outgraph: {}", myQanaryQuestion.getOutGraph());
			String sparql = "" //
					+ "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
					+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
					+ "INSERT { " //
					+ "GRAPH <" + myQanaryQuestion.getOutGraph() + "> { " //
					+ "  ?a a qa:AnnotationOfConcepts . " //
					+ "  ?a oa:hasTarget [ " //
					+ "           a oa:SpecificResource; " //
					+ "             oa:hasSource    ?source; " //
					+ "             oa:hasSelector  [ " //
					+ "                    a oa:TextPositionSelector ; " //
					+ "                    oa:start \"" + mappedConcept.getBegin() + "\"^^xsd:nonNegativeInteger ; " //
					+ "                    oa:end   \"" + mappedConcept.getEnd() + "\"^^xsd:nonNegativeInteger  " //
					+ "             ] " //
					+ "  ] . " //
					+ "  ?a oa:hasBody ?mappedConceptURI;" //
					+ "     oa:annotatedBy qa:ConceptIdentifier; " //
					+ "}} " //
					+ "WHERE { " //
					+ "  BIND (IRI(str(RAND())) AS ?a) ."//
					+ "  BIND (now() AS ?time) ." //
					+ "  BIND (<" + mappedConcept.getURI() + "> AS ?mappedConceptURI) ." //
					+ "  BIND (<" + myQanaryQuestion.getUri() + "> AS ?source  ) ." //
					+ "}";
			logger.debug("Sparql query to add concepts to Qanary triplestore: {}", sparql);
			myQanaryUtils.updateTripleStore(sparql, myQanaryQuestion.getEndpoint().toString());
		}

		for (Concept mappedConcept : yago2mappedConcepts) {
			// insert data in QanaryMessage.outgraph
			logger.info("apply vocabulary alignment on outgraph: {}", myQanaryQuestion.getOutGraph());
			String sparql = "" //
					+ "PREFIX qa: <http://www.wdaqua.eu/qa#> " //
					+ "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> " //
					+ "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " //
					+ "INSERT { " //
					+ "GRAPH <" + myQanaryQuestion.getOutGraph() + "> { " //
					+ "  ?a a qa:AnnotationOfConcepts . " //
					+ "  ?a oa:hasTarget [ " //
					+ "           a oa:SpecificResource; " //
					+ "             oa:hasSource    ?source; " //
					+ "             oa:hasSelector  [ " //
					+ "                    a oa:TextPositionSelector ; " //
					+ "                    oa:start \"" + mappedConcept.getBegin() + "\"^^xsd:nonNegativeInteger ; " //
					+ "                    oa:end   \"" + mappedConcept.getEnd() + "\"^^xsd:nonNegativeInteger  " //
					+ "             ] " //
					+ "  ] . " //
					+ "  ?a oa:hasBody ?mappedConceptURI;" //
					+ "     oa:annotatedBy qa:ConceptIdentifier; " //
					+ "}} " //
					+ "WHERE { " //
					+ "  BIND (IRI(str(RAND())) AS ?a) ."//
					+ "  BIND (now() AS ?time) ." //
					+ "  BIND (<" + mappedConcept.getURI() + "> AS ?mappedConceptURI) ." //
					+ "  BIND (<" + myQanaryQuestion.getUri() + "> AS ?source  ) ." //
					+ "}";
			logger.debug("Sparql query to add concepts to Qanary triplestore: {}", sparql);
			myQanaryUtils.updateTripleStore(sparql, myQanaryQuestion.getEndpoint().toString());
		}
		return myQanaryMessage;
	}

}
