import sys

from2to = []
for entry in sys.argv[1].split(","):
    from2to.append([int(entry.split(":")[0]), int(entry.split(":")[1])])

f1 = open(sys.argv[2])
f2 = open(sys.argv[3])

for line1 in f1:
    parts1 = line1.strip().split("\t")
    line2 = f2.readline()
    parts2 = line2.strip().split("\t")
   
    if line1.strip() == "":
        print
    else:
        for source, target in from2to:
            parts2[target] = parts1[source]
        print "\t".join(parts2)
