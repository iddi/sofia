#!/usr/bin/python
# 
# In comes a line 
# Stable Model: "audiodev"("moo_587d8f98-4fea-4148-bbad-9fcc05c08e3a") "audiodev"("moo_637bfddd-886f-43e2-a4be-dc6854e88993")
#
# and need to split it by a single space, but strings inside the "" marks
# may contain spaces which should not count. 
# Prints out the tokes line-by-line. 
#
# This is useless without the ssls program, which this file should accompany. 
#
# This code is in public domain. 
#
# Author: vesa.luukkala@nokia.com 
#

import sys

l = sys.stdin.readlines()

def next_space(s):
    """
    Get the index of the next space which is not inside "'s.
    """
    inside = False
    ctr = 0
    for i in s:
        if i == '"':
            if inside:
                inside = False
            else:
                inside = True
        if i == ' ':
            if not inside:
                return ctr
        ctr = ctr + 1
    return ctr

def deprolog(s):
    """
    Takes in a prolog predicate 
    "rdf:type"("moo_4f86546c-dcda-48e0-a17f-76f9cb5e6c24","ns_1:Activity")
    and produces a string more suitable for ssls parsing:
    rdf:type moo_4f86546c-dcda-48e0-a17f-76f9cb5e6c24,ns_1:Activity
    so:
    a space after the name and the rest separated by commas.
    """
    res = ''
    inside = False
    for i in s:
        if i == '"':
            if inside:
                inside = False
            else:
                inside = True
        elif i == '(':
            if not inside:
                res = res + ' '
            else:
                res = res + i
        elif i == ')':
            if inside:
                res = res + i
            else:
                # just ignore it 
                pass 
        else:
            res = res + i
    return res



if len(l) == 0:
    print 'None'
    sys.exit(0)

found = False

for i in l:
    # Now i is a single line that needs to be parsed
    pref = 'Stable Model: '
    if i.startswith(pref):
        found = True
        content = i[len(pref):]
        clen = len(content)
        while clen > 0:
            nxt = next_space(content)
            token = content[0:nxt]
            if not token.isspace():
                print deprolog(token)
            content = content[(nxt+1):]
            clen = len(content)
            #print 'YYY remaining at', clen, content
        #print 'Done'
        #sys.exit(0)

if not found:
    print 'None'






