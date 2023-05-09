#########################################################################################################
# @Author: --
# @Description: calculate cosine similarity between two descriptions
# @Usage: used to perform semantic matching between the predefined ontology and extracted information
#########################################################################################################
import logging

from allennlp.modules.elmo import Elmo, batch_to_ids
from sklearn.metrics.pairwise import cosine_similarity

logging.basicConfig(logging.WARN)

options_file = "https://s3-us-west-2.amazonaws.com/allennlp/models/elmo/2x4096_512_2048cnn_2xhighway/" \
               "elmo_2x4096_512_2048cnn_2xhighway_options.json"
weight_file = "https://s3-us-west-2.amazonaws.com/allennlp/models/elmo/2x4096_512_2048cnn_2xhighway/" \
              "elmo_2x4096_512_2048cnn_2xhighway_weights.hdf5"

# Note the "1", since we want only 1 output representation for each token.
elmo = Elmo(options_file, weight_file, 1, dropout=0)


class EmbeddingSimilarity:
    @staticmethod
    def average(a):
        return sum(a) / len(a)

    @staticmethod
    def cosine(vec1, vec2):
        return cosine_similarity([vec1], [vec2])[0][0]

    def similarity(self, sentence1, sentence2):
        sentences = [list(sentence1.split()), list(sentence2.split())]
        characterids = batch_to_ids(sentences)
        embeddings = elmo(characterids)['elmo_representations'][0]
        s1_embedding = list(map(self.average, zip(*embeddings[0])))
        s2_embedding = list(map(self.average, zip(*embeddings[1])))
        return self.cosine(s1_embedding, s2_embedding)

    def contextualised_similarity(self, context1, context2, phrase1, phrase2):
        context1_tokens = list(context1.lower().split())
        context2_tokens = list(context2.lower().split())
        phrase1_tokens = list(phrase1.lower().split())
        phrase2_tokens = list(phrase2.lower().split())
        idx1 = context1_tokens.index(phrase1_tokens[0])
        idx2 = context2_tokens.index(phrase2_tokens[0])
        sentences = [context1_tokens, context2_tokens]
        characterids = batch_to_ids(sentences)
        embeddings = elmo(characterids)['elmo_representations'][0]
        ph1_embedding = list(map(self.average, zip(*embeddings[0][idx1:idx1 + len(phrase1_tokens)])))
        ph2_embedding = list(map(self.average, zip(*embeddings[1][idx2:idx2 + len(phrase2_tokens)])))
        return self.cosine(ph1_embedding, ph2_embedding)


def init():
    string = 'This is a test to load ELMo.'
    characterids = batch_to_ids([string])
    embeddings = elmo(characterids)['elmo_representations'][0]
    print('init done: {}'.format(embeddings))
