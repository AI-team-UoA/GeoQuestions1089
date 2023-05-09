#########################################################################################
# @Author: --
# @Description: Use SPARQL queries to identify place names inside the Yago2 KB
# @Usage: Perform concept identification for place names
#########################################################################################
import json
import logging

import pysolr

from request_handler import Configuration, RequestHandler

# initialization -- this may take time but only run once
logging.basicConfig(level=logging.INFO)
CONFIG = Configuration(ip='88.197.53.173', port='3030', datasource='ds')
REQUEST_HANDLER = RequestHandler(CONFIG)
SOLR_HANDLER = pysolr.Solr('http://88.197.53.173:8983/solr/places/')
PLACE_TYPES = []
with open('data/place_type/type-set.txt', 'r', encoding='utf-8') as fp:
    for line in fp:
        PLACE_TYPES.append(line.lower().strip())
PLACE_TYPES = list(set(PLACE_TYPES))
PLACE_TYPES.sort(key=len, reverse=True)
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
        extra_chars[m['uri'][0]] = min_char
        if '/' + start_name in m['uri'][0]:
            valid_uris.append(m['uri'][0])
            no_candidate = False
        elif '/Greater' in m['uri'][0] or '/Republic' in m['uri'][0]:
            valid_uris.append(m['uri'][0])
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
                exacts.append(doc['uri'][0])
            all.append(doc['uri'][0])
        candidates = []
        if len(exacts) > 0:
            candidates.extend(list(set(exacts)))
        filtered_uris = filtered(place, matched_list=search_results)
        candidates.extend(filtered_uris)
        if len(candidates) > 0:
            return list(set(candidates))
        return all[0:10]
    else:
        return [search_results.docs[0]['uri'][0]]


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
