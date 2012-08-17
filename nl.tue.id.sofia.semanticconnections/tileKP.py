#!/usr/bin/env python
#-*- coding:utf-8 -*-

'''
Created on 11 June 2010

@author: Gerrit Niezen (g.niezen@tue.nl)
'''

from TripleStore import *
import uuid, os, sys
from datetime import datetime
from RDFTransactionList import * #helper functions for working with triples
import serial
sys.path.append('./lib/')
import RFIDIOtconfig


def addEvent(eventType, tagID, position):
    """Adds new event of *eventType* with metadata (**inXSDDateTime**, **hasRFIDTag** and **hasPosition**) to smart space"""
    t = RDFTransactionList()
    u1 = ns + "event" + str(uuid.uuid4())
    t.setType(u1, ns + eventType)
    #t.add_literal(u1, ns + "generatedBy", ns + self.deviceID)
    dt = datetime.now()
    xsddt = '"'+dt.strftime('%Y-%m-%dT%H:%M:%S%z')  + '"^^<http://www.w3.org/2001/XMLSchema#dateTime>'
    t.add_literal(u1, ns + "inXSDDateTime", xsddt)
    t.add_literal(u1, ns + "hasRFIDTag", "\"" + tagID + "\"^^<http://www.w3.org/2001/XMLSchema#string>");
    t.add_literal(u1, ns + "hasPosition", "\"" + str(position) + "\"^^<http://www.w3.org/2001/XMLSchema#integer>")
    print eventType, ": Object ", tagID, " next to position ", position
    ts.insert(t.get())
    return u1
        
def sendToTile(connected, pos):
    """Sends position and connection possibility to tile (``c`` if a connection exists, ``p`` if one is possible, 
    and ``n`` if no connection is possible."""
    global ser
    
    print "Sending to tile: (", connected, ", ", pos, ")"
    ser.write(connected)
    ser.write(chr(pos))    
    
    
def showConnections():
    """
    Determines which devices are currently next to the interaction tile by querying the smart space, 
    and an internal array storing the positions relative to the tile. 

    Sends a response to the interaction tile (using ``sendToTile()``
    on whether a connection exists or whether a connection is possible.)
    """
    global rfid
    
    #Count number of devices
    devices = 4 - rfid.count("")    
    print "Number of devices: ", devices
    
    for item in rfid:
        if item != "":
        
            if devices < 2:
                #If there is only one (or zero) devices, show that no connections are possible
                sendToTile('n',rfid.index(item))
                
            #If the device isn't connected to anything, it won't be found and set in the search below, 
            #so at the end it should be set to "Possible"
            alreadySet = False
            
            device = getNameFromTag(item)
                    
            qs = node.CreateQueryTransaction(smartSpace)
            result = qs.rdf_query([((device, ns + "connectedTo", None ),"uri")])
            for connDevice in result:
                print device.split("#")[1], " is connected to ", connDevice[0][2].split("#")[1]
                
                #Is connDevice next to tile?
                for connItem in rfid:
                    connName = getNameFromTag(connItem)
                            
                    if(connName == connDevice[0][2]):
                        #Yes, it's next to the tile, show as connected
                        sendToTile('c',rfid.index(item))
                        sendToTile('c',rfid.index(connItem))
                        alreadySet = True
            
            if (alreadySet == False) and (devices > 1):
                sendToTile('p',rfid.index(item))
                
            node.CloseQueryTransaction(qs)
                

def getNameFromTag(tag):
    """Retrieves device name from smart space based on RFID *tag*."""
    qs = node.CreateQueryTransaction(smartSpace)
    result = qs.rdf_query([((None, ns+"hasRFIDTag", tag),"literal")])
    for name in result:
        if str(name[0]).find("event") == -1: #Search for the device name
            node.CloseQueryTransaction(qs)
            return name[0][0]
            
    node.CloseQueryTransaction(qs)
    return "Not found"
    
def exitObject(position):
    """Adds an **NFCExitEvent** at *position* when an object leaves the tile.""" 
    print "Exit: Object at position ", position
    if(rfid[position] != ""):
        addEvent("NFCExitEvent", rfid[position], position)
        sendToTile('n',position)
        rfid[position] = ""
        showConnections()

            
def connect(device1,device2, make):
    """Adds or removes a **connectedTo** relationship between *device1* and *device2* based on whether *make* is ``True`` or ``False``"""   
    if make:
        print "Connecting " + device1.split("#")[1] + " to " + device2.split("#")[1]
        pro = node.CreateInsertTransaction(smartSpace)
        triple = [((device1, ns+'connectedTo', device2), 'uri', 'uri')]
        pro.send(triple, confirm = True)
        node.CloseInsertTransaction(pro)
    else:
        print "Disconnecting " + device1.split("#")[1] + " from " + device2.split("#")[1]  
        t = RDFTransactionList()
        t.add_uri(device1,ns+'connectedTo', device2)
        t.add_uri(device2,ns+'connectedTo',device1)  
        ds = node.CreateRemoveTransaction(smartSpace)
        ds.remove(t.get(), confirm = True)
        node.CloseRemoveTransaction(ds)               
                
def connectDevices(make):
    """
    Connects or disconnects devices currently next to the interaction tile, based on the contents of the *rfid* array
    @param make If True connect, otherwise disconnect
    """
    global rfid
    
    for item in rfid:
        if item != "":
    
            #Get first device name    
            device1 = getNameFromTag(item)
                    
            #Get second device name, and make sure it's not the same device
            for item2 in rfid:
                if (item2 != "") and (item != item2):
                    device2 = getNameFromTag(item2)
                    connect(device1,device2, make)
                    
                    #Get 
                    
    showConnections()


#MAIN START
    
#Connects to Arduino inside interaction tile
marker = 'S'
cardEvent = 'E'
ser = serial.Serial('/dev/ttyUSB0',115200)

#Connects to smart space
try:
    ts = TripleStore()
except:
    print "Cannot connect to smart space."    

position = -1
rfid = [""] * 4
possibleExit = -1

try:
    #Connects to RFID reader inside interaction tile
    card= RFIDIOtconfig.card
    card.info('rfidReader v0.1')
    card.settagtype(card.ALL)
except (NameError, AttributeError):
    print "No RFID available (Sphinx compilation?)"    
except:
    print "Unknown RFID error"
    os._exit(True)

print "Ctrl+C to exit"

try:
    while 42:
                
        if card.select():
            print '    Tag ID: ' + card.uid,
            print
            #sys.stdout.flush()
                          
            card.acs_halt()
            if 9 <= position <= 13: #10-13 are enter positions, just subtract 10
            
                addEvent("NFCEnterEvent", card.uid, position-10)
                rfid[position-10] = card.uid #save tag of object next to position
                showConnections()
        
        
        k = ser.read(2)
        position = ord(k[0])
        print "Position: ", position
       
       
        if position < 4: # 0-3 from tile is exit position
            exitObject(position)
                    
        if position == 67:
            print "Connect"
            connectDevices(True)
            
        if position == 68:
            print "Disconnect"
            connectDevices(False)
            
            
except KeyboardInterrupt: #So use Ctrl+C to exit event loop
    print "Leaving smart space.."
    node.leave(smartSpace)
    
#except (NameError, AttributeError):
#    print "No RFID library available."
    

