#!/usr/bin/env python
#-*- coding:utf-8 -*-

'''
Created on 1 February 2012

@author: Gerrit Niezen (g.niezen@tue.nl)
'''

from TripleStore import *
import uuid, os, sys
from datetime import datetime
import time
import iso8601
from RDFTransactionList import * #helper functions for working with triples
import threading
import pytz

connected = False
deviceID = "wakeup1"
wakeupPeriod = 20
subscriptions = {}
alarms = {} # alarms[sleepSeconds, [alarmXSD, thread] ]



#timezone = pytz.timezone("UTC")
timezone = pytz.timezone('Europe/Amsterdam')

ie_ns = "https://raw.github.com/iddi/nl.tue.id.sofia/master/nl.tue.id.sofia.ontologies/src/InteractionEvents.owl#"
sc_ns = "https://raw.github.com/iddi/nl.tue.id.sofia/master/nl.tue.id.sofia.ontologies/src/SemanticConnections.owl#"

class Alarm(threading.Thread):
    def __init__(self, zzz):
        threading.Thread.__init__(self)
        self.setDaemon(True) #thread should end when main thread ends
        self.zzz = zzz
        self._stop = threading.Event()

    def run(self):
    
        if self.zzz < 0:
            print "Not enough time to commence wakeup sequence."
        else:
            
            time.sleep(self.zzz) # Wait for wakeup sequence
        
            if self.stopped() == True:
                print "Alarm was cancelled."
            else:    
                print("Commencing wakeup sequence")

                for i in range(0,255,255/(wakeupPeriod/2)):
                    start_time = time.time()
                    addEvent("AdjustLevelEvent",i)
                    duration = time.time() - start_time
                    print "Response time: %.3f" % duration
                    if (duration<2):
                        time.sleep(2-(duration))
                
                del alarms[self.zzz]

    def stop(self):
        self._stop.set()

    def stopped(self):
        return self._stop.isSet()
        

def setAlarm(alarmTime):
    alarm = Alarm(alarmTime)
    alarm.start()
    return alarm
 

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
            
            #TODO: Remove existing wakup sequences?


class eventHandler:	
    def handle(self, added, removed):
        """Callback function for subcribe_rdf method"""
        global node, wakeupPeriod
                
        print "Subscription:"
        for i in added:
            #New event occurred
            eventID = i[0][0]
            print "Event: " + eventID
            
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
                
                if(eventType == ie_ns+"AlarmSetEvent"):

                    alarm = iso8601.parse_date(dataValue) #strptime doesn't parse timezones correctly, install this via easy_install iso8601
                    
                    #Check that alarm is not already set
                    for i in alarms.values():
                        if alarm == i[0]:
                            print "Alarm already set."
                            node.CloseQueryTransaction(qs)
                            node.CloseQueryTransaction(qs2)
                            return
                    
                    #delta = alarm - dateti^^<http://www.w3.org/2001/XMLSchema#dateTime>me.now(tz=timezone) #now iso8601 screws up the timezone
                    zeTime = datetime.now()
                    delta = alarm.replace(tzinfo=None) - zeTime
                    
                    wakeup = wakeupPeriod + 4
                    
                    seconds = delta.seconds - wakeup #start 'wakeupPeriod' seconds earlier
                    
                    print "Wakeup: Alarm " + str(alarm) + " minus " +str(wakeup)+ " seconds"
                    print "Wakeup sequence is set for " + str(seconds) + " seconds from now"
                    
                    # Set the alarm
                    threadID = setAlarm(seconds)
                    print "Thread ID: " + str(threadID)
                    alarms[seconds] = [alarm, threadID]
                    
                    addEvent("IndicatorEvent"); # Since functional feedback is in future, we need to provide feedback
                    
                    
                if(eventType == ie_ns + "AlarmRemoveEvent"):
                    
                    found = None
                    alarm = iso8601.parse_date(dataValue) 
                    print "Removing alarm .. " + str(alarm.replace(tzinfo=None))
                    for i in alarms.iterkeys():
                        if alarms[i][0].replace(tzinfo=None) == alarm.replace(tzinfo=None):
                            print "Found alarm. Removing.."
                            found = i
                            alarms[i][1].stop()
                            
                    if(found):
                        print "Deleting alarm from list."
                        del alarms[found]
                    
#                if(eventType == ie_ns + "AlarmEndEvent"):
#                    print "Alarm dismissed. Light level to 0."
#                    addEvent("AdjustLevelEvent",0);
                
                if(eventType == ie_ns + "PreviewEvent"):
                    #addEvent("IncreaseLevelEvent","255","3", "PreviewEvent")
                    addEvent("IncreaseLevelEvent","255","3");
                    addEvent("PreviewEvent");
                    
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
    
    #Checking for outgoing connections and if source is connected to sink 
    #is only possible for semantic transformer, and maybe not necessary
            
#    print "Checking for outgoing connections.."
#    qs = node.CreateQueryTransaction(smartSpace)
#    result = qs.rdf_query([((ie_ns+deviceID, sc_ns+"connectedTo", None),"uri")])
#    
#    for item in result:
#        sink = item[0][0]
#        print "Is source connected to sink?"
#        qs = node.CreateQueryTransaction(smartSpace)
#        result = qs.rdf_query([((source, sc_ns+"connectedTo", sink),"uri")])
        
#    if(result != None):
    print "Subscribing to source events: " + source # TODO: Use qnames
    rs1 = node.CreateSubscribeTransaction(smartSpace)
    result_rdf1 = rs1.subscribe_rdf([((None, ie_ns+"generatedBy", source), 'uri')], eventHandler())
    subscriptions[source] = rs1
            
def registerDevice():
    t = RDFTransactionList()
    u1 = ie_ns + deviceID
    
    t.setType(u1,ie_ns + "SmartObject")
    #t.add_uri(u1,ie_ns + "functionalitySource",ie_ns + "AdjustLevel")
    #t.add_uri(u1,ie_ns + "functionalitySink",ie_ns + "Alarm")
    
#    u2 = ns + "id" + str(uuid.uuid4())
#    t.add_uri(u1, sofia_ns + "hasIdentification",u2)
#    t.add_uri(u2, semint_ns + "ofIDType", semint_ns + "RFID_Mifare")
#    t.add_literal(u2, semint_ns + "idValue", "\"" + RFID + "\"^^<http://www.w3.org/2001/XMLSchema#string>")
            
    ts.insert(t.get())
    print "Device registered."

        
def main():
    #Connects to smart space
    
    global ts, sc
    
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
    

