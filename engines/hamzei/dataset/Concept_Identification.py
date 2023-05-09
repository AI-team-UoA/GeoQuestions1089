#########################################################################################
# @Author: --
# @Description: Use SPARQL queries to identify place names inside the Yago2 KB
# @Usage: Perform concept identification for place names
#########################################################################################
import json
import logging

import pysolr

from RequestHandler import Configuration, RequestHandler

# initialization -- this may take time but only run once
logging.basicConfig(level=logging.INFO)
CONFIG = Configuration(ip='45.113.235.161', port='3030', datasource='ds')
REQUEST_HANDLER = RequestHandler(CONFIG)
SOLR_HANDLER = pysolr.Solr('http://45.113.235.161:8983/solr/places/')
PLACE_TYPES = ['underground station', 'monument', 'hotel', 'district', 'bridge', 'county', 'country', 'city', 'tower',
               'street', 'square', 'stadium', 'village', 'castle', 'palace', 'river', 'way']
QUERY_TEMPLATE = 'PREFIX yago: <http://yago-knowledge.org/resource/>\n' \
                 'PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n' \
                 '\nSELECT distinct ?subject\n' \
                 'WHERE {{\n' \
                 '?subject a yago:yagoGeoEntity .\n' \
                 '?subject rdfs:label ?name.\n' \
                 'FILTER LANGMATCHES(LANG(?name), "eng" ) .\n' \
                 'FILTER(LCASE(STR(?name)) = LCASE("{place}")).\n' \
                 '}} \n LIMIT 50'


def from_fuseki(place):
    results = REQUEST_HANDLER.request(QUERY_TEMPLATE.format(place=place))
    logging.info('matched: {0} to {1}'.format(place, results['results']['bindings']))
    return results['results']['bindings']


def filtered(place, matched_list):
    extra_chars = {}
    valid_uris = []
    no_candidate = True
    place = place.lower().strip()
    start_name = place.split(' ')[0]
    for m in matched_list.docs:
        min_char = 10000
        for t in m['toponym']:
            remaining = t.lower().strip().replace(place, '')
            if len(remaining) < min_char:
                min_char = len(remaining)
        extra_chars[m['uri']] = min_char
        if '/' + start_name in m['uri']:
            valid_uris.append(m['uri'])
            no_candidate = False
    results = set()
    for uri, distance in extra_chars.items():
        if distance < 10 and (uri in valid_uris or (no_candidate and start_name in uri)):
            results.add(uri)
    return list(results)


def from_SOLR(place, hl='true', fragsize=10, rows=500, fl='uri, toponym', recursive=False, type=''):
    place = place.replace(" 's", 's')
    search_results = SOLR_HANDLER.search('toponym:"' + place + '"'
                                         , **{'hl': hl, 'hl.fragsize': fragsize, 'rows': rows, 'fl': fl})
    if len(search_results.docs) == 0:
        if recursive:
            return []
        normalized = place.lower().strip()
        matched_type = ''
        for type in PLACE_TYPES:
            if type.lower() + ' of ' in normalized:
                normalized = normalized.replace(type.lower() + ' of ', '')
                matched_type = type
                break
            elif type.lower() + ' ' in normalized:
                normalized = normalized.replace(type.lower() + ' ', '')
                matched_type = type
                break
            elif ' ' + type.lower() in normalized:
                normalized = normalized.replace(' ' + type.lower(), '')
                matched_type = type
                break
        if matched_type != '':
            return from_SOLR(normalized, recursive=True, type=matched_type)
    elif len(search_results) > 1:
        exacts = []
        all = []
        for doc in search_results.docs:
            if is_in_list(place, doc['toponym'], type=type):
                exacts.append(doc['uri'])
            all.append(doc['uri'])
        if len(exacts) > 0:
            return list(set(exacts))
        filtered_uris = filtered(place, matched_list=search_results)
        if len(filtered_uris) > 0:
            return filtered_uris
        return all[0:5]
    else:
        return [search_results.docs[0]['uri']]


def is_in_list(place, toponyms, type=''):
    if place in toponyms:
        return True
    place_t = place.lower().strip() + ' ' + type.lower().strip()
    place_t2 = type.lower().strip() + ' of ' + place.lower().strip()
    for t in toponyms:
        if t.lower().strip() in [place.lower().strip(), place_t, place_t2]:
            return True
    return False


if __name__ == "__main__":
    with open('data/name-type-attribute.json', 'r', encoding='utf-8') as f:
        name_type_attributes = json.load(f)
    matched = {}
    source = 'SOLR'
    places = name_type_attributes['name']
    for place in places:
        if source == 'FUSEKI':
            matched[place] = from_fuseki(place)
        else:
            matched[place] = from_SOLR(place)
    with open('data/name_matched.json', 'w', encoding='utf-8') as fp:
        json.dump(matched, fp=fp)
