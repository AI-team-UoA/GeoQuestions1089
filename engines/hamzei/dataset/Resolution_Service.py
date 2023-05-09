#########################################################################################
# @Author: --
# @Description: Use Ontology mapping and concept identification to match extracted information to KB
# @Usage: Perform ontology mapping and concept identification -- Base webservice
#########################################################################################
import json
import logging

import flask
from flask_cors import CORS

import Concept_Identification as CI
from Ontology_Mapping import MappingPipeline as Mapping

typeMapping = Mapping()
propertyMapping = Mapping(type='attribute')
app = flask.Flask(__name__)
CORS(app)
app.config["DEBUG"] = True


@app.route('/test', methods=['GET'])
def test():
    results = {}
    results['cafe'] = typeMapping.mapping('cafe')
    results['population'] = propertyMapping.mapping('population')
    results['London'] = CI.from_SOLR('London')
    return results


@app.route('/resolve', methods=['POST'])
def resolve():
    req_vals = flask.request.get_json()
    print(req_vals)
    if 'value' not in req_vals.keys() or 'key' not in req_vals.keys():
        flask.abort(400)
    key = str(req_vals.get('key'))
    value = str(req_vals.get('value'))
    result = None
    if key == 'type':
        result = typeMapping.mapping(value)
    elif key == 'property':
        result = propertyMapping.mapping(value)
    elif key == 'toponym':
        result = CI.from_SOLR(value)
    else:
        logging.error('The key is invalid, it should "type", "property" or "toponym"')
        flask.abort(400)
    return json.dumps(result)


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8070)
