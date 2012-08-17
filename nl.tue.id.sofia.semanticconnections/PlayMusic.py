#!/usr/bin/env python
#-*- coding:utf-8 -*-

'''
Created on 13 okt 2009

@author: Gerrit Niezen (g.niezen@tue.nl)

Notes:
audio module also provides set_volume(), current_volume(), set_position(us), current_position(), stop()

'''

from TripleStore import *
import uuid
from datetime import datetime
from RDFTransactionList import * #helper functions for working with triples

try:
    import audio, appuifw, e32, graphics, key_codes, math
    
    sound = audio.Sound.open("E:\\groove.mp3")
    
except ImportError:
    print "Not running on Symbian S60."


playing = False
cueAt = 0

class RdfMsgHandler:	
    def handle(self, added, removed):
        """Callback function for subcribe_rdf method"""
        global playing, node, cueAt
                
        print "Subscription:"
        for i in added:
            #New event occurred
            eventID = i[0][0]
            eventType = i[0][2]
            print "Event: " + eventID
            print "Type:" + eventType

            #Determine device that generated event
            qs = node.CreateQueryTransaction(smartSpace)
            result = qs.rdf_query([((eventID, ns+"generatedBy", None),"uri")])
            for item in result:
                device = item[0][2]
                print "Device ID: " + device

                #Determine if that device is connected to me
                qs2 = node.CreateQueryTransaction(smartSpace)
                result2 = qs2.rdf_query([((device, ns+"connectedTo", None),"uri")])
                print device + " is connected to: "
                for item in result2:
                    connectedDevice = item[0][2]
                    print connectedDevice + ", "
                    if connectedDevice == ns + d.deviceID:
                        print "Connected to me!"
                                                                                
                        if(eventType == ns+"PlayEvent"):
                            playing = True
                            
                        if(eventType == ns+"StopEvent"):
                            playing = False
                        
                        if(eventType == ns+"CueEvent"):
                            qs3 = node.CreateQueryTransaction(smartSpace)
                            print "CUE EVENT: ", eventID
                            result3 = qs3.rdf_query([((eventID, ns+"cueAt", None),"literal")])
                            for item in result3:
                                cueAt = int(item[0][2].lstrip(ns))
                                print "Set to cue at: " + str(cueAt)
                                
                            node.CloseQueryTransaction(qs3)	
                            
                node.CloseQueryTransaction(qs2)
                
            node.CloseQueryTransaction(qs)

class PlayMusic():
	
    def __init__(self):
        
        self.connected = False
        self.deviceID = ""
        self.running = False
               
        #Create menu and wait for user input
        appuifw.app.screen='normal'
        appuifw.app.menu=[(u'Play',self.playSong),(u'Stop',self.stopSong),(u'Forward',self.forwardSong),(u'Exit',self.exit_handler)]
        
        appuifw.app.directional_pad = False    
        appuifw.app.exit_key_handler = self.exit_handler 
  	
        if appuifw.touch_enabled():
            self.deviceID = "nokiaXM"
        else:
            self.deviceID = "nokiaN95"
	
        self.running = True
        
        
        
    def play_callback(self, prev_state, current_state, err ):
        """When the song is finished, a **StopEvent** is added to the smart space."""
        if current_state == audio.EOpen:
            if self.connected:
                self.stopSong()
                
        
    def playSong(self, event=None):
        global playing
        
        """Starts song and adds **PlayEvent** to smart space."""
        sound.play(callback = self.play_callback)
        playing = True
        print(u"Playing..")
        if self.connected:
            self.addEvent("PlayEvent")
            
    def stopSong(self, event=None):
        """Stops song and adds **StopEvent** to smart space."""
        global playing
        
        sound.stop()
        playing = False
        print(u"Stopped..")
        if self.connected:
            self.addEvent("StopEvent")
            
    def forwardSong(self, event=None):
        """Forwards song 5s and adds **CueEvent** to smart space."""
        global playing
               
        sound.set_position(5000000)
        print(u"Forwarding..")
        
        if self.connected:
        	eventID = self.addEvent("CueEvent")
        	t = RDFTransactionList()
        	t.add_literal(eventID, ns + "cueAt", "5000")
        	self.ts.insert(t.get())
        
    
    def exit_handler(self):
        """On exit, the app adds **DisconnectEvent** to smart space. and leaves smart space"""
        self.running = False  
        
        print "Unsubscribing RDF subscriptions"
        node.CloseSubscribeTransaction(self.rs1)
        node.CloseSubscribeTransaction(self.rs2)
        node.CloseSubscribeTransaction(self.rs3)
        
        if self.connected:
            self.addEvent("DisconnectEvent")
            node.leave(smartSpace)
        print "Exiting.."
        
    def addEvent(self, eventType):
    	"""Adds new event with metadata (**generatedBy**, **inXSDDateTime**) to smart space"""
        t = RDFTransactionList()
        u1 = ns + "event" + str(uuid.uuid4())
        t.setType(u1, ns + eventType)
        t.add_literal(u1, ns + "generatedBy", ns + self.deviceID)
        dt = datetime.now()
        xsddt = '"'+dt.strftime('%Y-%m-%dT%H:%M:%S%z')  + '"^^<http://www.w3.org/2001/XMLSchema#dateTime>'
        t.add_literal(u1, ns + "inXSDDateTime", xsddt)
        self.ts.insert(t.get())
        return u1
	
    def run(self):
        """Joins smart space, adds subscriptions and enters main event loop"""
        global playing, cueAt
        
        print(u"Joining smart space..")
        
        self.ts = TripleStore()
           
        self.connected = True
        self.addEvent("ConnectEvent")
        
        if self.connected:
        
            #Add subscriptions for play, stop and forward
            print "Subscribing to PlayEvents.."
            self.rs1 = node.CreateSubscribeTransaction(smartSpace)
            result_rdf1 = self.rs1.subscribe_rdf([((None, None, ns+"PlayEvent"), 'uri')], RdfMsgHandler())
            print "Subscribing to StopEvents.."
            self.rs2 = node.CreateSubscribeTransaction(smartSpace)
            result_rdf2 = self.rs2.subscribe_rdf([((None, None, ns+"StopEvent"), 'uri')], RdfMsgHandler())
            print "Subscribing to CueEvents.."
            self.rs3 = node.CreateSubscribeTransaction(smartSpace)
            result_rdf3 = self.rs3.subscribe_rdf([((None, None, ns+"CueEvent"), 'uri')], RdfMsgHandler())
           
        
        #Event loop
        while self.running:
            e32.ao_sleep(0.1)
            
            if playing:
                if sound.state() != audio.EPlaying:
                    sound.play(callback = self.play_callback)
                
                if cueAt > 0 :
                    print "Cue to " + str(cueAt)
                    sound.set_position(cueAt * 1000)
                    cueAt = 0
                    
            else:
                sound.stop()
            
            
        
        print "End."
          

if __name__ == '__main__':
    d = PlayMusic()
    d.run()




