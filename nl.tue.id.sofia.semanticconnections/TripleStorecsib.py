from smart_m3 import Node
from smart_m3.Node import TCPConnector
import uuid

ns = "https://raw.github.com/iddi/nl.tue.id.sofia/master/nl.tue.id.sofia.ontologies/src/SemanticConnections.owl#"
smartSpace = ('test', (TCPConnector, ('localhost', 10010)))
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
