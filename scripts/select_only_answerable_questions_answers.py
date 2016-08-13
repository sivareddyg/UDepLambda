import sys
import json

has_answers = set()
for line in open(sys.argv[1]):
    answers = json.loads(line.strip().split("\t")[2])
    if answers != []:
        has_answers.add(line.split("\t")[0])

for line in sys.stdin:
    if line.startswith("#") or line.strip() == "":
        continue
    sent = line.split("\t")[0]
    if sent in has_answers:
        sys.stdout.write(line)
