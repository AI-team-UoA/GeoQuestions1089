#########################################################################################################
# @Author: --
# @Description: fix bad URIs in Yago2 turtle file
# @Usage: used to cleanup the urls
#########################################################################################################

import re

from cleantext import clean


def clean_line(line):
    cleaned_line = line
    for m in re.finditer(r'\<[^\>]*\>', line):
        original = line[m.span()[0]:m.span()[1]]
        cleaned = clean_item(original)
        cleaned_line = cleaned_line.replace(original, cleaned)
    return cleaned_line


def clean_item(item):
    if '<' in item and '>' in item:
        item = clean(item, fix_unicode=True, lower=False)
        if 'http:' in item:
            return re.sub(r'[^a-zA-Z0-9_\-\/<>:\.\#]+', '', item).replace(' ', '')
        if ':' in item or "'" in item or '"' in item:
            item = item.replace(':', '_').replace("'", '').replace('"', '').replace(' ', '')
        return re.sub(r'[^a-zA-Z0-9_\-\/<>]+', '', item).replace(' ', '')
    return item


def valid_line(line):
    if '<http:///>' in line:
        return False
    if 'http://' in line:
        if '<http://' in line and line.count('http://') == line.count('<http://'):
            return True
        else:
            return False
    return True


def read_clean_write(base, input, output):
    filereader = open(base + '/' + input, 'r')
    filewritter = open(base + '/' + output, 'w')
    count = 0
    write = []
    while True:
        count += 1
        # Get next line from file
        line = filereader.readline()
        # end of file is reached
        if not line:
            break
        cleaned = clean_line(line)
        if valid_line(cleaned):
            write.append(cleaned)
        if len(write) % 2000000 == 0:
            print('counter is {}'.format(count))
            filewritter.writelines(write)
            write = []

    filewritter.writelines(write)
    filereader.close()
    filewritter.close()


if __name__ == "__main__":
    base_uri = '/mnt/yago2/ehsan/dev/yago2geo/raw_data/raw_files/'
    files = ['GADM_extended.ttl', 'GADM_ontology.ttl', 'GAG_new.ttl', 'OS_extended.ttl', 'OSI_new.ttl', 'OS_matches.nt',
             'OS_new.ttl', 'OSNI_new.ttl', 'OS_ontology.ttl', 'GADM_matches.nt', 'GAG_extended.ttl', 'GAG_ontology.ttl',
             'OSI_extended.ttl', 'OSI_ontology.ttl', 'OSM_extended.ttl', 'OSNI_extended.ttl', 'OSNI_ontology.ttl',
             'OS_topological.nt', 'GADM_new.ttl', 'GAG_matches.nt', 'GAG_topological.nt', 'OSI_matches.nt',
             'OSI_topological.nt', 'OSM_ontology.ttl', 'OSNI_matches.nt', 'OSNI_topological.nt']
    for file in files:
        output = 'cleaned_' + file
        print('***********************************************************\n')
        print('Analysing input: {0} and output: {1}'.format(file, output))
        read_clean_write(base=base_uri, input=file, output=output)
        print('The input: {0} and output: {1} completely analysed'.format(file, output))
        print('***********************************************************\n\n')
