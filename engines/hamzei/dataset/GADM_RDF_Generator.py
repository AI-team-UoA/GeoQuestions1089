#########################################################################################################
# @Author: --
# @Description: Read shapefile of UK GADM dataset and generate RDF
# @Usage: Generate complementary dataset for UK administrative levels
#########################################################################################################
import json
import logging
import uuid

import geopandas as gp
from rdflib import Namespace, Graph
from rdflib.namespace import OWL, RDF, RDFS
from rdflib.term import URIRef, Literal

logging.basicConfig(level=logging.INFO)


def define_class(graph, type, base):
    gadm_class = base.term('GADM_' + type.replace(' ', '_').replace('(', '').replace(')', '').strip())
    graph.add((gadm_class, RDF_TYPE, OWL_CLASS))
    graph.add((gadm_class, RDFS_NAME, Literal(type)))
    return gadm_class


if __name__ == '__main__':
    IRL_DIR = 'data/GADM/gadm36_IRL_shp/'
    IRL_SHP_FILES = IRL_DIR + 'gadm36_IRL_{}.shp'
    UK_DIR = 'data/GADM/gadm36_GBR_shp/'
    UK_SHP_FILES = UK_DIR + 'gadm36_GBR_{}.shp'
    UK_LEVELS = ['0', '1', '2', '3']
    IRL_LEVELS = ['0', '1']

    GADM_MATCHES_NT_FILE = 'data/GADM/GADM_matches.nt'

    IRL_GADM_OUTPUT_FILE = 'gadm_irl.nt'

    PREFIX_WKT = '<http://www.opengis.net/def/crs/EPSG/0/4326> '

    GEO = Namespace("http://www.opengis.net/ont/geosparql#")
    YAGO2GEO_RES = Namespace("http://kr.di.uoa.gr/yago2geo/resource/")
    YAGO2GEO_ONT = Namespace("http://kr.di.uoa.gr/yago2geo/ontology/")
    GEO = Namespace("http://www.opengis.net/ont/geosparql#")
    GADM_BASE = Namespace('http://www.app-lab.eu/gadm/')

    RDFS_NAME = RDFS.label
    OWL_CLASS = OWL.term('class')
    RDF_TYPE = RDF.type
    RDFS_SUBCLASS = RDFS.subClassOf
    GEO_WKT = GEO.term("asWKT")
    GEO_GEOM = GEO.term("hasGeometry")

    graph = Graph()

    logging.info('start reading the NT file and shape files...')
    matches = {}
    with open('data/GADM/GADM_matches.nt', encoding='utf-8') as fp:
        for line in fp:
            splits = line.split()
            matches[splits[0]] = splits[2]

    UK = False
    if UK:
        LEVELS = UK_LEVELS
        SHP_FILES = UK_SHP_FILES
        DIR = UK_DIR
    else:
        LEVELS = IRL_LEVELS
        SHP_FILES = IRL_SHP_FILES
        DIR = IRL_DIR

    with open(DIR + 'outs/remaining.nt', encoding='utf-8') as fp:
        for line in fp:
            splits = line.split()
            matches[splits[0]] = splits[2]

    SOLR_ADDITIONAL = {}
    classes = {}

    for level in LEVELS:
        df = gp.read_file(SHP_FILES.format(level))
        df['wkt'] = df.geometry.to_wkt()
        logging.info('start analyzing level {}'.format(level))
        for index, row in df.iterrows():
            name = str(row['NAME_' + level])
            wkt = PREFIX_WKT + str(row['wkt'])
            gid = str(row['GID_' + level])
            if 'ENGTYPE_' + level in row:
                type = str(row['ENGTYPE_' + level])
            else:
                type = 'COUNTRY'

            # create GADM URI
            guri = '<' + str(GADM_BASE.term(name.replace(' ', '_').replace(',', '') + '_' + gid)) + '>'

            # find it matches --> real URI
            if guri in matches.keys():
                if type is not None and type not in classes.keys():
                    classes[type] = str(define_class(graph, type, YAGO2GEO_ONT))
                if type is not None:
                    class_uri = classes[type]
                triples = []

                # add class and name using RDF_NAME and RDF_TYPE triples
                uri = URIRef(matches[guri].replace('<', '').replace('>', ''))
                if type is not None:
                    triples.append((uri, RDF_TYPE, URIRef(class_uri)))
                triples.append((uri, RDFS_NAME, Literal(name)))

                # add to SOLR_ADDITIONAL URI: NAME
                SOLR_ADDITIONAL[str(uri)] = name

                # GEOM -> WKT -> RDF
                geom_uri = YAGO2GEO_RES.term('Geometry_gadm_' + str(uuid.uuid4()))
                triples.append((uri, GEO_GEOM, geom_uri))
                wkt_uri = Literal(wkt, datatype=GEO.wktLiteral)
                triples.append((geom_uri, GEO_WKT, wkt_uri))

                # add to graph
                for triple in triples:  # add to graph
                    graph.add(triple)
            else:
                print(str(guri) + ' not found ' + str(name))
        logging.info('analyzing level {} is finished'.format(level))

    logging.info('writing files...')
    # write graph as a NT file...
    with open(DIR + 'outs/gadm.nt', 'w', encoding='utf-8') as fp:
        fp.write(graph.serialize(format='nt').decode('utf-8'))

    # write classes
    with open(DIR + 'outs/classes.json', 'w', encoding='utf-8') as fp:
        json.dump(classes, fp, ensure_ascii=False)

    # write SOLR additional
    with open(DIR + 'outs/solr.json', 'w', encoding='utf-8') as fp:
        json.dump(SOLR_ADDITIONAL, fp, ensure_ascii=False)
