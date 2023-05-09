#########################################################################################################
# @Author: --
# @Description: calculate cosine similarity between two documents
# @Usage: used to perform semantic matching between the predefined ontology and extracted information
#########################################################################################################
import logging
import re

import numpy as np
import pandas as pd
import requests
from PyDictionary import PyDictionary
from nltk.corpus import stopwords
from nltk.corpus import wordnet as wn
from sentence_transformers import SentenceTransformer
from sklearn.metrics.pairwise import cosine_similarity

logging.basicConfig(level=logging.INFO)
stop_words_l = stopwords.words('english')


class Definition:
    @staticmethod
    def get_wn_definition(token):
        whole_def = ''
        synsets = wn.synsets(token.strip().replace(' ', '_'))
        for syn in synsets:
            whole_def += syn.definition() + ' '
        return whole_def

    @staticmethod
    def get_wiki_snippet(token):
        api_template = 'https://en.wikipedia.org/w/api.php?action=query&list=search&prop=images&format=json&' \
                       'srsearch={}&srnamespace=0&srprop=snippet&srlimit=1&imlimit=1'
        try:
            res = requests.get(api_template.format(token))
            api_res = res.json()['query']
            return Definition.__clean(api_res['search'][0]['snippet'])
        except:
            return ''

    @staticmethod
    def get_pydict_meaning(token):
        try:
            m_list = PyDictionary(token.strip().replace(' ', '_')
                                  ).getMeanings()[token.strip().replace(' ', '_')]['Noun']
            return ' '.join(m_list)
        except:
            return ''

    @staticmethod
    def __clean(text):
        cleanr = re.compile('<.*?>|&([a-z0-9]+|#[0-9]{1,6}|#x[0-9a-f]{1,6});')
        cleantext = re.sub(cleanr, '', text)
        return cleantext

    @staticmethod
    def get_meaning(text):
        wn = Definition.get_wn_definition(text)
        wiki = Definition.get_wiki_snippet(text)
        return wn + ' ' + wiki


class DocumentSimilarity:
    def __init__(self, base):
        self.base = base
        self.model = SentenceTransformer('bert-base-nli-mean-tokens')
        self.df = pd.DataFrame(self.base, columns=['documents'])
        self.df['documents_cleaned'] = self.df.documents.apply(lambda x: " ".
                                                               join(re.sub(r'[^a-zA-Z]', ' ', w).lower()
                                                                    for w in x.split() if
                                                                    re.sub(r'[^a-zA-Z]', ' ', w)
                                                                    .lower() not in stop_words_l))
        self.raw_embeddings = self.model.encode(self.df['documents_cleaned'])
        self.similarity_index = None
        self.similarity_vector = None
        self.gloss = None

    def rank(self, gloss):
        self.gloss = gloss
        definition = [gloss]
        embedding = self.model.encode(definition)[0]
        document_embeddings = [embedding]
        document_embeddings.extend(self.raw_embeddings)
        pairwise_similarities = cosine_similarity(document_embeddings)
        if max(pairwise_similarities[0][1:]) < 0.5:
            self.similarity_index = []
            return []
        similar_ix = np.argsort(pairwise_similarities[0])[::-1]
        for ix in similar_ix:
            if ix == 0:
                continue
            logging.debug(f'\nDocument: {self.df.iloc[ix - 1]["documents"]}')
            logging.debug(f'Cosine Similarity : {pairwise_similarities[0][ix]}')
        self.similarity_index = similar_ix
        self.similarity_vector = pairwise_similarities[0]
        logging.debug('max similarity doc: {}'.format(max(pairwise_similarities[0][1:])))
        return similar_ix

    def most_similar(self, gloss=None, th=0.55):
        if self.similarity_index is None or (gloss is not None and self.gloss != gloss):
            self.rank(gloss=gloss)
        if len(self.similarity_index) == 0:
            return []
        logging.debug(self.similarity_index)
        logging.debug(list(self.df["documents"]))
        docs = list(self.df["documents"])
        return [docs[i - 1] for i in self.similarity_index[0:5] if self.similarity_vector[i - 1] > th]

    def flush(self):
        self.similarity_index = None
        self.similarity_vector = None
        self.gloss = None
        logging.debug('Document Similarity is flushed -- you can calculate similarity to new base document')


if __name__ == "__main__":
    base = 'An airfield equipped with control tower and hangars as well as accommodations for passengers and cargo.'
    yago2 = pd.read_csv('data/yago2_type.csv')
    yago2['gloss'] = yago2.gloss.apply(str)
    sem = DocumentSimilarity(list(yago2['gloss']))
    logging.info('**************************************')
    ranked = sem.rank(base)  # similarity of others to document 0
    logging.info('\n\n Max similarity is {}'.format(sem.most_similar()))

    sem.flush()
