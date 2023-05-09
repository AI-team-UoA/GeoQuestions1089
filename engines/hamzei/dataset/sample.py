#########################################################################################################
# @Author: --
# @Description: Use Regex to truncate float numbers to fixed precision (p=4)
#########################################################################################################
import re

dir = '/home/ehsan/Desktop/MRA/Datasources/GADM/gadm36_GBR_shp/outs/'
uris = set()
no_uris = set()
with open(dir+'gadm.nt', encoding='utf-8') as fp:
    for line in fp:
        if line.startswith('<http://yago-knowledge.org/resource/'):
            uris.add(line.split()[0])

with open(dir+'remaining.nt', encoding='utf-8') as fp:
    for line in fp:
        no_uris.add(line.split()[2])

final = []
for uri in uris:
    if uri not in no_uris:
        final.append('<sample> <sample> '+uri + ' .\n')

with open(dir+'sample.nt', 'w', encoding='utf-8') as fp:
    fp.writelines(final)
