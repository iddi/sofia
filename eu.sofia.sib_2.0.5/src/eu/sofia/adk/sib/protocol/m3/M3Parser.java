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

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import eu.sofia.adk.sib.exception.RDFM3ParseException;
import eu.sofia.adk.sib.protocol.m3.RDFM3Triple.TypeAttribute;

/**
 * This class implements the parser for RDF-M3 in XML format.
 * 
 * @author Fran Ruiz, fran.ruiz@tecnalia.com, ESI-Tecnalia
 *
 */
public class M3Parser {

	private static final String TAG_TRIPLELIST = "triple_list";
	private static final String TAG_TRIPLE = "triple";
	private static final String TAG_SUBJECT = "subject";
	private static final String ATT_TYPE = "type";
	private static final String TAG_PREDICATE = "predicate";
	private static final String TAG_OBJECT = "object";
	
	/**
	 * Parses the content of a string into rdf-m3 format
	 * @param results a string containing a rdf-m3 in xml format
	 * @return the RDF-M3 representation of incoming string
	 * @throws Exception when something goes not well
	 */
	public RDFM3 parse(String results) throws RDFM3ParseException {
		RDFM3 m3 = new RDFM3();
		
		try {
			ByteArrayInputStream bis = new ByteArrayInputStream(results.getBytes());
			
			// We will use StaX pull
			XMLInputFactory factory = XMLInputFactory.newInstance();
			XMLStreamReader reader = factory.createXMLStreamReader(bis);
			
			// START_DOCUMENT
			int eventType;
			
			while (reader.hasNext()) {
				eventType = reader.next();
				if(eventType == XMLStreamConstants.START_ELEMENT && 
						reader.getLocalName().equals(TAG_TRIPLELIST)){
					m3.addNamespaces(this.getNamespaces(reader));
					m3.addTriples(this.getTriples(reader));
				}
			}
		} catch (XMLStreamException ex) {
			ex.printStackTrace();
			throw new RDFM3ParseException("Cannot parse rdf-m3 due to " + ex.getMessage(), ex);
		} 
		
		return m3;
	}
	
	/**
	 * Gets the results associated to the triples
	 * @param reader the XML Stream reader
	 */
	private Collection<RDFM3Triple> getTriples(XMLStreamReader reader) {
		int eventType = -1;
		ArrayList<RDFM3Triple> triples = new ArrayList<RDFM3Triple>();
		
		try {
			do {
				eventType = reader.next();
				
				if(eventType == XMLStreamConstants.START_ELEMENT && 
						reader.getLocalName().equals(TAG_TRIPLE)){
					// Gets the triples
					RDFM3Triple triple = this.getTriple(reader);
					if (triple != null) {
						triples.add(triple);
					}
				}
			} while(!reader.hasNext() || !(eventType == XMLStreamConstants.END_ELEMENT &&
					reader.getLocalName().equals(TAG_TRIPLELIST)));
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
		
		return triples;
	}
		
	/**
	 * Gets one result associated to the SPARQL-results results tag
	 * @param reader the XML Stream reader
	 */
	private RDFM3Triple getTriple(XMLStreamReader reader) {
		int eventType = -1;
		
		RDFM3Triple triple = null;
		String subject = null;
		TypeAttribute subjectType = null;
		String predicate = null;
		String object = null;
		TypeAttribute objectType = null;
		
		try {
			do {
				eventType = reader.next();
				
				//if (reader.getLocalName() != null) {
					if(eventType == XMLStreamConstants.START_ELEMENT && 
							reader.getLocalName().equals(TAG_SUBJECT)){
						String type = getAttribute(reader, ATT_TYPE);
						if (type != null && !type.equals("")) {
							subjectType = TypeAttribute.findType(type);
						}
						
						do {
							eventType = reader.next();
							
							if (eventType == XMLStreamConstants.CHARACTERS) {
								subject = reader.getText();
							}
						} while(!reader.hasNext() || !(eventType == XMLStreamConstants.END_ELEMENT &&
								reader.getLocalName().equals(TAG_SUBJECT)));
					}
					
					if(eventType == XMLStreamConstants.START_ELEMENT && 
							reader.getLocalName().equals(TAG_PREDICATE)){
						do {
							eventType = reader.next();
							
							if (eventType == XMLStreamConstants.CHARACTERS) {
								predicate = reader.getText();
							}
						} while(!reader.hasNext() || !(eventType == XMLStreamConstants.END_ELEMENT &&
								reader.getLocalName().equals(TAG_PREDICATE)));
					}					

					
					if(eventType == XMLStreamConstants.START_ELEMENT && 
							reader.getLocalName().equals(TAG_OBJECT)){
						String type = getAttribute(reader, ATT_TYPE);
						if (type != null && !type.equals("")) {
							objectType = TypeAttribute.findType(type);
						}
						
						// resolves multiline bug
						StringBuilder sb = new StringBuilder();
						
						do {
							eventType = reader.next();
							
							if (eventType == XMLStreamConstants.CHARACTERS) {
								sb.append(reader.getText()); 					
							}
						} while(!reader.hasNext() || !(eventType == XMLStreamConstants.END_ELEMENT &&
								reader.getLocalName().equals(TAG_OBJECT)));
						
						object = sb.toString();
					}					
				//}
			} while(!reader.hasNext() || !(eventType == XMLStreamConstants.END_ELEMENT &&
					reader.getLocalName().equals(TAG_TRIPLE)));
			
			// create m3-triple
			triple = new RDFM3Triple(subject, subjectType, predicate, object, objectType);
			
		} catch (XMLStreamException e) {
			e.printStackTrace();
		}
		
		return triple;
	}
	
	/**
	 * Gets the namespaces associated to a tag
	 * @param reader the XML Stream reader
	 */
    private HashMap<String,String> getNamespaces(XMLStreamReader reader) {
    	HashMap<String,String> namespaces = new HashMap<String,String>();
    	
        int count = reader.getNamespaceCount();
        if(count > 0) {
            for(int i = 0 ; i < count ; i++) {
            	String prefix = reader.getNamespacePrefix(i);
            	String namespace = reader.getNamespaceURI(i);
        		if (namespace != null) {
        			namespaces.put(prefix, namespace);
        		}
            }            
        }
        return namespaces;
    }
    
	/**
	 * Gets the attributes assocaited to a tag
	 * @param reader the XML Stream reader
	 * @param attributeName the attribute name
	 */
    private String getAttribute(XMLStreamReader reader, String attributeName) {
        int count = reader.getAttributeCount() ;
        if(count > 0) {
            for(int i = 0 ; i < count ; i++) {
            	if (reader.getAttributeName(i).toString().equals(attributeName)) {
            		return reader.getAttributeValue(i);
            	}
            }            
        }
        
        return null;
    }    
}
