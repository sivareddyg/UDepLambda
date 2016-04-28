'''
Created on 15 Apr 2016

@author: siva

Extracts a subset of sentences given a larger file.

python extract_subset.py file_containing_the_subset_of_setences < the_file_containing_all_sentences 

'''

import json
import sys


subset = set()
for line in open(sys.argv[1]):
    subset.add(line.strip())

for line in sys.stdin:
    sentence = json.loads(line)
    if sentence["original"] in subset:
        sys.stdout.write(line)
