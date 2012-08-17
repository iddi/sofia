#!/usr/bin/env python
#-*- coding:utf-8 -*-

from TripleStore import *
import uuid
from datetime import datetime
from RDFTransactionList import * #helper functions for working with triples

class RdfMsgHandler:	
    def handle(self, added, removed):
        print "ok"


ts = TripleStore()

#insert something into smart space
t = RDFTransactionList()

'''
u1 = ns + "event" + str(uuid.uuid4())
#t.setType(u1, ns + "PlayEvent")
t.setType(u1, ns + "CueEvent")
t.add_literal(u1, ns + "generatedBy", ns+"nokiaN95")
t.add_literal(u1, ns + "cueAt", '"5000"^^<http://www.w3.org/2001/XMLSchema#integer>')
dt = datetime.now()
xsddt = '"'+dt.strftime('%Y-%m-%dT%H:%M:%S%z')  + '"^^<http://www.w3.org/2001/XMLSchema#dateTime>'
t.add_literal(u1, ns + "inXSDDateTime", xsddt)
ts.insert(t.get())
'''
prop = ns+"connectedTo"
#prop = ns+"is"
update_ins = []
t.add_uri(ns+"woop",prop,ns+"func")
t.add_uri(ns+"func",prop,ns+"woop")
'''
upd = node.CreateUpdateTransaction(smartSpace)
upd.update(update_ins, "RDF-M3", t.get(), "RDF-M3", confirm = True)
node.CloseUpdateTransaction(upd)
'''

'''
triple = [((None, ns+'xsd', None), 'uri', 'literal')]
ds = node.CreateRemoveTransaction(smartSpace)
ds.remove(triple, confirm = True)
node.CloseRemoveTransaction(ds)     
'''


semint="https://raw.github.com/iddi/nl.tue.id.sofia/master/nl.tue.id.sofia.ontologies/src/SemanticInteraction.owl#"
qs = node.CreateQueryTransaction(smartSpace)
#result = qs.rdf_query([((None, ns + "inXSDDateTime", None ),"literal")])
#result = qs.rdf_query([((None, ns + "inXSDDateTime", None ),"literal")])
result = qs.rdf_query([((None, prop, ns + "nokiaXM" ),"uri")])
for item in result:
    print item 

node.leave(smartSpace)


