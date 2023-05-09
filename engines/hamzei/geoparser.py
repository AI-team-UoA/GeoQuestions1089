##############################################
# Author: -- (--)
# Description:
#   GeoParser main class
##############################################

import json
import logging
import re

from quantulum3 import parser

from ner import NER, CPARSER, Embedding, DPARSER
from parse_tree import FOLGenerator
from query_generator import SPARQLGenerator
from request_handler import Configuration

logging.basicConfig(level=logging.INFO)

COMPOUNDS_QW = ['How many', 'Are there', 'Is there', 'how many', 'are there', 'is there', 'In which', 'In what',
                'Through which', 'Through what']
COMPOUNDS_QW_ROLE = {'How many': '6', 'Are there': '8', 'Is there': '8', 'In which': '3', 'In what': '2',
                     'Through which': '3', 'Through what': '2'}


# load place type
def load_pt(fpt):
    pt_set = set()
    pt_dict = dict()
    fpt = open(fpt, 'r', encoding="utf-8")
    for line in fpt.readlines():
        trimmed = line.strip()
        if len(trimmed) > 0:
            pt_set.add(trimmed)
            pt_set.add('the ' + trimmed)
            pt_dict[trimmed] = 1
            pt_dict['the ' + trimmed] = 1
    fpt.close()
    return pt_set, pt_dict


# load word
def load_word(fword):
    words = set()
    fword = open(fword, 'r', encoding="utf8")
    for line in fword.readlines():
        word = line.strip()
        words.add(word)
    fword.close()
    return words


# load dataset
def load_dataset(path):
    questions = []
    fdataset = open(path, 'r', encoding='utf-8-sig')
    for line in fdataset.readlines():
        questions.append(line)
    fdataset.close()
    return questions

def load_dataset_json(path):
    with open(path, 'r') as questions_file:
        questions = []
        questions_data = json.load(questions_file)
        for i in (questions_data):
            questions.append(questions_data[str(i)]['Question'])
        return questions


def load_dummy_dataset():
    return ["Where is the University of Manchester?", "How many universities are in London?",
            "What is the population of Liverpool?"]


def standardize(question):
    try:
        quants = parser.parse(question)
        for q in quants:
            val = str(q.value)
            unit = q.unit.name
            if unit.endswith('re'):
                unit = unit[:-2] + 'er'
            if val.endswith('.0'):
                val = val[0:-2]
            if q.unit.name == 'dimensionless':
                question = question.replace(q.surface, val)
            else:
                question = question.replace(q.surface, val + ' ' + unit)
    except:
        logging.info('error in normalization...')
    return question


# find toponyms
def find_toponyms(question):
    return NER.extract_place_names(question)


# find events
def find_events(question):
    return NER.extract_events(question)


# find place types and event types
def find_types(question, excluded, types, specifics=[]):
    whole_question = question
    for ex in excluded:
        question = question.replace(ex, '')
    question = question.lower().strip()
    splits = question.split()
    captured = []
    for type in types:
        if (' ' not in type and type in splits) or (' ' in type and type in question):
            captured.append(type)
            for specific in specifics:
                if type + ' ' + specific in whole_question:
                    captured.append(type + ' ' + specific)
                elif specific + ' ' + type in whole_question:
                    captured.append(specific + ' ' + type)
                elif not type.endswith(
                        "s") and 'the ' + type + ' of ' + specific in whole_question:
                    captured.append('the ' + type + ' of ' + specific)
    captured = sorted(captured, key=len, reverse=True)
    return captured


# find dates
def find_dates(question):
    return NER.extract_dates(question)


def find_compound_question_words(question):
    res = {}
    for comp in COMPOUNDS_QW:
        if comp in question:
            if comp in COMPOUNDS_QW_ROLE.keys():
                res[comp + '--' + str(question.index(comp))] = {'start': question.index(comp),
                                                                'end': question.index(comp) + len(comp),
                                                                'role': COMPOUNDS_QW_ROLE[comp], 'pos': 'ADV'}
            else:
                res[comp + '--' + str(question.index(comp))] = {'start': question.index(comp),
                                                                'end': question.index(comp) + len(comp),
                                                                'role': '', 'pos': 'ADV'}
    return res


# extract information
def extract_information(question, ptypes, etypes):
    toponyms = find_toponyms(question)
    events = find_events(question)
    dates = find_dates(question)

    excluded = []
    excluded.extend(toponyms)
    excluded.extend(events)
    excluded.extend(dates)

    place_types = find_types(question, excluded, ptypes, toponyms)
    for toponym in toponyms:
        for place_type in place_types:
            if toponym in place_type:
                idx = toponyms.index(toponym)
                toponyms[idx] = place_type
                idx = place_types.index(place_type)
                del place_types[idx]
                break
    excluded.extend(place_types)

    event_types = find_types(question, excluded, etypes, events)
    for event in events:
        for type in event_types:
            if event in type:
                events[events.index(event)] = type
                del event_types[event_types.index(type)]
                break
    results = {}
    results['toponyms'] = toponyms
    results['events'] = events
    results['dates'] = dates
    results['place_types'] = place_types
    results['event_types'] = event_types

    return results


def construct_cleaning_labels(results, question):
    orders = ['toponyms', 'events', 'dates', 'place_types', 'event_types']
    indices = []
    labelled = {}
    for order in orders:
        values = results[order]
        role = ENCODINGS[order]
        for v in values:
            temp = v
            if temp not in question:
                temp = temp.replace(" 's", "'s")
            matches = re.finditer(temp, question)
            matches_positions = [[match.start(), match.end()] for match in matches]
            for position in matches_positions:
                if not is_overlap(position, indices):
                    labelled[v + '--' + str(position[0])] = {'start': position[0],
                                                             'end': position[1],
                                                             'role': role,
                                                             'pos': 'NOUN'}
            indices.extend(matches_positions)
    return labelled


def is_overlap(position, indices):
    for index in indices:
        if position[0] >= index[0] and position[1] <= index[1]:
            return True
    return False


def clean_extracted_info(info):
    clean_info = {}
    for k1, v1 in info.items():
        correct = True
        for k2, v2 in info.items():
            if k1 != k2:
                if re.split('--', k1.strip())[0] in k2 and v1['start'] >= v2['start'] and v1['end'] <= v2['end']:
                    correct = False
                    break
        if correct:
            clean_info[k1] = v1
    return clean_info


# standardization of addresses and 's as containment
def refine_questions(question, toponyms, types):
    for t in toponyms:
        for t2 in toponyms:
            if t + ', ' + t2 in question:
                question = question.replace(t + ', ' + t2, t + ' in ' + t2)
        for t2 in types:
            if t + "'s " + t2 in question:
                question = question.replace(t + "'s " + t2, 'the ' + t2 + ' of ' + t)

    for key, pattern in SUPERLATIVE_SP_REGEX.items():
        reg_search = re.search(pattern, question)
        if reg_search is not None:
            current = question[reg_search.regs[0][0]: reg_search.regs[0][1]]
            refined = reg_search.group(1) + ' ' + key
            question = question.replace(current, refined)

    return question


def write_labels():
    with open('evaluation/eval.json', "w", encoding='utf-8') as jsonfile:
        json.dump(eval, jsonfile, ensure_ascii=False)


def read_labels():
    with open('evaluation/eval.json', encoding='utf-8') as jsonfile:
        data = json.load(jsonfile)
    return data


def append_to_file(string):
    with open('console.txt', 'a') as redf:
        redf.write(string)


def clean_file():
    with open('console.txt', 'w') as redf:
        redf.write("")


def ask_eval_input(key):
    res = {}
    f = lambda x: '' if x is None else x
    res['TP'] = f(input('how many {} are correctly detected?'.format(key)))
    res['FP'] = f(input('how many {} are incorrectly detected?'.format(key)))
    res['FN'] = f(input('how many {} are missing?'.format(key)))
    return res


PRONOUN = dict(
    {'Where': '1', 'What': '2', 'Which': '3', 'When': '4', 'How': '5', 'Why': '7', 'Does': '8',
     'Is': '8', 'Are': '8', 'Do': '8'})
CONDITIONAL = ['are', 'is', 'was', 'were', 'did', 'do', 'does']

ENCODINGS = dict(
    {'toponyms': 'P', 'place_types': 'p', 'events': 'E', 'event_types': 'e', 'dates': 'd', 'spatial_relationship': 'r',
     'qualities': 'q', 'activities': 'a', 'situations': 's', 'non-platial_objects': 'o'})

COMPARISON = {'more than': '>', 'less than': '<', 'greater than': '>', 'smaller than': '<', 'equal to': '=',
              'at most': '<=', 'at least': '>=', 'over': '>'}

COMPARISON_REGEX = {'more .* than': 'more than', 'less .* than': 'less than', 'greater .* than': 'greater than',
                    'smaller .* than': 'smaller than'}

SUPERLATIVE_SP_REGEX = {'nearest to': 'nearest (.*) to', 'closest to': 'closest (.*) to',
                        'farthest to': 'farthest (.*) to'}

fpt = 'data/place_type/type-set.txt'
factv = 'data/verb/action_verb.txt'
fstav = 'data/verb/stative_verb.txt'
fcountries = 'data/gazetteer/countries.txt'
fet = 'data/event_type/event_types'

pt_set, pt_dict = load_pt(fpt)
et_set, et_dict = load_pt(fet)
actv = load_word(factv)
stav = load_word(fstav)
countries = load_word(fcountries)

Embedding.set_stative_active_words(stav, actv)

is_test = False  # IF TRUE: ONLY READ DUMMY QUESTIONS AND RUN THE PROGRAM

# logging.info('running parameters: test: {0}'.format(str(is_test)))
# logging.info('reading dataset...')
# if not is_test:
#     questions = load_dataset('data/datasets/GeoQuestion201.csv')
# else:
#     questions = load_dummy_dataset()  # if you want to just test to check the function...


def analyze(question, executable=False):
    try:
        logging.info('Normalized question: {}'.format(question))
        results = {'fol': None, 'encoding': None, 'query': None}
        original_question = question
        # extract NER using fine-grained NER model
        result = extract_information(question.replace('?', ''), pt_set, et_set)
        # logging.info('NER results: \n' + str(result))
        question = refine_questions(question, result['toponyms'], result['place_types'])
        question = standardize(question)

        # construct and constituency tree dependency tree
        tree = CPARSER.construct_tree(question)

        # logging.info('initial constituency tree:\n' + str(tree))
        labelled = {}
        for k, v in PRONOUN.items():
            if question.startswith(k + ' '):
                tree.label_role(k, v, question_words=True)
                labelled[k + "--" + str(question.index(k))] = {'start': question.index(k),
                                                            'end': question.index(k) + len(k), 'role': v,
                                                            'pos': 'ADV'}
        compound_qw = find_compound_question_words(question)
        for qw in compound_qw.keys():
            role = ''
            if re.split('--', qw.strip())[0] in COMPOUNDS_QW_ROLE.keys():
                role = COMPOUNDS_QW_ROLE[re.split('--', qw.strip())[0]]
            tree.label_role(re.split('--', qw.strip())[0], role, clean=True, question_words=True)
        labelled = {**labelled, **compound_qw}

        ners = construct_cleaning_labels(result, question)
        # logging.debug('clean NERS:\n' + str(ners))

        for k, v in ners.items():
            tree.label_role(re.split('--', k.strip())[0], v['role'], clean=True)

        labelled = {**labelled, **ners}
        labelled = {**labelled, **tree.label_tree()}

        verbs = tree.get_verbs()
        decisions = Embedding.verb_encoding(tree.root.name, verbs)
        labelled = {**labelled, **tree.label_situation_activities(verbs=verbs, decisions=decisions)}
        tree.label_events_actions()
        labelled = {**labelled, **tree.label_qualities()}
        tree.clean_phrases()
        tree.clean_tree()

        labelled = {**labelled, **tree.label_spatiotemporal_relationships()}

        for c, v in COMPARISON.items():
            if c in question:
                tree.label_role(c, v, comparison=True)
                labelled[c + '--' + str(question.index(c))] = {'start': question.index(c),
                                                            'end': question.index(c) + len(c),
                                                            'role': v, 'pos': 'ADJ'}
        for creg, c in COMPARISON_REGEX.items():
            reg_search = re.search(creg, question)
            if reg_search is not None:
                tree.label_complex_comparison(reg_search, c, COMPARISON[c])
                labelled[c + '--' + str(reg_search.regs[0][0])] = {'start': reg_search.regs[0][0],
                                                                'end': reg_search.regs[0][1], 'role': COMPARISON[c],
                                                                'pos': 'ADJ'}

        tree.label_events_actions()
        tree.clean_phrases()
        # logging.info('constituency tree:\n' + str(tree))
        labelled = clean_extracted_info(labelled)
        # logging.info('encoded elements:\n' + str(labelled))

        # construct dependency tree, cleaning
        d_tree = DPARSER.construct_tree(question)
        # logging.info('initial dependency tree:\n' + str(d_tree))

        d_tree.clean_d_tree(labelled)
        # logging.info('refined dependency tree:\n' + str(d_tree))

        # use FOLGenerator to detect dependencies inside both parsing trees
        # intent recognition
        # generate FOL statements based on deps (FOLGenerator class)
        fol = FOLGenerator(cons_tree=tree, dep_tree=d_tree)
        fol.generate_dependencies()

        # fol.print_dependencies()

        # print FOL statements
        # log_string = fol.print_logical_form()

        # generate GeoSPARQL queries from FOL statements (deps)
        generator = SPARQLGenerator(fol.dependencies, fol.variables)
        geosparql = generator.to_SPARQL()
        logging.info(geosparql)


        if executable:
            config = Configuration()
            config.set_url('http://localhost:1313/resolve')  # just for test
            exectuable_result = SPARQLGenerator(fol.dependencies, fol.variables, executable=True, config=config)
            results['executable'] = exectuable_result.to_SPARQL()
            logging.info(results['executable'])

        results['encoding'] = labelled
        # results['fol'] = log_string
        results['query'] = geosparql
        return results
    except:
        exectuable_result = None
        results['executable'] = None
        return results

import sys
import os

if __name__ == "__main__":
    questions = load_dataset_json(sys.argv[1])
    queries = {}
    i = 1
    for question in questions:
        query = analyze(question, executable=True)['executable']
        queries[str(i)] = query if query is not None else ""
        i += 1

    if os.path.exists(sys.argv[2]):
        os.remove(sys.argv[2])
    with open(sys.argv[2], "w") as outfile:
        outfile.write(json.dumps(queries, indent=4))
