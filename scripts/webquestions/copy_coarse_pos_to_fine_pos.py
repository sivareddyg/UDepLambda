import sys

for line in sys.stdin:
	line = line.strip()
	if line == "":
		print
		continue
	parts = line.split("\t")
	parts[4] = parts[3]
	print "\t".join(parts)
