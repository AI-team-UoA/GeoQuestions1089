##############################################
# Author: -- (--)
# Description:
#   Query generator and SPARQL templates
##############################################
import logging
from random import randint

from request_handler import RequestHandler


class SPARQLTemplates:
    PREFIXES = 'PREFIX geo: <http://www.opengis.net/ont/geosparql#>\n' \
               'PREFIX geof: <http://www.opengis.net/def/function/geosparql/>\n' \
               'PREFIX db: <http://spatial.au/ontology#>\n' \
               'PREFIX spatialF: <http://jena.apache.org/function/spatial#>\n' \
               'PREFIX units: <http://www.opengis.net/def/uom/OGC/1.0/>\n' \
               'PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n' \
               'PREFIX owl:<http://www.w3.org/2002/07/owl#>\n' \
               'PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n\n'

    DEFINE_CONCEPT = '\t?<PI> a db:<CONCEPT> .\n' \
                     '\t?<PI> db:name ?<PI>NAME; \n' \
                     '\t\tgeo:hasGeometry ?<PI>GEOM. \n' \
                     '\tFILTER(regex(?<PI>NAME, \"<PIVALUE>\", \"i\" )) .\n'

    GENERAL_FORMAT_ASK = PREFIXES + 'ASK {\n' \
                                    '<WHERE>\n}\n' \
                                    '<GROUP>\n'

    GENERAL_FORMAT_SIMPLE_FUNCTION = PREFIXES + "SELECT <FUNCTION> \n" \
                                                "WHERE { \n" \
                                                "<WHERE>}\n" \
                                                "<GROUP>\n"

    GENERAL_FORMAT_SORT_FUNCTION = PREFIXES + "SELECT DISTINCT <PILIST> \n" \
                                              "WHERE { \n" \
                                              "<WHERE>}\n" \
                                              "<GROUP>\n" \
                                              "<SORT>\n LIMIT <LIMIT>"

    TOPIC_DEFINITION = '\t?<PI> db:<TOPIC> ?<PI><TOPIC>. \n'

    TOPIC_DEFINITION_EXEC = 'VALUES ?<ATTRIBUTE> {<PIURIS>}. \n' \
                            '\t?<PI> ?<ATTRIBUTE> ?<PI><TOPIC>. \n'

    GENERAL_FORMAT = PREFIXES + "SELECT DISTINCT <PILIST> \n" \
                                "WHERE { \n<WHERE>" \
                                "}\n" \
                                '<GROUP>\n'

    DEFINE_TYPE = '\t?<PI> db:type ?<PI>TYPE;\n' \
                  '\t\geo:hasGeometry ?<PI>GEOM;\n' \
                  '\t\tdb:name ?<PI>NAME.\n' \
                  '\tFILTER(regex(?<PI>TYPE, \"<PTVALUE>\", \"i\" )) .\n'

    DEFINE_TYPE_KNOWN = '\t?<PI> rdf:type ?<PI>TYPE;\n' \
                        '\t\tgeo:hasGeometry ?<PI>G .\n' \
                        '\t?<PI>G geo:asWKT ?<PI>GEOM .\n' \
                        '\tVALUES ?<PI>TYPE {<PIURIS>} . \n'

    DEFINE_TYPE_EXCEPT = '\tFILTER NOT EXISTS { ?<PI> (owl:sameAs|^owl:sameAs) ?<EX>}. \n'

    OBJECT_RELATION = '\t?<PI> <OBJ_REL> ?<OBJECT>. \n' \
                      '\t?<OBJECT> db:name ?<OBJECT>NAME;\n' \
                      '\t\tfilter(regex(?<OBJECT>NAME, \"<OBJECT_NAME>\", \"i\" )). \n'

    ATTRIBUTE_COMPARISON = '\tfilter(?<ATTRIBUTE1> <SIGN> ?<ATTRIBUTE2>) . \n'

    KNOWN_RESOURCES = '\tVALUES ?<PI>  {<PIURIS>}. \n' \
                      '\t?<PI> geo:hasGeometry ?<PI>G .\n' \
                      '\t?<PI>G geo:asWKT ?<PI>GEOM .\n'

    ATTRIBUTE_RELATION = '\t?<PI> db:has_<ATTRIBUTE> ?<OBJECT>.\n'

    ATTRIBUTE_RELATION_FULL = '\tVALUES ?<ATTRIBUTE> {<PIURIS>}.\n' \
                              '\t?<PI> ?<ATTRIBUTE> ?<OBJECT>.\n'

    ATTRIBUTE_RELATION_MINIMAL = '\t?<PI> ?<ATTRIBUTE> ?<OBJECT>.\n'

    QUALITY_RELATION = '\t?<PI> a db:<ATTRIBUTE>. \n'

    DISTANCE_ATTRIBUTE = '\tBIND (geof:distance(?<PI1>GEOM, ?<PI2>GEOM, units:meter) as ?<PI1>distance).\n'

    DISTANCE_RELATIONSHIP = '\tFILTER(geof:distance(?<PI1>GEOM, ?<PI2>GEOM, <UNIT>) < <DISTANCE>).\n'

    SPATIAL_RELATION_MAPPING = {'in': '\tFILTER (geof:sfContains(?<PI2>GEOM, ?<PI1>GEOM)).\n',
                                'of': '\tFILTER (geof:sfContains(?<PI2>GEOM, ?<PI1>GEOM)).\n',
                                'part of': '\tFILTER (geof:sfContains(?<PI2>GEOM, ?<PI1>GEOM)).\n',
                                'near': '\tFILTER (geof:distance(?<PI1>GEOM, ?<PI2>GEOM, units:metre) < 5000).\n',
                                'close to': '\tFILTER (geof:distance(?<PI1>GEOM, ?<PI2>GEOM, units:metre) < 5000).\n',
                                'north of': '\tFILTER (spatialF:northGeom(?<PI1>GEOM, ?<PI2>GEOM)).\n',
                                'south of': '\tFILTER (spatialF:southGeom(?<PI1>GEOM, ?<PI2>GEOM)).\n',
                                'east of': '\tFILTER (spatialF:eastGeom(?<PI1>GEOM, ?<PI2>GEOM)).\n',
                                'west of': '\tFILTER (spatialF:westGeom(?<PI1>GEOM, ?<PI2>GEOM)).\n',
                                'southeast of': '\tFILTER (spatialF:southGeom(?<PI1>GEOM, ?<PI2>GEOM)).\n'
                                                '\tFILTER (spatialF:eastGeom(?<PI1>GEOM, ?<PI2>GEOM)).\n',
                                'southwest of': '\tFILTER (spatialF:southGeom(?<PI1>GEOM, ?<PI2>GEOM)).\n'
                                                '\tFILTER (spatialF:westGeom(?<PI1>GEOM, ?<PI2>GEOM)).\n',
                                'northeast of': '\tFILTER (spatialF:northGeom(?<PI1>GEOM, ?<PI2>GEOM)).\n'
                                                '\tFILTER (spatialF:eastGeom(?<PI1>GEOM, ?<PI2>GEOM)).\n',
                                'northwest of': '\tFILTER (spatialF:northGeom(?<PI1>GEOM, ?<PI2>GEOM)).\n'
                                                '\tFILTER (spatialF:westGeom(?<PI1>GEOM, ?<PI2>GEOM)).\n',
                                'border': '\tFILTER(geof:sfTouches(?<PI1>GEOM,?<PI2>GEOM)).\n',
                                'borders': '\tFILTER(geof:sfTouches(?<PI1>GEOM,?<PI2>GEOM)).\n',
                                'discharge': '\tFILTER(geof:sfTouches(?<PI1>GEOM,?<PI2>GEOM)).\n',
                                'discharges': '\tFILTER(geof:sfTouches(?<PI1>GEOM,?<PI2>GEOM)).\n',
                                'cross': '\tFILTER(geof:sfCrosses(?<PI1>GEOM,?<PI2>GEOM)).\n',
                                'crosses': '\tFILTER(geof:sfCrosses(?<PI1>GEOM,?<PI2>GEOM)).\n',
                                'flow': '\tFILTER(geof:sfCrosses(?<PI1>GEOM,?<PI2>GEOM)).\n',
                                'flows': '\tFILTER(geof:sfCrosses(?<PI1>GEOM,?<PI2>GEOM)).\n',
                                'OTHER': '\tFILTER(geof:sfIntersects(?<PI1>GEOM,?<PI2>GEOM)).\n'}

    GROUP_BY_HAVING = 'GROUP BY ?<PI1> \nHAVING (count(DISTINCT ?<PI2>) > <COUNT>)'

    UNIT_MAPPING = {'km': 'kilometer', 'm': 'meter'}


class SPARQLGenerator:
    COMPLEX_RELATIONSHIPS = ['within \d.* ',
                             'at most \d.* ',
                             'less than \d.* away ',
                             'more than \d.* away ',
                             'in \d.* radius ',
                             'in a range of \d.* ',
                             'in the range of \d.* ']

    def __init__(self, dependencies, variables, executable=False, config=None):
        self.dependencies = dependencies
        self.variables = variables
        self.group_by = ''
        self.select_substitute = ''
        self.concept_varids = {}
        self.executable = executable
        self.config = config
        self.handler = None
        if self.executable:
            self.handler = RequestHandler(config)

    def to_SPARQL(self):
        template = SPARQLTemplates.GENERAL_FORMAT
        criteria = self.dependencies['criteria']
        intents = self.dependencies['intent']
        is_ask = False
        if len(intents) == 1 and intents[0].arg1.role == '8':
            template = SPARQLTemplates.GENERAL_FORMAT_ASK
            is_ask = True
        elif intents[0].arg1.role != '6':
            sort = None
            for criterion in criteria:
                if criterion.relation.link == 'SUPERLATIVE' and criterion.arg1.name in self.variables.keys():
                    template = SPARQLTemplates.GENERAL_FORMAT_SORT_FUNCTION
                    sort = self.construct_sort(criterion)
                elif criterion.relation.name in ['closest to', 'nearest to', 'farthest to'] and \
                        criterion.relation.role == 'R':
                    template = SPARQLTemplates.GENERAL_FORMAT_SORT_FUNCTION
                    sort = self.construct_sort(criterion)

            if sort is not None:
                template = template.replace('<SORT>', sort).replace('<LIMIT>', '1')

        # use declaration to define vars in where-clause
        # use criteria to bound them in where-clause
        where_clause = self.construct_where()

        # use intent to construct select/ask statements
        # define overall template (sorting?) (functions?)
        if '<PILIST>' in template:
            select = self.construct_select()
            template = template.replace('<PILIST>', select)

        template = template.replace('<WHERE>', where_clause)
        # return the results

        template = template.replace('<GROUP>', self.group_by)
        if is_ask and self.group_by != '':
            template = template.replace('ASK {', 'ASK {\nSELECT * \nWHERE {')
            template = template + '}\n'

        return template

    def declare(self):
        declare_statments = ''
        varid = 0
        declarations = self.dependencies['declaration']
        for declaration in declarations:
            if declaration.arg2.nodeType == 'VARIABLE':
                if declaration.arg1.role in ['p', 'e']:
                    declare_statments += self.define_variable(declaration)
            else:
                self.concept_varids[declaration.arg1.name] = 'c' + str(varid)
                varid += 1
                declare_statments += self.define_concept(declaration,
                                                         self.concept_varids[declaration.arg1.name])
        return declare_statments

    def define_concept(self, dependency, varid):
        if not self.executable:
            template = SPARQLTemplates.DEFINE_CONCEPT
            template = template.replace('<PI>', varid)
            template = template.replace('<CONCEPT>', dependency.arg2.name)
            template = template.replace('<PIVALUE>', dependency.arg1.name)
        else:
            uris = self.handler.post_minimal({'key': 'toponym', 'value': dependency.arg1.name})
            template = SPARQLTemplates.KNOWN_RESOURCES
            template = template.replace('<PI>', varid)
            template = template.replace('<PIURIS>', ' '.join(['<' + uri + '>' for uri in list(set(uris))]))
        return template

    def construct_sort(self, superlative):
        role = None
        try:
            role = superlative.relation.role
        except:
            print('superlative has no role...')
        if role is not None and role == 'R':
            resolver = AdjectiveResolver(superlative.relation.name)
        else:
            resolver = AdjectiveResolver(superlative.arg2.name)
        var_id = self.variables[superlative.arg1.name]
        topic = resolver.get_type()
        sort = ''
        if superlative.arg1.role in ['P', 'p']:
            sort = 'ORDER BY ' + resolver.asc_or_desc().upper() + '(?' + var_id + topic + ')'
        else:
            sort = 'ORDER BY ' + resolver.asc_or_desc().upper() + '(?' + var_id + ')'
        return sort

    def define_variable(self, dependency):
        if not self.executable:
            template = SPARQLTemplates.DEFINE_TYPE
            template = template.replace('<PI>', dependency.arg2.name)
            template = template.replace('<PTVALUE>', dependency.arg1.name)
        else:
            uris = self.handler.post_minimal({'key': 'type', 'value': dependency.arg1.name})
            template = SPARQLTemplates.DEFINE_TYPE_KNOWN
            template = template.replace('<PI>', dependency.arg2.name)
            template = template.replace('<PIURIS>', ' '.join(['<' + uri + '>' for uri in list(set(uris))]))
        return template

    def define_attribute(self, dependency, simple=True):
        if not self.executable:
            template = SPARQLTemplates.ATTRIBUTE_RELATION
            if simple:
                template = template.replace('<PI>', self.find_var(dependency.arg2.name)) \
                    .replace('<OBJECT>', self.find_var(dependency.arg1.name)) \
                    .replace('<ATTRIBUTE>', dependency.arg1.name.replace(' ', '_'))
            else:
                template = template.replace('<PI>', self.find_var(dependency.arg1.name)) \
                    .replace('<OBJECT>', self.find_var(dependency.arg2.name)) \
                    .replace('<ATTRIBUTE>', dependency.arg2.name.replace(' ', '_'))
        else:
            template = SPARQLTemplates.ATTRIBUTE_RELATION_FULL
            var_attribute = 'a' + str(randint(100, 999))
            template = template.replace('<ATTRIBUTE>', var_attribute)

            if simple:
                template = template.replace('<PI>', self.find_var(dependency.arg2.name))
                template = template.replace('<OBJECT>', self.find_var(dependency.arg1.name))
                uris = self.handler.post_minimal({'key': 'property', 'value': dependency.arg1.name})
            else:
                template = template.replace('<PI>', self.find_var(dependency.arg1.name))
                template = template.replace('<OBJECT>', self.find_var(dependency.arg2.name))
                uris = self.handler.post_minimal({'key': 'property', 'value': dependency.arg2.name})
            template = template.replace('<PIURIS>', ' '.join(['<' + uri + '>' for uri in list(set(uris))]))
        return template

    def find_var(self, string):
        if string in self.variables.keys():
            return self.variables[string]
        if string in self.concept_varids.keys():
            return self.concept_varids[string]
        for concept in self.concept_varids.keys():
            if concept.startswith(string) or string.startswith(concept):
                return self.concept_varids[concept]

    def define_spatial_relationship(self, dependency, distance=False, binary=True):
        try:
            template = ''
            var1 = self.find_var(dependency.arg1.name)
            var2 = self.find_var(dependency.arg2.name)

            if binary and not distance:
                if dependency.relation.name in SPARQLTemplates.SPATIAL_RELATION_MAPPING.keys():
                    template = SPARQLTemplates.SPATIAL_RELATION_MAPPING[dependency.relation.name]
                    template = template.replace("<PI1>", var1).replace("<PI2>", var2)
                elif dependency.relation.name in ['closest to', 'nearest to', 'farthest to']:
                    template = SPARQLTemplates.DISTANCE_ATTRIBUTE
                    template = template.replace("<PI1>", var1).replace("<PI2>", var2)
            elif distance:
                template = SPARQLTemplates.DISTANCE_RELATIONSHIP
                template = template.replace("<PI1>", var1).replace("<PI2>", var2)
                measure = dependency.extra[0]
                val_unit = measure.name.replace('most ', '').strip().split()
                val = val_unit[0]
                unit = val_unit[1]
                if unit in SPARQLTemplates.UNIT_MAPPING.keys():
                    unit = SPARQLTemplates.UNIT_MAPPING[unit]
                if unit.endswith('s') and unit not in SPARQLTemplates.UNIT_MAPPING.values():
                    unit = unit[:-1]
                template = template.replace('<DISTANCE>', val).replace('<UNIT>', 'units:' + unit)
            return template
        except:
            logging.error('error in formulating spatial relationships')
            return ''

    def construct_where(self):
        where_clause = ''

        where_clause += self.declare()

        criteria = self.dependencies['criteria']
        for criterion in criteria:
            if criterion.relation.link == 'AND/OR':
                continue
            if criterion.relation.name == 'IS/ARE':
                if criterion.relation.link == 'SUPERLATIVE' and criterion.arg1.role in ['p', 'P']:
                    resolver = AdjectiveResolver(criterion.arg2.name)
                    topic = resolver.get_type()
                    if not self.executable:
                        where_clause += SPARQLTemplates.TOPIC_DEFINITION.replace('<TOPIC>', topic) \
                            .replace('<PI>', self.variables[criterion.arg1.name])
                    else:
                        var_attribute = 'a' + str(randint(100, 999))
                        uris = self.handler.post_minimal({'key': 'property', 'value': topic})
                        template = SPARQLTemplates.TOPIC_DEFINITION_EXEC.replace('<TOPIC>', topic) \
                            .replace('<PI>', self.variables[criterion.arg1.name]).replace('<ATTRIBUTE>', var_attribute)
                        where_clause += template.replace('<PIURIS>', ' '.join(['<' + uri + '>' for uri in
                                                                               list(set(uris))]))
                else:
                    if criterion.arg1.role == 'p':
                        if not self.executable:
                            where_clause += SPARQLTemplates.QUALITY_RELATION.replace('<ATTRIBUTE>', criterion.arg2.name) \
                                .replace('<PI>', self.variables[criterion.arg1.name])
                        else:
                            uris = self.handler.post_minimal({'key': 'type', 'value': criterion.arg2.name + ' ' +
                                                                                      criterion.arg1.name})
                            template = SPARQLTemplates.DEFINE_TYPE_KNOWN
                            ptype = self.find_var(criterion.arg1.name)
                            template = template.replace('<PI>', ptype)
                            template = template.replace('<PIURIS>',
                                                        ' '.join(['<' + uri + '>' for uri in list(set(uris))]))
                            where_clause += template

            elif criterion.relation.name in ['have', 'has'] and criterion.relation.role == 's':
                where_clause += self.define_attribute(criterion, simple=False)
            elif criterion.relation.link == 'PROPERTY':
                where_clause += self.define_attribute(criterion)
            elif criterion.relation.link == 'NOT':
                where_clause += SPARQLTemplates.DEFINE_TYPE_EXCEPT.replace('<PI>', self.variables[criterion.arg1.name]) \
                    .replace('<EX>', self.find_var(criterion.arg2.name))
            elif criterion.relation.role == 'R':  # spatial relationships
                if len(criterion.extra) == 0:
                    where_clause += self.define_spatial_relationship(criterion)
                else:
                    where_clause += self.define_spatial_relationship(criterion, distance=True)
            elif criterion.relation.role in ['<', '>', '<>', '=', '>=', '<=']:  # comparison
                where_clause += self.define_comparison(criterion)

        return where_clause

    def construct_select(self):
        pis = ''
        for intent in self.dependencies['intent']:
            if intent.arg2.name in self.variables.keys():
                var_id = self.variables[intent.arg2.name]
            else:
                if intent.arg2.name in self.concept_varids.keys():
                    var_id = self.concept_varids[intent.arg2.name]
                else:
                    var_id = intent.arg2.name
            if intent.arg1.role == '6':  # how many
                pis += '(COUNT(distinct ?' + var_id + ') as ?count' + var_id + ')'
            elif intent.arg1.role == '1':  # where
                pis += '?' + var_id + 'GEOM '
            else:
                pis += '?' + var_id + ' '
        return pis

    def define_comparison(self, dependency):
        comparison = ''
        sign = ''
        if dependency.relation.role == '<>':
            resolver = AdjectiveResolver(dependency.relation.name)
            if resolver.asc_or_desc() == 'asc':
                sign = '<'
            else:
                sign = '>'
            if dependency.arg2.role in ['p', 'P']:
                topic = resolver.get_type()
                if not self.executable:
                    template = SPARQLTemplates.ATTRIBUTE_RELATION
                    template = template.replace('<PI>', self.find_var(dependency.arg1.name)) \
                        .replace('<OBJECT>', self.find_var(dependency.arg1.name) + topic) \
                        .replace('<ATTRIBUTE>', topic.replace(' ', '_').upper())
                    comparison += template
                    template = SPARQLTemplates.ATTRIBUTE_RELATION
                    template = template.replace('<PI>', self.find_var(dependency.arg2.name)) \
                        .replace('<OBJECT>', self.find_var(dependency.arg2.name) + topic) \
                        .replace('<ATTRIBUTE>', topic.replace(' ', '_').upper())
                    comparison += template
                else:
                    uris = self.handler.post_minimal({'key': 'property', 'value': topic})
                    template = SPARQLTemplates.ATTRIBUTE_RELATION_FULL
                    var_attribute = 'a' + str(randint(100, 999))
                    template = template.replace('<ATTRIBUTE>', var_attribute)
                    template = template.replace('<PIURIS>', ' '.join(['<' + uri + '>' for uri in list(set(uris))]))
                    template = template.replace('<OBJECT>', self.find_var(dependency.arg1.name) + topic)
                    template = template.replace('<PI>', self.find_var(dependency.arg1.name))
                    comparison += template
                    template = SPARQLTemplates.ATTRIBUTE_RELATION_MINIMAL
                    template = template.replace('<ATTRIBUTE>', var_attribute)
                    template = template.replace('<OBJECT>', self.find_var(dependency.arg2.name) + topic)
                    template = template.replace('<PI>', self.find_var(dependency.arg2.name))
                    comparison += template
                template = SPARQLTemplates.ATTRIBUTE_COMPARISON.replace(
                    '<ATTRIBUTE1>', self.find_var(dependency.arg1.name) + topic).replace(
                    '<ATTRIBUTE2>', self.find_var(dependency.arg2.name) + topic).replace(
                    '<SIGN>', sign)
                comparison += template
                return comparison
        else:
            if dependency.arg2.role in ['n', 'MEASURE']:
                if dependency.arg1.role == 'o':
                    template = SPARQLTemplates.ATTRIBUTE_COMPARISON.replace(
                        '<ATTRIBUTE1>', self.find_var(dependency.arg1.name)).replace(
                        '?<ATTRIBUTE2>', dependency.arg2.name.split()[0]).replace(
                        '<SIGN>', dependency.relation.role)
                    comparison += template
                    return comparison
                elif dependency.arg1.role == 'p':
                    criteria = self.dependencies['criteria']
                    for c in criteria:
                        if c.arg1.name == dependency.arg1.name and c.arg2.role in ['p', 'P']:
                            self.group_by = SPARQLTemplates.GROUP_BY_HAVING.replace('<PI1>',
                                                                                    self.find_var(c.arg2.name))
                            self.group_by = self.group_by.replace('<PI2>', self.find_var(dependency.arg1.name))
                            self.group_by = self.group_by.replace('<COUNT>', dependency.arg2.name.split()[0])
                            break
                        elif c.relation.link == 'prep' and c.relation.role == 'R':
                            if c.arg2.name == dependency.arg1.name:
                                self.group_by = SPARQLTemplates.GROUP_BY_HAVING.replace('<PI1>',
                                                                                        self.find_var(c.arg1.name))
                                self.group_by = self.group_by.replace('<PI2>', self.find_var(dependency.arg1.name))
                                self.group_by = self.group_by.replace('<COUNT>', dependency.arg2.name.split()[0])
                                break
                            elif c.arg1.name == dependency.arg1.name:
                                self.group_by = SPARQLTemplates.GROUP_BY_HAVING.replace('<PI1>',
                                                                                        self.find_var(c.arg2.name))
                                self.group_by = self.group_by.replace('<PI2>', self.find_var(dependency.arg1.name))
                                self.group_by = self.group_by.replace('<COUNT>', dependency.arg2.name.split()[0])
                                break
                elif dependency.arg1.role in ['s', 'R']:
                    criteria = self.dependencies['criteria']
                    for c in criteria:
                        if c.relation.name == dependency.arg1.name:
                            self.group_by = SPARQLTemplates.GROUP_BY_HAVING
                            if c.arg1.role == 'p' or c.arg2.role != 'p':
                                self.group_by = self.group_by.replace('<PI1>', self.find_var(c.arg1.name))
                                self.group_by = self.group_by.replace('<PI2>', self.find_var(c.arg2.name))
                            else:
                                self.group_by = self.group_by.replace('<PI1>', self.find_var(c.arg2.name))
                                self.group_by = self.group_by.replace('<PI2>', self.find_var(c.arg1.name))
                            self.group_by = self.group_by.replace('<COUNT>', dependency.arg2.name.split()[0])

        return comparison


class AdjectiveResolver:
    TYPE = ['elevation', 'distance', 'length', 'area', 'age', 'type', 'population']

    def __init__(self, adjective, superlative=True):
        self.adjective = adjective
        self.superlative = superlative

    def get_type(self):
        if 'elevated' in self.adjective or 'highest' in self.adjective or 'lowest' in self.adjective or \
                'higher' in self.adjective or 'lower' in self.adjective:
            return AdjectiveResolver.TYPE[0]
        elif 'closest' in self.adjective or 'nearest' in self.adjective or 'farthest' in self.adjective or \
                'closer' in self.adjective or 'nearer' in self.adjective or 'farther' in self.adjective:
            return AdjectiveResolver.TYPE[1]
        elif 'longest' in self.adjective or 'longer' in self.adjective:
            return AdjectiveResolver.TYPE[2]
        elif 'largest' in self.adjective or 'biggest' in self.adjective or 'smallest' in self.adjective or \
                'larger' in self.adjective or 'smaller' in self.adjective or 'bigger' in self.adjective:
            return AdjectiveResolver.TYPE[3]
        elif 'oldest' in self.adjective or 'newest' in self.adjective or \
                'older' in self.adjective or 'newer' in self.adjective:
            return AdjectiveResolver.TYPE[4]
        elif 'populated' in self.adjective:
            return AdjectiveResolver.TYPE[6]
        return AdjectiveResolver.TYPE[5]

    def asc_or_desc(self):
        if 'close' in self.adjective or 'near' in self.adjective or 'small' in self.adjective or \
                'new' in self.adjective or 'low' in self.adjective or 'least' in self.adjective:
            return 'asc'
        return 'desc'

    def is_distance(self):
        if self.get_type() == AdjectiveResolver.TYPE[1]:
            return True
        return False
