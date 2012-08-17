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
import time
from time import sleep
sys.path.append('./lib/')


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
        
def sendToConnector(connected):
    """Sends connection possibility to Connector (``c`` if a connection exists, ``p`` if one is possible, 
    and ``n`` if no connection is possible."""
    global ser
    
    print "Sending to Connector: (", connected, ")"
    ser.write(connected)
    

def getNameFromTag(tag):
	"""Retrieves device name from smart space based on RFID *tag*."""
	global notFound

	print "Retrieving name linked to tag.."

	start_time = time.time()

	qs = node.CreateQueryTransaction(smartSpace)
	result = qs.rdf_query([((None, ns+"hasRFIDTag", tag),"literal")])
	
	f = open('statsConnector.txt', 'a')
	duration = time.time() - start_time
	print "Query time: %.3f" % duration
	f.write("%.3f\n" % duration)
	f.close	

	for name in result:
		if str(name[0]).find("event") == -1: #Search for the device name
			node.CloseQueryTransaction(qs)
			return name[0][0]
	
	node.CloseQueryTransaction(qs)
	notFound = True
	return "Not found"

            
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


def areConnected(device1, device2):
	global notFound

	if(notFound):
		return 'N'

	connected = 'P'

	start_time = time.time()

	qs = node.CreateQueryTransaction(smartSpace)
	result = qs.rdf_query([((device1, ns + "connectedTo", None ),"uri")])
	print "returned"
	f = open('statsConnector.txt', 'a')
	duration = time.time() - start_time
	print "Query time: %.3f" % duration
	f.write("%.3f\n" % duration)
	f.close	


	for connDevice in result:
		print device1.split("#")[1], " is connected to ", connDevice[0][2].split("#")[1]
                
		if(device2 == connDevice[0][2]):
			#Connected!
			connected = 'C'
			break

	node.CloseQueryTransaction(qs)

	return connected
	

def readSerial():
		global ser, device1, device2

		first = ser.read(1)

		if ord(first) == 0x02:	#Start of tag
			k = ser.read(10)

			if(str(k) != "0000000000"):
				print "Tag:", k
				last = ser.read(1) #also read terminator
				return k
			else:
				last = ser.read(1) #also read terminator
				print "Exit"
				return "E"

		if first == 'C':
			print "Confirmed"
			a = ser.read(1)			
			return first + a

		if first == 'S':
			print "Success"

		if first == 'D':
			print "Disconnect"

		if first == 'R':
			print "Reset"
		
		return first
		


#MAIN START
    
#ser = serial.Serial('/dev/ttyUSB0',115200)
#ser = serial.Serial('/dev/rfcomm0',115200)
ser = serial.Serial('/dev/tty.FireFly-451A-SPP',115200)

#Connects to smart space
try:
    ts = TripleStore()
except:
    print "Cannot connect to smart space."    


print "Ctrl+C to exit"

ser.flushInput()


try:
	while 42:
		device1 = ""
		device2 = ""
		notFound = False

		tag = readSerial()

		if(len(tag) > 2):
			device1 = getNameFromTag(tag)
			print "Device 1: " + device1

			#Check for Confirmed, then wait for Exit
			tag2 = readSerial()

			if(tag2 == 'E'):
				print "Device 1 exited early."
				continue

			while(tag2 == 'C1'):
				print "Confirmed for device 1."
				tag2 = readSerial()
				
			if(tag2 == 'E'):
				print "Device 1 exited, waiting for device 2"
				tag2 = readSerial()

				if(len(tag2) > 2):				
					device2 = getNameFromTag(tag2)
					print "Device 2: " + device2

					#Determine whether they're connected
					connected = areConnected(device1,device2)
					print "Connected: ", connected
					
					#Check for Confirmed, then wait for Exit
					tag2 = readSerial()
					
					while(tag2 == 'E'):
						print "Device 2 exited early."

						tag2 = readSerial()

						if(len(tag2) > 2):				
							device2 = getNameFromTag(tag2)
							print "Device 2: " + device2

							#Determine whether they're connected
							connected = areConnected(device1,device2)
							print "Connected: ", connected
					
							#Check for Confirmed, then wait for Exit
							tag2 = readSerial()
					
					if(tag2 == 'C2'):
						sleep(0.5)
						sendToConnector(connected)
						tag2 = readSerial()

						while(tag2 == 'C2'):
							print "Still confirmed for device 2."
							tag2 = readSerial()

						while(tag2 == 'E' and notFound == False):
							print "Device 2 exited prematurely, still waiting.."
							tag2 = readSerial()

						if(tag2 == 'S'):
							if(device1 != "" and device2 != ""):
								connect(device1,device2,True)
							else:
								print "Unable to connect device 1 (" + device1 + ") to device 2 (" + device2 +")"

						if(tag2 == 'D'):
							if(device1 != "" and device2 != ""):
								connect(device1,device2,False)
							else:
								print "Unable to disconnect device 1 (" + device1 + ") from device 2 (" + device2 +")"
					
						if(tag2 == 'E'):
							print "Device 2 exited."

					
				
			else:
				print "Huh?"

            
except KeyboardInterrupt: #So use Ctrl+C to exit event loop
    print "Leaving smart space.."
    node.leave(smartSpace)
    

