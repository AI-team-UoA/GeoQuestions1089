package eu.wdaqua.qanary.geosparqlgenerator;

import org.apache.jena.query.*;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class EvaluationTest {

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



    public static void testTypeA(String goldQuery,String genQuery){

    }

    public static void executeAskQuery(String filenametoRead,String filename ){
        try{
            BufferedReader br = new BufferedReader(new FileReader(filenametoRead));
            BufferedWriter bw = new BufferedWriter(new FileWriter(filename));
            String prefixes = "PREFIX geo: <http://www.opengis.net/ont/geosparql#> " +
                    "PREFIX geof: <http://www.opengis.net/def/function/geosparql/> " +
                    "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                    "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                    "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
                    "PREFIX yago: <http://yago-knowledge.org/resource/> " +
                    "PREFIX y2geor: <http://kr.di.uoa.gr/yago2geo/resource/> " +
                    "PREFIX y2geoo: <http://kr.di.uoa.gr/yago2geo/ontology/> " +
                    "PREFIX strdf: <http://strdf.di.uoa.gr/ontology#> " +
                    "PREFIX uom: <http://www.opengis.net/def/uom/OGC/1.0/> " +
                    "PREFIX owl: <http://www.w3.org/2002/07/owl#> ";
            String queryString = "";
            while((queryString = br.readLine())!=null){
                if(!queryString.contains("can not answer")){
                    Query query = QueryFactory.create(prefixes+" "+queryString);
                    System.out.println("sparql query :" + query.toString());
                    QueryExecution exec = QueryExecutionFactory.sparqlService("http://pyravlos2.di.uoa.gr:8080/yago2geo/Query", query);
                    if(!queryString.contains("select")){
                        boolean answer = exec.execAsk();
                        String answerString = answer+"";
                        bw.write(answerString);
                    }else {
                        bw.write("0");
                    }
                    bw.newLine();
                }
                else {
                    System.out.println("inside can not answer");
                }
            }
            bw.close();
            br.close();
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void main(String s[]){
            executeAskQuery("/home/dharmenp/typeBgeneratedQueries.txt","/home/dharmenp/typeBgeneratedQueriesAnswers.txt");
    }
}
