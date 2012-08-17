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
 * 
 * This class implements the Parameters for the SSAP Message.
 *
 * @author Fran Ruiz, ESI
 * 
 */
public class SSAPMessageParameter {
	
	protected NameAttribute name;
	protected String content;
	
	private static final String CDATA_PREFIX = "<![CDATA[";	
	private static final String CDATA_SUFFIX = "]]>";	
	
	/**
	 * Enumerated response names
	 */
	public enum NameAttribute {  
		STATUS			("status"),
		CREDENTIALS		("credentials"),
		INSERTGRAPH		("insert_graph"),
		REMOVEGRAPH		("remove_graph"),
		CONFIRM			("confirm"),
		BNODES			("bnodes"),
		TYPE			("type"), 
		ENCODING		("encoding"),
		FORMAT          ("format"), 
		QUERY			("query"),
		SUBSCRIPTIONID	("subscription_id"),
		RESULTS			("results"),
		NEWRESULTS		("new_results"),
		OBSOLETERESULTS	("obsolete_results");
		
		private final String value;
		
		NameAttribute(String value) {
			this.value = value;
		}
		
		public String getValue() {
			return value;
		}
		
		public static NameAttribute findValue(String v) {
			for(NameAttribute na : values()) {
				if(na.getValue().equals(v)) {
					return na;
				}
			}
			return null;
		}
	}
	
	// CONFIRMATION PARAMETER
	public static String TRUE = "TRUE";
	public static String FALSE = "FALSE";
	
	/**
	 * Constructor of named parameter in a SSAP message
	 * @param name the value for the attribute <code>name</code>
	 * @param content content of the parameter
	 */
	public SSAPMessageParameter(NameAttribute name, String content) {
		this.name = name;
		this.content = content;
	}

	/**
	 * Gets the name of the parameter
	 * @return A String with the name of the parameter
	 */
	public NameAttribute getName() {
		return name;
	}

	/**
	 * Sets the value for the attribute <code>name</code>
	 * @param name a NameAttribute indicating the attribute <code>name</code> value
	 */
	public void setName(NameAttribute name) {
		this.name = name;
	}

	/**
	 * Returns the content of the parameter
	 * @return A String with the content of the parameter
	 */
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
	
	/**
	 * Sets the content for the property tag
	 * @param content a String indicating the content of the tag
	 */
	public void setContent(String content) {
		this.content = content;
	}	
	
	/**
	 * Gets the string representation of this SSAPMessage
	 * @return a string representation of the SSAPMessage
	 */
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(openXML());
		sb.append(attributeXML());
		sb.append(">");
		sb.append(contentXML());
		sb.append(closeXML());
		return sb.toString();
	}
	
	/**
	 * Returns the opening tag of a SSAPMessage
	 * @return a string with the opening tag of a XML representation of SSAPMessage
	 */
	protected String openXML() {
		return "  <parameter";
	}
	
	/**
	 * Gets the content of the SSAPMessage, including parameters
	 * @return a string representation of the content of a SSAPMessage
	 */
	protected String attributeXML() {
		StringBuilder sb = new StringBuilder();
		sb.append(" name=\"");
		sb.append(name.getValue());
		sb.append("\"");
		return sb.toString();
	}
	
	/**
	 * Gets the content of the SSAPMessage, including parameters
	 * @return a string representation of the content of a SSAPMessage
	 */
	protected String contentXML() {
		return content;
	}
	
	/**
	 * Returns the opening tag of a SSAPMessage
	 * @return a string with the closing tag of a XML representation of SSAPMessage
	 */
	protected String closeXML() {
		return "</parameter>\n";
	}
}
