#########################################################################################################
# @Author: --
# @Description: calculate cosine similarity between two descriptions
# @Usage: used to perform semantic matching between the predefined ontology and extracted information
#########################################################################################################
import json
import logging

from bert_embedding import BertEmbedding
from sklearn.metrics.pairwise import cosine_similarity

logging.basicConfig(level=logging.WARN)

bert_embedding = BertEmbedding()


def write_json(object, address):
    with open(address, 'w', encoding='utf-8') as f:
        json.dump(object, f)


def label_vectors(labels, persist_as_json=False, file_address='data/type_label_vectors.json'):
    vectors = bert_embedding(labels)
    res = {labels[i]: list(vectors[i][1][0]) for i in range(len(labels))}
    if persist_as_json:
        write_json(res, file_address)
    return res


def label_vectors_list(labels):
    embs = bert_embedding(labels)
    vectors = [list(map(average, zip(*embs[i][1]))) for i in range(len(embs))]
    res = []
    res.append(labels)
    res.append(vectors)
    return res


def token_vector(label):
    emb = bert_embedding([label])[0][1]
    return list(map(average, zip(*emb)))


def average(a):
    return sum(a) / len(a)


def similarity(label1, label2):
    vectors = bert_embedding(label1, label2)
    return cosine_similarity(vectors[0], vectors[1])


def similarity(vec1, vec2):
    return cosine_similarity([vec1], [vec2])[0][0]


def similarities(vec, vec_list):
    return cosine_similarity([vec], vec_list)


if __name__ == "__main__":
    values = ['car park', 'car parks', 'drainage area', 'university college', 'university colleges', 'royal boroughs',
              'royal boroughs', 'airfields', 'historic building']
    label_vectors_dict = label_vectors(values)
    print('\t\t' + '\t'.join(values))
    for l in values:
        string = l + '\t'
        for l2 in values:
            string += str(round(similarity(label_vectors_dict[l], label_vectors_dict[l2]), 4)) \
                      + ' \t'
        print(string)
