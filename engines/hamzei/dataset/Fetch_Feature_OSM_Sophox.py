#########################################################################################################
# @Author: --
# @Description: Retrieve Overpass data for specific key-values and create GeoJSON files
# @Usage: Create GeoJSON data for specific key value tags from OSM
#########################################################################################################

import json
import logging
import os.path
import time

import pandas as pd
from OSMPythonTools.overpass import overpassQueryBuilder

from RequestHandler import Configuration, RequestHandler

logging.basicConfig(level=logging.INFO)

BBOXES = {'UK': [49.9599, -7.572, 58.63500, 1.6815],
          'IRL': [51.6692, -9.977084, 55.1317, -6.03299]}
OSM_TYPES1 = pd.read_csv('data/osm/osm_key_values.csv')
OSM_TYPES2 = pd.read_csv('data/osm/osm_key_values_additional.csv')
OSM_TYPES3 = pd.read_csv('data/osm/osm_key_values_amenity.csv')
OSM_TYPES4 = pd.read_csv('data/osm/osm_key_values_natural.csv')
OSM_TYPES5 = pd.read_csv('data/osm/osm_key_values_highways_airways.csv')
OSM_TYPES = pd.concat([OSM_TYPES1[['key', 'value', 'description']], OSM_TYPES2[['key', 'value', 'description']],
                       OSM_TYPES3[['key', 'value', 'description']], OSM_TYPES4[['key', 'value', 'description']],
                       OSM_TYPES5[['key', 'value', 'description']]])

# Read OSM types -- selected key values for small places and amenities
logging.info('read OSM types, a series of selected key-value pairs')
config = Configuration()
config.set_url('http://overpass-api.de/api/interpreter')
HEADERS = {'content-type': 'application/x-www-form-urlencoded; charset=UTF-8',
           'User-Agent': 'Mozilla/5.0 (X11; Linux x86_64) '
                         'AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.114 Safari/537.36',
           'Accept': '*/*'}
HANDLER = RequestHandler(config)


def fetch(bbox, type_key, type_val, timeout=50):
    query = overpassQueryBuilder(bbox=bbox, elementType=['node', 'way', 'relation'],
                                 selector='"{}"="{}"'.format(type_key, type_val),
                                 includeGeometry=True)
    params = {'data': '[timeout:{}][out:json];'.format(timeout) + str(query)}
    return HANDLER.post(params, HEADERS)


# Generate complementary dataset in RDF format for UK, Ireland
for index, row in OSM_TYPES.iterrows():
    key = str(row['key'])
    val = str(row['value'])
    for country in BBOXES.keys():
        try:
            if os.path.isfile('data/osm/{}.json'.format(country + '-' + key + '-' + val.replace(' ', '_'))):
                continue
            elif os.path.isfile('data/osm/{}.json'.format(country + '-' + key + '-' + val)):
                continue
            else:
                result = fetch(BBOXES[country], key, val, timeout=1000)
                logging.info(
                    '{0} for {1} is fetched, number of records {2}'.format(key + '--' + val, country, len(result)))
                with open('data/osm/{}.json'.format(country + '-' + key + '-' + val.replace(' ', '_')), 'w') as fp:
                    json.dump(result, fp)
                logging.info('json file is written: {}\n\n'.format('data/osm/{}.json'.
                                                                   format(
                    country + '-' + key + '-' + val.replace(' ', '_'))))
                time.sleep(60)
        except:
            print('ERROR: **************** {}'.format(key + '--' + val + '--' + country))
