#!/usr/bin/env python
#-*- coding:utf-8 -*-

'''
Created on 7 March 2012

@author: Gerrit Niezen (g.niezen@tue.nl)
'''

from TripleStore import *
import uuid, os, sys
from RDFTransactionList import * #helper functions for working with triples
import serial
import time

connected = False
deviceID = "lamp1"
RFID = "440085D44D"
subscriptions = {}

lightLevel = 0

ie_ns = "http://sofia.gotdns.com/ontologies/InteractionEvents.owl#"
sc_ns = "http://sofia.gotdns.com/ontologies/SemanticConnections.owl#"

class newConnectionHandler:
    def handle(self, added, removed):
        global node
    
        for i in added:
           subscribeToSourceEvents(i[0][0])
           
        for i in removed:
            source = i[0][0]
            print "Disconnected from " + source 
            
            print "Unsubscribing from events.."
            node.CloseSubscribeTransaction(subscriptions[source])
            del subscriptions[source]

class eventHandler:	
    def handle(self, added, removed):
        """Callback function for subcribe_rdf method"""
        global node
                
        print "Subscription:"
        for i in added:
            #New event occurred
            eventID = i[0][0]
            print "Event: " + eventID
            
            duration = 0
            light = -1
            
            qs = node.CreateQueryTransaction(smartSpace)
            result = qs.rdf_query([((eventID, ie_ns+"dataValue", None),"uri")])
            for item in result:
                dataValue = item[0][2]
                print "dataValue: " + dataValue
            
            qs2 = node.CreateQueryTransaction(smartSpace)
            result2 = qs2.rdf_query([((eventID, "rdf:type", None),"uri")])
            for item2 in result2:
                eventType = item2[0][2]
                print "Type:" + eventType
                
                if(eventType == ie_ns+"IncreaseLevelEvent"):
                    print "IncreaseLevelEvent" 
                    qs3 = node.CreateQueryTransaction(smartSpace)
                    result3 = qs2.rdf_query([((eventID, ie_ns + "duration", None),"uri")])
                    
                    for item3 in result3:
                        duration = item3[0][2]
                        print "Duration: " + str(duration)

                    node.CloseQueryTransaction(qs3)
                
                if(eventType == ie_ns+"AdjustLevelEvent"):
                    print "AdjustLevelEvent"
                    
                
                if(eventType == ie_ns+"IndicatorEvent"):
                    print "IndicatorEvent"
                    setLightLevel(0)
                    time.sleep(0.25)
                    setLightLevel(255)
                    time.sleep(0.25)
                    setLightLevel(0)
                    setLightLevel(lightLevel)

            if(duration != 0):
                increaseLevelEvent(duration, light)
            else:
                if(light != -1):
                #Graceful degradation: Only an adjustLevelEvent
                    setLightLevel(light)


            node.CloseQueryTransaction(qs)
            node.CloseQueryTransaction(qs2)




def addEvent(eventType, dataValue=None, duration=None, additionalEventType=None):
    """Adds new event with metadata (generatedBy, datetime) to smart space"""
    global ts
    
    print "Adding event: " + eventType

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

    if(duration != None):
        print "Duration: " + str(duration)
        t.add_literal(u1, ie_ns + "duration", str(duration))
        # for xsd:duration, this should be declared as PT3S^^xsd:duration, where P=Period, T=Time and S=Seconds
    
    if(additionalEventType != None):
        print "Additional event type: " + str(additionalEventType)
        t.add_uri(u1, "rdf:type", ie_ns+ str(additionalEventType))
        
    ts.insert(t.get())
    return u1



def subscribeToSourceEvents(source):
    global node

    print "Subscribing to source events: " + source # TODO: Use qnames
    rs1 = node.CreateSubscribeTransaction(smartSpace)
    result_rdf1 = rs1.subscribe_rdf([((None, ie_ns+"generatedBy", source), 'uri')], eventHandler())
    subscriptions[source] = rs1


def registerDevice():
    t = RDFTransactionList()
    u1 = ie_ns + deviceID
    
    t.setType(u1,ie_ns + "SmartObject")
    t.add_uri(u1,ie_ns + "functionalitySink",ie_ns + "AdjustLevel")
    
    u2 = ns + "id" + str(uuid.uuid4())
    t.add_uri(u1, sofia_ns + "hasIdentification",u2)
    t.add_uri(u2, semint_ns + "ofIDType", semint_ns + "RFID_Mifare")
    t.add_literal(u2, semint_ns + "idValue", "\"" + RFID + "\"^^<http://www.w3.org/2001/XMLSchema#string>")
            
    ts.insert(t.get())
    print "Device registered."
    
def setLightLevel(value):
    lightPort.write('V')
    lightPort.write(value)
    print "Set light level: " + str(value)
    
def increaseLevelEvent(duration, value):
    lightPort.write('D')
    lightPort.write(duration)
    lightPort.write(value)
    print "Increase light level, duration: " + str(duration) + ", value: " + str(value) 

        
def main():
    #Connects to smart space
    
    global ts, sc
    
    lightPort = serial.Serial("/dev/tty.ARDUINOBT-BluetoothSeri",115200)
    
    try:
        ts = TripleStore()
        connected = True
        
        addEvent("ConnectEvent")

    except:
        print "Cannot connect to smart space."
        return
        
    if connected:
        registerDevice()
        
        print "Checking for incoming connections.."
        qs = node.CreateQueryTransaction(smartSpace)
        result = qs.rdf_query([((None, sc_ns+"connectedTo", ie_ns+deviceID),"uri")])
        for source in result:
            subscribeToSourceEvents(source[0][0])          
    
        print "Subscribing to new connections..."
        rs1 = node.CreateSubscribeTransaction(smartSpace)
        result_rdf1 = rs1.subscribe_rdf([((None, sc_ns+"connectedTo", ie_ns+deviceID), 'uri')], newConnectionHandler())
        

    print "Ctrl+C to exit"
    
    lightPort.flushInput()

    try:
        while 42:
            raw_input()
                
    except KeyboardInterrupt: #So use Ctrl+C to exit event loop
        
        print "Unsubscribing RDF subscriptions"
        node.CloseSubscribeTransaction(rs1)

        print "Leaving smart space.."
        if connected:
            addEvent("DisconnectEvent")
            node.leave(smartSpace)
            print("Exiting..")
            os._exit(1) #this is required because excepting a KeyboardInterrupt makes it difficult to exit the program


if __name__ == "__main__":
    main()
    

