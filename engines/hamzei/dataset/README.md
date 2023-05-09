# Fuseki Client
A python client for Apache GeoSPARQL Fuseki that enable send and receive request, ontology matching and getting results from spatially-aware knowledge base

## Installation
### Requirements
```bash
pip install numpy
pip install pandas
pip install allennlp
pip install requests
pip install scikit-learn
pip install bert_embedding
pip install clean_text
pip install sentence_transformers
pip install osm2geojson
pip install geomet
```

## Usage
This project includes several scripts to calculate description similarity, perform ontology matching, and connect to Fuseki Server.

### Word Embedding Similarity
Several similarity approaches are implemented in this package using ELMo and BERT.

#### ELMo-Based Similarity
Word embedding similarity script is the basis for ontology mapping. The extracted information from the questions can be mapped to pre-defined ontology using the calculated word embedding similarities.
```python
import Embedding_Similarity

emb = Embedding_Similarity.EmbeddingSimilarity()

# simple phrase or sentence similarity
print(emb.similarity('children hospital', 'health care'))

# contextualised similarity between phrases or sentences
context1 = 'where can I find a children hospital in London ?'
context2 = 'Health care services includes public or private hospitals .'

print(emb.contextualised_similarity(context1, context2, 'children hospital', 'health care'))
```

#### BERT-Based Similarity (token level)
Word embedding similarity script is the basis for ontology mapping. The extracted information from the questions can be mapped to pre-defined ontology using the calculated word embedding similarities.
```python
import Bert_Similarity as bs

values = ['car park', 'car parks', 'drainage area', 'university college', 'university colleges', 'royal boroughs',
              'royal boroughs', 'airfields', 'historic building']
label_vectors_dict = bs.label_vectors(values)
print('\t\t'+'\t'.join(values))
for l in values:
    string = l+'\t'
    for l2 in values:
        string += str(round(bs.similarity(label_vectors_dict[l], label_vectors_dict[l2]), 4))\
                 +' \t'
    print(string)
```


#### BERT-Based Similarity (sentence/paragraph level)
Word embedding similarity script is the basis for ontology mapping. The extracted information from the questions can be mapped to pre-defined ontology using their definition and glossary information in the knowledge bases.
```python
import DocumentSimilarity
documents = ['', '']  # the document list (paragraph/sentences)

sem = DocumentSimilarity(documents)
ranked = sem.rank_similarity(0)  # similarity of others to document 0
print('\n\n Max similarity is {}'.format(sem.most_similar(0)))  # most similar to document 0
```


### GeoSPARQL Requests
Configuration class is a data structure to produce a connection URL to GeoSPARQL Fuseki server. Request Handler script can fetch the results of a GeoSPARQL query from the available GeoSPARQL Fuseki servers.
```python
from RequestHandler import Configuration, RequestHandler


config = Configuration(ip='localhost', port='3030', datasource='ds')
rq = RequestHandler(config)
sample_query = 'SELECT ?a ?b WHERE {?a ?r ?b} LIMIT 100'
result = rq.request(sample_query)
print(result)
```
