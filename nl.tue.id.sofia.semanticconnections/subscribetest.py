#!/usr/bin/env python
#-*- coding:utf-8 -*-

from smart_m3 import Node
from smart_m3.Node import TCPConnector

ns = "https://raw.github.com/iddi/sofia/master/nl.tue.id.sofia.ontologies/src/SemanticConnections.owl#"
ie_ns = "https://raw.github.com/iddi/sofia/master/nl.tue.id.sofia.ontologies/src/InteractionEvents.owl#"

#join smart space
node = Node.ParticipantNode("SIB Subscribe Tester")
smartSpace = ('test', (TCPConnector, ('192.168.1.4', 23000)))
if not node.join(smartSpace): 
    sys.exit('Could not join to Smart Space')
print "--- Member of SS:", node.member_of


class RdfMsgHandler:	
    def handle(self, added, removed):
    	"""Callback function for subcribe_rdf method"""
        print "Subscription:"
        for i in added:
            print "Added:", str(i)
            eventID = str(i[0][0])
            print "Event: " + eventID
            qs = node.CreateQueryTransaction(smartSpace)
            result = qs.rdf_query([((eventID, ns+"generatedBy", None),"literal")])
            for item in result:
		print "QUERY: Got triple(s): ", item
		deviceID = item[0][2]
		print "Device ID: " + deviceID
		qs2 = node.CreateQueryTransaction(smartSpace)
		result = qs2.rdf_query([((deviceID, ns+"connectedTo", None),"literal")])
		print deviceID + " is connected to: "
		for item in result:
			connectedDevice = item[0][2]
			print connectedDevice + ", "
			if connectedDevice == ns + "nokiaXpressMusic5800":
				print "Connected to me!"
		node.CloseQueryTransaction(qs)
	    	node.CloseQueryTransaction(qs2)
	    	
            
        for i in removed:
            print "Removed:", str(i)



#subscribe
rs1 = node.CreateSubscribeTransaction(smartSpace)
rs2 = node.CreateSubscribeTransaction(smartSpace)
rs3 = node.CreateSubscribeTransaction(smartSpace)
rs4 = node.CreateSubscribeTransaction(smartSpace)
result_rdf = rs1.subscribe_rdf([((None, None, ie_ns+"AdjustLevelEvent"), 'uri')], RdfMsgHandler())
result_rdf = rs2.subscribe_rdf([((None, None, ns+"StopEvent"), 'uri')], RdfMsgHandler())
result_rdf = rs3.subscribe_rdf([((None, None, ns+"CueEvent"), 'uri')], RdfMsgHandler())
result_rdf = rs4.subscribe_rdf([((None, ns+"cueAt", None), 'literal')], RdfMsgHandler())

#wait
print "Press enter to continue"
raw_input()

#cleanup
print "Unsubscribing RDF subscriptions"
node.CloseSubscribeTransaction(rs1)
node.CloseSubscribeTransaction(rs2)
node.CloseSubscribeTransaction(rs3)
node.CloseSubscribeTransaction(rs4)
print "Leaving smart space"
node.leave(smartSpace)
