##############################################
# Author: -- (--)
# Description:
#   Parse tree (dependency and constituency parsing)
##############################################
import logging
import re

import anytree.cachedsearch as search
from anytree import AnyNode, RenderTree, PostOrderIter


class PlaceQuestionParseTree:
    spatiotemporal_propositions = ['in', 'of', 'on', 'at', 'within', 'from', 'to', 'near', 'close', 'between', 'beside',
                                   'by', 'since', 'until', 'before', 'after', 'close to', 'near to', 'closest to',
                                   'nearest to']
    complex_spatial_propositions = [' within \d.* ',
                                    ' at most \d.* ',
                                    ' less than \d.* away ',
                                    ' more than \d.* away ',
                                    ' in \d.* radius ',
                                    ' in a range of \d.* ',
                                    ' in the range of \d.* ',
                                    ' north ', ' south ', ' east ', ' west ', ' part ',
                                    ' northeast ', ' southeast ', ' northwest ', ' southwest ']

    def __init__(self, parse_dict):
        self.parse_dict = parse_dict
        self.tree = None
        self.root = None
        self.construct_tree()

    def construct_tree(self):
        root = AnyNode(name=self.parse_dict['word'], nodeType=self.parse_dict['nodeType'], role='',
                       spans={'start': 0, 'end': len(self.parse_dict['word'])})
        if 'children' in self.parse_dict.keys():
            for child in self.parse_dict['children']:
                self.add_to_tree(child, root)
        self.root = root
        self.tree = RenderTree(root)

    def add_to_tree(self, node, parent):
        local_start = parent.name.find(node['word'])
        n = AnyNode(name=node['word'], nodeType=node['nodeType'], parent=parent, role='',
                    spans={'start': parent.spans['start'] + local_start,
                           'end': parent.spans['start'] + local_start + len(node['word'])})
        if 'children' in node.keys():
            for child in node['children']:
                self.add_to_tree(child, n)

    def render(self):
        self.tree = RenderTree(self.root)

    def __repr__(self):
        if self.tree is None:
            return "Empty Tree"
        res = ""
        for pre, fill, node in self.tree:
            res += "%s%s (%s) {%s}" % (pre, node.name, node.nodeType, node.role) + "\n"
        return res

    def label_tree(self):
        self.clean_tree()
        res = self.label_conjunctions()
        res = {**res, **self.label_non_platial_objects()}
        res = {**res, **self.label_numbers()}
        self.update()
        return res

    def find_node_by_exact_name(self, string):
        return search.findall_by_attr(self.root, string)

    def find_node_by_name(self, string):
        res = self.find_node_by_exact_name(string)
        if len(res) > 0:
            return res
        return search.findall(self.root, filter_=lambda node: node.name in string.split())

    def label_role(self, name, role, clean=False, question_words=False, comparison=False):
        nodes = self.find_node_by_name(name)
        if len(nodes) == 1:
            nodes[0].role = role
            if question_words:
                nodes[0].nodeType = 'WH'
            if clean:
                nodes[0].children = []
        else:
            min_depth = 1000
            selected = None
            for node in nodes:
                if node.depth < min_depth and node.name not in ['of']:
                    min_depth = node.depth
                    selected = node
                else:
                    node.parent = None
                selected.name = name
                selected.spans = {'start': self.root.name.index(name), 'end': self.root.name.index(name) + len(name)}
                if question_words:
                    selected.nodeType = 'WH'
                elif comparison:
                    selected.nodeType = 'JJR'
                elif not question_words and not comparison:
                    selected.nodeType = 'NP'
                selected.role = role

    def clean_tree(self):
        named_objects = search.findall(self.root, filter_=lambda node: node.role in ("E", "P", "e", "p", "d", "o"))
        for named_object in named_objects:
            if len(named_object.siblings) == 1 and (named_object.siblings[0].nodeType == 'DT'):
                named_object.parent.role = named_object.role
                named_object.parent.name = named_object.name
                named_object.parent.nodeType = named_object.nodeType
                named_object.parent.children = []
            elif len(named_object.siblings) == 1 and named_object.siblings[0].role == named_object.role:
                named_object.parent.role = named_object.role

    def label_spatiotemporal_relationships(self):
        named_objects = search.findall(self.root, filter_=lambda node: node.role in ("P", "p", "d"))
        res_relationships = {}
        for named_object in named_objects:
            for sibling in named_object.siblings:
                if sibling.nodeType == 'IN' and named_object.parent.nodeType in ['PP', 'VP'] and \
                        sibling.name in PlaceQuestionParseTree.spatiotemporal_propositions:
                    if named_object.role == 'd':
                        sibling.role = 'r'
                        if sibling.name + '--' + str(sibling.spans['start']) not in res_relationships.keys():
                            res_relationships[sibling.name + '--' + str(sibling.spans['start'])] = {
                                'start': sibling.spans['start'], 'end': sibling.spans['end'], 'role': 'r', 'pos': 'ADP'}
                    else:  # complex spatial relationship with ['of', 'from', 'to']
                        sibling.role = 'R'
                        if sibling.name in ['of', 'to', 'from']:
                            for reg in PlaceQuestionParseTree.complex_spatial_propositions:
                                pattern = reg + sibling.name
                                regex_search = re.search(pattern, self.root.name)
                                if regex_search is not None:
                                    res_relationships[self.root.name[
                                                      regex_search.regs[0][0]:regex_search.regs[0][1]]
                                                      + '--' + str(regex_search.regs[0][0])] = {
                                        'start': regex_search.regs[0][0],
                                        'end': regex_search.regs[0][1],
                                        'role': 'R',
                                        'pos': 'ADP'}
                                    self.label_complex_spatial_relationships(sibling, pattern)
                                else:
                                    if sibling.name + '--' + str(
                                            sibling.spans['start']) not in res_relationships.keys():
                                        res_relationships[sibling.name + '--' + str(sibling.spans['start'])
                                                          ] = {'start': sibling.spans['start'],
                                                               'end': sibling.spans['end'],
                                                               'role': 'R', 'pos': 'ADP'}
                        else:
                            if sibling.name + '--' + str(sibling.spans['start']) not in res_relationships.keys():
                                res_relationships[sibling.name + '--' + str(sibling.spans['start'])
                                                  ] = {'start': sibling.spans['start'],
                                                       'end': sibling.spans['end'], 'role': 'R', 'pos': 'ADP'}
                    named_object.parent.role = 'LOCATION'
        return res_relationships

    def all_encodings(self):
        res = {}
        roles = search.findall(self.root, filter_=lambda node: node.role != '')
        for role in roles:
            key = role.role
            val = role.name
            if key not in res.keys():
                res[key] = []
            res[key].append(val)
        return res

    def label_complex_spatial_relationships(self, prep, pattern):
        matched = False
        context = prep.parent
        text = ''
        while not matched:
            regex_search = re.search(pattern.strip(), context.name)
            if regex_search is not None:
                matched = True
                text = context.name[regex_search.regs[0][0]: regex_search.regs[0][1]]
                break
            if context.parent is None:
                break
            context = context.parent
        if matched:
            if context.name == text:
                context.role = 'R'
            else:
                nodes = PlaceQuestionParseTree.iterate_and_find(context, text)
                new_node = AnyNode(name=text, nodeType='IN', role='R', spans={'start': nodes[0].spans['start'],
                                                                              'end': nodes[len(nodes) - 1].spans[
                                                                                  'end']})
                before = []
                after = []

                firstparent = nodes[0].parent
                if firstparent != context:
                    for child in context.children:
                        if self.root.name.index(child.name) + len(child.name) <= self.root.name.index(text):
                            before.append(child)

                for child in firstparent.children:
                    if child in nodes:
                        break
                    before.append(child)

                lastparent = prep.parent
                for child in lastparent.children:
                    if child not in nodes:
                        after.append(child)
                while lastparent != context:
                    lastparent = lastparent.parent
                    for child in lastparent.children:
                        if self.root.name.index(text) + len(text) <= self.root.name.index(child.name):
                            after.append(child)
                context.children = []
                for b in before:
                    b.parent = context

                for node in nodes:
                    node.parent = new_node

                new_node.parent = context

                for a in after:
                    a.parent = context

    @staticmethod
    def iterate_and_find(node, text):
        res = []
        for child in node.children:
            if child.name in text:
                res.append(child)
                text = text.replace(child.name, '', 1)
            elif text.strip() != '':
                res.extend(PlaceQuestionParseTree.iterate_and_find(child, text))
        return res

    def label_complex_comparison(self, reg_results, comparison, role):
        contexts = search.findall(self.root, filter_=lambda node: node.spans['start'] <= reg_results.regs[0][0] and
                                                                  node.spans['end'] >= reg_results.regs[0][1])
        context = None
        vals = comparison.split()
        max_depth = -1
        for c in contexts:
            if c.depth >= max_depth:
                context = c
                max_depth = c.depth
        first = search.findall(context, filter_=lambda node: node.name == vals[0])[0]

        if first.parent.children.index(first) + 1 == len(first.parent.children):
            return
        elif first.parent.children[first.parent.children.index(first) + 1].role not in ['p', 'e', 'o']:
            return

        second = search.findall(context, filter_=lambda node: node.name == vals[1])[0]
        if first.parent != second.parent:
            second.parent.name = second.parent.name.replace(second.name, '').strip()
            second.parent = None
            first.parent.name = first.parent.name + ' ' + second.name
            first.parent.spans = {'start': first.parent.spans['start'], 'end': second.spans['end']}
        first.name = comparison
        first.role = role

    def clean_locations(self):
        named_objects = search.findall(self.root, filter_=lambda node: node.role == 'LOCATION')
        if len(named_objects) == 2:
            if named_objects[0].depth < named_objects[1].depth:
                if self.root.name.index(named_objects[0].name) < self.root.name.index(named_objects[1].name):
                    PlaceQuestionParseTree.merge(node1=named_objects[0], node2=named_objects[1])
                else:
                    PlaceQuestionParseTree.merge(node1=named_objects[0], node2=named_objects[1], order=False)
            else:
                if self.root.name.index(named_objects[0].name) < self.root.name.index(named_objects[1].name):
                    PlaceQuestionParseTree.merge(node1=named_objects[1], node2=named_objects[0], order=False)
                else:
                    PlaceQuestionParseTree.merge(node1=named_objects[1], node2=named_objects[0])

    def clean_phrases(self):
        single_child_nodes = search.findall(self.root, filter_=lambda node: len(node.children) == 1)
        for node in single_child_nodes:
            try:
                if node.role == '':
                    node.role = node.children[0].role
                node.nodeType = node.children[0].nodeType
                children = node.children[0].children
                node.children[0].parent = None
                node.children = children
            except:
                print('error in cleaning...')

        incorrect_types = search.findall(self.root, filter_=lambda node: len(node.children) > 0 and
                                                                         node.role in ['p', 'P'])
        for it in incorrect_types:
            if len(search.findall(it, filter_=lambda node: node != it and node.role in ['p', 'P'])) == 0:
                it.role = ''

    @staticmethod
    def merge(node1, node2, order=True):
        node = None
        start = min(node1.spans['start'], node2.spans['start'])
        end = max(node1.spans['end'], node2.spans['end'])
        if order:
            node = AnyNode(name=node1.name + ' ' + node2.name, nodeType=node1.nodeType, role=node1.role,
                           spans={'start': start, 'end': end})
        else:
            node = AnyNode(name=node2.name + ' ' + node1.name, nodeType=node1.nodeType, role=node1.role,
                           spans={'start': start, 'end': end})
        node.parent = node1.parent
        if order:
            node1.parent = node
            node2.parent = node
        else:
            node2.parent = node
            node1.parent = node

    def update(self):
        for node in PostOrderIter(self.root):
            if len(node.children) > 0:
                name = ''
                for child in node.children:
                    name += child.name + ' '
                if node.name != name:
                    node.name = name.strip()
                if len(node.children) == 1 and (node.role == '' or node.role == node.children[0].role) and \
                        node.nodeType == node.children[0].nodeType:
                    node.role = node.children[0].role
                    node.children = node.children[0].children

    def label_non_platial_objects(self):
        npos = search.findall(self.root, filter_=lambda node: node.nodeType.startswith('N') and
                                                              node.role == '' and len(node.children) == 0)
        res = {}
        for npo in npos:
            npo.role = 'o'
            if npo.name in ['border', 'cross', 'crosses', 'borders', 'flow', 'flows']:
                npo.role = 's'

        for npo in npos:
            parent = npo.parent
            if parent is not None:
                all_objects = True
                for child in parent.children:
                    if child.role != 'o' and child.nodeType != 'DT' and child.role != 'p':
                        all_objects = False
                if all_objects:
                    parent.role = 'o'
                    parent.children = []
                    res[parent.name + '--' + str(parent.spans['start'])] = {'start': parent.spans['start'],
                                                                            'end': parent.spans['end'],
                                                                            'role': 'o',
                                                                            'pos': 'NOUN'}
                else:
                    res[npo.name + '--' + str(npo.spans['start'])] = {'start': npo.spans['start'],
                                                                      'end': npo.spans['end'],
                                                                      'role': npo.role,
                                                                      'pos': 'NOUN'}
        return res

    def get_verbs(self):
        verb_nodes = search.findall(self.root,
                                    filter_=lambda node: node.nodeType.startswith("VB") and ' ' not in node.name)
        verbs = []
        for node in verb_nodes:
            verbs.append(node.name)
        return verbs

    def label_situation_activities(self, verbs, decisions):
        res = {}
        verb_nodes = search.findall(self.root,
                                    filter_=lambda node: node.nodeType.startswith("VB") and node.name in verbs)
        for i in range(len(verbs)):
            node = verb_nodes[i]
            decision = decisions[i]
            if decision != 'u' and node.name not in ['is', 'are', 'do', 'does', 'be', 'was', 'were', 'located']:
                node.role = decision
                res[node.name + '--' + str(node.spans['start'])] = {'start': node.spans['start'],
                                                                    'end': node.spans['end'],
                                                                    'role': node.role, 'pos': 'VERB'}
            else:
                print("this verb is suspicious: " + str(node.name))
        situations = search.findall(self.root, filter_=lambda node: node.role == 's')
        for situation in situations:
            for sibiling in situation.siblings:
                if sibiling.role == '' and sibiling.nodeType == 'PP':
                    if len(search.findall(sibiling, filter_=lambda node: node.role in ('e', 'o', 'E'))) > 0:
                        sibiling.role = 's'

        activities = search.findall(self.root, filter_=lambda node: node.role == 'a')
        for activity in activities:
            for sibiling in activity.siblings:
                if sibiling.role == '' and sibiling.nodeType == 'PP':
                    if len(search.findall(sibiling, filter_=lambda node: node.role in ('o'))) > 0:
                        sibiling.role = 'a'
        return res

    def label_events_actions(self):
        nodes = search.findall(self.root,
                               filter_=lambda node: node.nodeType.startswith("V") and 'P' in node.nodeType and
                                                    node.role == '')
        for node in nodes:
            actions = 0
            events = 0
            for child in node.children:
                if child.role == 'a':
                    actions += 1
                if child.role == 'e' or child.role == 'E':
                    events += 1
            if events > 0 and actions == 0:
                node.role = 'EVENT'
            elif actions > 0 and events == 0:
                node.role = 'ACTION'

    def label_numeric_values(self):
        nodes = search.findall(self.root, filter_=lambda node: node.nodeType == 'CD' and node.role == '' and
                                                               len(node.children) == 0)
        for node in nodes:
            node.role = 'n'

    def label_conjunctions(self):
        res = {}
        try:
            nodes = search.findall(self.root, filter_=lambda node: node.nodeType in ('CC', 'IN', 'SCONJ', 'CCONJ')
                                                                   and node.role == '' and len(node.children) == 0)
            for node in nodes:
                if node.name in ['and', 'both']:
                    node.role = '&'
                    res[node.name + '--' + str(node.spans['start'])] = {'start': node.spans['start'],
                                                                        'end': node.spans['end'], 'role': node.role,
                                                                        'pos': 'CCONJ'}
                elif node.name in ['or', 'whether']:
                    node.role = '|'
                    res[node.name + '--' + str(node.spans['start'])] = {'start': node.spans['start'],
                                                                        'end': node.spans['end'], 'role': node.role,
                                                                        'pos': 'CCONJ'}
                elif node.name in ['not', 'neither', 'nor', 'but', 'except']:
                    node.role = '!'
                    res[node.name + '--' + str(node.spans['start'])] = {'start': node.spans['start'],
                                                                        'end': node.spans['end'], 'role': node.role,
                                                                        'pos': 'SCONJ'}

                siblings = search.findall(node.parent, filter_=lambda node: node.role not in ('&', '|', '!', 'q') and
                                                                            node.nodeType != 'DT' and (
                                                                                    node.role != '' or node.nodeType == ','))
                sibling_roles = set()
                for sibling in siblings:
                    if sibling.nodeType == ',':
                        sibling.role = node.role
                        res[sibling.name + '--' + str(sibling.spans['start'])] = {
                            'start': sibling.spans['start'], 'end': sibling.spans['end'], 'role': sibling.role,
                            'pos': res[node.name]['pos']}
                    else:
                        sibling_roles.add(sibling.role)
                if len(sibling_roles) == 1:
                    node.parent.role = list(sibling_roles)[0]
            self.update()
        except:
            logging.error('error in finding conjunctions...')
        return res

    def label_numbers(self):
        numbers = search.findall(self.root, filter_=lambda node: node.role == '' and node.nodeType == 'CD')
        units = {}
        for num in numbers:
            num.role = 'n'
            check = False
            added = False
            for sibling in num.parent.children:
                if sibling == num:
                    check = True
                elif check and sibling.name in PlaceDependencyTree.UNITS:
                    if num.parent.role == '':
                        num.parent.role = 'MEASURE'
                    if num.name + ' ' + sibling.name in self.root.name:
                        units[num.name + ' ' + sibling.name + '--' + str(num.spans['start'])] = {
                            'start': num.spans['start'],
                            'end': sibling.spans['end'] + 1,
                            'role': 'n',
                            'pos': 'NUM'}
                        added = True
            if not added and num.parent.nodeType == 'QP' and num.parent.parent is not None:
                found = False
                for child in num.parent.parent.children:
                    if child == num.parent:
                        found = True
                    elif found and child.name in PlaceDependencyTree.UNITS:
                        new_node = AnyNode(child.parent, role='MEASURE', name=num.name + ' ' + child.name,
                                           nodeType='NP', spans={
                                'start': self.root.name.index(num.name + ' ' + child.name),
                                'end': self.root.name.index(num.name + ' ' + child.name) +
                                       len(num.name + ' ' + child.name)
                            })
                        num.parent = new_node
                        child.parent = new_node
                        units[new_node.name + '--' + str(new_node.spans['start'])] = {'start': new_node.spans['start'],
                                                                                      'end': new_node.spans['end'],
                                                                                      'role': 'n',
                                                                                      'pos': 'NUM'
                                                                                      }
            else:
                units[num.name + '--' + str(num.spans['start'])] = {'start': num.spans['start'],
                                                                    'end': num.spans['end'],
                                                                    'role': 'n',
                                                                    'pos': 'NUM'
                                                                    }
        return units

    def label_qualities(self):
        compounds = {}
        adjectives = search.findall(self.root, filter_=lambda node: node.nodeType.startswith('AD'))
        for adj in adjectives:
            if len(search.findall(adj, filter_=lambda node: node.nodeType in ['CC', 'NP', 'NNS', 'NN'])) == 0:
                res = PlaceQuestionParseTree.label_adjective_roles(adj)
                compounds = {**compounds, **res}
        other_adjectives = search.findall(self.root,
                                          filter_=lambda node: node.nodeType.startswith('J') and node.parent.role == '')
        for adj in other_adjectives:
            res = PlaceQuestionParseTree.label_adjective_roles(adj)
            compounds = {**compounds, **res}
        return compounds

    @staticmethod
    def label_adjective_roles(adj):
        compounds = {}
        found = False
        for child in adj.parent.children:
            if not found and adj.nodeType.startswith('J') and child.nodeType == 'RBS':
                if child.name + ' ' + adj.name in adj.parent.name:
                    adj.name = child.name + ' ' + adj.name
                    adj.nodeType = 'JJS'
                    child.parent = None
            if child == adj:
                found = True
            elif found and child.nodeType.startswith('N'):
                if child.role in ['o', 'e', 'E']:
                    adj.role = 'q'
                elif child.role in ['p', 'P']:
                    adj.role = 'Q'
                else:
                    print('unresolved adjective! ' + adj.name + ' ' + child.name)
                # if ' ' in adj.name:
                compounds[adj.name + '--' + str(adj.spans['start'])] = {'start': adj.spans['start'],
                                                                        'end': adj.spans['end'],
                                                                        'role': adj.role, 'pos': 'ADJ'}
                break
            elif found and child.nodeType in ['PP', 'IN']:
                if child.nodeType == 'IN':
                    adj.parent = None
                    child.name = adj.name + ' ' + child.name
                    if child.name.endswith('than'):
                        child.role = '<>'
                        compounds[child.name + '--' + str(child.spans['start'])] = {'start': child.spans['start'],
                                                                                    'end': child.spans['end'],
                                                                                    'role': child.role, 'pos': 'ADJ'}
                elif child.nodeType == 'PP' and child.children[0].nodeType == 'IN':
                    if adj.parent is not None and len(adj.parent.children) == 2:
                        child.parent = adj.parent
                        child.name = adj.name + ' ' + child.name
                        child.spans = {'start': adj.spans['start'], 'end': child.spans['end']}
                        adj.parent = None
                        child.children[0].name = adj.name + ' ' + child.children[0].name
                        child.children[0].spans = {'start': adj.spans['start'], 'end': child.children[0].spans['end']}
                        if child.children[0].name.endswith('than'):
                            child.children[0].role = '<>'
                            compounds[child.children[0].name + '--' + str(child.children[0].spans['start'])] = {
                                'start': child.children[0].spans['start'], 'end': child.children[0].spans['end'],
                                'role': child.children[0].role, 'pos': 'ADJ'}
                    else:
                        adj.parent = None
                        child.children[0].name = adj.name + ' ' + child.children[0].name
                        child.children[0].spans = {'start': adj.spans['start'], 'end': child.children[0].spans['end']}
                        if child.children[0].name.endswith('than'):
                            child.children[0].role = '<>'
                            compounds[child.children[0].name + '--' + str(child.children[0].spans['start'])] = {
                                'start': child.children[0].spans['start'],
                                'end': child.children[0].spans['end'],
                                'role': child.children[0].role, 'pos': 'ADJ'}
                else:
                    print('unresolved adjective ' + adj.name + ' ' + child.name)
        return compounds

    @staticmethod
    def context_builder(list_str, node):
        boolean_var = True
        for string in list_str:
            boolean_var = boolean_var and string in node.name  # multi-word?
        return boolean_var

    def search_context(self, list_str):
        nodes = search.findall(self.root, filter_=lambda node: PlaceQuestionParseTree.context_builder(list_str, node))
        max_depth = -1
        selected = None
        for node in nodes:
            if node.depth > max_depth:
                max_depth = node.depth
                selected = node
        return selected

    def apply_dependencies(self, dependencies):
        verb_deps = []
        cc_deps = []
        adj_noun_deps = []
        complex_prep = []
        comparisons = []
        units = []
        for dependency in dependencies:
            if dependency.relation.link == 'HAS/RELATE' and 'VERB' in dependency.arg1.attributes and (
                    'NOUN' in dependency.arg2.attributes or 'PROPN' in dependency.arg2.attributes):
                verb_deps.append(dependency)
            elif dependency.relation.link == 'IS/ARE' and dependency.relation.name == 'ADJ':
                adj_noun_deps.append(dependency)
            elif dependency.relation.link == 'IS/ARE' and dependency.relation.name == 'PRP':
                complex_prep.append(dependency)
            elif dependency.relation.name == 'UNIT':
                units.append(dependency)
            elif dependency.relation.attributes is not None:
                if 'CCONJ' in dependency.relation.attributes or 'SCONJ' in dependency.relation.attributes:
                    cc_deps.append(dependency)
                elif dependency.relation.name != 'RELATION' and 'ADJ' in dependency.relation.attributes:
                    comparisons.append(dependency)
        print('Complex Prepositions:')
        self.apply_complex_relationships_dependencies(complex_prep)
        print('Verb-Noun Relationships:')
        self.apply_verb_noun_dependencies(verb_deps)
        print('Conjunctions:')
        self.apply_conjunction_dependencies(cc_deps)
        print('Adjective-Noun Relationships:')
        self.apply_adj_noun_dependencies(adj_noun_deps)
        print('Comparisons:')
        self.apply_comparison_dependencies(comparisons)
        print('Units:')
        self.apply_unit_dependencies(units)

    def apply_verb_noun_dependencies(self, dependencies):
        for dep in dependencies:
            str_list = [dep.arg1.name, dep.arg2.name]
            context = self.search_context(str_list)
            print(context)

    def apply_complex_relationships_dependencies(self, dependencies):
        for dep in dependencies:
            str_list = [dep.arg1.name, dep.arg2.name]
            context = self.search_context(str_list)
            print(context)

    def apply_conjunction_dependencies(self, dependencies):
        for dep in dependencies:
            str_list = [dep.relation.name, dep.arg1.name, dep.arg2.name]
            context = self.search_context(str_list)
            print(context)

    def apply_adj_noun_dependencies(self, dependencies):
        for dep in dependencies:
            str_list = [dep.arg1.name, dep.arg2.name]
            context = self.search_context(str_list)
            print(context)

    def apply_unit_dependencies(self, dependencies):
        for dep in dependencies:
            str_list = [dep.arg1.name, dep.arg2.name]
            context = self.search_context(str_list)
            print(context)

    def apply_comparison_dependencies(self, dependencies):
        for dep in dependencies:
            str_list = [dep.relation.name, dep.arg1.name, dep.arg2.name]
            context = self.search_context(str_list)
            firsts = search.findall(context, filter_=lambda node: dep.arg1.name in node.name and node != context)
            seconds = search.findall(context, filter_=lambda node: dep.arg2.name in node.name and node != context)
            first = PlaceQuestionParseTree.valid_node_selection(firsts, ['NN', 'NNS', 'NP', 'NPS'],
                                                                ['VB', 'VP', 'VBZ'])
            second = PlaceQuestionParseTree.valid_node_selection(seconds, ['NN', 'NNS', 'NP', 'NPS'],
                                                                 ['VB', 'VP', 'VBZ'])
            relation = PlaceQuestionParseTree.find_exact_match(context, dep.relation.name)
            print(first)
            print(second)
            print(relation)
            first.parent = relation
            second.parent = relation
            relation.role = 'COMPARISON'
            relation.parent.children = [relation]
            relation.parent.name = ' '.join([first.name, relation.name, second.name])
            self.clean_tree()

    @staticmethod
    def valid_node_selection(nodes, valid_pos_tags, invalid_tags):
        if len(nodes) == 1:
            return nodes[0]
        max_depth = -1
        selected = None
        for node in nodes:
            invalid_child = search.findall(node, filter_=lambda child: child != node and child.nodeType in invalid_tags)
            if len(invalid_child) == 0 and node.nodeType in valid_pos_tags and max_depth < node.depth:
                max_depth = node.depth
                selected = node
        return selected

    @staticmethod
    def find_exact_match(context, name):
        matches = search.findall(context, filter_=lambda node: node.name == name)
        max_depth = 1000
        selected = None
        for match in matches:
            if max_depth > match.depth:
                max_depth = match.depth
                selected = match
        selected.children = []
        return selected


class Dependency:
    def __init__(self, node1, relation, node2=None):
        self.arg1 = node1
        self.relation = relation
        self.arg2 = node2
        self.extra = []

    def is_binary(self):
        if self.arg2 is None:
            return False
        return True

    def __repr__(self):
        string = '\n' + str(self.relation) + ':\n\t' + str(self.arg1)
        if self.is_binary():
            string += '\n\t' + str(self.arg2)
        for ex in self.extra:
            string += '\n\t\t' + str(ex)
        return string


class FOLGenerator:
    CONCEPTS = {'P': 'PLACE', 'E': 'EVENT', 'L': 'LOCATION', 'd': 'DATE'}
    SPECIAL_CHARS = {'and': 8743, 'or': 8744, 'not': 172, 'implies': 8658, 'universal': 8704, 'existential': 8707}

    # e.g., result = chr(SPECIAL_CHARS['existential'])+' x0: Place(Tehran) '
    # +chr(SPECIAL_CHARS['and'])+' IN(x0, Tehran) '+chr(SPECIAL_CHARS['and'])+' City(x0)'

    def __init__(self, cons_tree, dep_tree):
        self.cons = cons_tree
        self.dep = dep_tree
        self.dependencies = {}
        self.variables = {}
        self.constants = []
        self.dep_places = []
        self.rels = {'property': [], 'spatial': []}

    def generate_dependencies(self):
        self.dependencies['intent'] = self.extract_intent_dependency()
        # order -- declaration, conjunction, spatial relationships, qualities, comparison
        self.dependencies['declaration'] = []
        self.declare()
        self.dependencies['criteria'] = []
        self.extract_conjunctions()
        self.extract_property_relationships()
        self.extract_quality_relations()

        self.extract_spatiotemporal_relationships()
        self.extract_situations()

        self.extract_comparisons()
        return self.dependencies

    def declare(self):
        specifics = search.findall(self.dep.root, filter_=lambda node: node.role in ['P', 'E', 'd'])
        for node in specifics:
            first = PlaceDependencyTree.clone_node_without_children(node)
            self.constants.append(node.name)
            relation = AnyNode(name='DECLARE', spans=[{}], attributes=None, link='IS', nodeType='RELATION')
            second = AnyNode(name=FOLGenerator.CONCEPTS[node.role], spans=[{}], attributes=None,
                             link=node.name, nodeType='CONCEPT')
            self.dependencies['declaration'].append(Dependency(first, relation, second))

        var_id = 0
        generics = search.findall(self.dep.root, filter_=lambda node: node.role in ['p', 'o', 'e'])
        for generic in generics:
            first = PlaceDependencyTree.clone_node_without_children(generic)
            relation = AnyNode(name='DECLARE', spans=[{}], attributes=None, link='IS', nodeType='RELATION')
            second = AnyNode(name='x' + str(var_id), spans=[{}], attributes=None,
                             link=PlaceDependencyTree.preprocess_names(generic.name), nodeType='VARIABLE')
            self.dependencies['declaration'].append(Dependency(first, relation, second))
            self.variables[first.name] = 'x' + str(var_id)
            var_id += 1

    def extract_intent_dependency(self):
        question_words = search.findall(self.cons.root, filter_=lambda node: node.nodeType == 'WH')
        selected = None
        if len(question_words) > 1:
            min_start = 1000
            for node in question_words:
                if node.spans['start'] < min_start:
                    min_start = node.spans['start']
                    selected = node
        elif len(question_words) == 1:
            selected = question_words[0]
        if selected is None:
            selected = AnyNode(name='what', spans=[{}], attributes=None, link='IS/ARE', nodeType='WH', role='1')

        first = PlaceDependencyTree.clone_node_without_children(selected, cons_tree=True)
        if selected.role == '8':
            relation = AnyNode(name='INTENT', spans=[{}], attributes=None, link='IS/ARE', nodeType='RELATION')
            intent = Dependency(node1=first, relation=relation)
            return [intent]

        seconds = search.findall(self.cons.root, filter_=lambda node: node.role in ['o', 'p',
                                                                                    'ACTION', 'EVENT', 'SITUATION'])
        what = None
        if len(seconds) == 0:
            seconds = search.findall(self.cons.root, filter_=lambda node: node.role == 'P')

        if len(seconds) == 1:
            what = seconds[0]
        else:
            max_depth = -1
            min_start = 1000
            for second in seconds:
                if second.spans['start'] < min_start:
                    if second.role == 'P' and (selected.nodeType.startswith('WH') or
                                               second.parent.nodeType != 'PP'):
                        continue
                    else:
                        what = second
                        min_start = second.spans['start']
                        max_depth = second.depth
                elif second.spans['start'] == min_start and second.depth > max_depth:
                    what = second
                    max_depth = second.depth
        if what is None:
            what = seconds[0]
        second = PlaceDependencyTree.clone_node_without_children(what, cons_tree=True)
        if selected.role == '1':  # where questions
            relation = AnyNode(name='INTENT', spans=[{}], attributes=None, link='LOCATION', nodeType='RELATION')
        elif selected.role == '6':  # how many
            relation = AnyNode(name='INTENT', spans=[{}], attributes=None, link='COUNT', nodeType='RELATION')
        else:
            if ' ' in first.name:
                first.name = first.name.split()[1]
            relation = AnyNode(name='INTENT', spans=[{}], attributes=None, link=first.name.upper(), nodeType='RELATION')
        intent = Dependency(node1=first, relation=relation, node2=second)
        return [intent]

    def print_dependencies(self):
        str_deps = ''
        for k, v in self.dependencies.items():
            print(k)
            str_deps += k + '\n'
            print('value: \n' + str(v))
            str_deps += str(v) + '\n'

        return str_deps

    def print_logical_form(self):
        # intent
        logical_form = ''
        intent = self.dependencies['intent'][0]
        complex_intents = self.apply_conjunction_intent()
        self.dependencies['intent'].extend(complex_intents)
        if intent.arg1.role == '8':
            if intent.arg2 is not None:
                logical_form += chr(FOLGenerator.SPECIAL_CHARS['existential']) + ' ' + intent.arg2.name
                for i in complex_intents:
                    logical_form += ', ' + i.arg2.name
        elif intent.arg1.role == '6' or intent.arg1.role == '1':  # how many, where
            logical_form += intent.relation.link + '(' + intent.arg2.name + ')'
            for i in complex_intents:
                logical_form += ', ' + intent.relation.link + '(' + i.arg2.name + ')'
        else:
            logical_form += intent.arg2.name
            for i in complex_intents:
                logical_form += ', ' + i.arg2.name
        if logical_form != '':
            logical_form += ': '

        # declarations
        declarations = self.dependencies['declaration']
        for declaration in declarations:
            if declaration.arg2.nodeType == 'VARIABLE':
                logical_form += declaration.arg2.link.replace(' ', '_').upper() + '(' + declaration.arg2.name + ') ' + \
                                chr(FOLGenerator.SPECIAL_CHARS['and']) + ' '
            else:
                logical_form += declaration.arg2.name + '(' + declaration.arg1.name + ') ' + \
                                chr(FOLGenerator.SPECIAL_CHARS['and']) + ' '

        # criteria
        self.apply_conjunction_criteria()
        criteria = self.dependencies['criteria']
        counter = 1
        for criterion in criteria:
            if criterion.relation.link == 'AND/OR':
                counter += 1
                continue
            if counter == len(criteria):
                logical_form = self.generate_FOL_criterion(criterion, logical_form, last=True)
            else:
                logical_form = self.generate_FOL_criterion(criterion, logical_form)
                counter += 1

        if logical_form.endswith(chr(FOLGenerator.SPECIAL_CHARS['and']) + ' '):
            logical_form = logical_form[0: len(logical_form) - 2]

        for key, var in self.variables.items():
            logical_form = logical_form.replace(key, var)

        print(logical_form)
        print()
        return logical_form

    def generate_FOL_criterion(self, criterion, logical_form, last=False):
        # if criterion.arg1.name in self.variables.keys():
        #     criterion.arg1.name = self.variables[criterion.arg1.name]
        # if criterion is not None and criterion.arg2.name in self.variables.keys():
        #     criterion.arg2.name = self.variables[criterion.arg2.name]

        if criterion.relation.link in ['PROPERTY', 'NOT']:
            logical_form += criterion.relation.name.upper().replace(' ', '_') + '(' + criterion.arg1.name
            if criterion.arg2 is not None:
                logical_form += ', ' + criterion.arg2.name
            logical_form += ') '

        elif criterion.relation.link == 'SUPERLATIVE':
            if criterion.arg1.name in self.variables.keys() and criterion.arg1.role == 'p':
                logical_form = logical_form.replace(criterion.arg1.name, criterion.arg2.name.replace(' ', '_').upper() +
                                                    '(' + criterion.arg1.name + ')', 1)
                if last:
                    logical_form = logical_form[0: len(logical_form) - 2]
                last = True
            else:
                logical_form += criterion.arg2.name.replace(' ', '_').upper() + '(' + criterion.arg1.name + ') '
        else:  # other
            logical_form += criterion.relation.name.upper().replace(' ', '_') + '(' + criterion.arg1.name
            if criterion.arg2 is not None:
                logical_form += ', ' + criterion.arg2.name
            extra = ''
            for ex in criterion.extra:
                logical_form += ', ' + ex.name
                extra += ex.name + ' '
            logical_form += ') '
            logical_form = logical_form.replace(extra.upper().replace(" ", "_"), '')
        if not last:
            logical_form += chr(FOLGenerator.SPECIAL_CHARS['and']) + ' '
        return logical_form

    def apply_conjunction_intent(self):
        result = []
        intent = self.dependencies['intent'][0]
        if intent.arg2 is None:
            return result
        name = intent.arg2.name
        criteria = self.dependencies['criteria']
        for criterion in criteria:
            found = False
            if criterion.relation.link == 'AND/OR':
                second = None
                if criterion.arg1.name == name:
                    second = PlaceDependencyTree.clone_node_without_children(intent.arg2)
                    second.name = criterion.arg2.name
                    found = True
                elif criterion.arg2.name == name:
                    second = PlaceDependencyTree.clone_node_without_children(intent.arg2)
                    second.name = criterion.arg1.name
                    found = True
                if found:
                    first = PlaceDependencyTree.clone_node_without_children(intent.arg1)
                    relation = intent.relation
                    result.append(Dependency(first, relation, second))
        return result

    def check_valid_relationships(self, first, second):
        if first.name + '-' + second.name in self.rels['spatial']:
            return False
        elif first.name + '-' + second.name in self.rels['property']:
            return False
        return True

    def apply_conjunction_criteria(self):
        criteria = self.dependencies['criteria']
        and_or_criteria = []
        for criterion in criteria:
            if criterion.relation.link == 'AND/OR':
                and_or_criteria.append(criterion)
        if len(and_or_criteria) == 0:
            return
        new_criteria = []
        for criterion in criteria:
            if criterion.relation.link == 'AND/OR':
                continue
            for ao in and_or_criteria:
                first = None
                relation = None
                second = None
                found = False
                if criterion.arg1.name == ao.arg1.name:
                    first = PlaceDependencyTree.clone_node_without_children(criterion.arg1)
                    first.name = ao.arg2.name
                    found = True
                elif criterion.arg2.name == ao.arg1.name:
                    second = PlaceDependencyTree.clone_node_without_children(criterion.arg2)
                    second.name = ao.arg2.name
                    found = True
                elif criterion.arg1.name == ao.arg2.name:
                    first = PlaceDependencyTree.clone_node_without_children(criterion.arg1)
                    first.name = ao.arg1.name
                    found = True
                elif criterion.arg2.name == ao.arg2.name:
                    second = PlaceDependencyTree.clone_node_without_children(criterion.arg2)
                    second.name = ao.arg1.name
                    found = True
                if found:
                    relation = criterion.relation
                    if first is None:
                        first = PlaceDependencyTree.clone_node_without_children(criterion.arg1)
                    else:
                        second = PlaceDependencyTree.clone_node_without_children(criterion.arg2)

                    dependency = Dependency(first, relation, second)
                    dependency.extra = criterion.extra
                    new_criteria.append(dependency)
        self.dependencies['criteria'].extend(new_criteria)

    def extract_comparisons(self):
        comps = search.findall(self.cons.root, filter_=lambda node: node.role in ['>', '<', '<>', '=', '>=', '<='])
        for comp in comps:
            d_comp = search.findall(self.dep.root, filter_=lambda node: node.name.strip() == comp.name.strip())
            if len(d_comp) != 1:
                continue
            d_comp = d_comp[0]
            found = False
            right = None
            for child in comp.parent.children:
                if child == comp:
                    found = True
                elif found:
                    right = child
                    break

            while right.role == '' and len(right.children) > 0:
                right = right.children[0]
            if right.role == '':
                continue

            # pattern 1 -- path to root
            left = d_comp.parent
            while left is not None and left.parent is not None and left.role not in ['p', 'P', 's', 'o']:
                left = left.parent
            if (left is None or left.role not in ['p', 'P', 's', 'o']) \
                    and len(d_comp.children) > 0 and left.children[0].name != right.name:
                # pattern 2 -- valid children[0][0]...
                left = left.children[0]
                while len(left.children) > 0 and left.role not in ['p', 'P', 's', 'o']:
                    left = left.children[0]
            if left.role not in ['p', 'P', 's', 'o']:
                continue
            first = PlaceDependencyTree.clone_node_without_children(left)
            second = PlaceDependencyTree.clone_node_without_children(right, cons_tree=True)
            relation = PlaceDependencyTree.clone_node_without_children(comp, cons_tree=True)
            self.dependencies['criteria'].append(Dependency(first, relation, second))

    def extract_situations(self):
        situations = search.findall(self.dep.root, filter_=lambda node: node.role == 's')
        for situation in situations:
            first = None
            second = None
            relation = None
            if situation.name in ['is', 'are', 'located', 'do', 'dose']:
                continue
            elif situation.name in ['have', 'has']:
                nsubjects = search.findall(situation, filter_=lambda node: node.nodeType == 'nsubj')
                if len(nsubjects) == 2:
                    # implicit spatial relationships
                    if nsubjects[0].role in ['P', 'p'] and nsubjects[1].role in ['P', 'p']:
                        if nsubjects[0].role == 'p' or nsubjects[1].role == 'P':
                            first = PlaceDependencyTree.clone_node_without_children(nsubjects[0])
                            second = PlaceDependencyTree.clone_node_without_children(nsubjects[1])
                        elif nsubjects[1].role == 'p':
                            first = PlaceDependencyTree.clone_node_without_children(nsubjects[1])
                            second = PlaceDependencyTree.clone_node_without_children(nsubjects[0])
                        if first is not None and second is not None:
                            relation = AnyNode(name='in', spans=[{}], attributes=None, link='prep', role='R',
                                               nodeType='dep')
                    # situation + object (attribute) + place
                    elif (nsubjects[0].role in ['P', 'p'] and nsubjects[1].role in ['o']) or \
                            (nsubjects[1].role in ['P', 'p'] and nsubjects[0].role in ['o']):
                        relation = PlaceDependencyTree.clone_node_without_children(situation)
                elif len(nsubjects) == 1:
                    dobjs = search.findall(situation, filter_=lambda node: node.parent == situation and
                                                                           node.nodeType == 'dobj' and
                                                                           node.role in ['o', 'p'])
                    if len(dobjs) == 1:
                        if dobjs[0].role == 'o':
                            first = PlaceDependencyTree.clone_node_without_children(nsubjects[0])
                            second = PlaceDependencyTree.clone_node_without_children(dobjs[0])
                            relation = PlaceDependencyTree.clone_node_without_children(situation)
                        else:
                            first = PlaceDependencyTree.clone_node_without_children(dobjs[0])
                            second = PlaceDependencyTree.clone_node_without_children(nsubjects[0])
                            relation = AnyNode(name='in', spans=[{}], attributes=None, link='prep', role='R',
                                               nodeType='dep')
                    elif len(dobjs) == 0 and situation.parent is not None and situation.parent.role == 'o':
                        first = PlaceDependencyTree.clone_node_without_children(nsubjects[0])
                        second = PlaceDependencyTree.clone_node_without_children(situation.parent)
                        relation = PlaceDependencyTree.clone_node_without_children(situation)

                elif len(nsubjects) == 0:
                    if situation.parent is not None and situation.parent.role == 'o':
                        second = PlaceDependencyTree.clone_node_without_children(situation.parent)
                    elif len(situation.children) == 1 and situation.children[0].role == 'o':
                        second = PlaceDependencyTree.clone_node_without_children(situation.children[0])
                    if second is not None:
                        generics = search.findall(situation.parent, filter_=lambda node: node.role == 'p')
                        if len(generics) == 1:
                            first = PlaceDependencyTree.clone_node_without_children(generics[0])
                            relation = PlaceDependencyTree.clone_node_without_children(situation)
                    else:
                        generics = search.findall(
                            situation, filter_=lambda node: node.parent == situation and node.role == 'p')
                        objects = search.findall(
                            situation, filter_=lambda node: node.role == 'o')
                        if len(generics) == 1 and len(objects) == 1:
                            first = PlaceDependencyTree.clone_node_without_children(generics[0])
                            second = PlaceDependencyTree.clone_node_without_children(objects[0])
                            relation = PlaceDependencyTree.clone_node_without_children(situation)
            elif situation.name in ['border', 'borders', 'cross', 'crosses', 'flow', 'flows', 'discharge',
                                    'discharges', 'run', 'runs']:
                relation = AnyNode(name=situation.name, spans=[{}], attributes=None, link='prep', role='R',
                                   nodeType='dep')
                generic_places = search.findall(self.dep.root, filter_=lambda node: node.role == 'p')
                specific_places = search.findall(self.dep.root, filter_=lambda node: node.role == 'P')
                if len(generic_places) == 2:
                    first = PlaceDependencyTree.clone_node_without_children(generic_places[0])
                    second = PlaceDependencyTree.clone_node_without_children(generic_places[1])
                elif len(generic_places) == 1:
                    first = PlaceDependencyTree.clone_node_without_children(generic_places[0])
                    if len(specific_places) == 1:
                        second = PlaceDependencyTree.clone_node_without_children(specific_places[0])
                    elif len(specific_places) > 1:
                        for place in specific_places:
                            if place.name + ' ' + first.name not in self.dep_places and \
                                    first.name + ' ' + place.name not in self.dep_places:
                                second = PlaceDependencyTree.clone_node_without_children(place)
                                break
                elif len(generic_places) == 0 and len(specific_places) == 2:
                    first = PlaceDependencyTree.clone_node_without_children(specific_places[0])
                    second = PlaceDependencyTree.clone_node_without_children(specific_places[1])

            if first is not None and second is not None and relation is not None:
                if self.check_valid_relationships(first, second):
                    self.dependencies['criteria'].append(Dependency(first, relation, second))
                    if relation.role is not None and relation.role == 'R':
                        self.rels['spatial'].append(first.name + '-' + second.name)

    def extract_quality_relations(self):
        qualities = search.findall(self.cons.root, filter_=lambda node: node.role in ['Q', 'q'])
        quality_map = {'Q': ['p', 'P'], 'q': ['e', 'E', 'o']}
        for q in qualities:
            reference = search.findall(q.parent, filter_=lambda node: node.parent == q.parent and
                                                                      node.role in quality_map[q.role])
            if len(reference) == 0:
                continue
            reference = reference[0]
            d_q = search.findall(self.dep.root, filter_=lambda node: node.name.strip() == q.name.strip())
            if len(d_q) != 1:
                continue
            d_q = d_q[0]
            if q.nodeType == 'JJS' or len(q.children) > 0 and len(search.findall(q, filter_=lambda node:
            node.nodeType in ['RBS', 'JJS'])) > 0:
                first = PlaceDependencyTree.clone_node_without_children(reference, cons_tree=True)
                second = PlaceDependencyTree.clone_node_without_children(d_q)
                relation = AnyNode(name='IS/ARE', spans=[{}], attributes=None, link='SUPERLATIVE',
                                   nodeType='RELATION')
                self.dependencies['criteria'].append(Dependency(first, relation, second))
            elif q.role == 'JJR' or len(q.children) > 0 and len(search.findall(q, filter_=lambda node:
            node.nodeType in ['RBR', 'JJR'])) > 0:
                print('Comparative')
            else:
                first = PlaceDependencyTree.clone_node_without_children(reference, cons_tree=True)
                second = PlaceDependencyTree.clone_node_without_children(d_q)
                relation = AnyNode(name='IS/ARE', spans=[{}], attributes=None, link='PROPERTY',
                                   nodeType='RELATION')
                if self.check_valid_relationships(first, second):
                    self.dependencies['criteria'].append(Dependency(first, relation, second))
                    self.rels['property'].append(first.name + '-' + second.name)

    def extract_conjunctions(self):
        and_or = ['&', '|']
        conjs = search.findall(self.cons.root, filter_=lambda node: node.role in and_or)
        for conj in conjs:
            siblings = search.findall(conj.parent, filter_=lambda node: node.parent == conj.parent and
                                                                        node.role in ['p', 'P', 'e', 'E'])
            pairs = []
            for s1 in siblings:
                for s2 in siblings:
                    if s1 != s2 and s1.name + ' ' + s2.name not in pairs:
                        pairs.append(s1.name + ' ' + s2.name)
                        pairs.append(s2.name + ' ' + s1.name)
                        first = PlaceDependencyTree.clone_node_without_children(s1, cons_tree=True)
                        second = PlaceDependencyTree.clone_node_without_children(s2, cons_tree=True)
                        rel_name = FOLGenerator.SPECIAL_CHARS['and']
                        if conj.role == '|':
                            rel_name = FOLGenerator.SPECIAL_CHARS['or']
                        relation = AnyNode(name=rel_name, spans=[{}], attributes=None, link='AND/OR',
                                           nodeType='RELATION')
                        self.dependencies['criteria'].append(Dependency(first, relation, second))
        not_nor = ['!']
        negations = search.findall(self.cons.root, filter_=lambda node: node.role in not_nor)
        for negation in negations:
            next = search.findall(negation.parent, filter_=lambda node: node.parent == negation.parent and
                                                                        node.spans['start'] > negation.spans[
                                                                            'start'] and node.role in ['p', 'P', 'e',
                                                                                                       'E'])
            if len(next) > 0:
                next = next[0]
                d_conj = search.findall(self.dep.root, filter_=lambda node: node.name.strip() == negation.name.strip())
                if len(d_conj) == 1:
                    d_conj = d_conj[0]
                    if d_conj.parent.role in ['p', 'e']:
                        first = PlaceDependencyTree.clone_node_without_children(d_conj.parent)
                        second = PlaceDependencyTree.clone_node_without_children(next, cons_tree=True)
                        relation = AnyNode(name=negation.name.upper(), spans=[{}], attributes=None, link='NOT',
                                           nodeType='RELATION')
                        self.dependencies['criteria'].append(Dependency(first, relation, second))

    def extract_spatiotemporal_relationships(self):
        locations = search.findall(self.cons.root, filter_=lambda node: node.role == 'LOCATION')
        for location in locations:
            done = False
            relationships = search.findall(location, filter_=lambda node: node.parent == location and
                                                                          node.role in ['R', 'r'])
            anchors = search.findall(location, filter_=lambda node: node.role in ['p', 'P', 'd'])
            anchor_names = list(map(lambda node: node.name, anchors))
            first = None
            relation = None
            second = None
            if len(relationships) == 0:
                continue
            elif len(relationships) > 1:
                continue
            else:
                relationship = relationships[0]
                r_nodes = search.findall(self.dep.root, filter_=lambda node: node.name.strip() == relationship.name and
                                                                             node.spans[0]['start'] >=
                                                                             relationship.spans['start'] - 2
                                                                             and node.spans[0]['end'] <=
                                                                             relationship.spans['end'] + 3)
                if len(r_nodes) != 1:
                    continue
                r_node = r_nodes[0]
                subjects = search.findall(self.dep.root, filter_=lambda node: node.role in ['p', 'P', 'e', 'E'] and
                                                                              node.name not in anchor_names)
                found = False
                if r_node.parent is not None:
                    # pattern 1 -- sibling (before)
                    for sibling in r_node.parent.children:
                        if sibling == r_node:
                            break
                        if sibling.role in ['p', 'P', 'e', 'E']:
                            first = PlaceDependencyTree.clone_node_without_children(sibling)
                            found = True
                            break

                    # pattern 2 -- father
                    if not found:
                        parent = r_node.parent
                        while parent is not None:
                            if parent.role in ['p', 'P', 'e', 'E']:
                                first = PlaceDependencyTree.clone_node_without_children(parent)
                                found = True
                                break
                            parent = parent.parent

                # pattern 3 -- child
                if not found:
                    childs = search.findall(r_node, filter_=lambda node: node.role in ['P', 'p', 'E', 'e'] and
                                                                         node.name not in anchor_names)
                    if len(childs) == 1:
                        first = PlaceDependencyTree.clone_node_without_children(childs[0])

                if found:
                    relation = PlaceDependencyTree.clone_node_without_children(r_node)
                # pattern 4 -- not a spatial relationship but a property-preposition
                if not found and len(subjects) == 0:
                    r_node.role = ''
                    location.role = ''
                    relationship.role = ''
                    if r_node.parent is not None and r_node.parent.role == 'o':
                        first = PlaceDependencyTree.clone_node_without_children(r_node.parent)
                        relation = AnyNode(name=r_node.name, spans=[{}], attributes=None, link='PROPERTY',
                                           nodeType='RELATION')
                if first is not None and relation is not None:
                    for anchor in anchors:
                        second = PlaceDependencyTree.clone_node_without_children(anchor, cons_tree=True)
                        if self.check_valid_relationships(first, second):
                            self.dependencies['criteria'].append(Dependency(first, relation, second))
                            if relation.link == 'PROPERTY':
                                self.rels['property'].append(first.name + '-' + second.name)
                            else:
                                self.rels['spatial'].append(first.name + '-' + second.name)
                        done = True
                        if len(locations) > 1:
                            break
                else:
                    if len(subjects) == 1:
                        first = PlaceDependencyTree.clone_node_without_children(subjects[0])
                        relation = PlaceDependencyTree.clone_node_without_children(r_node)
                        for anchor in anchors:
                            second = PlaceDependencyTree.clone_node_without_children(anchor, cons_tree=True)
                            if self.check_valid_relationships(first, second):
                                self.dependencies['criteria'].append(Dependency(first, relation, second))
                                if relation.link == 'PROPERTY':
                                    self.rels['property'].append(first.name + '-' + second.name)
                                else:
                                    self.rels['spatial'].append(first.name + '-' + second.name)
                            done = True
                            if len(locations) > 1:
                                break
                    else:
                        if len(r_node.children) > 0:
                            filtered = search.findall(r_node.children[0], filter_=lambda node:
                            node.role in ['p', 'P', 'e', 'E'] and node.name not in anchor_names)
                            if len(filtered) == 1:
                                first = PlaceDependencyTree.clone_node_without_children(filtered[0])
                                relation = PlaceDependencyTree.clone_node_without_children(r_node)
                                for anchor in anchors:
                                    second = PlaceDependencyTree.clone_node_without_children(anchor, cons_tree=True)
                                    if self.check_valid_relationships(first, second):
                                        self.dependencies['criteria'].append(Dependency(first, relation, second))
                                        if relation.link == 'PROPERTY':
                                            self.rels['property'].append(first.name + '-' + second.name)
                                        else:
                                            self.rels['spatial'].append(first.name + '-' + second.name)
                                    done = True
                                    if len(locations) > 1:
                                        break
            if done:
                self.dep_places.append(first.name + ' ' + second.name)
                if len(relationship.children) > 0:
                    measures = search.findall(relationship, filter_=lambda node: node.role == 'MEASURE')
                    if len(measures) == 1:
                        measure = measures[0]
                        additional = search.findall(measure, filter_=lambda node: node.role not in ['o', 'n'] and
                                                                                  node != measure)
                        extra = PlaceDependencyTree.clone_node_without_children(measure, cons_tree=True)
                        for a in additional:
                            extra.name = extra.name.replace(a.name, '').strip()
                        self.dependencies['criteria'][len(self.dependencies['criteria']) - 1].extra.append(extra)

    def extract_property_relationships(self):
        non_spatial_prepositions = search.findall(self.dep.root, filter_=lambda node: node.link == 'prep' and
                                                                                      node.role == '')
        for p in non_spatial_prepositions:
            first = None
            second = None
            relation = None
            if p.parent is not None and p.parent.role == 'o':
                first = PlaceDependencyTree.clone_node_without_children(p.parent)
            for child in p.children:
                if child.role in ['p', 'P']:
                    second = PlaceDependencyTree.clone_node_without_children(child)
            if first is not None and second is not None:
                relation = AnyNode(name=p.name, spans=[{}], attributes=None, link='PROPERTY',
                                   nodeType='RELATION')
                if self.check_valid_relationships(first, second):
                    self.dependencies['criteria'].append(Dependency(first, relation, second))
                    self.rels['property'].append(first.name + '-' + second.name)


class PlaceDependencyTree:
    UNITS = ['meters', 'kilometers', 'miles', 'mile', 'meter', 'kilometer',
             'km', 'm', 'mi', 'yard', 'hectare']

    def __init__(self, dependency_dict):
        self.dict = dependency_dict
        self.root = None
        self.tree = None
        self.construct_dependencies()
        self.dependencies = []

    def construct_dependencies(self):
        root = AnyNode(name=self.dict['word'], nodeType=self.dict['nodeType'],
                       attributes=self.dict['attributes'], spans=self.dict['spans'], link=self.dict['link'], role='')
        if 'children' in self.dict.keys():
            for child in self.dict['children']:
                self.add_to_tree(child, root)
        self.root = root
        self.tree = RenderTree(root)

    def add_to_tree(self, node, parent):
        n = AnyNode(name=node['word'], nodeType=node['nodeType'], parent=parent,
                    attributes=node['attributes'], spans=node['spans'],
                    link=node['link'], role='')
        if 'children' in node.keys():
            for child in node['children']:
                self.add_to_tree(child, n)

    def render(self):
        self.tree = RenderTree(self.root)

    def __repr__(self):
        if self.tree is None:
            return "Empty Tree"
        res = ""
        for pre, fill, node in self.tree:
            res += "%s%s (%s) {%s} [%s]" % (pre, node.name, node.nodeType, node.attributes, node.role) + "\n"
        return res

    def detect_dependencies(self):
        if self.tree is not None:
            self.detect_conjunctions()
            self.detect_adjectives()
            self.detect_verb_noun_relationships()
            self.detect_complex_prepositions()
            self.detect_units()

    def clean_d_tree(self, str_dict):
        for k, v in str_dict.items():
            nodes = search.findall(self.root, filter_=lambda node: node.spans[0]['start'] >= v['start'] and
                                                                   node.spans[0]['end'] <= v[
                                                                       'end'] + 3 and node.name.strip() ==
                                                                   re.split('--', k.strip())[0])
            if len(nodes) > 0:
                for n in nodes:
                    n.role = v['role']
                    n.attributes = [v['pos']]
            else:
                nodes = search.findall(self.root, filter_=lambda node: node.spans[0]['start'] >= v['start'] and
                                                                       node.spans[0]['end'] <= v[
                                                                           'end'] + 3 and node.name in k)
                selected = None
                depth = 1000
                for node in nodes:
                    if depth > node.depth:
                        selected = node
                        depth = node.depth
                children = []
                if selected is not None:
                    for node in nodes:
                        if node != selected:
                            node.parent = None
                            for child in node.children:
                                children.append(child)
                    selected.name = re.split('--', k.strip())[0]
                    selected.role = v['role']
                    selected.attributes = [v['pos']]
                for child in children:
                    if child.parent is not None:
                        child.parent = selected

        stop_words = search.findall(self.root, filter_=lambda node: node.name in ['the', 'a', 'an'] and
                                                                    node.nodeType == 'DT')
        for stop_word in stop_words:
            if len(stop_word.children) == 0:
                stop_word.parent = None
            else:
                parent = stop_word.parent
                children = stop_word.children
                new_children = []
                for sibling in parent.children:
                    if sibling == stop_word:
                        new_children.extend(children)
                    else:
                        new_children.append(sibling)
                parent.children = new_children

    def detect_conjunctions(self):
        conjunctions = search.findall(self.root,
                                      filter_=lambda node: ('SCONJ' in node.attributes or 'CCONJ' in node.attributes)
                                                           and node.nodeType in ['punct', 'dep', 'prep'])
        for conj in conjunctions:
            is_cc = 'CCONJ' in conj.attributes
            relation = PlaceDependencyTree.clone_node_without_children(conj)
            first = None
            if 'AUX' in conj.parent.attributes or 'VERB' in conj.parent.attributes:
                temp = search.findall(conj.parent,
                                      filter_=lambda node: node.parent == conj.parent and node.link == 'nsubj')
                print(temp)
                if len(temp) == 1:
                    first = PlaceDependencyTree.clone_node_without_children(temp[0])
            else:
                first = PlaceDependencyTree.clone_node_without_children(conj.parent)
            if is_cc:
                pairs = search.findall(conj.parent, filter_=lambda node: node.parent == conj.parent and
                                                                         conj.parent.attributes[
                                                                             0] in node.attributes and node.link == 'dep')
                for pair in pairs:
                    if first is not None:
                        second = PlaceDependencyTree.clone_node_without_children(pair)
                        dep = Dependency(first, relation, second)
                        self.dependencies.append(dep)
            else:
                nodes = search.findall(conj, filter_=lambda node: node.link in ['dep', 'pobj'] and
                                                                  (
                                                                          'PROPN' in node.attributes or 'NOUN' in node.attributes))
                for node in nodes:
                    if first is not None:
                        second = PlaceDependencyTree.clone_node_without_children(node)
                        dep = Dependency(first, relation, second)
                        self.dependencies.append(dep)

        excepts = search.findall(self.root, filter_=lambda node: node.nodeType == 'case' and 'ADP' in node.attributes)
        for ex in excepts:
            if ex.name in ['except', 'excluding']:
                override = {'nodeType': 'cc', 'attributes': ['SCONJ'], 'link': 'conj'}
                relation = PlaceDependencyTree.clone_node_without_children(ex, override)
                first = PlaceDependencyTree.clone_node_without_children(ex.parent)
                dep = Dependency(first, relation)
                self.dependencies.append(dep)

    def detect_adjectives(self):
        adjectives = search.findall(self.root, filter_=lambda node: 'ADJ' in node.attributes and
                                                                    node.link in ['amod', 'case', 'dep', 'pobj',
                                                                                  'root'])
        for adj in adjectives:
            num_comparisons = search.findall(adj, filter_=lambda node: 'NUM' in node.attributes and
                                                                       (node.parent == adj or node.parent.link in [
                                                                           'pobj', 'prep', 'dep']))
            noun_comparisons = search.findall(adj, filter_=lambda
                node: ('PROPN' in node.attributes or 'NOUN' in node.attributes) and node.link == 'dep' and
                      (node.parent == adj or node.parent.link in ['prep']))
            if len(num_comparisons) > 0:  # value comparison
                for d in num_comparisons:
                    parent = PlaceDependencyTree.find_first_parent_based_on_attribute(adj, ['NOUN', 'PROPN'])
                    if parent is not None:
                        first = PlaceDependencyTree.clone_node_without_children(parent)
                        relation = PlaceDependencyTree.clone_node_without_children(adj)
                        second = PlaceDependencyTree.clone_node_without_children(d)
                        dep = Dependency(first, relation, second)
                        self.dependencies.append(dep)
                    children = search.findall(d.parent, filter_=lambda node: node.link in ['dep', 'pobj'] and
                                                                             node.name in PlaceDependencyTree.UNITS)
                    for child in children:
                        first = PlaceDependencyTree.clone_node_without_children(d)
                        second = PlaceDependencyTree.clone_node_without_children(child)
                        relation = AnyNode(name='UNIT', spans=[{}], attributes=None, link='IS/ARE', nodeType='RELATION')
                        dep = Dependency(first, relation, second)
                        self.dependencies.append(dep)

            elif len(noun_comparisons) > 0 and adj.parent is not None:  # noun comparison
                for n in noun_comparisons:
                    first = PlaceDependencyTree.clone_node_without_children(adj.parent)
                    relation = PlaceDependencyTree.clone_node_without_children(adj)
                    second = PlaceDependencyTree.clone_node_without_children(n)
                    dep = Dependency(first, relation, second)
                    self.dependencies.append(dep)
            else:
                relation = AnyNode(name='ADJ', spans=[{}], attributes=None, link='IS/ARE', nodeType='RELATION')
                dependency = None
                if adj.parent is not None:
                    dependency = search.findall(adj.parent,
                                                filter_=lambda node: (node.parent == adj.parent or node == adj.parent
                                                                      or adj in node.ancestors)
                                                                     and node.attributes[0] in ['NOUN', 'PROPN'])
                else:
                    dependency = search.findall(adj, filter_=lambda node: node.attributes[0] in ['NOUN',
                                                                                                 'PROPN'] and node.parent == adj)
                if len(dependency) == 1:
                    first = PlaceDependencyTree.clone_node_without_children(dependency[0])
                    dep = Dependency(first, relation, adj)
                    self.dependencies.append(dep)
                else:
                    print('error -- adjective with multiple deps ' + str(adj))

            adverbs = search.findall(adj, filter_=lambda node: node.link in ['advmod', 'dep'] and
                                                               node.parent == adj and 'ADV' in node.attributes)
            if len(adverbs) > 0:
                for adv in adverbs:
                    first = PlaceDependencyTree.clone_node_without_children(adj)
                    second = PlaceDependencyTree.clone_node_without_children(adv)
                    relation = AnyNode(name='ADV', spans=[{}], attributes=None, link='IS/ARE', nodeType='RELATION')
                    dep = Dependency(first, relation, second)
                    self.dependencies.append(dep)

    def detect_verb_noun_relationships(self):
        verbs = search.findall(self.root, filter_=lambda node: 'VERB' in node.attributes)
        for verb in verbs:
            nouns = search.findall(verb, filter_=lambda node: 'NOUN' in node.attributes or 'PROPN' in node.attributes)
            for noun in nouns:
                if (noun.parent == verb and noun.link == 'dep') or (noun.parent.parent == verb and noun.link == 'pobj'):
                    first = PlaceDependencyTree.clone_node_without_children(verb)
                    second = PlaceDependencyTree.clone_node_without_children(noun)
                    relation = AnyNode(name='OBJ', spans=[{}], attributes=None, link='HAS/RELATE', nodeType='RELATION')
                    dep = Dependency(first, relation, second)
                    self.dependencies.append(dep)

    def detect_units(self):
        numbers = search.findall(self.root, filter_=lambda node: 'NUM' in node.attributes)
        for num in numbers:
            context = num.parent
            if context is None:
                context = num
            units = search.findall(context, filter_=lambda node: node.name in node.link in ['dep', 'pobj'] and
                                                                 node.name in PlaceDependencyTree.UNITS)
            selected = None
            if len(units) == 1:
                selected = units[0]
            else:
                selected_depth = 1000
                for unit in units:
                    if selected_depth > unit.depth:
                        selected = unit
                        selected_depth = unit.depth

            if selected is not None:
                first = PlaceDependencyTree.clone_node_without_children(num)
                second = PlaceDependencyTree.clone_node_without_children(selected)
                relation = AnyNode(name='UNIT', spans=[{}], attributes=None, link='IS/ARE', nodeType='RELATION')
                dep = Dependency(first, relation, second)
                self.dependencies.append(dep)

    def detect_complex_prepositions(self):
        preps = search.findall(self.root, filter_=lambda node: node.link == 'prep')
        for prep in preps:
            if 'ADV' in prep.parent.attributes and len(prep.parent.children) == 1:
                first = PlaceDependencyTree.clone_node_without_children(prep)
                second = PlaceDependencyTree.clone_node_without_children(prep.parent)
                relation = AnyNode(name='PRP', spans=[{}], attributes=None, link='IS/ARE', nodeType='RELATION')
                dep = Dependency(first, relation, second)
                self.dependencies.append(dep)
            elif 'NUM' in prep.parent.attributes and len(prep.parent.children) > 1 \
                    and prep.parent.parent is not None and 'ADP' in prep.parent.parent.attributes:
                first = PlaceDependencyTree.clone_node_without_children(prep.parent.parent)
                relation = PlaceDependencyTree.clone_node_without_children(prep.parent)
                second = PlaceDependencyTree.clone_node_without_children(prep)
                dep = Dependency(first, relation, second)
                self.dependencies.append(dep)

                modifiers = search.findall(prep.parent,
                                           filter_=lambda node: 'ADV' in node.attributes and len(node.children) == 0)
                if len(modifiers) == 1:
                    relation = AnyNode(name='PRP', spans=[{}], attributes=None, link='IS/ARE', nodeType='RELATION')
                    second = PlaceDependencyTree.clone_node_without_children(modifiers[0])
                    dep = Dependency(first, relation, second)
                    self.dependencies.append(dep)

    @staticmethod
    def find_first_parent_based_on_attribute(node, attributes):
        ancestors = node.ancestors
        first_parent = None
        depth = 0
        for ancestor in ancestors:
            if ancestor.attributes[0] in attributes:
                if depth < ancestor.depth:
                    first_parent = ancestor
                    depth = ancestor.depth
        return first_parent

    @staticmethod
    def clone_node_without_children(node, override={}, cons_tree=False):
        if len(override) == 0:
            if cons_tree:
                return AnyNode(name=PlaceDependencyTree.preprocess_names(node.name), spans=node.spans,
                               attributes=[node.nodeType],
                               link='', nodeType=node.nodeType, role=node.role)
            return AnyNode(name=PlaceDependencyTree.preprocess_names(node.name), spans=node.spans,
                           attributes=node.attributes,
                           link=node.link, nodeType=node.nodeType, role=node.role)
        else:
            return AnyNode(name=PlaceDependencyTree.preprocess_names(node.name), spans=node.spans,
                           attributes=override['attributes'],
                           link=override['link'], nodeType=override['nodeType'], role=node.role)

    @staticmethod
    def preprocess_names(string):
        if string.startswith('a '):
            return string.replace('a ', '')
        elif string.startswith('an '):
            return string.replace('an ', '')
        elif string.startswith('the '):
            return string.replace('the ', '')
        return string

    def print_dependencies(self):
        for dep in self.dependencies:
            print(dep)
