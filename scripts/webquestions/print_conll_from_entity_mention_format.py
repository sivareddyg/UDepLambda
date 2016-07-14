#!/usr/bin/env python
# -*- coding: utf-8 -*-

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
parser.add_argument("-f", "--feat", action="store_true")
parser.add_argument("-s", "--sent", action="store_true")
args = parser.parse_args()

for line in sys.stdin:
    sent = json.loads(line)
    if not args.sent:
        words = sent['sentence'].split()
        postags = sent['posSequence'].split()
        index = sent["index"]
        for i in range(len(words)):
            tag = postags[i] if args.postag else "_"
            feat = index if args.feat and i == 0 else "_"
            sys.stdout.write("%d\t" % (i + 1))
            sys.stdout.write("%s" % (words[i].encode("utf-8", "ignore")))
            sys.stdout.write("\t_\t%s\t%s\t%s\t_\t_\t_\t_\n" %
                             (tag, tag, feat))
    else:
        sys.stdout.write(sent['sentence'].encode("utf-8"))
    sys.stdout.write("\n")
