package eu.wdaqua.qanary.utils;

import java.util.*;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.trees.Constituent;
import edu.stanford.nlp.trees.LabeledScoredConstituentFactory;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
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

public class CoreNLPUtilities {
    /* Avoid creating a lot of pipelines to avoid slowdown https://stanfordnlp.github.io/CoreNLP/memory-time.html#avoid-creating-lots-of-pipelines */
    static StanfordCoreNLP pipeline_tspl;
    static StanfordCoreNLP pipeline_tspd;
    static StanfordCoreNLP pipeline_tspp;
    static StanfordCoreNLP pipeline_tsp;

    static {
        var props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
        pipeline_tspl = new StanfordCoreNLP(props);

        props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, depparse");
        pipeline_tspd = new StanfordCoreNLP(props);

        props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, parse");
        pipeline_tspp = new StanfordCoreNLP(props);

        props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos");
        pipeline_tsp = new StanfordCoreNLP(props);
    }

    public static String lemmatize(String documentText) {
        // Create an empty Annotation just with the given text and run all Annotators on this text
        Annotation document = new Annotation(documentText);
        pipeline_tspl.annotate(document);

        // Iterate over all the sentences found
        var lemmatizedQuestion = new StringBuilder();
        List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            // Iterate over all tokens in a sentence
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                lemmatizedQuestion.append(token.get(LemmaAnnotation.class)).append(" ");
            }
        }

        return lemmatizedQuestion.toString();
    }

    public static ArrayList<String> tagBasedGet(String documentText, String[] tags) {
        // Create an empty Annotation just with the given text and run all Annotators on this text
        Annotation document = new Annotation(documentText);
        pipeline_tspl.annotate(document);

        // Iterate over all the sentences found
        var postags = new ArrayList<String>();
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            // Iterate over all tokens in a sentence
            for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
                String pos = token.get(PartOfSpeechAnnotation.class);
                for (var tag : tags) {
                    if (pos.contains(tag)) {
                        postags.add(token.get(LemmaAnnotation.class));
                        break;
                    }
                }
            }
        }

        return postags;
    }

    public static List<String> getVerbs(String documentText) {
        var postags = tagBasedGet(documentText, new String[] {"VB", "IN", "VP", "VBP", "VBZ"});

        if(documentText.contains("crosses"))
            postags.add("crosses");

        return postags;
    }

    public static List<String> getVerbsNounsAdjectivesPrepositions(String documentText) {
        return tagBasedGet(documentText, new String[] {"VB", "IN", "NN", "JJ"});
    }

    public static List<String> getNouns(String documentText) {
        return tagBasedGet(documentText, new String[] {"NN"});
    }

    public static List<String> getNounsExtended(String documentText) {
        return tagBasedGet(documentText, new String[] {"NN", "JJ", "NP", "NNP"});
    }

    public static List<String> getW(String documentText) {
        return tagBasedGet(documentText, new String[] {"WRB"});
    }

    public static boolean isJJSNN(String documentText) {
        // Create an empty Annotation just with the given text and run all Annotators on this text
        Annotation document = new Annotation(documentText);
        pipeline_tspd.annotate(document);

        // Iterate over all the sentences found
        boolean retVal = false;
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
                }
            }
        }

        return retVal;
    }

    public static boolean isRBSMost(String documentText) {
        // Create an empty Annotation just with the given text and run all Annotators on this text
        Annotation document = new Annotation(documentText);
        pipeline_tspd.annotate(document);

        // Iterate over all the sentences found
        boolean retVal = false;
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            SemanticGraph dependencies = sentence.get(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class);
            List<SemanticGraphEdge> edges = dependencies.edgeListSorted();
            for (SemanticGraphEdge edge : edges) {
                if ((edge.getSource().toString().contains("JJ") || edge.getSource().toString().contains("NNS")) && edge.getDependent().toString().contains("RBS")) {
                    retVal = true;
                } else if (edge.getSource().toString().contains("NN") && edge.getDependent().toString().contains("JJS")) {
                    retVal = true;
                }
            }
        }

        return retVal;
    }

    public static ArrayList<String> getADJPConstituents(String question){
        // build annotation for a review
        Annotation annotation = new Annotation(question);
        pipeline_tspp.annotate(annotation);

        // get tree
        Tree tree = annotation.get(CoreAnnotations.SentencesAnnotation.class).get(0).get(TreeCoreAnnotations.TreeAnnotation.class);
//		System.out.println(tree);

        var retValues = new ArrayList<String>();
        Set<Constituent> treeConstituents = tree.constituents(new LabeledScoredConstituentFactory());
        for (Constituent constituent : treeConstituents) {
//			System.out.println("Constituent : "+constituent.label() + " : : "+constituent.value());
            if (constituent.label() != null && (constituent.label().toString().equals("ADJP"))) {
                System.out.println("found constituent: "+constituent.toString());
                retValues.add(tree.getLeaves().subList(constituent.start(), constituent.end()+1).toString());
                System.out.println(tree.getLeaves().subList(constituent.start(), constituent.end()+1));
            }
        }
        return retValues;
    }

    public static String getJJS(String documentText) {
        // Create an empty Annotation just with the given text and run all Annotators on this text
        Annotation document = new Annotation(documentText);
        pipeline_tspd.annotate(document);

        // Iterate over all the sentences found
        String retVal = "";
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
                } else if (edge.getSource().toString().contains("NN") && edge.getDependent().toString().contains("JJS")) {
                    retVal = edge.getDependent().toString();
                    retVal = retVal.substring(0, retVal.indexOf('/') );
                }
            }
        }

        System.out.println("ret value : " + retVal);
        return retVal;
    }

    public static boolean isJJSClosestOrNearest(String documentText) {
        // Create an empty Annotation just with the given text and run all Annotators on this text
        Annotation document = new Annotation(documentText);
        pipeline_tsp.annotate(document);

        // Iterate over all the sentences found
        boolean retVal = false;
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

    public static String getPosTagofWord(String documentText, String word) {
        // Create an empty Annotation just with the given text and run all Annotators on this text
        Annotation document = new Annotation(documentText);
        pipeline_tsp.annotate(document);

        // Iterate over all  the sentences found
        String postags = "";
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

    public static ArrayList<String> getQPConstituents(String question){
        // Create an empty Annotation just with the given text and run all Annotators on this text
        Annotation annotation = new Annotation(question);
        pipeline_tspp.annotate(annotation);

        // get tree
        Tree tree = annotation.get(CoreAnnotations.SentencesAnnotation.class).get(0).get(TreeCoreAnnotations.TreeAnnotation.class);
//		System.out.println(tree);

        var retValues = new ArrayList<String>();
        Set<Constituent> treeConstituents = tree.constituents(new LabeledScoredConstituentFactory());
        for (Constituent constituent : treeConstituents) {
//			System.out.println("Constituent : "+constituent.label() + " : : "+constituent.value());
            if (constituent.label() != null && (constituent.label().toString().equals("QP"))) {
//				System.out.println("found constituent: "+constituent.toString());
                retValues.add(tree.getLeaves().subList(constituent.start(), constituent.end()+1).toString());
                System.out.println(tree.getLeaves().subList(constituent.start(), constituent.end()+1));
            }
        }

        return retValues;
    }

    public static String isCDNNS(String documentText) {
        // Create an empty Annotation just with the given text and run all Annotators on this text
        Annotation document = new Annotation(documentText);
        pipeline_tspd.annotate(document);

        // Iterate over all the sentences found
        String retVal = "";
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
        // Create an empty Annotation just with the given text and run all Annotators on this text
        Annotation document = new Annotation(documentText);
        pipeline_tspd.annotate(document);

        // Iterate over all the sentences found
        double retVal = 0.0;
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

    public static String getMostRBS(String documentText) {
        // Create an empty Annotation just with the given text and run all Annotators on this text
        Annotation document = new Annotation(documentText);
        pipeline_tspd.annotate(document);

        // Iterate over all the sentences found
        String retVal = "";
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
        // Build annotation for a review
        Annotation annotation = new Annotation(question);
        pipeline_tspp.annotate(annotation);

        // get tree
        Tree tree = annotation.get(CoreAnnotations.SentencesAnnotation.class).get(0).get(TreeCoreAnnotations.TreeAnnotation.class);
//        System.out.println(tree);

        var retValues = new ArrayList<String>();
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
        // Create an empty Annotation just with the given text and run all Annotators on this text
        Annotation document = new Annotation(documentText);
        pipeline_tsp.annotate(document);

        // Iterate over all the sentences found
        String postags = "";
        List<CoreMap> sentences = document.get(SentencesAnnotation.class);
        for (CoreMap sentence : sentences) {
            // Iterate over all tokens in a sentence
            for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
                String pos = token.get(PartOfSpeechAnnotation.class);
                if (pos.contains("CC")) {
                    System.out.println("CC postag : " + token.originalText());
                    postags = token.originalText();
                }
            }
        }

        return postags;
    }

    public static ArrayList<String> getNPConstituents(String question){
        // Build annotation for a review
        Annotation annotation = new Annotation(question);
        pipeline_tspp.annotate(annotation);

        // get tree
        Tree tree = annotation.get(CoreAnnotations.SentencesAnnotation.class).get(0).get(TreeCoreAnnotations.TreeAnnotation.class);
//        System.out.println(tree);

        var retValues = new ArrayList<String>();
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

    public static int wordcount(String string)	{
        return new StringTokenizer(string).countTokens();
    }
}
