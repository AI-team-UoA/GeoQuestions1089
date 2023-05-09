#########################################################################################################
# @Author: --
# @Description: Read RDF files and insert names (toponyms) and their URIs to filter Yago2 dataset
# @Usage: Use describe query to filter Yago2 and write down a mini turtle file
#########################################################################################################
import pandas as pd
import requests

DESCRIBE_QUERY = 'DESCRIBE {}'
HEADERS = {'Accept': 'text/turtle,*/*;q=0.9'}


def extract_uris(matching_file):
    uris = set()
    with open(matching_file, encoding='utf-8') as fp:
        for line in fp:
            splits = line.split()
            if len(splits) >= 3:
                uris.add(splits[2])
    return list(uris)


def describe_query(uri):
    r = requests.post('http://localhost:3030/ds', {'query': DESCRIBE_QUERY.format(uri)}, HEADERS)
    return r.text.replace('##', '01')


if __name__ == "__main__":
    dir = '/mnt/yago2/ehsan/dev/yago2geo/all/'
    matching_files = ['cleaned_OS_matches.nt', 'cleaned_OSI_matches.nt', 'cleaned_OSNI_matches.nt', 'gadm_matches.nt',
                      'remaining.nt']
    uris = set()
    for matching_file in matching_files:
        uris.update(extract_uris(dir + matching_file))

    print('URIS length: {}'.format(len(uris)))

    counter = 0
    data = ''
    for uri in uris:
        data += describe_query(uri.strip()) + '\n'
        counter += 1
        if counter % 200 == 0:
            print('200 more is investigated {}'.format(counter))
            with open(dir + 'yago-filtered.ttl', 'a+', encoding='utf-8') as fp:
                fp.write(data)
            data = ''
    with open(dir + 'yago-filtered.ttl', 'a+', encoding='utf-8') as fp:
        fp.write(data)

    dir = 'data/yago2/'
    file_names = '{0}{1}.csv'
    countries = ['ireland', 'ireland_rep', 'wales', 'england', 'uk', 'scotland']
    levels = ['1', '2', '3']
    data = []
    for country in countries:
        for level in levels:
            data.append(pd.read_csv(dir + file_names.format(country, level)))
    full = pd.concat(data)
    uris_list = full[' x'].tolist()
    uris_set = set()
    for uri in uris_list:
        uris_set.add('<' + uri.replace('"', '').strip() + '>')

    for uri in uris_set:
        if uri not in uris:
            data += describe_query(uri.strip()) + '\n'
            counter += 1
            if counter % 200 == 0:
                print('200 more is investigated {}'.format(counter))
                with open(dir + 'yago-filtered-additional.ttl', 'a+', encoding='utf-8') as fp:
                    fp.write(data)
                data = ''
    with open(dir + 'yago-filtered-additional.ttl', 'a+', encoding='utf-8') as fp:
        fp.write(data)
