package eu.wdaqua.qanary.relationdetection;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import eu.wdaqua.qanary.commons.QanaryQuestion;

/**
 * processor of question containing geospatial relation information
 * 
 * @author AnBo
 *
 */
@Component
public class RelationDetector {
	private static final Logger logger = LoggerFactory.getLogger(RelationDetector.class);

	/**
	 * check for the existence of a geospatial relation in the question
	 * 
	 * @param myQanaryQuestion
	 * @return
	 * @throws Exception
	 */
	public List<RelationDetectorAnswer> process(QanaryQuestion<String> myQanaryQuestion) throws Exception {
		String myQuestion = myQanaryQuestion.getTextualRepresentation();
		return this.process(myQuestion);
	}

	
	
	public static List<String> getVerbs(String documentText) {
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
				if (pos.contains("VB")|| pos.contains("IN") || pos.contains("VP") || pos.contains("VBP") || pos.contains("VBZ")) {
					postags.add(token.get(LemmaAnnotation.class));
				}
			}
		}
		if(documentText.contains("crosses"))
			postags.add("crosses");
		System.out.println("Postags: "+postags);
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
		System.out.println("lemmetized question: "+lemmetizedQuestion);
		return lemmetizedQuestion;
	}
	
	public List<Integer> getIndexListOfSpatialRelation(String word, String myQuestion) {
		List<Integer> indexesOfSpatialRelation = new ArrayList<Integer>();
		Pattern p = Pattern.compile("\\b" + word + "\\b", Pattern.CASE_INSENSITIVE);
		Matcher m;
		if(myQuestion.contains("crosses")||myQuestion.contains("includes") || myQuestion.contains("located"))
			m = p.matcher(myQuestion);
		else
			m = p.matcher(lemmatize(myQuestion));

		while (m.find()) {
			indexesOfSpatialRelation.add(m.start());
		}
		if (indexesOfSpatialRelation.size() == 0)
			return null;
		return indexesOfSpatialRelation;
	}

	public List<Integer> getIndexListOfSpatialRelationLem(String word, String myQuestion) {
		List<Integer> indexesOfSpatialRelation = new ArrayList<Integer>();
		Pattern p = Pattern.compile("\\b" + word + "\\b", Pattern.CASE_INSENSITIVE);
		Matcher m;
		m = p.matcher(myQuestion);
		while (m.find()) {
			indexesOfSpatialRelation.add(m.start());
		}
		if (indexesOfSpatialRelation.size() == 0)
			return null;
		return indexesOfSpatialRelation;
	}

	/**
	 * check for the existence of a geospatial relation in the question
	 * 
	 * @param myQuestion
	 * @return
	 * @throws URISyntaxException
	 */
	public List<RelationDetectorAnswer> process(String myQuestion) throws URISyntaxException {
		logger.debug("The relationDetection Component is running with this question: {}", myQuestion);
		// for all allowed locations, check their existence for all of their
		// allowed labels
		List<RelationDetectorAnswer> relationDetectorAnswerList = null;
		List<String> filteredPosTags = new ArrayList<String>();
		String lemmatizedQuestion = lemmatize(myQuestion);
		// ResultSet r;
		// String questionForProcessing = myQuestion;
		//
		// String sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> "
		// + "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
		// + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "//
		// + "SELECT ?start ?end ?uri " + "FROM <" +
		// myQanaryQuestion.getInGraph() + "> " //
		// + "WHERE { " //
		// + " ?a a qa:AnnotationOfConcepts . " + "?a oa:hasTarget [ "
		// + " a oa:SpecificResource; " //
		// + " oa:hasSource ?q; " //
		// + " oa:hasSelector [ " //
		// + " a oa:TextPositionSelector ; " //
		// + " oa:start ?start ; " //
		// + " oa:end ?end " //
		// + " ] " //
		// + " ] . " //
		// + " ?a oa:hasBody ?uri ; " + " oa:annotatedBy ?annotator " //
		// + "} " + "ORDER BY ?start ";
		//
		// r = myQanaryUtils.selectFromTripleStore(sparql);
		//
		// while (r.hasNext()) {
		// QuerySolution s = r.next();
		// questionForProcessing =
		// questionForProcessing.substring(s.getLiteral("start").getInt(),
		// s.getLiteral("end").getInt());
		// }
		//
		// sparql = "PREFIX qa: <http://www.wdaqua.eu/qa#> "
		// + "PREFIX oa: <http://www.w3.org/ns/openannotation/core/> "
		// + "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> "//
		// + "SELECT ?start ?end ?uri " + "FROM <" +
		// myQanaryQuestion.getInGraph() + "> " //
		// + "WHERE { " //
		// + " ?a a qa:AnnotationOfInstance . " + "?a oa:hasTarget [ "
		// + " a oa:SpecificResource; " //
		// + " oa:hasSource ?q; " //
		// + " oa:hasSelector [ " //
		// + " a oa:TextPositionSelector ; " //
		// + " oa:start ?start ; " //
		// + " oa:end ?end " //
		// + " ] " //
		// + " ] . " //
		// + " ?a oa:hasBody ?uri ; " + " oa:annotatedBy ?annotator " //
		// + "} " + "ORDER BY ?start ";
		//
		// r = myQanaryUtils.selectFromTripleStore(sparql);
		//
		//
		// while (r.hasNext()) {
		// QuerySolution s = r.next();
		// questionForProcessing =
		// questionForProcessing.substring(s.getLiteral("start").getInt(),
		// s.getLiteral("end").getInt());
		// }
		int count = 0, index = -1;
		// Filter the POS tag that are verb, adverb, adjective to restrict input
		// for relation detector
//		Properties prop = new Properties();
//		prop.setProperty("annotators", "tokenize,ssplit,pos");
//		StanfordCoreNLP pipeline = new StanfordCoreNLP(prop);
//		Annotation document = new Annotation(myQuestion);
//		// annnotate the document
//		pipeline.annotate(document);
//		List<CoreMap> sentences = document.get(SentencesAnnotation.class);
//		for (CoreMap sentence : sentences) {
//			// traversing the words in the current sentence
//			// a CoreLabel is a CoreMap with additional token-specific methods
//			for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
//				// this is the text of the token
//				String word = token.get(TextAnnotation.class);
//				// this is the POS tag of the token
//				String pos = token.get(PartOfSpeechAnnotation.class);
//				if (pos.contains("VB") || pos.contains("IN")) {
//					filteredPosTags.add(word);
//				}
//				System.out.println("word: " + word + " pos: " + pos);
//			}
//		}

		for (GeospatialRelation myGeospatialRelation : GeospatialRelation.values()) {
			for (String textualRepresentation : myGeospatialRelation.getLabels()) {
			//	for (String verb : filteredPosTags) {
					Pattern p = Pattern.compile("\\b" + textualRepresentation + "\\b", Pattern.CASE_INSENSITIVE);
					Matcher m = p.matcher(myQuestion);
					Matcher m1 = p.matcher(lemmatizedQuestion);
					if (m.find()) {
						if (count == 0) {
							relationDetectorAnswerList = new ArrayList<RelationDetectorAnswer>();
							count++;
						}
						List<Integer> indexesOfSpatialRelation = getIndexListOfSpatialRelation(textualRepresentation,
								myQuestion);
						if (indexesOfSpatialRelation != null) {
							for (Integer indexOfSpatialRelation : indexesOfSpatialRelation) {
								logger.info("processed question: {}, found {} at{}", myQuestion, myGeospatialRelation,
										indexOfSpatialRelation);
								relationDetectorAnswerList.add(
										new RelationDetectorAnswer(true, myGeospatialRelation, indexOfSpatialRelation,textualRepresentation));
							}
						}
					}
					else if (m1.find()) {
						if (count == 0) {
							relationDetectorAnswerList = new ArrayList<RelationDetectorAnswer>();
							count++;
						}
						List<Integer> indexesOfSpatialRelation = getIndexListOfSpatialRelationLem(textualRepresentation,
								lemmatizedQuestion);
						if (indexesOfSpatialRelation != null) {
							for (Integer indexOfSpatialRelation : indexesOfSpatialRelation) {
								logger.info("processed question: {}, found {} at{}", lemmatizedQuestion, myGeospatialRelation,
										indexOfSpatialRelation);
								relationDetectorAnswerList.add(
										new RelationDetectorAnswer(true, myGeospatialRelation, indexOfSpatialRelation,textualRepresentation));
							}
						}
					}
				//}
			}
		}
		return relationDetectorAnswerList;
	}

}
