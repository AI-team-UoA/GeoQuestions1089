package eu.wdaqua.qanary.conceptidentifier;

import org.apache.jena.query.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class DBpediaConceptsAndURIs {

	private static final Logger logger = LoggerFactory.getLogger(ConceptIdentifier.class);
	private static List<String> allConceptWordUri = new ArrayList<String>();
	private static Map<String, String> conceptURIMap = new HashMap<String, String>();

	public static Map<String, String> getDBpediaConceptsAndURIs() {
		return conceptURIMap;
	}

	/**
	 * compute a map of DBpedia concepts which will be identified within the
	 * 
	 *
	 */
	static {

		// TODO: move endpoint definition to application.properties
		String endpoint = "http://dbpedia.org/sparql";

		// TODO: move timeout definition to application.properties
		int timeout = 60 * 1000;

		QueryExecution objectToExec;

		// TODO: move this static query to a file and use QueryFactory.read()
		// query computes all geospatial concepts (with labels) in DBpedia,
		// i.e., dbo:place and all sub-classes
		// String query = "" //
		// + "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>" //
		// + "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>" //
		// + "PREFIX dbo: <http://dbpedia.org/ontology/>" //
		// + "PREFIX owl: <http://www.w3.org/2002/07/owl#>" //
		// + "SELECT DISTINCT ?type STR(?label) " //
		// + "FROM <http://dbpedia.org>" //
		// + "WHERE {" //
		// + " { " // only places concepts being sub-class of dbo:place
		// + " ?type (rdfs:subClassOf)+ dbo:Place . " //
		// + " }" //
		// + " UNION " //
		// + " { " // add all parent classes, should only be dbo:place
		// + " ?x rdfs:subClassOf dbo:Place, ?type. " //
		// + " }" //
		// + " ?type rdfs:label ?label ." //
		// // TODO: add language property to application properties
		// + " FILTER langMatches(lang(?label),'en')." // only English
		// + "}"; //
		// System.out.println(DBpediaConceptsAndURIs.class.getResource("DBPediaConceptGetInEnglish.sparql").toString());
//		String queryString = "";
//		try {
//			InputStream is = DBpediaConceptsAndURIs.class.getResourceAsStream("/config/DBPediaConceptGetInEnglish.sparql");
//			BufferedReader br = new BufferedReader(new InputStreamReader(is));
//			String line = "";
//
//			while ((line = br.readLine()) != null) {
//				queryString +=line +"\n";
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
		Query q = QueryFactory.create(QueryFactory.read("qanary_component-ConceptIdentifierYago/src/main/resources/DBPediaConceptGetInEnglish.sparql"));
		logger.info("generated query: {}", q.toString());
		objectToExec = QueryExecutionFactory.sparqlService(endpoint, q.toString());
		objectToExec.setTimeout(timeout);

		int iCount = 0;
		ResultSet results = null;

		try {
			results = objectToExec.execSelect();
			while (results.hasNext()) {
				// Get Result
				QuerySolution qs = results.next();

				// Get Variable Names
				Iterator<String> itVars = qs.varNames();

				// Count
				iCount++;

				// Display Result
				String uri = "", label = "";
				while (itVars.hasNext()) {
					String szVar = itVars.next().toString();
					String szVal = qs.get(szVar).toString();
					if (szVar.equalsIgnoreCase("type")) {
						uri = szVal;
					} else {
						label = szVal;
					}
					//logger.info("{}. [{}]: {}", iCount, szVar, szVal);
				}
				allConceptWordUri.add(label);
				conceptURIMap.put(label, uri);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
