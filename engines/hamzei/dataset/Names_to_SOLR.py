#########################################################################################################
# @Author: --
# @Description: Read RDF files and insert names (toponyms) and their URIs to SOLR for fast text search
# @Usage: Insert data to SOLR from RDF files -- only toponyms and their URIs
#########################################################################################################

import logging
import uuid

import pysolr

from RequestHandler import Configuration, RequestHandler

logging.basicConfig(level=logging.INFO)

SPARQL_TEMPLATE = 'SELECT DISTINCT ?uri ?label WHERE {' \
                  ' VALUES ?uri { <URIS> }  ' \
                  '?uri  <http://www.w3.org/2000/01/rdf-schema#label> ?label.' \
                  ' filter langMatches( lang(?label), "eng" ) }'

solr = pysolr.Solr('http://88.197.53.173:8983/solr/places/', always_commit=True)
config = Configuration(ip='88.197.53.173', port='3030', datasource='ds')
rq = RequestHandler(config)

names = [
    "http://kr.di.uoa.gr/yago2geo/ontology/hasGAG_Name",
    "http://kr.di.uoa.gr/yago2geo/ontology/hasOSNI_Name",
    "http://kr.di.uoa.gr/yago2geo/ontology/hasOS_Name",
    "http://kr.di.uoa.gr/yago2geo/ontology/hasOSM_Name",
    "http://kr.di.uoa.gr/yago2geo/ontology/hasOSI_Name",
    "http://kr.di.uoa.gr/yago2geo/ontology/hasGADM_Name",
    "http://kr.di.uoa.gr/yago2geo/ontology/hasONSI_Name",
    "http://www.w3.org/2000/01/rdf-schema#label"
]

solr.ping()


def dummy_insert():
    solr.add([
        {
            "id": "doc_1",
            "toponym": "just a Test",
            "uri": "http://fake_place.com/"
        },
        {
            "id": "doc_2",
            "toponym": ["Just test 2", "Test 2", "TEST2"],
            "uri": "httpL//fake_place2.com/"
        },
    ])


def dummy_delete():
    solr.delete(id=['doc_1', 'doc_2'])


def analyze(address):
    data = []
    with open(address, 'r', encoding='utf-8') as fp:
        for line in fp:
            splits = line.split('> <')
            if len(splits) == 2:
                for name in names:
                    if name in splits[1]:
                        toponym = splits[1].replace(name, '').replace('>', ''). \
                            replace('\n', '').replace('.', '').replace('"', '').replace("@eng", '').replace("@en", '').strip()
                        if toponym != '':
                            data.append({'id': str(uuid.uuid4()),
                                         'toponym': [toponym],
                                         'uri': splits[0].replace('<', '')})
            if len(data) > 500:
                solr.add(data)
                data = []
                logging.info('500 data added to SOLR')
    solr.add(data)
    data = []
    logging.info('500 data added to SOLR')


def query_yago(list_uri):
    try:
        query = SPARQL_TEMPLATE.replace('<URIS>', '\n'.join(list_uri))
        res = rq.post_query(query)
        results = {}
        for record in res['results']['bindings']:
            if record['uri']['value'] not in results.keys():
                results[record['uri']['value']] = []
            results[record['uri']['value']].append(record['label']['value'])
        return results
    except:
        print('error in getting data from Fuseki: {}'.format(list_uri))
        return {}


def analyze_yago(address):
    final_results = []
    with open(address, 'r', encoding='utf-8') as fp:
        uris = []
        for line in fp:
            splits = line.strip().split(' ')
            if len(splits) > 3:
                uri = splits[2]
                uris.append(uri)
            if len(uris) > 20:
                results = query_yago(uris)
                final_results.append(results)
                uris = []
            if len(final_results) > 500:
                data = []
                for record in final_results:
                    for key, val in record.items():
                        if val != '':
                            data.append({'id': str(uuid.uuid4()), 'toponym': val, 'uri': key})
                solr.add(data)
                final_results = []
                logging.info('500 data added to SOLR')
    data = []
    for record in final_results:
        for key, val in record.items():
            if val != '':
                data.append({'id': str(uuid.uuid4()), 'toponym': val, 'uri': key})
    solr.add(data)
    logging.info('Writing last part in SOLR')


if __name__ == '__main__':
    files = [
            #  '/home/sergios/GeoQuestions/kg/GADM_all.nt', 
            #  '/home/sergios/GeoQuestions/kg/GAG_all.nt', 
            #  '/home/sergios/GeoQuestions/kg/OS_all.nt', 
            #  '/home/sergios/GeoQuestions/kg/OSI_all.nt', 
            #  '/home/sergios/GeoQuestions/kg/OSM_all.nt', 
            #  '/home/sergios/GeoQuestions/kg/OSNI_all.nt', 
            #  '/home/sergios/GeoQuestions/kg/USA_all.nt',
             '/home/sergios/GeoQuestions/kg/yago2.nt'
             ]
    for file in files:
        print('\n\nAnalyzing file: {}'.format(file))
        analyze(file)
