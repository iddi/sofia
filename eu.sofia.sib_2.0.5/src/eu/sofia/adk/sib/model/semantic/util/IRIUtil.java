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

package eu.sofia.adk.sib.model.semantic.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * Helper class to obtain ontology IRI content
 * 
 * @author Fran Ruiz, fran.ruiz@tecnalia.com, ESI
 */
public class IRIUtil {

	/**
	 * Gets the class identifier
	 * @param iri the iri
	 * @return a String with the class/individual identifier
	 */
	private static String getElement(String iri) {
		return iri.substring(iri.lastIndexOf('#')+1);
	}

	/**
	 * Gets the namespace associated to the IRI
	 * @param iri the iri
	 * @return a String containing the namespace
	 */
	private static String getNamespace(String iri) {
		return iri.substring(0,iri.lastIndexOf('#')+1);
	}

	/**
	 * Gets the prefix associated for a given namespace
	 * @param namespace the namespace
	 * @param nsMap the namespace prefix map
	 * @return a string 
	 */
	private static String getPrefix(String namespace, Map<String, String> nsMap) {
		String retPrefix = null;
		
		for (String prefix : nsMap.keySet()) {
			if (prefix != "") {
				String ns = nsMap.get(prefix);
				if (ns.equals(namespace)) {
					retPrefix = prefix;
				}
			}
		}
		return retPrefix;
	}	
	
	/**
	 * Gets the resource value
	 * @param resource the resource
	 * @return the resource value
	 */
	private static String getValue(String resource) {
		if (resource.contains("^^")) {
			return resource.substring(0, resource.indexOf('^'));
		} else {
			return resource;
		}
	}

	/**
	 * Gets the datatype associated to the resource
	 * @param resource a string indicating the resource
	 * @return the datatype
	 */
	private static String getDatatype(String resource) {
		if (resource.contains("^^")) {
			return resource.substring(resource.lastIndexOf('^')+1);
		} else {
			return null;
		}
	}
	
	/**
	 * Gets the short name of a resource
	 * @param resource the resource
	 * @param nsMap the namespace prefix map
	 * @return the short name in the nsprefix:classid pattern
	 */
	public static String getShortName(String resource,
			Map<String, String> nsMap) {

		StringBuilder shortName = new StringBuilder();
		
		if (isFullIRI(resource)) { 
			String prefix = getPrefix(getNamespace(resource),nsMap);
			if (prefix != null && !prefix.equals("")) {
				shortName.append(getPrefix(getNamespace(resource),nsMap));
				shortName.append(":");
				shortName.append(getElement(resource));				
			} else {
				String elementPrefix  = getLastPartOf(resource);
				if (elementPrefix != null) {
					shortName.append(elementPrefix);
					shortName.append(":");
				}
				shortName.append(getElement(resource));
			}
			return shortName.toString();
		} else if (isDatatype(resource)) {
			String value = getValue(resource);
			String datatype = getDatatype(resource);
			shortName.append("\"");
			shortName.append(value);
			shortName.append("\"");
			shortName.append(" (xsd:");
			shortName.append(getElement(datatype));
			shortName.append(")");
			return shortName.toString();
		} else {
			return resource;
		}
	}

	public static String getLiteralShortName(String literal) {
		StringBuilder shortName = new StringBuilder();
		String value = getValue(literal);
		String datatype = getDatatype(literal);
		shortName.append("\"");
		shortName.append(value);
		shortName.append("\"");
		if (datatype != null) {
			shortName.append(" (xsd:");
			shortName.append(getElement(datatype));
			shortName.append(")");
		}
		return shortName.toString();
	}
	
	/**
	 * Gets the last part of an IRI
	 * @param iri a IRI
	 * @return a
	 */
	private static String getLastPartOf(String iri) {
		if (iri.contains("#")) {
			return iri.substring(iri.lastIndexOf('/')+1,iri.lastIndexOf('#'));
		} else {
			return null;
		}
	}

	/**
	 * Determines if the resource is a full iri
	 * @param resource the resource
	 * @return <code>true</code> if the resource if a full ontology iri
	 */
	private static boolean isFullIRI(String resource) {
		try {
			new URL(resource);
			return true;
		} catch (MalformedURLException ex) {
			return false;
		}
	}
	
	/**
	 * Determines if the resource is a XML-Schema datatype
	 * @param resource the resource
	 * @return <code>true</code> if the resource if a XML-Schema datatype
	 */
	private static boolean isDatatype(String resource) {
		return resource.contains("^^");
	}	
}
