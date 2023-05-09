#########################################################################################################
# @Author: --
# @Description: Query Yago2 to analyse the ontology and fetch the results in readable format
# @Usage: Write down the place type ontology with their description to perform ontology mapping
# @Usage: Write down the place properties with their description to perform ontology mapping
############################################################################################
import logging

from RequestHandler import Configuration, RequestHandler
from SPARQL_Generator import SPARQL, Results

logging.basicConfig(level=logging.INFO)

config = Configuration(ip='45.113.235.161', port='3030', datasource='ds')
rq = RequestHandler(config)

geoentity = SPARQL(where_clause='?class rdfs:subClassOf yago:yagoGeoEntity.'
                                '\n?class rdfs:label ?label.'
                                '\nFILTER (lang(?label) = "eng").'
                                '\n?class yago:hasGloss ?gloss')
geoentity_results = rq.request(geoentity.formulate())
type_description_results = Results(geoentity_results)
type_description_df = type_description_results.get_dataframe()
logging.info('type description dataframe (head): {}'.format(type_description_df.head()))
type_description_df.to_csv('data/type-description.csv')
logging.info('type description results are saved in data/type-description.csv')
