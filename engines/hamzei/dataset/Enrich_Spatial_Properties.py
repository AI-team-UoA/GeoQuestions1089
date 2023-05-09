#########################################################################################################
# @Author: --
# @Description: Add length and area to the dataset
# @Usage: Add length and area to the dataset
#########################################################################################################
import logging
from rdflib import Namespace, Graph
from rdflib.namespace import XSD
from rdflib.term import URIRef, Literal
import shapely.wkt
import shapely.ops as ops
from functools import partial
import pyproj

logging.basicConfig(level=logging.INFO)


def get_spatial_info(uri, wkt):
    wkt = wkt.replace('<http://www.opengis.net/def/crs/EPSG/0/4326> ', '').strip()
    shape = shapely.wkt.loads(wkt)
    shape_type = shape.type.lower()
    if 'point' in shape_type:
        return []
    triples = []
    geom = ops.transform(
        partial(
            pyproj.transform,
            pyproj.Proj('EPSG:4326'),
            pyproj.Proj(proj='aea', lat_1=shape.bounds[1], lat_2=shape.bounds[3])), shape)
    if 'polygon' in shape_type:
        triples.append((uri, URIRef('http://yago-knowledge.org/resource/hasArea'), Literal(geom.area,
                        datatype=XSD.float)))
    triples.append((uri, URIRef('http://yago-knowledge.org/resource/hasLength'), Literal(geom.length,
                        datatype=XSD.float)))
    return triples


def enrich(address, output):
    GEO = Namespace("http://www.opengis.net/ont/geosparql#")
    graph = Graph()
    WKT = str(GEO.term("asWKT"))
    GEOM = str(GEO.term("hasGeometry"))

    geom_uri = {}
    geom_wkt = {}

    with open(address, 'r', encoding='utf-8') as fp:
        for line in fp:
            splits = line.split('> <')
            if len(splits) >= 3:
                if GEOM in line:
                    uri = splits[0].replace('<', '').strip()
                    geom = splits[2].replace('> .\n', '').strip()
                    geom_uri[geom] = uri
            if len(splits) == 2:
                if WKT in line:
                    geom = splits[0].replace('<', '').replace('>', '').strip()
                    wkt = splits[1].replace('http://www.opengis.net/ont/geosparql#asWKT> '
                                            '"<http://www.opengis.net/def/crs/EPSG/0/4326> ', ''). \
                        replace('"^^<http://www.opengis.net/ont/geosparql#wktLiteral> .', '').replace('\n', '').strip()
                    geom_wkt[geom] = wkt
    logging.info('geoms, wkts, and uris are ready to be analyzed...')
    for g, wkt in geom_wkt.items():
        if g in geom_uri.keys():
            uri_string = geom_uri[g]
            uri = URIRef(uri_string)
            try:
                triples = get_spatial_info(wkt=wkt, uri=uri)
                for triple in triples:
                    graph.add(triple)
            except:
                logging.error('error in calculation of area and length for {}'.format(uri_string))
    with open(output, 'w', encoding='utf-8') as fp:
        fp.write(graph.serialize(format='turtle').decode('utf-8'))


if __name__ == '__main__':
    BASE = '/mnt/yago2/ehsan/dev/yago2geo/all/'
    files = ['gadm.nt', 'gadm-irl.nt', 'OS_new.nt', 'OSI_new.nt', 'OSI_ex.nt', 'OS_ex.nt', 'OSNI_ex.nt', 'OSNI_new.nt',
             'osm_full_matched.nt', 'yago2-minimal.nt']
    for file in files:
        print('\n\nAnalyzing file: {}'.format(file))
        try:
            enrich(BASE+file, output=BASE+file.replace('.nt', '_SP.ttl'))
        except:
            logging.error('************* Error in analyzing file {} **************'.format(file))
