import sys
import json

sent_index = {} 
for i, line in enumerate(json.loads(open(sys.argv[1]).read())):
    sent_index[line['utterance']] = i + 1

for line in open(sys.argv[2]):
    print "%d\t%s" %(sent_index[line.strip()], line.strip())
