#########################################################################################################
# @Author: --
# @Description: Convert GeoJSON results to RDF (not only for points)
# @Usage: Create RDF data for specific key value tags from OSM (not only for points)
#########################################################################################################
import json
import logging
import os.path
import re
import uuid
from functools import partial
from random import randint
import osm2geojson
import pandas as pd
import pyproj
import shapely.ops as ops
from cleantext import clean
from rdflib import Namespace, Graph
from rdflib.namespace import OWL, RDF, RDFS
from rdflib.term import URIRef, Literal

import Truncate_Geometries as tg

logging.basicConfig(level=logging.INFO)


def geometry(shape, uri, centroid=True):
    # create geometry and add to results (uri -> GEO_GEOM -> geom)
    geom = YAGO2GEO_RES.term('Geometry_osm_' + str(uuid.uuid4()))
    results = [(uri, GEO_GEOM, geom)]
    # create wkt and add to results (geom -> GEO_WKT -> wkt)
    if centroid:
        wkt = Literal(WKT_TEMPLATE.format(tg.match_and_trunc(shape.centroid.wkt)), datatype=GEO.wktLiteral)
    else:
        wkt = Literal(WKT_TEMPLATE.format(tg.match_and_trunc(shape.wkt)), datatype=GEO.wktLiteral)
    results.append((geom, GEO_WKT, wkt))
    return results


def all_properties(properties_dictionaries):
    properties_dictionaries.append({'class': str(YAGO2GEO_ONT.term('hasAddress')),
                                    'label': 'address', 'gloss': 'Address of a place or a building'})
    for key, value in CONTACT_KEYS.items():
        properties_dictionaries.append({'class': str(YAGO2GEO_ONT.term('has' + value)),
                                        'label': value, 'gloss': ''})
    for key, value in ATTRIBUTES.items():
        properties_dictionaries.append({'class': str(YAGO2GEO_ONT.term('has' + value)),
                                        'label': value, 'gloss': ''})
    return properties_dictionaries


def attributes(properties, uri):
    results = [
        (uri, YAGO2GEO_ONT.term('hasOSM_ID'), Literal(properties['type'] + '/' + str(properties['id'])))]  # osm id
    tags = properties['tags']
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


def transform(geom):
    geom_transformed = ops.transform(
        partial(
            pyproj.transform,
            pyproj.Proj('EPSG:4326'),
            pyproj.Proj(
                proj='aea',
                lat_1=geom.bounds[1],
                lat_2=geom.bounds[3])),
        geom)
    return geom_transformed


def define_spatial_properties(record, uri):
    triples = []
    if 'point' in record.type.lower():
        return triples
    else:
        geom = transform(record)
        if 'polygon' in geom.type.lower():
            triples.append((uri, URIRef('http://yago-knowledge.org/resource/hasArea'), Literal(geom.area)))
            triples.append((uri, URIRef('http://yago-knowledge.org/resource/hasLength'), Literal(geom.length)))
        else:
            triples.append((uri, URIRef('http://yago-knowledge.org/resource/hasLength'), Literal(geom.length)))
    return triples


def define_record(record, type_uri, osm_ids, centroid=True, with_spatial_info=False):
    properties = record['properties']
    tags = properties['tags']
    shape = record['shape']
    if 'name' in tags.keys():
        name = tags['name']
    elif 'name:en' in tags.keys():
        name = tags['name:en']
    elif 'source:name' in tags.keys():
        name = tags['source:name']
    elif 'official_name' in tags.keys():
        name = tags['official_name']
    else:
        return
    identifier = properties['type'] + '/' + str(properties['id'])
    already_exist = False
    if identifier not in osm_ids.keys():
        cleaned = clean_name_for_uri(name)
        osm_ids[identifier] = cleaned
    else:
        cleaned = osm_ids[identifier]
        already_exist = True

    if name != '' and cleaned.strip() != '':
        uri = YAGO2GEO_RES.term(cleaned)  # uri -> term (cleaned name) for YAGO2GEO_RES
        triples = [(uri, RDF_TYPE, type_uri)]  # class -> RDF_TYPE of suitable class
        if not already_exist:
            # geometry (geometry:GEO; wkt:GEO)
            triples.extend(geometry(shape, uri, centroid))
            # attributes (name, contact, description, address, attributes)
            triples.extend(attributes(properties, uri))
            if with_spatial_info:
                triples.extend(define_spatial_properties(shape, uri))
        for triple in triples:  # add to graph
            graph.add(triple)
        logging.debug('{0} added to graph about {1}'.format(len(triples), uri))


def clean_name_for_uri(name):
    uri_name = clean(name, fix_unicode=True, lower=False)
    uri_name = re.sub(r'[^a-zA-Z0-9_\-\/<>:\.\#]+', '', uri_name).replace(' ', '').replace(':', '')
    rand_number = randint(100, 999)
    return 'OSM_' + uri_name.replace('>', '').replace(' ', '_').replace('<', '').replace('#', '') + str(rand_number)


if __name__ == '__main__':
    graph = Graph()
    GEO = Namespace("http://www.opengis.net/ont/geosparql#")

    YAGO2GEO_ONT = Namespace("http://kr.di.uoa.gr/yago2geo/ontology/")
    OSM_BASE = URIRef('http://kr.di.uoa.gr/yago2geo/ontology/OSM_Feature')
    YAGO2GEO_RES = Namespace("http://kr.di.uoa.gr/yago2geo/resource/")

    WKT_TEMPLATE = "<http://www.opengis.net/def/crs/EPSG/0/4326> {}"

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

    # read csv files
    OSM_TYPES1 = pd.read_csv('data/osm/osm_key_values.csv')
    OSM_TYPES2 = pd.read_csv('data/osm/osm_key_values_additional.csv')
    OSM_TYPES3 = pd.read_csv('data/osm/osm_key_values_amenity.csv')
    OSM_TYPES4 = pd.read_csv('data/osm/osm_key_values_natural.csv')
    OSM_TYPES5 = pd.read_csv('data/osm/osm_key_values_highways_airways.csv')
    OSM_TYPES = pd.concat([OSM_TYPES1[['key', 'value', 'description']], OSM_TYPES2[['key', 'value', 'description']],
                           OSM_TYPES3[['key', 'value', 'description']], OSM_TYPES4[['key', 'value', 'description']],
                           OSM_TYPES5[['key', 'value', 'description']]])
    with_spatial_info = True

    class_dictionaries = []
    properties_dictionaries = all_properties([])
    classes = {}
    osm_ids = {}
    for index, row in OSM_TYPES.iterrows():
        centroid = True
        key = str(row['key'])
        if key in ['natural', 'water', 'waterway', 'landuse', 'place', 'aeroway', 'highway']:
            centroid = False
        val = str(row['value']).replace(' ', '_')
        if val in ['residential', 'house', 'tree']:  # skip not so important ones
            continue
        desc = str(row['description'])
        logging.info('reading {0}:{1}'.format(key, val))

        # read files in data/osm/ --> define their records
        for country in ['UK', 'IRL']:
            data = None
            shapes = None
            try:
                if os.path.isfile('data/osm/{}.json'.format(country + '-' + key + '-' + val)):
                    with open('data/osm/{}.json'.format(country + '-' + key + '-' + val),
                              encoding='utf-8') as fp:
                        data = json.load(fp)
                elif os.path.isfile('data/osm/{}.json'.format(country + '-' + key + '-' + val.replace('_', ' '))):
                    with open('data/osm/{}.json'.format(country + '-' + key + '-' + val.replace('_', ' ')),
                              encoding='utf-8') as fp:
                        data = json.load(fp)
                else:
                    print('file not found data/osm/{}.json'.format(country + '-' + key + '-' + val))
                    continue
                shapes = osm2geojson.json2shapes(data)
            except:
                print('file not found data/osm/{}.json'.format(country + '-' + key + '-' + val))
            if shapes is not None:
                if len(shapes) > 0:
                    if key + ':' + val not in classes.keys():
                        osm_class = define_class(key, val, desc)
                        classes[key + ':' + val] = osm_class
                        class_dictionaries.append({'class': str(osm_class), 'label': key + ':' + val, 'gloss': desc})
                    else:
                        osm_class = classes[key + ':' + val]
                for record in shapes:
                    define_record(record, osm_class, osm_ids, centroid=centroid, with_spatial_info=with_spatial_info)
                logging.info('data/osm/{}.json processed'.format(country + '-' + key + '-' + val))

    # serialize the graph
    if not with_spatial_info:
        address = 'data/rdf/osm_highway_airway.ttl'
    else:
        address = 'data/rdf/osm_with_spatial_properties.ttl'
    with open('data/rdf/osm_highway_airway_wsp.ttl', 'w', encoding='utf-8') as fp:
        fp.write(graph.serialize(format='turtle').decode('utf-8'))

    if not with_spatial_info:
        # write down class dictionary
        df = pd.DataFrame(class_dictionaries)
        df.to_csv('data/osm_types_highway_airway.csv')

        # write down class dictionary
        df = pd.DataFrame(properties_dictionaries)
        df.to_csv('data/osm_properties_full.csv')
