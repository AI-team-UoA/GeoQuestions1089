##############################################
# Author: -- (--)
# Description:
#   Query generator web service
#   Include: test web service and query generator service
##############################################
import json
import logging

import flask
from flask_cors import CORS

import concept_identification as CI
import geoparser
from ontology_mapping import MappingPipeline as Mapping

app = flask.Flask(__name__)
CORS(app)
app.config["DEBUG"] = True

typeMapping = Mapping()
propertyMapping = Mapping(type='attribute')
app = flask.Flask(__name__)
CORS(app)
app.config["DEBUG"] = True


@app.route('/testresolve', methods=['GET'])
def test_resolve():
    results = {}
    results['cafe'] = typeMapping.mapping('cafe')
    results['population'] = propertyMapping.mapping('population')
    results['London'] = CI.from_SOLR('Pallini')
    return results


@app.route('/resolve', methods=['POST'])
def resolve():
    req_vals = flask.request.get_json()
    print(req_vals)
    if 'value' not in req_vals.keys() or 'key' not in req_vals.keys():
        result = ""
        return json.dumps(result)
    key = str(req_vals.get('key'))
    value = str(req_vals.get('value'))
    result = None
    thl = None
    thg = None
    try:
        if key == 'type':
            result = list(set(typeMapping.mapping(value, thl=0.55, thg=0.55)))
            if len(result) == 0:
                result = list(set(typeMapping.mapping(value, thl=0.45, thg=0.40)))
        elif key == 'property':
            result = list(set(propertyMapping.mapping(value, thl=0.8, thg=0.8)))
            if len(result) == 0:
                result = list(set(propertyMapping.mapping(value, thl=0.45, thg=0.40)))
        elif key == 'toponym':
            result = list(set(CI.from_SOLR(value)))
        else:
            logging.error('The key is invalid, it should "type", "property" or "toponym"')
            result = ""
        return json.dumps(result)
    except:
        result = ""
        return json.dumps(result)


@app.route('/test', methods=['GET'])
def test():
    result = geoparser.analyze('Where is the highest building in UK?')
    return result


@app.route('/question', methods=['POST'])
def query_generator():
    try:
        req_vals = flask.request.get_json()
        print(req_vals)
        if not 'question' in req_vals.keys():
            flask.abort(400)
        question = str(req_vals.get('question'))
        if 'execute' in req_vals.keys():
            result = geoparser.analyze(question, executable=True)
        else:
            result = geoparser.analyze(question)
        return json.dumps(result)
    except:
        flask.abort(400)


if __name__ == "__main__":
    app.run(host='localhost', port=1313)
