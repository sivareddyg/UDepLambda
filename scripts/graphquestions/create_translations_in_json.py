import sys
import json

translations = {}
for line in open(sys.argv[1]):
    try:
        qid, source, target = line.strip().split("\t")
        translations[int(qid)] = target
    except:
        sys.stderr.write(line)

    

for line in sys.stdin:
    sent = json.loads(line)
    target = translations[sent['id']]
    sent['original'] = sent['sentence']
    sent['sentence'] = target
    print json.dumps(sent)
