import sys
import json
import re

f1 = open(sys.argv[1])
f2 = open(sys.argv[2])

def getSRLArgs(fp):
    srl = {}
    sent = []
    while True:
        line = fp.readline()
        if line.strip() == "":
            break
        sent.append(line.split())
        predicates = []
        for i, word in enumerate(sent):
            if word[13] != "_":
                predicates.append(i)
        for i, p in enumerate(predicates):
            for j, word in enumerate(sent):
                arg = word[14 + i]
                if arg != "_":
                    if arg.startswith("A0") or arg.startswith("R-A0") or arg.startswith("arg0"):
                        srl[(str(p),str(j))] = "arg1" # UDepLambda uses arg1 for agent
                    elif arg.startswith("A1") or arg.startswith("R-A1") or arg.startswith("arg1"):
                        srl[(str(p),str(j))] = "arg2" # UDepLambda uses arg1 for patient
    return srl

for line in f1:
    forest = json.loads(line)
    for sent in forest['forest']:
        srl = getSRLArgs(f2)
        parses = sent['dependency_lambda']
        mparses = []
        for parse in parses:
            mparse = []
            for predicate in parse:
                matches = re.findall(".*arg1\(([0-9]+):e , ([0-9]+):.*\)", predicate)
                if matches != []:
                    match = (matches[0][0], matches[0][1])
                    if match in srl and srl[match] != "arg1":
                        #print predicate, matches, srl[(matches[0][0], matches[0][1])]
                        predicate = predicate.replace("arg1(", srl[match] + "(")
                        
                matches = re.findall(".*arg2\(([0-9]+):e , ([0-9]+):.*\)", predicate)
                if matches != []:
                    match = (matches[0][0], matches[0][1])
                    if match in srl and srl[match] != "arg2":
                        # print predicate, matches, srl[(matches[0][0], matches[0][1])]
                        predicate = predicate.replace("arg2(", srl[match] + "(")

                mparse.append(predicate)
            mparses.append(mparse)
        sent['dependency_lambda'] = mparses
    print json.dumps(forest)
