# Translating Place-Related Questions to GeoSPARQL Queries (TheWebConf 2022)

## Abstract
Many place-related questions can only be answered by complex spatial reasoning, a task poorly supported by factoid question retrieval. Such reasoning using combinations of spatial and non-spatial criteria pertinent to place-related questions is increasingly possible on linked data knowledge bases. Yet, to enable question answering based on linked knowledge bases, natural language questions must first be re-formulated as formal queries. Here, we first present an enhanced version of YAGO2geo, the geospatially-enabled variant of the YAGO2 knowledge base, by linking and adding more than one million places from OpenStreetMap data to YAGO2. We then propose a novel approach to translate the place-related questions into logical representations, theoretically grounded in the _core concepts of spatial information_. Next, we use a dynamic template-based approach to generate fully executable GeoSPARQL queries from the logical representations. We test our approach using the Geospatial Gold Standard dataset and report substantial improvements over existing methods.

## Installation

### Requirements

The program is implemented in python (version 3.8.6). Several libraries should be installed (use pip command to install
the following libraries) before running the code:

```bash
pip install numpy
pip install pandas
pip install geopandas
pip install allennlp
pip install allennlp-models
pip install flask
pip install flask_cors
pip install scikit-learn
pip install quantulum3
pip install anytree
pip install document_similarity
pip install Pattern
pip install pysolr
```

## Usage

This project includes several scripts to parse questions.

Run the stand-alone script:

```python
python geoparser.py
```

Run the web service for query generation:

```python
python query_generator_service.py
```

Note that you need GeoSPARQL and SOLR servers running (check the dataset folder for creating solr indexes and ttl files)
To generate executable queries you should make sure that Apache Solr is working using the solr_index.
To generate answers, we used Apache GeoSARQL Fuseki and loaded Yago2, Yago2Geo and our dataset. Then executable queries can be used to retrieve the answers.
## License

[MIT](https://opensource.org/licenses/MIT)

## Οδηγίες

1. Τρέχεις το `query_generator_service.py` αφού πρώτα του έχεις δώσει IP address.
2. Τρέχεις το `geoparser.py` δίνοντας την IP address του προηγούμενου βηματος επειδή εκείνο το component κάνει τα queries executable.

SOLR είναι στημμένο από τον Hamzei, Fuseki δεν χρειάζεται. Αν πέσει του Hamzei πρέπει να στήσεις δικό σου χρησιμοποιώντας το `solr_index`.
