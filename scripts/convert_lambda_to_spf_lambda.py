'''
Created on 9 Apr 2015

@author: siva
'''

import re
import sys

#lambda: "(LAMBDA F G X : (EXISTS Y : (p:EVENT.ENTITY:$noun$.$prep$.arg_1 X X) (p:EVENT.ENTITY:$noun$.$prep$.arg_2 X Y) (F Y) (G X)))"


def find_closing_bracket(parts):
    open_bracket_count = 0
    for i, part in enumerate(parts):
        if part == "(":
            open_bracket_count += 1
        if part == ")":
            if open_bracket_count == 0:
                return i
            open_bracket_count += -1


def process_lambda(exp, old_to_new):
    exp = exp.strip()
    if re.match("\((LAMBDA|EXISTS)", exp):
        first = exp.find("(")
        last = exp.rfind(")")
        exp = exp[first + 1: last]
        vars, sub_exp = exp.split(":", 1)
        vars = vars.strip().split()
        var_type = vars[0]
        vars = vars[1:]
        for var in vars:
            old_to_new[var] = len(old_to_new.keys())
        print old_to_new
        new_sub_exp = process_lambda(sub_exp, old_to_new)

        new_exp = new_sub_exp
        for var in reversed(vars):
            if var_type == "LAMBDA":
                new_exp = "(lambda $%s %s)" % (
                    old_to_new[var], new_exp)
            elif var_type == "EXISTS":
                new_exp = "(exists:ex $%s %s)" % (
                    old_to_new[var], new_exp)
        return new_exp

    parts = re.split("([\(\)\s])", exp)
    i = 0
    while i < len(parts):
        if parts[i] == "(":
            j = find_closing_bracket(parts[i + 1:])
            sub_exp = process_lambda(exp, old_to_new)
        i += 1

    return "(and:<t*,t> %s)" % (exp)

lambda_pattern = re.compile("[\s]*lambda\:")
for line in sys.stdin:
    if lambda_pattern.match(line):
        parts = line.split("lambda: ")
        exp = parts[1].strip().strip('"')
        print process_lambda(exp, {})
    else:
        print line[:-1]
