#!/usr/bin/env python
#-*- coding:utf-8 -*-

'''
Created on 7 February 2012

@author: Gerrit Niezen (g.niezen@tue.nl)
'''

from TripleStore import *
import uuid, os, sys
from datetime import datetime
import time
import iso8601
from RDFTransactionList import * #helper functions for working with triples


deviceID = "wakeup1"
wakeupPeriod = 10

ie_ns = "https://raw.github.com/iddi/nl.tue.id.sofia/master/nl.tue.id.sofia.ontologies/src/InteractionEvents.owl#"
sc_ns = "https://raw.github.com/iddi/nl.tue.id.sofia/master/nl.tue.id.sofia.ontologies/src/SemanticConnections.owl#"

def addEvent(eventType, dataValue=None):
    """Adds new event with metadata (generatedBy, datetime) to smart space"""
    global ts

    t = RDFTransactionList()
    u1 = ie_ns + "event" + str(uuid.uuid4())
    t.setType(u1, ie_ns + eventType)
    t.add_uri(u1, ie_ns + "generatedBy", ie_ns + deviceID)
    dt = datetime.now()
    xsddt = '"'+dt.strftime('%Y-%m-%dT%H:%M:%S%z')  + '"^^<http://www.w3.org/2001/XMLSchema#dateTime>' # e.g. "2011-01-19T16:10:23"^^<http://www.w3.org/2001/XMLSchema#dateTime>
    t.add_literal(u1, ie_ns + "inXSDDateTime", xsddt)

    if(dataValue != None):
        print "Data value: " + str(dataValue)
        t.add_literal(u1, ie_ns + "dataValue", str(dataValue))
#        if type(dataValue).__name__=='int':
#            t.add_literal(u1, ie_ns + "dataValue", str(dataValue) + '^^<http://www.w3.org/2001/XMLSchema#integer>')

    ts.insert(t.get())
    return u1

ts=TripleStore()

print("Commencing wakeup sequence")

for i in range(0,255,255/wakeupPeriod):
    start_time = time.time()
    addEvent("AdjustLevelEvent",i)
    duration = time.time() - start_time
    print "Response time: %.3f" % duration
    if (duration<1):
        time.sleep(1-(duration))
        
        
