#########################################################################################################
# @Author: --
# @Description: Generate SPARQL queries with minimal efforts
# @Usage: Used for querying the Yago2 Ontology
#################################################################################################
import pandas as pd


class SPARQL:
    def __init__(self, where_clause, prefix_dict={}, select_vars=[]):
        self.prefix = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " \
                      "\nPREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " \
                      "\nPREFIX yago: <http://yago-knowledge.org/resource/>"
        self.add_prefix(prefix_dict)
        self.template = self.prefix + "\nSELECT <VARS> \n WHERE {\n <WHERE> \n}"
        self.where = where_clause
        if len(select_vars) == 0:
            self.select_vars = '*'
        else:
            self.select_vars = ' '.join(select_vars)

    def formulate(self):
        return self.template.replace('<VARS>', self.select_vars).replace('<WHERE>', self.where)

    def add_prefix(self, prefix_dict):
        for key, val in prefix_dict.items():
            self.prefix += "\nPREFIX " + key + ': <' + val + "> "


class Results:
    def __init__(self, raw):
        self.raw = raw

    def get_dataframe(self):
        return pd.DataFrame(self.to_dict_list())

    def to_dict_list(self):
        header = self.raw['head']['vars']
        bindings = self.raw['results']['bindings']
        results = []
        for binding in bindings:
            temp = {}
            for var in header:
                temp[var] = binding[var]['value']
            results.append(temp)
        return results
