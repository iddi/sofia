==========
How do I..
==========

add or remove semantic connections in the smart space?
=====================================================================

The latest version of the ontology used is available at:

`https://raw.github.com/iddi/sofia/master/nl.tue.id.sofia.ontologies/src/SemanticConnections.owl <https://raw.github.com/iddi/sofia/master/nl.tue.id.sofia.ontologies/src/SemanticConnections.owl>`_.

The ontology is best viewed using `Protegé 4 <http://protege.stanford.edu/download/download.html>`_.

**SmartObjects** currently defined as individuals are **ambientLighting**, **nokiaN900**, **nokiaXM** and **surroundSoundSystem**. Each **SmartObject** has a **hasRFIDTag** property associated with it, which is linked to the RFID tag of the proxy cube representing the smart object.

A **connectedTo** relationship can be added or removed between two existing **SmartObjects**. To determine whether a **connectedTo** relationship exists between two smart objects, you can do the following (in Python using the Smart-M3 libraries)::
    
    ns = "https://raw.github.com/iddi/sofia/master/nl.tue.id.sofia.ontologies/src/SemanticConnections.owl#"
    qs = node.CreateQueryTransaction(smartSpace)
    
    result = qs2.rdf_query([((device, ns+"connectedTo", None),"uri")])
    print device + " is connected to: "
        for item in result2:
            connectedDevice = item[0][2]
            
To add a new **connectedTo** relationship::

    triple = [((device1, ns+'connectedTo', device2), 'uri', 'uri')]
    print "Connecting " + device1.split("#")[1] + " to " + device2.split("#")[1]
    pro = node.CreateInsertTransaction(smartSpace)
    pro.send(triple, confirm = True)
    node.CloseInsertTransaction(pro)

To remove an existing **connectedTo** relationship::

    triple = [((device1, ns+'connectedTo', device2), 'uri', 'uri')]
    print "Disconnecting " + device1.split("#")[1] + " from " + device2.split("#")[1]    
    ds = node.CreateRemoveTransaction(smartSpace)
    ds.remove(triple, confirm = True)
    node.CloseRemoveTransaction(ds) 
            
