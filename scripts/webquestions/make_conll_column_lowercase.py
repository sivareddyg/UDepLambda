import sys

cols = [int(col) for col in sys.argv[1].split(";")]

for line in sys.stdin:
    parts = line.strip().split()
    if line.strip() == "":
        print
        continue
    for col in cols:
        parts[col] = parts[col].lower()
    print "\t".join(parts)
