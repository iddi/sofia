/*******************************************************************************
 * Copyright (c) 2009,2011 Tecnalia Research and Innovation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Raul Otaolea (Tecnalia Research and Innovation - Software Systems Engineering) - initial API, implementation and documentation
 *    Fran Ruiz (Tecnalia Research and Innovation - Software Systems Engineering) - initial API, implementation and documentation
 *******************************************************************************/ 

package eu.sofia.adk.sib.protocol.m3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * Represents a triple list defined in RDF-M3 format
 * 
 * @author Fran Ruiz, Fran.Ruiz@tecnalia.com, Tecnalia - Software Systems Engineering
 *
 */
public class RDFM3 {
	
	/**
	 * Enumerated ontology namespace prefixes
	 */
	private static enum OntologyNSPrefix {  
		OWL		("owl", "http://www.w3.org/2002/07/owl#"),
		RDF		("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#"),
		RDFS	("rdfs", "http://www.w3.org/2000/01/rdf-schema#"),
		XSD		("xsd", "http://www.w3.org/2001/XMLSchema#");
		
		private final String prefix;
		private final String namespace;
		
		OntologyNSPrefix(String prefix, String namespace) {
			this.prefix = prefix;
			this.namespace = namespace;
		}
		
		public String getPrefix() {
			return prefix;
		}
		
		public String getNamespace() {
			return namespace;
		}		
		
		public static OntologyNSPrefix findPrefix(String prefix) {
			for(OntologyNSPrefix ns : values()) {
				if(ns.getPrefix().equalsIgnoreCase(prefix)) {
					return ns;
				}
			}
			return null;
		}
		
		public static OntologyNSPrefix findNamespace(String url) {
			for(OntologyNSPrefix ns : values()) {
				if(url.contains(ns.getNamespace())) {
					return ns;
				}
			}
			return null;
		}		
	}
	
	/** Namespace map defined at triple_list level */
	private HashMap<String,String> namespaces;
	
	/** The list of RDF-M3 triples */
	private ArrayList<RDFM3Triple> triples;
	
	private static final String TRIPLELIST_OPENINGTAG = "<triple_list";
	private static final String TRIPLELIST_CLOSINGTAG = "</triple_list>";
	
	private static final String CR = "\n";
	private static final String NAMESPACE_PREFIX = "xmlns:";
	
	/**
	 * Constructor
	 */
	public RDFM3() {
		this.triples = new ArrayList<RDFM3Triple>();
		this.namespaces = new HashMap<String,String>();
	}
	
	/**
	 * Adds a prefix association to a namespace
	 * @param prefix the prefix
	 * @param namespace the namespace
	 * @return the previous value of the namespace associated to the prefix
	 */
	public String addNamespace(String prefix, String namespace) {
		return namespaces.put(prefix, namespace);
	}
	
	/**
	 * Adds the namespaces
	 * @param namespaces the hashmap with namespaces
	 */
	public void addNamespaces(HashMap<String, String> namespaces) {
		this.namespaces.putAll(namespaces);
	}	
	
	/**
	 * Determines if the namespace has some value
	 * @return <code>true</code> if the namespace has some value
	 */
	public boolean hasNamespaces() {
		return namespaces.size() > 0;
	}

	/**
	 * Lists the hashmap of namespaces
	 * @return a hashmap with prefix,namespaces
	 */
	public HashMap<String,String> listNamespaces() {
		return namespaces;
	}
	
	/**
	 * Gets the namespace corresponding to the provided prefix
	 * @param prefix a prefix
	 * @return a url pointing to the namespace corresponding to the prefix. if no namespace is defined for the prefix, returns <code>null</code>
	 */
	public String getNamespace(String prefix) {
		if (OntologyNSPrefix.findPrefix(prefix) != null) {
			OntologyNSPrefix ns = OntologyNSPrefix.findPrefix(prefix);
			return ns.getNamespace();
		} else {
			return namespaces.get(prefix);
		}
	}
	
	/**
	 * Gets the namespace corresponding to the provided prefix
	 * @param prefix a prefix
	 * @return a url pointing to the namespace corresponding to the prefix. if no namespace is defined for the prefix, returns <code>null</code>
	 */
	public String getPrefix(String url) {
		if (OntologyNSPrefix.findNamespace(url) != null) {
			OntologyNSPrefix ns = OntologyNSPrefix.findNamespace(url);
			return ns.getPrefix();
		} else {
			for(String prefix : namespaces.keySet()) {
				if(url.contains(namespaces.get(prefix))) {
					return prefix;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Adds a triple to the list of triples
	 * @param triple the triple to be added
	 */
	public void addTriple(RDFM3Triple triple) {
		triples.add(triple);
	}
	
	/**
	 * Add a set of triples triple
	 * @param triples a set of triples to be added
	 */
	public void addTriples(Collection<RDFM3Triple> triples) {
		this.triples.addAll(triples);
	}

	/**
	 * Determines if the RDF-M3 structure has triples
	 * @return <code>true</code> if the rdf-m3 has triples
	 */
	public boolean hasTriples() {
		return triples.size() > 0;
	}
	
	/**
	 * Gets the list of triples
	 * @return the list of triples contained in the current triple list
	 */
	public Collection<RDFM3Triple> listTriples() {
		return triples;
	}

	/**
	 * Gets the list of triples
	 * @return the list of triples contained in the current triple list
	 */
	public RDFM3Triple getFirstTriple() {
		return triples.get(0);
	}	
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		
		sb.append(TRIPLELIST_OPENINGTAG);
		if (namespaces.size() > 0) {
			for(String prefix : namespaces.keySet()) {
				sb.append(" ").append(NAMESPACE_PREFIX);
				sb.append(prefix).append("=\"");
				sb.append(namespaces.get(prefix));
				sb.append("\"\n");
			}
		}
		sb.append(">").append(CR);
		for(RDFM3Triple triple : triples) {
			sb.append(triple.toString());
		}
		sb.append(TRIPLELIST_CLOSINGTAG).append(CR);
		return sb.toString();
	}

	/**
	 * Gets a short representation of the RDF-M3 triples
	 * @return a string in the form [sub,pred,obj] for each triple
	 */
	public String toSortString() {
		// Gets the human readable format for the m3
		if (this.hasTriples()) {
			StringBuilder sb = new StringBuilder();
			
			for (RDFM3Triple triple : this.listTriples()) {
				sb.append("[");
				/**
				 * Subject can be either a wildcard, a blank node, a uri or a prefix:element 
				 */
				if (triple.isSubjectWildcard()) {
					sb.append("*");
				} else if (triple.isSubjectBNode()){
					sb.append(triple.getSubject());
				} else { // it should be a URI
					if (triple.isSubjectURL()) {
						String prefix = this.getPrefix(triple.getSubject());
						if (prefix != null) {
							sb.append(prefix).append(":");
						}
						sb.append(triple.getSubjectElement());
					} else if (triple.isSubjectNsPrefixFormat()){
						sb.append(triple.getSubject());
					} else {
						// take care on using this!
						sb.append(triple.getSubject());
					}
				}

				// separator
				sb.append(",");
				
				/**
				 * Predicate can be either a wildcard, a uri or a prefix:element 
				 */
				if (triple.isPredicateWildcard()) {
					sb.append("*");
				} else { // it should be a URI
					if (triple.isPredicateURL()) {
						String prefix = this.getPrefix(triple.getPredicate());
						if (prefix != null) {
							sb.append(prefix).append(":");
						}
						sb.append(triple.getPredicateElement());
					} else if (triple.isPredicateNsPrefixFormat()){
						sb.append(triple.getPredicate());
					} else {
						// take care on using this!
						sb.append(triple.getPredicate());
					}
				}				
				
				// separator
				sb.append(",");
				
				/**
				 * Object can be either a literal, a wildcard, a blank node, a uri or a prefix:element 
				 */
				if (triple.isObjectLiteral()) {
					sb.append("\"").append(triple.getObjectDataContent()).append("\"");
					sb.append(triple.getObjectDatatype());
					sb.append(triple.getObjectLang());
				} else if (triple.isObjectWildcard()) {
					sb.append("*");
				} else if (triple.isObjectBNode()){
					sb.append(triple.getObject());
				} else { // it should be a URI
					if (triple.isObjectURL()) {
						String prefix = this.getPrefix(triple.getObject());
						if (prefix != null) {
							sb.append(prefix).append(":");
						}
						sb.append(triple.getObjectElement());
					} else if (triple.isObjectNsPrefixFormat()){
						triple.getObject();
					} else {
						// take care on using this!
						triple.getObject();
					}
				}		
				
				sb.append("]");				
			}
			
			return sb.toString();
		}
		
		return null;
	}
	
	/**
	 * Creates a set of N-Triples representing the RDF-M3 format
	 * @return a String with a set of N-Triples
	 */
	public String toNTriples() throws Exception {
		StringBuffer sb = new StringBuffer();
		
		try {
			if (!this.hasTriples()) { // check the existence of ntriples
				return "";
			}
			
			for (RDFM3Triple triple : this.listTriples()) {
				/** SUBJECT */
				if (triple.isSubjectURL()) { // Subject is a URL
					sb.append("<").append(triple.getSubject()).append(">");
				} else if (triple.isSubjectBNode()) { // subject is a blank node / anonymous
					sb.append(triple.getSubject());
				} else if (triple.isSubjectWildcard()) { // subject is a wildcard
					sb.append(triple.getSubject());
				} else if (triple.isSubjectNsPrefixFormat()) {
					String prefix = triple.getSubjectPrefix();
					String namespace = this.getNamespace(prefix);
					if (namespace != null) {
						sb.append("<").append(namespace);
						if (!namespace.endsWith("#")) {
							sb.append("#");
						}
						sb.append(triple.getSubjectElement()).append(">");
					} else if (triple.getSubject().startsWith("\"") && triple.getSubject().endsWith("\"")) {
						sb.append(triple.getSubject());
					} else {
						sb.append("\"").append(triple.getSubject()).append("\"");
					}
				} else { // it is for sure a blank node
					if (triple.getSubject().startsWith("\"") && triple.getSubject().endsWith("\"")) {
						sb.append(triple.getSubject());
					} else {
						sb.append("\"").append(triple.getSubject()).append("\"");
					}					
				}
				sb.append(" ");

				if (triple.isPredicateURL()) { // Predicate is a URL
					sb.append("<").append(triple.getPredicate()).append(">");
				} else if (triple.isPredicateNsPrefixFormat()) {
					String prefix = triple.getPredicatePrefix();
					String namespace = this.getNamespace(prefix);
					if (namespace != null) {
						sb.append("<").append(namespace);
						if (!namespace.endsWith("#")) {
							sb.append("#");
						}
						sb.append(triple.getPredicateElement()).append(">");
					} else {
						throw new Exception("Predicate " + triple.getPredicate() + " is not correct for the triple \n" + triple.toString());
					}
				} else { // it is for sure a blank node
					throw new Exception("Predicate " + triple.getPredicate() + " is not correct for the triple \n" + triple.toString());
				}
				
				sb.append(" ");
				
				/** Object can be either url, blank node, literal or prefix:element formatted */
				if (triple.isObjectURL()) { // Object is a URL
					sb.append("<").append(triple.getObject()).append(">");
				} else if (triple.isObjectBNode()) { // Predicate is a blank node
					sb.append(triple.getObject());
				} else if (triple.isObjectWildcard()) { // subject is a wildcard
					sb.append(triple.getObject());
				} else if (triple.isObjectLiteral()) {
					sb.append("\"").append(triple.getObjectDataContent()).append("\"");
					String datatype = triple.getObjectDatatype();
					if (datatype != null && !datatype.equals("")) {
						if (triple.isURL(datatype)) {
							sb.append("^^<").append(datatype).append(">");								
						} else if (triple.isNSPrefixFormat(datatype)) {
							String namespace = this.getNamespace(triple.getNSPrefixFormatPrefix(datatype));
							String element = triple.getNSPrefixFormatElement(datatype);	
							if (namespace != null) {
								sb.append("^^<").append(namespace).append(element).append(">");		
							}
						}
					}

					String lang = triple.getObjectLang();
					if (lang != null && !lang.equals("")) {
						sb.append("@").append(lang);
					}
					
				} else if (triple.isObjectNsPrefixFormat()) { // maybe it is a ns_prefix
					String prefix = triple.getObjectPrefix();
					String namespace = this.getNamespace(prefix);
					if (namespace != null) {
						sb.append("<").append(namespace);
						if (!namespace.endsWith("#")) {
							sb.append("#");
						}
						sb.append(triple.getObjectElement()).append(">");
					} else if (triple.getObject().startsWith("\"") && triple.getObject().endsWith("\"")) { // do not add additional
						sb.append(triple.getObject());
					} else {
						sb.append("\"").append(triple.getObject()).append("\"");
					}
				} else { // it is for sure a blank node or a literal
					if (triple.getObject().startsWith("\"") && triple.getObject().endsWith("\"")) { // do not add additional
						sb.append(triple.getObject());
					} else {
						sb.append("\"").append(triple.getObject()).append("\"");
					}
				}
				
				sb.append(" ").append(".").append(" \n");					
			}
			return sb.toString();
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new Exception("Cannot transform current RDF-M3 into N-Triples format");
		}
	}

	/**
	 * Determines if triples have at least one wildcard triple
	 * @return <code>true</code> if has at least one wildcard
	 */
	public boolean containsWildcards() {
		for (RDFM3Triple triple : triples) {
			if (triple.containsWildcards()) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Gets the list of triples containing wildcards
	 * @return list of triples containing wildcards
	 */
	public Collection<RDFM3Triple> getTriplescontainingWildcards() {
		ArrayList<RDFM3Triple> tripleList = new ArrayList<RDFM3Triple>();
		for (RDFM3Triple triple : triples) {
			if (triple.containsWildcards()) {
				tripleList.add(triple);
			}
		}
		return tripleList;
	}

	/**
	 * Removes triples from from the m3
	 * @param removeTriples the set of triples to be removed
	 */
	public void removeTriples(ArrayList<RDFM3Triple> removeTriples) {
		for (RDFM3Triple triple : removeTriples) {
			if (triples.contains(triple)) {
				triples.remove(triple);
			}
		}
	}
}
