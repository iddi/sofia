from smart_m3 import Node
from smart_m3.Node import TCPConnector
import uuid

ns = "https://raw.github.com/iddi/sofia/master/nl.tue.id.sofia.ontologies/src/SemanticConnections.owl#"
ie_ns = "https://raw.github.com/iddi/sofia/master/nl.tue.id.sofia.ontologies/src/InteractionEvents.owl#"
sc_ns = "https://raw.github.com/iddi/sofia/master/nl.tue.id.sofia.ontologies/src/SemanticConnections.owl#"
semint_ns = "https://raw.github.com/iddi/sofia/master/nl.tue.id.sofia.ontologies/src/SemanticInteraction.owl#"
sofia_ns = "http://www.sofia-project.eu/ontologies/core/2010/01/19/sofia.owl#"

# connection to SIB running on a different computer
smartSpace = ('marijaSIB', (TCPConnector, ('192.168.1.110', 23000)))

# connection to SIB running on same computer
#smartSpace = ('test', (TCPConnector, ('127.0.0.1', 23000)))
node = Node.ParticipantNode("node"+str(uuid.uuid4()))

class TripleStore:
	def __init__(self):
		#join smart space		
		if not node.join(smartSpace): 
		    print "Could not join to Smart Space"
		print "--- Member of SS:", node.member_of


	def insert(self, triple):
		"""Insert triple in the format [(("NFCExitEvent", "hasRFIDTag", "1234"), "uri", "literal")]"""
	
		pro = node.CreateInsertTransaction(smartSpace)
		pro.send(triple, confirm = True)
		node.CloseInsertTransaction(pro)
