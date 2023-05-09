import flask
import requests

app = flask.Flask(__name__)
app.config["DEBUG"] = True


@app.after_request
def after_request(response):
    response.headers.add('Access-Control-Allow-Origin', '*')
    response.headers.add('Access-Control-Allow-Headers', 'Content-Type')
    return response


@app.route('/test', methods=['GET'])
def test_forward():
    params = {'query': 'SELECT ?s ?p ?r WHERE {?s ?p ?r} LIMIT 10'}
    r = requests.get('http://localhost:3030/ds', params)
    return r.json()


@app.route('/query', methods=['POST'])
def forward_query():
    print(flask.request)
    if not flask.request.form or not 'query' in flask.request.form:
        flask.abort(400)
    query = str(flask.request.form['query'])
    params = {'query': query}
    r = requests.get('http://localhost:3030/ds', params)
    return r.json()


@app.route('/queryJSON', methods=['POST'])
def forward_json_query():
    req_vals = flask.request.get_json()
    if 'query' not in req_vals.keys():
        flask.abort(400)
    r = requests.get('http://localhost:3030/ds', req_vals)
    return r.json()


app.run(host="0.0.0.0", port=3031)
