#########################################################################################################
# @Author: --
# @Description: Client class to connect to GeoSPARQL Fuseki server.
# @Usage: Send a query to server and receive the results.
#########################################################################################################
from RequestHandler import Configuration, RequestHandler

config = Configuration(ip='45.113.235.161', port='3031', datasource='query')
rq = RequestHandler(config)
sample_query = 'PREFIX geosparql: <http://www.opengis.net/ont/geosparql#> \n' + \
               'PREFIX geof: <http://www.opengis.net/def/function/geosparql/> \n' + \
               'PREFIX db: <http://kr.di.uoa.gr/yago2geo/ontology/> \n' + \
               'PREFIX spatialF: <http://jena.apache.org/function/spatial#> \n' + \
               'PREFIX uom: <http://www.opengis.net/def/uom/OGC/1.0/> \n' + \
               'PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> \n' + \
               'PREFIX owl:<http://www.w3.org/2002/07/owl#> \n' + \
               'select ?pName ?pID ?gName ?gID where { \n' \
               '?g geosparql:hasGeometry ?gG. \n' \
               ' ?gG geosparql:asWKT ?gWKT. \n' \
               ' ?g <http://www.w3.org/2000/01/rdf-schema#label> ?gName. \n' \
               ' ?g <http://kr.di.uoa.gr/yago2geo/ontology/hasOSM_ID> ?gID. \n' \
               ' ?p geosparql:hasGeometry ?pG. \n' \
               ' ?pG geosparql:asWKT ?pWKT. \n' \
               ' ?p <http://www.w3.org/2000/01/rdf-schema#label> ?pName. \n' \
               ' ?p <http://kr.di.uoa.gr/yago2geo/ontology/hasOSM_ID> ?pID. \n' \
               ' filter(geof:distance(?gWKT, ?pWKT, uom:metre)<1000). \n' \
               '} limit 5'

sample_query = 'PREFIX geosparql: <http://www.opengis.net/ont/geosparql#> ' \
               '\nPREFIX geof: <http://www.opengis.net/def/function/geosparql/>' \
               '\nPREFIX units: <http://www.opengis.net/def/uom/OGC/1.0/>' \
               '\nPREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>' \
               '\nSELECT ?x0  ' \
               '\n WHERE {' \
               '\n VALUES ?c1  {<http://kr.di.uoa.gr/yago2geo/resource/AnfieldStadium274>}. ' \
               '\n ?c1 geosparql:hasGeometry ?c1G .' \
               '\n ?c1G geosparql:asWKT ?c1GEOM.' \
               '\n ?x0 rdf:type ?x0TYPE;' \
               '\n geosparql:hasGeometry ?x0G .' \
               '\n ?x0G geosparql:asWKT ?x0GEOM.' \
               '\n VALUES ?x0TYPE {<http://kr.di.uoa.gr/yago2geo/ontology/OSM_amenity_pub> ' \
               '<http://yago-knowledge.org/resource/wordnet_barroom_102796995>} . ' \
               '\n FILTER (geof:distance(?x0GEOM, ?c1GEOM, units:metre) < 5000).}'
result = rq.post_query(sample_query)
print(result)
