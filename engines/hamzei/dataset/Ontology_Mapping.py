#########################################################################################
# @Author: --
# @Description: Use string similarity based on BERT embedding to perform ontology mapping
# @Usage: Perform ontology mapping for place types and place properties
#########################################################################################
import json
import logging

import numpy as np
import pandas as pd
from pattern.en import singularize

import Bert_Similarity as sem
from DocumentSimilarity import DocumentSimilarity, Definition

# initialization -- this may take time but only run once
logging.basicConfig(level=logging.WARN)
INFO = {'type': {'desc': 'gloss', 'label': 'label', 'label_vector': 'label_vector'}}


class Matching:
    def __init__(self, labels):
        self.label_vector = sem.label_vectors_list(labels)

    def match(self, value, size=5):
        vector = sem.token_vector(value)
        filtered = self.batch_eval(vector=vector, size=size)
        return filtered

    def batch_eval(self, vector, size, th=0.75):
        labels = self.label_vector[0]
        similiarities = sem.similarities(vector, self.label_vector[1])
        np_similiarity = np.array(similiarities[0])
        idx = (-np_similiarity).argsort()[:size]
        if max(np_similiarity) < th:
            return []
        return [labels[i] for i in idx if np_similiarity[i] >= th]


class MappingPipeline:
    def __init__(self, type='type'):
        self.type = type  # load the labels and gloss based on type value
        self.df = None
        self.mlabel = None
        self.mgloss = None
        self.__load()

    def __load(self):
        if self.type == 'type':
            yago2 = pd.read_csv('data/yago2_type.csv')
            yago2 = yago2[['class', 'label', 'gloss']]
            yago2geo = pd.read_csv('data/yago2geo_type.csv')
            osm = pd.read_csv('data/osm_types_full.csv')
        else:
            yago2 = pd.read_csv('data/yago2_property.csv')
            yago2geo = pd.read_csv('data/yago2geo_property.csv')
            osm = pd.read_csv('data/osm_properties_full.csv')
        self.df = pd.concat([yago2, yago2geo, osm])
        self.df['gloss'] = self.df.gloss.apply(str)
        self.mgloss = DocumentSimilarity(list(self.df['gloss']))
        self.df['label'] = self.df.label.apply(str)
        self.mlabel = Matching(labels=list(self.df['label']))
        self.df['class'] = self.df['class'].apply(str)

    # exact matching to labels
    def exact_matching(self, value):
        labels = list(self.df.label.unique())
        if value.strip().lower() in labels:
            return True
        return False

    # similarity matching using BERT vectors to labels -- can be pre calculated for labels
    def label_similarity(self, value):
        match = self.mlabel.match(value)
        return match

    # similarity matching using BERT document similarity -- check whether you can calculate it or not
    def gloss_similarity(self, value):
        # value to definition
        definition = Definition.get_meaning(value)
        logging.debug(definition)
        return self.mgloss.most_similar(definition)

    def mapping(self, value):
        matched = []
        value = singularize(value)
        if self.exact_matching(value):
            matched.extend(list(self.df[self.df['label'] == value]['class'].apply(str)))
        labels = self.label_similarity(value)
        if len(labels) > 0:
            logging.debug(labels)
            matched.extend(list(self.df[self.df['label'].isin(labels)]['class'].apply(str)))
        glosses = self.gloss_similarity(value)
        matched.extend(self.df[self.df['gloss'].isin(glosses)]['class'].apply(str))
        if len(matched) > 10:
            return matched[:10]
        return matched


if __name__ == "__main__":
    result = {}
    with open('data/name-type-attribute.json', 'r', encoding='utf-8') as fp:
        data = json.load(fp)
    types = data['type']

    mapping = MappingPipeline()
    for type in types:
        result[type] = list(set(mapping.mapping((type))))
        print('{0} matched to {1}'.format(type, result[type]))
    with open('data/type-matching.json', 'w') as f:
        json.dump(result, f)

    print('matching properties')
    mapping = MappingPipeline(type='properties')
    properties = data['attribute']
    result = {}
    for property in properties:
        result[property] = list(set(mapping.mapping(property)))
        print('{0} matched to {1}'.format(property, result[property]))
    with open('data/property-matching.json', 'w') as f:
        json.dump(result, f)
