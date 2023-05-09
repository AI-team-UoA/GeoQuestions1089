import json
import logging
import os.path

import pysolr

logging.basicConfig(level=logging.WARN)

solr = pysolr.Solr('http://45.113.235.161:8983/solr/places/', always_commit=True)


def analyze(address, valids):
    matched = {}
    with open(address, 'r', encoding='utf-8') as fp:
        counter = 0
        for line in fp:
            splits = line.split('> <')
            if len(splits) == 2:
                uri = line.split()[0]
                if uri not in valids:
                    continue
                if uri not in matched.keys():
                    if "http://www.w3.org/2000/01/rdf-schema#label" in splits[1]:
                        toponym = splits[1].replace("http://www.w3.org/2000/01/rdf-schema#label", '').replace('>', ''). \
                            replace('\n', '').replace('.', '').replace('"', '').strip()
                        if toponym != '':
                            try:
                                search_results = solr.search(q='toponym:"{}"'.format(toponym),
                                                             **{'fq': 'uri:/.*knowledge.*/'})
                                if len(search_results.docs) > 0:
                                    for doc in search_results.docs:
                                        if toponym.strip() == doc['toponym'][0].replace('@eng', '').strip():
                                            matched[uri] = doc['uri']
                                            print('{0}: {1}'.format(uri, doc['uri']))
                                            break;
                            except:
                                print('error in calling solr')
            counter += 1
            if counter % 1000 == 0:
                print('1000 more is  investigated {}'.format(counter))
    with open('data/rdf/osm_matches_old.json', 'w', encoding='utf-8') as fp:
        json.dump(fp=fp, obj=matched)
    return matched


def replace(address, matched, output):
    lines = []
    with open(address, 'r', encoding='utf-8') as fp:
        counter = 0
        for line in fp:
            splits = line.split()
            if len(splits) <= 2:
                lines.append(line)
                continue
            uri = splits[0]
            if uri in matched.keys():
                lines.append(line.replace(uri, '<' + matched[uri] + '>'))
            else:
                lines.append(line)
            if len(lines) > 10000:
                with open(output, 'a+', encoding='utf-8') as fpw:
                    fpw.writelines(lines)
                counter += len(lines)
                print('10000 more added {}'.format(counter))
                lines = []
    with open(output, 'a+', encoding='utf-8') as fpw:
        fpw.writelines(lines)
    counter += len(lines)
    print('final {}'.format(counter))


if __name__ == '__main__':
    directory = '/mnt/yago2/ehsan/dev/yago2geo/all/'
    # directory = '/home/ehsan/Desktop/MRA/Datasources/'
    address = directory + 'osm_full.nt'
    output = directory + 'osm_full_matched.nt'

    with open('data/valid-uris.json', encoding='utf-8') as fp:
        valids = json.load(fp)

    check_valids = []
    for valid in valids:
        check_valids.append('<' + valid + '>')

    if os.path.isfile('data/rdf/osm_matches.json'):
        with open('data/rdf/osm_matches.json', encoding='utf-8') as fp:
            matched = json.load(fp)
    else:
        print('analyzing uris')
        matched = analyze(address, check_valids)
    print('*********************************' + len(matched))
    replace(address, matched, output)
