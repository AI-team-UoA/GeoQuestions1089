package eu.wdaqua.qanary.tagmedisambiguate;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import org.apache.jena.query.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

public class TagmeTest {
    private final String tagMeKey = "150907b3-f257-4d8f-b22b-6d0e6c72f53d-843339462";
    private final String wikipediaLink = "https://en.wikipedia.org/wiki/";
    private final String yagoLink = "http://yago-knowledge.org/resource/";
    public final String yagoEndpoint = "http://pyravlos1.di.uoa.gr:8890/sparql";
    public final String dbpediaEndpoint = "https://dbpedia.org/sparql";
    public final String dbpediaLink = "http://dbpedia.org/resource/";
    public final String yago2geoOnlyEndpoint = "http://test.strabon.di.uoa.gr/yago2geo/";

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
        for (CoreMap sentence : sentences) {String yagoLink = "http://yago-knowledge.org/resource/";
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


    public static boolean getInstances(String sparqlQuery, String endpointURI) {

        List<String> retValues = new ArrayList<String>();
        Query query = QueryFactory.create(sparqlQuery);
        System.out.println("sparql query :" + query.toString());
        QueryExecution exec = QueryExecutionFactory.sparqlService(endpointURI, query);
        ResultSet results = ResultSetFactory.copyResults(exec.execSelect());

        if (!results.hasNext()) {
            System.out.println("There is no next!");
            return false;
        } else {
               return true;

        }
    }

    public static List<String> getPredicates(String sparqlQuery, String endpointURI) {

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
                String predicate = qs.getResource("p").toString();
                retValues.add(predicate);
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

    public static void main(String[] s){
        try{


           /* String yago2geoOnlyEndpoint = "http://pyravlos1.di.uoa.gr:8080/geoqa/Query";
            String query = "select distinct ?x where { ?x ?p ?o } LIMIT 10";
            System.out.println("response from yago2geo endpoint : "+getInstances(query,yago2geoOnlyEndpoint));*/
           /* List<String> entitiesList = new ArrayList<String>();
            String yagoLink = "http://yago-knowledge.org/resource/";
            String tagMeKey = "150907b3-f257-4d8f-b22b-6d0e6c72f53d-843339462";
            TagMeRequest tagMeRequest = new TagMeRequest(tagMeKey);*/
            BufferedReader br = new BufferedReader(new FileReader("/home/dharmenp/y2gclasses.txt"));
//            BufferedWriter bw = new BufferedWriter(new FileWriter("/home/dharmenp/y2gpredicates.txt"));
            String question = "";
            while((question = br.readLine())!=null){
                String classLabel = question.substring(question.lastIndexOf("/"));
                BufferedWriter bw = new BufferedWriter(new FileWriter("/home/dharmenp/yago2geoproperties/"+classLabel+".txt"));
                List<String> predicates = getPredicates("select distinct ?p where { ?x a <"+question+">; ?p ?o. } ","http://pyravlos2.di.uoa.gr:8080/yago2geo/Query");
                for(String predicate: predicates){

                    String label = predicate.substring(predicate.lastIndexOf("/")+1);
                    if(label.contains("_")) {
                        label = label.replace("has", "");
                        label = label.replace("_", " ");
                    }
                    if(label.contains("geosparql")){
                        label = label.substring(label.lastIndexOf("#")+1);
                        label = label.replace("sf","");
                    }
                    if(!predicate.contains("ns#type") && !predicate.contains("#hasGeometry")){
                        bw.write(predicate+" , "+label);
                        bw.newLine();
                    }

                }
                bw.close();
                /*TagMeResponse response = tagMeRequest.doRequest(question);
                ArrayList<NedAnnotation> annotations = response.getAnnotations();
                // Extract entities
                ArrayList<TagMeDisambiguate.Link> links = new ArrayList<TagMeDisambiguate.Link>();

                for (NedAnnotation ann : annotations) {
                    if (ann.getTitle() != null && !ann.getTitle().contains("(")) {
                        String yago2Entity = yagoLink + ann.getTitle();
                    }
                }*/
            }
            br.close();
//            bw.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

}
