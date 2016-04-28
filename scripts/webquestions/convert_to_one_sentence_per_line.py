'''
Created on 15 Apr 2016

@author: siva

Reads the original webquestions file and spits out sentences, each in a new
 line. Additionally it adds two keys "sentence" and "original" remove
  "utterance" key.

'''

import json
import sys

text = sys.stdin.read()
sentences = json.loads(text)

for sentence in sentences:
    sentence["sentence"] = sentence["utterance"]
    sentence["original"] = sentence["utterance"]
    del sentence["utterance"]
    print json.dumps(sentence)
