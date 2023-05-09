#########################################################################################################
# @Author: --
# @Description: Use Post Service to Insert Data to Parliament Server
# @Usage: Load RDF data to Parliament
#########################################################################################################
import requests


def post(statements, dataformat='TURTLE'):
    headers = {'User-Agent': 'Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:89.0) Gecko/20100101 Firefox/89.0',
               'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
               'Accept-Language': 'en-US,en;q=0.5',
               'Accept-Encoding': 'gzip, deflate',
               'Content-Type': 'application/x-www-form-urlencoded',
               'Connection': 'keep-alive',
               'Upgrade-Insecure-Requests': '1'}
    params = {'dataFormat': dataformat, 'statements': statements, 'graph': ''}
    r = requests.post('http://45.113.235.161:1313/parliament/bulk/insert', params, headers=headers)
    return str(r)


def post_file(file, dataformat='TURTLE'):
    headers = {'User-Agent': 'Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:89.0) Gecko/20100101 Firefox/89.0',
               'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8',
               'Accept-Language': 'en-US,en;q=0.5',
               'Accept-Encoding': 'gzip, deflate',
               'Content-Type': 'multipart/form-data',
               'Connection': 'keep-alive',
               'Upgrade-Insecure-Requests': '1'}
    params = {'dataFormat': dataformat, 'graph': ''}
    r = requests.post('http://45.113.235.161:1313/parliament/bulk/insert', data=params,
                      files={'upload_file': open(file, 'rb')},
                      headers=headers)
    return str(r)


def read_and_insert(file, bulk_size=5000, format='TURTLE'):
    lines = []
    inserted = 0
    with open(file, encoding='utf-8') as fp:
        for line in fp:
            lines.append(line)
            inserted += 1
            if len(line) % bulk_size == 0:
                result = post('\n'.join(lines), dataformat=format)
                print('{0} inserted to Parliament: result {1}'.format(inserted, result))
                lines = []
        result = post('\n'.join(lines), dataformat=format)
        print(result)
        print('{0} inserted to Parliament: result {1}'.format(inserted, result))
    print('{0} is finished and loaded.'.format(file))
    print('-------************************--------')


def read_and_insert_single(file, format='TURTLE'):
    res_line = ''
    counter = 0
    with open(file, encoding='utf-8') as fp:
        for line in fp:
            res_line += line.replace('\n', '')
            if res_line.strip().endswith('.'):
                result = post(res_line, dataformat=format)
                res_line = ''
                counter += 1
            if counter > 0 and counter % 1000 == 0:
                print('{} inserted'.format(counter))
    print('{0} is finished and loaded.'.format(file))
    print('-------************************--------')


if __name__ == "__main__":
    BASE = '/mnt/yago2/ehsan/dev/yago2geo/all/'
    files = ['cleaned_GADM_ontology.ttl', 'cleaned_OSM_ontology.ttl', 'cleaned_OS_extended.ttl', 'cleaned_OSI_new.ttl',
             'cleaned_OS_new.ttl', 'cleaned_OSNI_new.ttl', 'cleaned_OS_ontology.ttl', 'cleaned_GADM_new.ttl',
             'cleaned_OSI_extended.ttl', 'cleaned_OSI_ontology.ttl', 'cleaned_OSM_extended.ttl',
             'cleaned_OSNI_extended.ttl', 'cleaned_OSNI_ontology.ttl', 'cleaned_GADM_extended.ttl']
    for file in files:
        # read_and_insert(file=BASE+file)
        read_and_insert_single(file=BASE + file)
