package eu.wdaqua.qanary.geosparqlgenerator;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.Constituent;
import edu.stanford.nlp.trees.LabeledScoredConstituentFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class RootFinderExample {
	private StringBuilder out;
	private Set<IndexedWord> used;

//	private void formatSGNode(SemanticGraph sg, IndexedWord node, int spaces) {
//		used.add(node);
//		String oneline = formatSGNodeOneline(sg, node);
//		boolean toolong = (spaces + oneline.length() > width);
//		boolean breakable = sg.hasChildren(node);
//		if (toolong && breakable) {
//			formatSGNodeMultiline(sg, node, spaces);
//		} else {
//			out.append(oneline);
//		}
//	}

//	public String formatSemanticGraph(SemanticGraph sg) {
//		if (sg.vertexSet().isEmpty()) {
//			return "[]";
//		}
//		out = new StringBuilder(); // not thread-safe!!!
//		used = Generics.newHashSet();
//		if (sg.getRoots().size() == 1) {
////			formatSGNode(sg, sg.getFirstRoot(), 1);
//		} else {
//			int index = 0;
//			for (IndexedWord root : sg.getRoots()) {
//				index += 1;
//				out.append("root_").append(index).append("> ");
//				formatSGNode(sg, root, 9);
//				out.append("\n");
//			}
//		}
//		String result = out.toString();
//		if (!result.startsWith("[")) {
//			result = "[" + result + "]";
//		}
//		return result;
//	}

//	public static void traversalPrimitive(IndexedWord node) {
//		for(IndexedWord childNode: node.)
//	}
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
		for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
			String pos = token.get(CoreAnnotations.PartOfSpeechAnnotation.class);
//			System.out.println("token : "+token.originalText());
			if (pos.contains("JJS")) {
				if(token.originalText().equalsIgnoreCase("nearest")||token.originalText().equalsIgnoreCase("closest")) {
					retVal = true;

				}
			}
		}
	}
	return retVal;
}

	public static ArrayList<String> testConstituents(String question){
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
				System.out.println("found constituent: "+constituent.toString());
				retValues.add(tree.getLeaves().subList(constituent.start(), constituent.end()+1).toString());
				System.out.println(tree.getLeaves().subList(constituent.start(), constituent.end()+1));
			}
		}
		return retValues;
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
			dependencies.prettyPrint();
			List<SemanticGraphEdge>	edges = dependencies.edgeListSorted();

			for(SemanticGraphEdge edge: edges){


				if(edge.getSource().toString().contains("JJS") && edge.getDependent().toString().contains("NN")){
					System.out.println(" Source ================================================= Dest ");
					System.out.println("edge : "+edge.toString());
					System.out.println("source: "+edge.getSource());
					System.out.println("relation: "+edge.getRelation());
					System.out.println("dependent :"+edge.getDependent());
				}
				else if (edge.getSource().toString().contains("NN") && edge.getDependent().toString().contains("JJS")){
					System.out.println("Dest ================================================= Source");
					System.out.println("edge : "+edge.toString());
					System.out.println("source: "+edge.getSource());
					System.out.println("relation: "+edge.getRelation());
					System.out.println("dependent :"+edge.getDependent());
				}
			}
		}
		return retVal;
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
				}
			}
		}
		return retVal;
	}
	public static void processFile(){
		try{
			BufferedReader br = new BufferedReader(new FileReader("/home/dharmenp/questionsList.txt"));
			BufferedWriter bw = new BufferedWriter(new FileWriter("/home/dharmenp/questionsWithQPPhrashe.txt"));
			int count = 0;
			String line = "";
			while((line = br.readLine())!=null){
				String nps = testConstituents(line).toString();
//				System.out.println("nps : "+nps);
				bw.newLine();
				bw.write(line+":"+nps);
			}
			br.close();
			bw.close();
		}catch (Exception e){
			e.printStackTrace();
		}
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
	public static void main(String[] args) throws IOException {


		String question = "What's the name of the river that runs through London?";
//		int testVal = Integer.parseInt("twenty");
//		System.out.println("checking is : "+testVal);
		System.out.println("Does question ask nearest/closest ? : "+getADJPConstituents(question));
//		testConstituents(question);
//		processFile();
		/*MyGraph myGraph = new MyGraph();

		// build pipeline
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner, depparse");
		StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
		BufferedReader br = new BufferedReader(new FileReader("/home/dharmenp/somequestions.txt"));
		BufferedWriter bw = new BufferedWriter(new FileWriter("/home/dharmenp/somequestionsWithDependencies.csv"));
		String line = "";
		ArrayList<String> questionList = new ArrayList<String>();
		while((line = br.readLine())!=null){
			questionList.add(line);
		}
		br.close();
		String text = "Which pubs in Dublin are near Guinness Brewery?";
		for(String question:questionList){
			Annotation annotation = new Annotation(question);
			pipeline.annotate(annotation);
			List<CoreMap> sentences = annotation.get(SentencesAnnotation.class);
			bw.write(question+",");
			for (CoreMap sentence : sentences) {
				SemanticGraph dependencies = sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
				dependencies.prettyPrint();
				List<SemanticGraphEdge>	edges = dependencies.edgeListSorted();

				for(SemanticGraphEdge edge: edges){
//					System.out.println("edge : "+edge.toString());
//					System.out.println("source: "+edge.getSource());
//					System.out.println("relation: "+edge.getRelation());
//					System.out.println("dependent :"+edge.getDependent());

					bw.write(edge.toString()+",");
				}
			}
			bw.newLine();
		}
		bw.close();
*/


		System.out.println("finished");
	}

}