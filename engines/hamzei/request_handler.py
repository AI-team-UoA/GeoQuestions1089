#########################################################################################################
# @Author: --
# @Description: Request manager -- GET request handler
# @Usage: Used in Client.py as grounding library
#########################################################################################################
import json

import requests


class Configuration:
    def __init__(self, ip='localhost', port='3030', datasource='ds'):
        self.ip = ip
        self.port = port
        self.datasource = datasource
        self.endpoint = 'http://' + self.ip + ':' + self.port + '/' + self.datasource

    def get_url(self):
        return self.endpoint

    def set_url(self, endpoint):
        self.endpoint = endpoint


class RequestHandler:
    def __init__(self, config):
        self.config = config

    def request(self, query):
        params = {'query': query}
        r = requests.get(self.config.get_url(), params)
        return r.json()

    def post_query(self, query):
        params = {'query': query}
        r = requests.post(self.config.get_url(), params)
        return r.json()

    def post(self, params, headers):
        r = requests.post(self.config.get_url(), params, headers=headers)
        return r.json()

    def post_minimal(self, params):
        r = requests.post(self.config.get_url(), data=json.dumps(params), headers={'Content-type': 'application/json'})
        return r.json()
