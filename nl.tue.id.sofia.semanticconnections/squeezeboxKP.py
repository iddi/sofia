#!/usr/bin/env python
#-*- coding:utf-8 -*-

'''
Created on 12 January 2012

@author: Gerrit Niezen (g.niezen@tue.nl)
'''

from pysqueezecenter.server import Server
from pysqueezecenter.player import Player
from TripleStore import *
import uuid, os, sys
from datetime import datetime, timedelta
import time
import iso8601
from RDFTransactionList import * #helper functions for working with triples


connected = False
deviceID = "squeezebox1"
RFID = "440085D514"

ie_ns = "http://sofia.gotdns.com/ontologies/InteractionEvents.owl#"
sc_ns = "http://sofia.gotdns.com/ontologies/SemanticConnections.owl#"
subscriptions = {} #TODO: move to utilities
alarms = {}

currentTitle = ""

# configure your specific alarm and preview files with their full path:
# alarmSong = "file:///home/userName/Music/Album/Track.mp3"
alarmSong = ""
# previewSong = "file:///home/userName/Music/Album/Track.mp3"
previewSong = ""

#TODO: Fix timezone problem with pytz (see wakeup KP)

class SBMsgHandler:

    def alarmToXSD(self, alarmTime):
        
        midnight = datetime.now().replace(hour=0, minute=0, second=0, microsecond=0)
        sinceMidnight = datetime.now()-midnight
        
        if((sinceMidnight - alarmTime).days < 0): 
            print "Alarm was set for today"
            dttime = midnight+alarmTime
        else: 
            print "Alarm was set for tomorrow"
            dttime = midnight+timedelta(days=1)+alarmTime
        
        xsddt = '"' + dttime.strftime('%Y-%m-%dT%H:%M:%S+0100')  + '"^^<http://www.w3.org/2001/XMLSchema#dateTime>'
        return xsddt


    def handle(self, msg):
        
        print msg
        
        if( msg[0] == "alarm" and msg[1] == "end"):
            print "Alarm ended"
            addEvent("AlarmEndEvent")
        
         #e.g. [u'alarm', u'add', u'time:25200', u'enabled:1', u'useContextMenu:1', u'id:7d4e29f9']
        if(msg[0] == "alarm" and msg[1] == "add"):
            
        
            print "Adding new alarm.."
            try:
                alarmTime = timedelta(seconds=int(msg[2][5:]))
                alarmID = msg[5][3:]
                alarms[alarmID] = alarmTime 
                
            except:
                try:
                    print "Alarm time set with AlarmSetEvent?"
                    dow = msg[2]
                    print "Day of week: " + dow
                    alarmTime = timedelta(seconds=int(msg[3][5:])) #skip dow, and alarms[alarmID] already set
                
                except:
                    print "Alarm time not in correct format"
                    return
                
            print "Alarm time: " + str(alarmTime)
                
            xsddt = self.alarmToXSD(alarmTime)
            print xsddt
            
            addEvent("AlarmSetEvent",xsddt)
            

        
        if(msg[0] == "alarm" and msg[1] == "sound"):
            alarmID = msg[2]
            print "Alarm alert: " + str(alarmID)
            addEvent("AlarmAlertEvent")
            try:
                del alarms[alarmID]
            except:
                print "Alarm to be deleted not found"
                
        if(msg[0] == "alarm" and msg[1] == "delete"):
            alarmID = msg[2][3:]
            print "Alarm removed: " + str(alarmID)
            #TODO: Fix strftime timezone error
            xsddt = '"' + alarms[alarmID].strftime('%Y-%m-%dT%H:%M:%S+0100')  + '"^^<http://www.w3.org/2001/XMLSchema#dateTime>'
            addEvent("AlarmRemoveEvent",xsddt)
            del alarms[alarmID]
        



class eventHandler:	
    def handle(self, added, removed):
        """Callback function for subcribe_rdf method"""
        global node, currentTitle, alarmSong
                
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
                        if alarm == i:
                            print "Alarm already set."
                            node.CloseQueryTransaction(qs)
                            node.CloseQueryTransaction(qs2)
                            return
                    
                    dow = alarm.isoweekday()
                    midnight = alarm.replace(hour=0, minute=0, second=0, microsecond=0)
                    sinceMidnight = alarm - midnight
                    secondsStr = str(sinceMidnight.seconds)
                    alarmID = sq.set_alarm_add(dow, secondsStr, 0, url=alarmSong)
                    alarms[alarmID] = alarm
                    print "alarm id: " + alarmID
                    sq.display("Alarm set: ",alarm.strftime("%a %H:%M"),duration=15)

                if(eventType == ie_ns + "AlarmRemoveEvent"):
                    alarm = iso8601.parse_date(dataValue)
                    
                    for i in alarms.iterkeys():
                        if alarms[i] == alarm:
                            print "Found alarm. Removing.."
                            sq.delete_alarm(i)
                
                if(eventType == ie_ns + "AlarmEndEvent"):
                    print "Stopping alarm.."
                    sq.stop_alarm()
                
                if(eventType == ie_ns +"TimeSetEvent"): #SystemEvent
                    print "Setting time (sudo required).."
                    stime = iso8601.parse_date(dataValue) 
                    command = "date " + stime.strftime("%m%d%H%M%Y.%S")
                    os.system(command)
                    
                if(eventType == ie_ns + "PlayEvent"):
                    
                    tokens = dataValue.split("/")
                    title = tokens[2]
                    
                    if(title == currentTitle):
                        print "Continuing.."
                        sq.play()
                        return
                    
                    sq.playlist_load_tracks_title("'" + title +"'")
                    print "Playing song.." + title
                    currentTitle = title

                
                if(eventType == ie_ns + "PauseEvent"):
                    print "Pausing song."
                    sq.pause()
                    
                if(eventType == ie_ns + "IndicatorEvent"): 
                    print "Showing indicator."
                    indicatorSong = sq.get_track_path()
                    # configure file to play for an indicator event (with full path)
                    #sq.playlist_play("file:///home/username/Music/indicator.mp3")
                    sq.playlist_play("")
                    sq.play()
                    #TODO: Wait and restore title
                
                if(eventType == ie_ns + "PreviewEvent"):
                    print "Showing preview.."
                    
                    if(dataValue == ie_ns + "Alarm"):
                        print "Alarm"
                        sq.display("Alarm will be set.")
                        sq.playlist_play(alarmSong)
                        sq.play()
                        time.sleep(10)
                        sq.stop()
                    
                    if(dataValue == ie_ns + "Music"):
                        print "Music"
                        sq.display("Music will be played.")
                        sq.playlist_play(previewSong)
                        sq.play()
                        time.sleep(10)
                        sq.stop()
                    
                    #this is generated to let the connector knoww the temporary connection can be removed
                    addEvent("PreviewEvent") 
                    
                    

                    
            node.CloseQueryTransaction(qs)
            node.CloseQueryTransaction(qs2)


def subscribeToSourceEvents(source):
    global node
            
#    print "Checking for outgoing connections.."
#    qs = node.CreateQueryTransaction(smartSpace)
#    result = qs.rdf_query([((ie_ns+deviceID, sc_ns+"connectedTo", None),"uri")])
#    
#    for item in result:
#        sink = item[0][0]
#        print "Is source connected to sink?"
#        qs = node.CreateQueryTransaction(smartSpace)
#        result = qs.rdf_query([((source, sc_ns+"connectedTo", sink),"uri")])

#        if(result != None):

    print "Subscribing to source events: " + source # TODO: Use qnames
    rs1 = node.CreateSubscribeTransaction(smartSpace)
    result_rdf1 = rs1.subscribe_rdf([((None, ie_ns+"generatedBy", source), 'uri')], eventHandler())
    subscriptions[source] = rs1


class newConnectionHandler:
    def handle(self, added, removed):
        global node
    
        for i in added:
           subscribeToSourceEvents(i[0][0])
           
        for i in removed:
            source = i[0][0]
            print "Disconnected from " + source 
            sq.pause()
            
            print "Unsubscribing from events.."
            node.CloseSubscribeTransaction(subscriptions[source])
            del subscriptions[source]
            
def registerDevice():
    t = RDFTransactionList()
    u1 = ie_ns + deviceID
    
    t.setType(u1,sofia_ns + "SmartObject")
    t.add_uri(u1,ie_ns + "functionalitySource",ie_ns + "Alarm")
    t.add_uri(u1,ie_ns + "functionalitySink",ie_ns + "Alarm")
    t.add_uri(u1,ie_ns + "functionalitySink",ie_ns + "Music")
    
    u2 = ns + "id" + str(uuid.uuid4())
    t.add_uri(u1, sofia_ns + "hasIdentification",u2)
    t.add_uri(u2, semint_ns + "ofIDType", semint_ns + "RFID_Mifare")
    t.add_literal(u2, semint_ns + "idValue", "\"" + RFID + "\"^^<http://www.w3.org/2001/XMLSchema#string>")
    #infers hasRFIDTag
            
    ts.insert(t.get())
    print "Device registered."


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
	
	
def getPlayer():
    global sc, sq
    
    sq = sc.get_player("00:04:20:2A:B0:A5")

    print "Name: %s | Mode: %s | Time: %s | Connected: %s | WiFi: %s" % (sq.get_name(), sq.get_mode(), sq.get_time_elapsed(), sq.is_connected, sq.get_wifi_signal_strength())

    print sq.get_track_title()
    print sq.get_time_remaining()
    raw_input()
        
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
        
        print "Checking for existing connections.."
        qs = node.CreateQueryTransaction(smartSpace)
        result = qs.rdf_query([((None, sc_ns+"connectedTo", ie_ns+deviceID),"uri")])
        for source in result:
            subscribeToSourceEvents(source[0][0])          
    
        print "Subscribing to new connections..."
        rs1 = node.CreateSubscribeTransaction(smartSpace)
        result_rdf1 = rs1.subscribe_rdf([((None, sc_ns+"connectedTo", ie_ns+deviceID), 'uri')], newConnectionHandler())
        
        print "Subscribing to SystemEvents..."
        rs2 = node.CreateSubscribeTransaction(smartSpace)
        result_rdf2 = rs2.subscribe_rdf([((None, "rdf:type", ie_ns+"SystemEvent"), 'uri')], eventHandler())
        
    
                 
#        print "Subscribing to AlarmSetEvents.."
#        rs1 = node.CreateSubscribeTransaction(smartSpace)
#        result_rdf1 = rs1.subscribe_rdf([((None, None, ie_ns+"AlarmSetEvent"), 'uri')], RdfMsgHandler())
#        rs2 = node.CreateSubscribeTransaction(smartSpace)
#        result_rdf2 = rs2.subscribe_rdf([((None, None, ie_ns+"TimeSetEvent"), 'uri')], RdfMsgHandler())

    sc = Server(hostname="127.0.0.1", port=9090, username="user", password="password")
    sc.connect()
    sc.subscribe(SBMsgHandler(),"00:04:20:2A:B0:A5")
    
    print "Logged in: %s" % sc.logged_in
    print "Version: %s" % sc.get_version()

    print "Ctrl+C to exit"

    try:
	    while 42:
	        getPlayer()
            raw_input()
                
    except KeyboardInterrupt: #So use Ctrl+C to exit event loop
        
        print "Unsubscribing RDF subscriptions"
        node.CloseSubscribeTransaction(rs1)
        node.CloseSubscribeTransaction(rs2)
        
        
        sc.unsubscribe()

        print "Leaving smart space.."
        if connected:
            addEvent("DisconnectEvent")
            node.leave(smartSpace)
            print("Exiting..")
            os._exit(1) #this is required because excepting a KeyboardInterrupt makes it difficult to exit the program


if __name__ == "__main__":
    main()
    

