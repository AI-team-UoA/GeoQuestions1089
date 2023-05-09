#########################################################################################################
# @Author: --
# @Description: Convert GeoJSON results to RDF
# @Usage: Create RDF data for specific key value tags from OSM
#########################################################################################################
import json
import logging
import re
import uuid
from random import randint

import pandas as pd
from cleantext import clean
from rdflib import Namespace, Graph
from rdflib.namespace import OWL, RDF, RDFS
from rdflib.term import URIRef, Literal

logging.basicConfig(level=logging.INFO)

graph = Graph()
GEO = Namespace("http://www.opengis.net/ont/geosparql#")

YAGO2GEO_ONT = Namespace("http://kr.di.uoa.gr/yago2geo/ontology/")
OSM_BASE = URIRef('http://kr.di.uoa.gr/yago2geo/ontology/OSM_Feature')
YAGO2GEO_RES = Namespace("http://kr.di.uoa.gr/yago2geo/resource/")

POINT_WKT = "<http://www.opengis.net/def/crs/EPSG/0/4326> POINT ({0} {1})"

RDFS_NAME = RDFS.label
OWL_CLASS = OWL.term('class')
RDF_TYPE = RDF.type
RDFS_SUBCLASS = RDFS.subClassOf
GEO_WKT = GEO.term("asWKT")
GEO_GEOM = GEO.term("hasGeometry")

NAME_KEYS = ['name', 'gns:N:xx:FULL_NAME_ND', 'gns:N:xx:FULL_NAME', 'gns:N:xx:SORT_NAME',
             'alt_name', 'name:en', 'source:name', 'short_name', 'gns:N:gle:SORT_NAME',
             'gns:N:eng:FULL_NAME', 'gns:N:eng:FULL_NAME_ND', 'gns:N:eng:SORT_NAME',
             'gns:N:gle:FULL_NAME', 'gns:N:gle:FULL_NAME_ND', 'gns:V:xx:FULL_NAME',
             'gns:V:xx:FULL_NAME_ND', 'gns:V:xx:SORT_NAME', 'official_name', 'alt_name_1',
             'alt_name:en', 'old_name']

DESC_KEYS = ['note', 'description']

ADDRESS_KEYS = ['addr:housenumber', 'addr:street', 'addr:postcode', 'addr:city',
                'addr:county', 'addr:country', 'postal_code', 'source:postcode', 'source:addr:postcode']

CONTACT_KEYS = {'wikidata': 'Wikidata', 'contact:website': 'Website', 'website': 'Website',
                'phone': 'Phone', 'contact:phone': 'Phone', 'wikipedia': 'Wikipedia', 'email': 'Email',
                'fax': 'Fax', 'contact:twitter': 'Twitter', 'contact:email': 'Email'}

ATTRIBUTES = {'garden:type': 'GardenType', 'landuse': 'Landuse', 'access': 'Access', 'religion': 'Religion',
              'denomination': 'Denomination', 'capacity': 'Capacity', 'height': 'Height',
              'building:material': 'BuildingMaterial', 'opening_hours': 'OpeningHours', 'wheelchair': 'Wheelchair',
              'tourism': 'Tourism', 'year_built': 'YearBuilt', 'area': 'Area', 'population': 'Population',
              'smoking': 'Smoking', 'service': 'Service', 'cuisine': 'Cuisine', 'internet_access': 'InternetAccess',
              'food': 'Food'}


def define_centroid(bound):
    lat = "{:.4f}".format((bound['minlat'] + bound['maxlat']) / 2)
    lon = "{:.4f}".format((bound['minlon'] + bound['maxlon']) / 2)
    return POINT_WKT.format(lon, lat)


def define_point(lat, lon):
    return POINT_WKT.format("{:.4f}".format(lon), "{:.4f}".format(lat))


def get_wkt(record):
    if record['type'] == 'node':
        point = define_point(lat=record['lat'], lon=record['lon'])
    else:
        point = define_centroid(record['bounds'])
    return Literal(point, datatype=GEO.wktLiteral)


def geometry(record, uri):
    # create geometry and add to results (uri -> GEO_GEOM -> geom)
    geom = YAGO2GEO_RES.term('Geometry_osm_' + str(uuid.uuid4()))
    results = [(uri, GEO_GEOM, geom)]
    # create wkt and add to results (geom -> GEO_WKT -> wkt)
    wkt = get_wkt(record)
    results.append((geom, GEO_WKT, wkt))
    return results


def attributes(record, uri):
    results = [(uri, YAGO2GEO_ONT.term('hasOSM_ID'), Literal(record['type'] + '/' + str(record['id'])))]  # osm id
    tags = record['tags']
    # mapping (name, contact, description, address, attributes)
    # name
    for name in NAME_KEYS:
        if name in tags.keys():
            results.append((uri, RDFS_NAME, Literal(tags[name])))
    # description
    for desc in DESC_KEYS:
        if desc in tags.keys():
            results.append((uri, RDFS.comment, Literal(tags[desc])))
    # contact
    for key, value in CONTACT_KEYS.items():
        if key in tags.keys():
            results.append((uri, YAGO2GEO_ONT.term('has' + value), Literal(tags[key])))
    # address
    address = get_address(tags)
    if address != '':
        results.append((uri, YAGO2GEO_ONT.term('hasAddress'), Literal(address)))
    # attributes
    for key, value in ATTRIBUTES.items():
        if key in tags.keys():
            results.append((uri, YAGO2GEO_ONT.term('has' + value), Literal(tags[key])))
    return results


def define_class(key, val, desc):
    osm_class = YAGO2GEO_ONT.term('OSM_' + key.strip() + '_' + val.replace(' ', '_').strip())
    graph.add((osm_class, RDF_TYPE, OWL_CLASS))
    graph.add((osm_class, RDFS_NAME, Literal(key + ':' + val)))
    graph.add((osm_class, RDFS.comment, Literal(desc)))
    graph.add((osm_class, RDFS_SUBCLASS, OSM_BASE))
    return osm_class


def get_address(tags):
    address = ''
    for key in ADDRESS_KEYS:
        if key in tags.keys():
            address += tags[key]
    return address


def define_record(record, type_uri):
    if 'name' in record['tags'].keys():
        name = record['tags']['name']
    elif 'name:en' in record['tags'].keys():
        name = record['tags']['name:en']
    elif 'source:name' in record['tags'].keys():
        name = record['tags']['source:name']
    elif 'official_name' in record['tags'].keys():
        name = record['tags']['official_name']
    else:
        return
    cleaned = clean_name_for_uri(name)
    if name != '' and cleaned.strip() != '':
        uri = YAGO2GEO_RES.term(cleaned)  # uri -> term (cleaned name) for YAGO2GEO_RES

        triples = [(uri, RDF_TYPE, type_uri)]  # class -> RDF_TYPE of suitable class

        # geometry (geometry:GEO; wkt:GEO)
        triples.extend(geometry(record, uri))

        # attributes (name, contact, description, address, attributes)
        triples.extend(attributes(record, uri))

        for triple in triples:  # add to graph
            graph.add(triple)
        logging.debug('{0} added to graph about {1}'.format(len(triple), uri))


def clean_name_for_uri(name):
    uri_name = clean(name, fix_unicode=True, lower=False)
    uri_name = re.sub(r'[^a-zA-Z0-9_\-\/<>:\.\#]+', '', uri_name).replace(' ', '').replace(':', '')
    rand_number = randint(100, 999)
    return uri_name.replace('>', '').replace('<', '') + str(rand_number)


# read csv files
OSM_TYPES1 = pd.read_csv('data/osm/osm_key_values.csv')
OSM_TYPES2 = pd.read_csv('data/osm/osm_key_values_additional.csv')
OSM_TYPES3 = pd.read_csv('data/osm/osm_key_values_amenity.csv')
OSM_TYPES = pd.concat([OSM_TYPES1[['key', 'value', 'description']], OSM_TYPES2[['key', 'value', 'description']],
                       OSM_TYPES3[['key', 'value', 'description']]])
logging.info('read OSM types, a series of selected key-value pairs')
class_dictionaries = []
classes = {}
for index, row in OSM_TYPES.iterrows():
    key = str(row['key'])
    val = str(row['value'])
    desc = str(row['description'])
    if key + ':' + val not in classes.keys():
        osm_class = define_class(key, val, desc)
        classes[key + ':' + val] = osm_class
        class_dictionaries.append({'class': str(osm_class), 'label': key + ':' + val, 'gloss': desc})
    else:
        osm_class = classes[key + ':' + val]
    # read files in data/osm/ --> define their records
    for country in ['UK', 'IRL']:
        data = None
        try:
            with open('data/osm/{}.json'.format(country + '-' + key + '-' + val), encoding='utf-8') as fp:
                data = json.load(fp)
        except:
            print('file not found data/osm/{}.json'.format(country + '-' + key + '-' + val))
        if data is not None:
            for record in data['elements']:
                define_record(record, osm_class)
            logging.info('data/osm/{}.json processed'.format(country + '-' + key + '-' + val))

# serialize the graph
with open('data/rdf/osm.ttl', 'w', encoding='utf-8') as fp:
    fp.write(graph.serialize(format='turtle').decode('utf-8'))

# write down class dictionary
# df = pd.DataFrame(class_dictionaries)
# df.to_csv('data/osm_type.csv')
