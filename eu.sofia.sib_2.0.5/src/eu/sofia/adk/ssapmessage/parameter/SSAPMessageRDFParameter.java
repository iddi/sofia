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

package eu.sofia.adk.ssapmessage.parameter;

/**
 * The specialized parameter for rdf triples
 * 
 * @author Fran Ruiz, ESI
 * @author Raul Otaolea, ESI
 *
 */
public class SSAPMessageRDFParameter extends SSAPMessageParameter {
	
	/**
	 * Enumerated response names
	 */
	public enum TypeAttribute {  
		RDFM3			("RDF-M3"),
		RDFXML			("RDF-XML"),
		WQLVALUES		("wql-values"),
		WQLRELATED		("wql-related"),
		WQLNODETYPES	("wql-nodetypes"),
		WQLISTYPE		("wql-istype"),
		WQLISSUBTYPE	("wql-issubtype"),
		SPARQL			("sparql");
		
		private final String value;
		
		TypeAttribute(String value) {
			this.value = value;
		}
		
		public String getValue() {
			return value;
		}
		
		public static TypeAttribute findValue(String v) {
			for(TypeAttribute ta : values()) {
				if(ta.getValue().equals(v)) {
					return ta;
				}
			}
			return null;
		}
	}

	private static final String CDATA_PREFIX = "<![CDATA[";	
	private static final String CDATA_SUFFIX = "]]>";	
	
	private TypeAttribute encoding;

	/**
	 * Constructor of named parameter in a SSAP message
	 * @param name the value for the attribute <code>name</code>
	 * @param type the value for the attribute <code>type</code>
	 * @param content content of the parameter (rdf format)
	 */
	public SSAPMessageRDFParameter(NameAttribute name, TypeAttribute type, String content) {
		super(name,content);
		this.encoding = type;
	}
	
	/**
	 * Constructor of named parameter in a SSAP message
	 * @param name the value for the attribute <code>name</code>
	 * @param type the value for the attribute <code>type</code>
	 * @param content content of the parameter (rdf format)
	 */
	public SSAPMessageRDFParameter(NameAttribute name, String content) {
		super(name, content);
		this.encoding = null;
	}	
	
	/**
	 * Gets the type of the parameter
	 * @return A String with the name of the parameter
	 */
	public TypeAttribute getType() {
		return encoding;
	}

	/**
	 * Sets the value for the attribute <code>type</code>
	 * @param type a String indicating the attribute <code>type</code> value
	 */
	public void setType(TypeAttribute type) {
		this.encoding = type;
	}
	
	/**
	 * Gets the content of the SSAPMessage, including parameters
	 * @return a string representation of the content of a SSAPMessage
	 */
	@Override
	protected String attributeXML() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.attributeXML());
		if (encoding != null) {
			sb.append(" encoding='");
			sb.append(encoding.getValue());
			sb.append("'");
		}
		return sb.toString();
	}
	
	/**
	 * Returns the content of the parameter
	 * @return A String with the content of the parameter
	 */
	@Override
	public String getContent() {
		if (isCDATA()) {
			return getCDATAContent();
		} else {
			return content;
		}
	}
	
	/**
	 * Determines if the content is a CDATA
	 * @return <code>true</code> if the content is CDATA
	 */
	private boolean isCDATA() {
		if (content.startsWith(CDATA_PREFIX)) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * Gets the CDATA content
	 * @return a String with the CDATA content
	 */
	private String getCDATAContent() {
		return content.substring(content.indexOf(CDATA_PREFIX)+CDATA_PREFIX.length(), content.lastIndexOf(CDATA_SUFFIX));
	}
	
}
