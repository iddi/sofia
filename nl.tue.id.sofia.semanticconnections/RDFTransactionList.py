class RDFTransactionList:
	def __init__(self):
		self.rdftranslist = []

	def add_uri( self, s, p, o, bnode = False ):
		s1=str(s)	
		p1=str(p)
		o1=str(o)
		if bnode:
			self.rdftranslist.append(((s1, p1, o1), "bnode", "uri"))
		else:
			self.rdftranslist.append(((s1, p1, o1), "uri", "uri"))

	def add_literal( self, s, p, o, bnode = False):
		s1=str(s)	
		p1=str(p)
		o1=str(o)	
		if bnode:
			self.rdftranslist.append(((s1,p1,o1), "bnode", "literal"))
		else:
			self.rdftranslist.append(((s1, p1, o1), "uri", "literal"))
				
	
	def setType(self,s,t):
		t1=str(t)
		s1=str(s)
		self.add_uri(s1, "rdf:type", t1)
		self.add_uri(t1, "rdf:type", "rdfs:Class")

	def add_Class(self,c):
		c1=str(c)
		self.add_uri(c1, "rdf:type", "rdfs:Class")

	def add_subClass(self,c,p):
		c1=str(c)
		p1=str(p)
		self.add_uri(c1, "rdfs:subClassOf", p1)
		self.add_uri(c1, "rdf:type", "rdfs:Class")
		self.add_uri(p1, "rdf:type", "rdfs:Class")

	def unify(self,s,t):
		u1=str(s)
		u2=str(t)
		self.add_uri(u1,"owl:sameAs",u2)

	def get(self):
		return self.rdftranslist
