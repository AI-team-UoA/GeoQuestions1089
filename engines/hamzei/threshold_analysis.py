##############################################
# Author: -- (--)
# Description:
#   Analyze thresholds for ontology mapping
#   Include: Perform a greedy search to find the optimum threshold for type and properties
##############################################

import collections
import functools
import operator
import json
from request_handler import RequestHandler, Configuration
import numpy as np

config = Configuration()
config.set_url('http://45.113.235.161:1313/resolve')
handler = RequestHandler(config)


def mapping(label, thg, thl, key='type'):
    return handler.post_minimal({'key': key, 'value': label, 'thg': thg, 'thl': thl})


def measure(real, matched):
    intersection = len(list(set(real).intersection(matched)))
    if intersection == 0:
        precision = 0
        recall = 0
    else:
        precision = intersection/len(matched)
        recall = intersection/len(real)
    if precision == 0 and recall == 0:
        fscore = 0
    else:
        fscore = 2 * precision * recall / (precision + recall)
    return {'fscore': fscore, 'precision': precision, 'recall': recall}


def average(measures):
    add = dict(functools.reduce(operator.add, map(collections.Counter, measures)))
    return {k: v / len(measures) for k, v in add.items()}


if __name__ == "__main__":
    print('start fixing thresholds')
    with open('evaluation/types-manually-labelled.json', encoding='utf-8') as fp:
        types = json.load(fp)
    with open('evaluation/properties-manually-labelled.json', encoding='utf-8') as fp:
        props = json.load(fp)
    thgs = np.arange(0.5, 1, 0.05).tolist()
    thls = np.arange(0.5, 1, 0.05).tolist()
    measures = []
    result = {}
    is_type = False
    for thg in thgs:
        result[thg] = {}
        for thl in thls:
            if is_type:
                for key, real in types.items():
                    measures.append(measure(real=real, matched=mapping(label=key, thg=thg, thl=thl)))
            else:
                for key, real in props.items():
                    measures.append(measure(real=real, matched=mapping(label=key, thg=thg, thl=thl, key='property')))
            result[thg][thl] = average(measures)
            print('average measure gloss threshold: {0} label threshold:{1}: {2}'.format(thg, thl, result[thg][thl]))
            measures = []
    print('process finished... ')
    if is_type:
        with open('evaluation/type-threshold-results.json', 'w', encoding='utf-8') as fp:
            json.dump(obj=result, fp=fp)
    else:
        with open('evaluation/property-threshold-results.json', 'w', encoding='utf-8') as fp:
            json.dump(obj=result, fp=fp)
