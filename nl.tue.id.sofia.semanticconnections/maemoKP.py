'''
Created on 15 June 2010

@author: Gerrit Niezen (g.niezen@tue.nl)

'''

import sys, time
from PyQt4 import QtGui,QtCore
from PyQt4.phonon import Phonon
from maemoKP_ui import *

from TripleStore import *
import uuid
from datetime import datetime
from RDFTransactionList import * #helper functions for working with triples

cueAt = 0

class MediaPlayer(QtGui.QMainWindow):
    def __init__(self, parent=None):
        
        #build parent user interface
        QtGui.QWidget.__init__(self, parent)
        self.ui = Ui_MainWindow()
        self.ui.setupUi(self)
        self.text = QtCore.QString()
        self.connected = False
        self.deviceID = "nokiaN900"

        self.m_media = Phonon.MediaObject(self)
        audioOutput = Phonon.AudioOutput(Phonon.MusicCategory, self)
        Phonon.createPath(self.m_media, audioOutput)
        self.song = QtCore.QString("/home/user/scripts/groove.mp3")
        self.m_media.setCurrentSource(Phonon.MediaSource(self.song))
        
        self.ui.cueButton.hide()
        self.ui.textBrowser.hide()
        
        #connect signals
        QtCore.QObject.connect(self.ui.playButton, QtCore.SIGNAL('clicked()'), self.playSong)
        QtCore.QObject.connect(self.ui.stopButton, QtCore.SIGNAL('clicked()'), self.stopSong)
        QtCore.QObject.connect(self.ui.cueButton, QtCore.SIGNAL('clicked()'), self.forwardSong)
        #QtCore.QObject.connect(self.m_media, QtCore.SIGNAL('stateChanged(Phonon::State, Phonon::State)'), self.stateChanged)
        QtCore.QObject.connect(self.m_media, QtCore.SIGNAL('finished()'), self.finished)
        QtCore.QObject.connect(app, QtCore.SIGNAL('aboutToQuit()'), self.exit_handler)
        
        
        self.note("Joining smart space..")
        self.ts = TripleStore()
           
        self.connected = True
        self.addEvent("ConnectEvent")
        
        if self.connected:
        
            #Add subscriptions for play, stop and forward
            self.note("Subscribing to PlayEvents..")
            self.rs1 = node.CreateSubscribeTransaction(smartSpace)
            result_rdf1 = self.rs1.subscribe_rdf([((None, None, ns+"PlayEvent"), 'uri')], RdfMsgHandler())
            self.note("Subscribing to StopEvents..")
            self.rs2 = node.CreateSubscribeTransaction(smartSpace)
            result_rdf2 = self.rs2.subscribe_rdf([((None, None, ns+"StopEvent"), 'uri')], RdfMsgHandler())
            self.note("Subscribing to CueEvents..")
            self.rs3 = node.CreateSubscribeTransaction(smartSpace)
            result_rdf3 = self.rs3.subscribe_rdf([((None, None, ns+"CueEvent"), 'uri')], RdfMsgHandler())
       
        
        
    #def stateChanged(self, newState, oldState):
    def finished(self):
        """When the song is finished, we need to send to handle it"""
        #if newState == Phonon.StoppedState and oldState != Phonon.StoppedState:
            #self.stopSong()
        print "Finished.. restarting"
        self.m_media.setCurrentSource(Phonon.MediaSource(self.song))
        self.m_media.play()
        
    def note(self, newText):
        self.text.append(QtCore.QString(newText + "\n"))
        self.ui.textBrowser.setPlainText(self.text)
        
    def playSong(self, event=None):
        """Starts song and sends notification"""
        self.m_media.play()
        self.note("Playing..")
        if self.connected:
            self.start_time = time.time()
            self.addEvent("PlayEvent")
            
    def stopSong(self, event=None):
        """Stops song and sends notification"""
        self.m_media.stop()
        playing = False
        self.note("Stopped..")
        if self.connected:
            self.start_time = time.time()
            self.addEvent("StopEvent")
            
    def forwardSong(self, event=None):
        """Forwards song 5s and sends notification"""
        global cueAt
               
        if self.connected and cueAt == 0: #If cueAt is 0, the button was pressed on this device
        	self.start_time = time.time()
        	eventID = self.addEvent("CueEvent")
        	t = RDFTransactionList()
        	t.add_literal(eventID, ns + "cueAt", '"5000"^^<http://www.w3.org/2001/XMLSchema#integer>')
        	self.ts.insert(t.get())

        self.m_media.seek(5000)
        self.note("Forwarding..")
        cueAt = 0
        	
    def exit_handler(self):
        """On exit, the app sends a notification and leaves smart space"""
        
        print "Unsubscribing RDF subscriptions"
        node.CloseSubscribeTransaction(self.rs1)
        node.CloseSubscribeTransaction(self.rs2)
        node.CloseSubscribeTransaction(self.rs3)
        
        if self.connected:
            self.addEvent("DisconnectEvent")
            node.leave(smartSpace)
        print "Exiting.."
        
    def addEvent(self, eventType):
    	"""Adds new event with metadata (generatedBy, datetime) to smart space"""
        t = RDFTransactionList()
        u1 = ns + "event" + str(uuid.uuid4())
        t.setType(u1, ns + eventType)
        t.add_uri(u1, ns + "generatedBy", ns + self.deviceID)
        dt = datetime.now()
        xsddt = '"'+dt.strftime('%Y-%m-%dT%H:%M:%S%z')  + '"^^<http://www.w3.org/2001/XMLSchema#dateTime>' # e.g. "2011-01-19T16:10:23"^^<http://www.w3.org/2001/XMLSchema#dateTime>
        t.add_literal(u1, ns + "inXSDDateTime", xsddt)
        self.ts.insert(t.get())
        return u1
        
        
        
class RdfMsgHandler:	
    def handle(self, added, removed):
        """Callback function for subcribe_rdf method"""
        global node, cueAt
                
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
                
                #If I generated the event myself, record the duration
                if device == ns + mediaPlayer.deviceID:
                        f = open('statsN900.txt', 'a')
                        duration = time.time() - mediaPlayer.start_time
                        print "Response time: %.3f" % duration
                        f.write("%.3f\n" % duration)
                        f.close()    

                #Determine if that device is connected to me
                qs2 = node.CreateQueryTransaction(smartSpace)
                result2 = qs2.rdf_query([((device, ns+"connectedTo", None),"uri")])
                print device + " is connected to: "
                for item in result2:
                    connectedDevice = item[0][2]
                    print connectedDevice + ", "
                    if connectedDevice == ns + mediaPlayer.deviceID:
                        print "Connected to me!"
                                                                                
                        if(eventType == ns+"PlayEvent"):
                            mediaPlayer.ui.playButton.click()
                            
                        if(eventType == ns+"StopEvent"):
                            mediaPlayer.ui.stopButton.click()
                        
                        if(eventType == ns+"CueEvent"):
                            qs3 = node.CreateQueryTransaction(smartSpace)
                            print "CUEVENT: ", eventID
                            result3 = qs3.rdf_query([((eventID, ns+"cueAt", None),"literal")])
                            for item in result3:
                                cueAt = int(item[0][2].lstrip(ns))
                                print "Set to cue at: " + str(cueAt)
                                mediaPlayer.ui.cueButton.click()
                                
                            node.CloseQueryTransaction(qs3)	
                            
                node.CloseQueryTransaction(qs2)
                
            node.CloseQueryTransaction(qs)
            

if __name__ == "__main__":  
    app = QtGui.QApplication(sys.argv)
    QtGui.QApplication.setApplicationName("MaemoKP")
    mediaPlayer = MediaPlayer()
    mediaPlayer.show()
    sys.exit(app.exec_())
