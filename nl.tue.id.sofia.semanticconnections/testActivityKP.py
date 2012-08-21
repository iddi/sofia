#!/usr/bin/env python
#-*- coding:utf-8 -*-

'''
Created on 11 October 2010

@author: Gerrit Niezen (g.niezen@tue.nl)
'''

'''
This must be added in ssls:

add_ns sc https://raw.github.com/iddi/sofia/master/nl.tue.id.sofia.ontologies/src/SemanticConnections.owl

rule managed_cap sc:a_e0cfb0c7-55c2-4d67-a4dc-5cd7e633101d.
ins sc:a_e0cfb0c7-55c2-4d67-a4dc-5cd7e633101d,wp1:managed,"1"
rule max_users sc:a_e0cfb0c7-55c2-4d67-a4dc-5cd7e633101d,1.
rule def_max_users sc:a_e0cfb0c7-55c2-4d67-a4dc-5cd7e633101d.
'''

from TripleStore import *
import uuid, os, sys
from RDFTransactionList import * #helper functions for working with triples

#Namespaces
dcs = "http://ontology.ist-spice.org/mobile-ontology/1/0/dcs/0/dcs.owl#"
core = "http://ontology.ist-spice.org/mobile-ontology/1/0/core/0/core.owl#"
wp1 = "http://sofia.org/wp1#"
wp1p = "http://sofia.org/personal#"

#Subscriptions
rs1 = node.CreateSubscribeTransaction(smartSpace)

def registerDevice(personID):
    t = RDFTransactionList()
    u1 = ns + "ss_52998c1d-3f40-49b1-81d3-a64dfffacc88" 
    #t.setType(u1, ns + "SmartObject") #should be made equivalent to core:Device
    t.setType(u1, core + "Device")
    
    #Register new capabilities, uuids hard-coded for now, as we still manually insert rules in ssls
    capability = ns + "a_e0cfb0c7-55c2-4d67-a4dc-5cd7e633101d"
    t.add_uri(capability, 'rdf:type', dcs + 'AcousticOutputModalityCapability')
    #These next ones should be inferred by a reasoner! >:-(
    t.add_uri(capability, 'rdf:type', dcs + 'Capability') 
    t.add_uri(capability, 'rdf:type', dcs + 'AcousticModalityCapability')
    t.add_uri(capability, 'rdf:type', dcs + 'ModalityCapability')  
    t.add_uri(capability, wp1 + "has_policy","pol_d138fcbd-d307-4d86-8d98-558c5d911870")
    
    t.add_literal(u1, ns + "hasRFIDTag","0401C4D9A12581")
    t.add_uri(u1, dcs + "hasDeviceCapability", capability) #this is actually inconsistent with the DCS ontology, which uses hasModalityCapability to link Devices to ModalityCapabilities
    
    ts.insert(t.get())

     #t.add_uri(personID, "wp1p:owner",u1) # device -owner-> Person -prefers-> Collection -set_item-> Preference
    print "Person ID: ", personID
    triple = [((personID, wp1p + 'owner', u1), 'uri', 'uri')]
    pro = node.CreateInsertTransaction(smartSpace)
    pro.send(triple, confirm = True)
    node.CloseInsertTransaction(pro)
    
def registerActivity():
    global rs1, activity
    
    t = RDFTransactionList()
    u1 = ns + "activity_" + str(uuid.uuid4())
    print "Activity: ", u1
    t.add_uri(u1,"rdf:type", wp1 + "Activity");
    t.add_literal(u1, wp1 + "active", 'request')
    t.add_literal(u1, wp1 + "importance", 'recreation') #'recreation' is a Preference specific to a person - shouldn't this be an individual?
    t.add_uri(u1, wp1 + "requires", dcs + 'AcousticOutputModalityCapability')
    
    #uses capability of above device. here we could also search for avaiable capabilities
    t.add_uri(u1, wp1 + "uses", ns + "a_e0cfb0c7-55c2-4d67-a4dc-5cd7e633101d")
    
    #To test, use this existing AcousticOutputModalityCapability
    #t.add_uri(u1, wp1 + "uses", "a_2cdb1dae-0c0a-4fc4-a881-1993c3e2b49d")  
    
    ts.insert(t.get())
    
    #subscribe to changes in active, should change to "yes"
    result_rdf = rs1.subscribe_rdf([((u1, wp1+"active", None), 'literal')], RdfMsgHandler())

    return u1


class RdfMsgHandler:	
    def handle(self, added, removed):
    	"""Callback function for subcribe_rdf method"""
        print "Subscription:"
        for i in added:
            print "Added:", str(i)
            activityID = str(i[0][0])
            print "Activity: " + activityID
            
            #qs = node.CreateQueryTransaction(smartSpace)
            #result = qs.rdf_query([((activityID, ns+"active", None),"literal")])
            #for item in result:
        	#	print "QUERY: Got triple(s): ", item
        	#node.CloseQueryTransaction(qs)   


#Connect to smart space
try:
    ts = TripleStore()
except:
    print "Cannot connect to smart space."

#Get random (first) Person ID to assign device to    
qs = node.CreateQueryTransaction(smartSpace)
result = qs.rdf_query([((None,"rdf:type",wp1p+"Person"),"uri")])
personID = result[0][0][0]

registerDevice(personID)
activity = registerActivity()

print "Ctrl+C to exit"
try:
    while 42:
        a = raw_input()          

except KeyboardInterrupt: #So use Ctrl+C to exit event loop
    #cleanup
    node.CloseSubscribeTransaction(rs1)
    
    #Remove activity (This should rather be handled by smart space? Activities only valid when connected.)
    triple = [((activity, None, None), 'uri', 'uri')]
    ds = node.CreateRemoveTransaction(smartSpace)
    ds.remove(triple, confirm = True)
    node.CloseRemoveTransaction(ds)  
    
    print "Leaving smart space.."
    node.leave(smartSpace)    
