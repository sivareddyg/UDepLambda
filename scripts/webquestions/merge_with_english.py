'''
Created on 15 Apr 2016

@author: siva
'''

import json
import sys

spanish_sentences_file = sys.argv[1]
english_sentences_file = sys.argv[2]

spanish_sentences = []
for line in open(spanish_sentences_file):
    spanish_sentences.append(line.strip())

english_to_spanish = {}
for line in open(english_sentences_file):
    english = line.strip()
    english_to_spanish[english] = spanish_sentences[len(english_to_spanish)]

for line in sys.stdin:
    sentence = json.loads(line)
    sentence["sentence"] = english_to_spanish[sentence['original']]
    print json.dumps(sentence)
