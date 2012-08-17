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

package eu.sofia.adk.common.xsd;

import java.util.HashMap;

/**
 * A helper class that maps the correspondence between the XSD datatypes and the Java types.
 * This mapping is based on the defined mapping for the JAXB.
 * @see <a href="http://java.sun.com/webservices/docs/2.0/tutorial/doc/JAXBWorks4.html">JAXB Mapping</a>
 *  
 * @author Fran Ruiz, Fran.Ruiz@tecnalia.com, Tecnalia
 */
public class XSDMapping {

	/** The java correspondence mapping */
	private static HashMap<String, String> javaCorrespondence = new HashMap<String, String>();
	
	/** The default java datatype */
	private static String defaultDatatype = "java.lang.Object";

	/** the shared instance */
	static private XSDMapping _instance = null;
	
	/**
	 * Gets the shared instance
	 * @return the shared instance of XSDMapping
	 */
	static public XSDMapping getInstance() {
		if(null == _instance) {
			_instance = new XSDMapping();
		}
		return _instance;
	}
	
	/**
	 * Gets the Java datatype corresponding to a XSD uri
	 * @param uri the uri that represents the XSD datatype
	 * @return the corresponding java type
	 */
	public static String getJavaDatatype(String uri) {
		// if not populate, populate it
		if (!isPopulated()) {
			init();
		}
		
		// see if there is a correspondence with XML Schema datatypes
		if (javaCorrespondence.get(uri) == null) {
			return defaultDatatype;
		} else {
			return javaCorrespondence.get(uri);
		}
	}
	
	/**
	 * Inits the mapping table
	 */
	private static void init() {
		String xsd = "http://www.w3.org/2001/XMLSchema#";
		javaCorrespondence.put(xsd+"string", "String");
		javaCorrespondence.put(xsd+"integer", "java.math.BigInteger");
		javaCorrespondence.put(xsd+"int", "Integer");
		javaCorrespondence.put(xsd+"long", "Long");
		javaCorrespondence.put(xsd+"short", "Short");
		javaCorrespondence.put(xsd+"decimal", "java.math.BigDecimal");
		javaCorrespondence.put(xsd+"float", "Float");
		javaCorrespondence.put(xsd+"double", "Double");
		javaCorrespondence.put(xsd+"boolean", "Boolean");
		javaCorrespondence.put(xsd+"byte", "Byte");
		javaCorrespondence.put(xsd+"QName", "javax.xml.namespace.QName");
		javaCorrespondence.put(xsd+"dateTime", "javax.xml.datatype.XMLGregorianCalendar");
		javaCorrespondence.put(xsd+"base64Binary", "byte[]");
		javaCorrespondence.put(xsd+"hexBinary", "byte[]");
		javaCorrespondence.put(xsd+"unsignedInt", "Long");
		javaCorrespondence.put(xsd+"unsignedShort", "Integer");
		javaCorrespondence.put(xsd+"unsignedByte", "Short");
		javaCorrespondence.put(xsd+"time", "javax.xml.datatype.XMLGregorianCalendar");
		javaCorrespondence.put(xsd+"date", "javax.xml.datatype.XMLGregorianCalendar");
		javaCorrespondence.put(xsd+"g", "javax.xml.datatype.XMLGregorianCalendar");
		javaCorrespondence.put(xsd+"duration", "javax.xml.datatype.Duration");
		javaCorrespondence.put(xsd+"NOTATION", "javax.xml.namespace.QName");
	}
	
	/**
	 * A helper method that queries if the mapping table is initialized.
	 * @return <code>true</code> if the mapping is already initialized
	 */
	private static boolean isPopulated() {
		return javaCorrespondence.size() > 0;
	}
}
