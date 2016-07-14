'''
Created on 13 Jul 2016

@author: siva
'''

import argparse
import json
import sys

parser = argparse.ArgumentParser()
parser.add_argument("-t", "--postag", action="store_true")
parser.add_argument("-l", "--lemma", action="store_true")
args = parser.parse_args()


def printer(sentence):
    if "forest" in sentence:
        index = sentence["index"] if "index" in sentence else None
        for sent in enumerate(sentence["forest"]):
            sentence_printer(sent, parser, forest_index=index)
    else:
        sentence_printer(sentence, parser)


def sentence_printer(sentence, parser, forest_index=None):
    for index, word in enumerate(sentence['words']):
        lex = word['word']
        lemma = word['lemma'] if args.lemma and "lemma" in word else "_"
        tag = word['pos'] if args.postag and "pos" in word else "_"
        feats = []
        if index == 0 and forest_index:
            feats.append("forest_id=%s" % (forest_index))
        if index == 0 and "index" in sentence:
            feats.append("sentence_id=%s" % (sentence["index"]))
        feats_str = "|".join(feats) if len(feats) > 0 else "_"
        print "%d\t%s\t%s\t%s\t%s\t%s\t_\t_\t_\t_" % (index + 1, lex, lemma, tag, tag, feats_str)
    print

for line in sys.stdin:
    if line.startswith("#") or line.strip() == "":
        continue
    sent = json.loads(line)
    printer(sent)
