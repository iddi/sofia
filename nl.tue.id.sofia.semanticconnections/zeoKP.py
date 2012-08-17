#!/usr/bin/env python
#-*- coding:utf-8 -*-

'''
Created on 18 January 2012

@author: Gerrit Niezen (g.niezen@tue.nl)
'''

from ZeoRawData.BaseLink import BaseLink
from ZeoRawData.Parser import Parser
from TripleStore import *
import uuid, os, sys
from datetime import datetime
import time
import iso8601
from RDFTransactionList import * #helper functions for working with triples

connected = False
deviceID = "zeo"

ie_ns = "http://sofia.gotdns.com/ontologies/InteractionEvents.owl#"

def addEvent(eventType):
	"""Adds new event with metadata (generatedBy, datetime) to smart space"""
	global ts
	
	t = RDFTransactionList()
	u1 = ie_ns + "event" + str(uuid.uuid4())
	t.setType(u1, ie_ns + eventType)
	t.add_uri(u1, ie_ns + "generatedBy", ie_ns + deviceID)
	dt = datetime.now()
	xsddt = '"'+dt.strftime('%Y-%m-%dT%H:%M:%S%z')  + '"^^<http://www.w3.org/2001/XMLSchema#dateTime>' # e.g. "2011-01-19T16:10:23"^^<http://www.w3.org/2001/XMLSchema#dateTime>
	t.add_literal(u1, ie_ns + "inXSDDateTime", xsddt)
	ts.insert(t.get())
	return u1
	
def sliceCallback(slice):
    return
	
def eventCallback(timestamp, version, event):
    print event

def main():
    #Connects to smart space
    
    global ts, sc
    
    try:
        ts = TripleStore()
        connected = True
        addEvent("ConnectEvent")

    except:
        print "Cannot connect to smart space."    

    # Initialize
    link = BaseLink('/dev/ttyUSB0')
    parser = Parser()

    # Add the Parser to the BaseLink's callback
    link.addCallback(parser.update)

    # Add your callback functions
    parser.addEventCallback(eventCallback)
    parser.addSliceCallback(sliceCallback)

    # Start link
    link.start()

    print "Ctrl+C to exit"

    try:
        while 42:
            raw_input()

    #So use Ctrl+C to exit event loop
    except KeyboardInterrupt:

        print "Leaving smart space.."
        if connected:
            addEvent("DisconnectEvent")
            node.leave(smartSpace)
            print("Exiting..")
            os._exit(1) #this is required because excepting a KeyboardInterrupt makes it difficult to exit the program


if __name__ == "__main__":
    main()
