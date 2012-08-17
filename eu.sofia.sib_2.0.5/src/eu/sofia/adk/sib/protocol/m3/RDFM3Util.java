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

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Helper class for simple checking
 * 
 * @author Fran Ruiz, Fran.Ruiz@tecnalia.com, Tecnalia - Software Systems Engineering
 */
public class RDFM3Util {

	/** Useful constants */
	private static final String BNODE_PREFIX = "_:";
	public static final String CDATA_OPENINGTAG = "<![CDATA[";
	public static final String CDATA_CLOSINGTAG = "]]>";

	/** Wildcards */
	protected static final String WILDCARD = "sib:any";
	protected static final String WILDCARD_NOKIA = "http://www.nokia.com/NRC/M3/sib#any";
	
	private static final String XSDDATATYPE_SEPARATOR = "^^";
	private static final String XSDLANG_SEPARATOR = "@";
	private static final String XSDRDFDATATYPE_URL = "http://www.w3.org/2000/01/rdf-schema#";
	private static final String OPENINGTAG = "<";
	private static final String CLOSINGTAG = ">";
	private static final String XSDRDFPREFIXDATATYPE_OPENINGTAG = "xsd";
	private static final String QUOTES = "\"";
	
	private static final String URLRESOURCE_DESCRIPTOR = "#";
	
	/**
	 * Void constructor
	 */
	public RDFM3Util() {
	}

	/**
	 * Determines if the element is a blank node
	 * @param element the element to check 
	 * @return <code>true</code> if the element has a blank node pattern (e.g., starts with '_:')
	 */
	public boolean isBNode(String element) {
		if (this.hasContent(element)) {
			// take care of CDATA
			String elementContent = element;
			if (this.isCDATA(elementContent)) {
				elementContent = this.getCDATAContent(elementContent);
			}
			
			if (this.hasContent(elementContent)) {				
				return elementContent.startsWith(BNODE_PREFIX);
			}
		}
		return false;
	}

	/**
	 * Determines if the element has embedded content inside a CDATA structure
	 * @param element the element to check 
	 * @return <code>true</code> if the element has a CDATA pattern (e.g., starts with <code>'<![CDATA['</code> and ends with <code>']]>'</code>)
	 */
	public boolean isCDATA(String element) {
		if (this.hasContent(element)) {
			return element.startsWith(CDATA_OPENINGTAG) && element.endsWith(CDATA_CLOSINGTAG);
		} 
		return false;
	}
	
	/**
	 * Gets the CDATA content
	 * @param element the element to get the data 
	 * @return the content of CDATA. <code>null</code> if there if the element is null-valued or zero-sized
	 */
	public String getCDATAContent(String element) {
		if (this.hasContent(element)) {
			return element.substring(CDATA_OPENINGTAG.length(), element.indexOf(CDATA_CLOSINGTAG));
		} 
		return "";
	}	
	
	/**
	 * Determines if an element has content
	 * @param element the element to check
	 * @return <code>true</code> if the element is not <code>null</code> valued or is empty
	 */
	public boolean hasContent(String element) {
		return (element != null && !element.equals(""));
	}
	
	/**
	 * Determines if an element is a wildcard
	 * @param element the element to check
	 * @return <code>true</code> if the element has a wild card pattern (e.g. 'sib:any')
	 */
	public boolean isWildcard(String element) {
		if (this.hasContent(element)) {
			if (this.hasContent(element)) {			
				return element.equals(WILDCARD) ||
				element.equals(WILDCARD_NOKIA);
			}
		}
		return false;
	}

	/**
	 * Determines if an element is a URL
	 * @param element the element to check
	 * @return <code>true</code> if the element has a URL pattern (e.g. <code>'http://something/path/element#data'</code>)
	 */
	public boolean isURL(String element) {
		try {
			if (this.hasContent(element)) {
				
				if (this.hasContent(element)) {
					new URL(element);
					return true;
				}
			}
		} catch (MalformedURLException ex) {
			return false;
		}
		return false;
	}

	/**
	 * Determines if an element has a namespace prefix format
	 * @param element the element to check
	 * @return <code>true</code> if the element has a namespace prefix pattern (e.g. <code>'prefix:element'</code>)
	 */
	public boolean isNSPrefixFormat(String element) {
		if (this.hasContent(element)) {
			// take care of CDATA
			String elementContent = element;
			if (this.isCDATA(elementContent)) {
				elementContent = this.getCDATAContent(elementContent);
			}
			
			if (this.hasContent(elementContent) && !elementContent.startsWith("\"")) {			
				String[] splitString = elementContent.split(":");
				// if the string has two parts, separated by two points ':', then it should be some kind of
				// prefix namespace structure (prefix:element)
				if (splitString.length == 2) {
					// check if splitted elements has content
					if (splitString[0].length() > 0 && splitString[1].length() > 0) {
						return true;
					}
				}
			}
		}
			
		return false;
	}
	
	/**
	 * Determines if an element is a URL
	 * @param element the element to check
	 * @return the prefix part of a ns-prefix pattern <code>prefix:element</code>. <code>null</code> if prefix part cannot be get
	 */
	public String getNSPrefixFormatPrefix(String element) {
		if (this.hasContent(element)) {
			// take care of CDATA
			String elementContent = element;
			if (this.isCDATA(elementContent)) {
				elementContent = this.getCDATAContent(elementContent);
			}
			
			if (this.hasContent(elementContent)) {			
				String[] splitString = elementContent.split(":");
				// if the string has two parts, it returns the part corresponding to the prefix
				if (splitString.length == 2) {
					// check if splitted elements has content
					if (splitString[0].length() > 0 && splitString[1].length() > 0) {
						return splitString[0];
					}
				}
			}
		}
		return null;
	}	
	
	/**
	 * Gets the element part of an ns-prefix structure
	 * @param element the element to check
	 * @return the element part of a ns-prefix pattern <code>prefix:element</code>. <code>null</code> if prefix part cannot be get
	 */
	public String getNSPrefixFormatElement(String element) {
		if (this.hasContent(element)) {
			// take care of CDATA
			String elementContent = element;
			if (this.isCDATA(elementContent)) {
				elementContent = this.getCDATAContent(elementContent);
			}
			
			if (this.hasContent(elementContent)) {				
				String[] splitString = elementContent.split(":");
				// if the string has two parts, it returns the part corresponding to the prefix
				if (splitString.length == 2) {
					// check if splitted elements has content
					if (splitString[0].length() > 0 && splitString[1].length() > 0) {
						return splitString[1];
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Determines if the element is an XML-Schema datatype 
	 * @param element the element to check
	 * @return <code>true</code> if the element is an XML-Schema datatype: <code>"data"^^<http://www.w3.org/2000/01/rdf-schema#datatype></code>
	 * or <code>"data"^^xsd:datatype</code> or <code>""@lang</code>
	 */
	public boolean isXMLSchemaFormatLiteral(String element) {
		if (this.hasContent(element)) {
			// take care of CDATA
			String elementContent = element;
			if (this.isCDATA(elementContent)) {
				elementContent = this.getCDATAContent(elementContent);
			}
			
			if (this.hasContent(elementContent)) {
				if (this.isXMLSchemaDatatypeLiteral(elementContent) || this.isXMLSchemaLangLiteral(elementContent)) {
					return true;
				} else if (elementContent.startsWith(QUOTES) && elementContent.endsWith(QUOTES)) {
					return true;
				}
			}
		}
		return false;		
	}
	
	/**
	 * Determines if the element is an XML-Schema lang structure 
	 * @param element the element to check
	 * @return <code>true</code> if the element is an XML-Schema datatype: <code>"data"^^<http://www.w3.org/2000/01/rdf-schema#datatype></code>
	 * or <code>"data"^^xsd:datatype</code> 
	 */
	public boolean isXMLSchemaDatatypeLiteral(String element) {
		if (this.hasContent(element)) {
			// take care of CDATA
			String elementContent = element;
			if (this.isCDATA(elementContent)) {
				elementContent = this.getCDATAContent(elementContent);
			}
			
			if (this.hasContent(elementContent)) {
				if (elementContent.contains(XSDDATATYPE_SEPARATOR)) { // seems to be a xml schema datatype literal. lets check if true
					String data = elementContent.substring(0, elementContent.lastIndexOf(XSDDATATYPE_SEPARATOR));
					String datatype = elementContent.substring(elementContent.lastIndexOf(XSDDATATYPE_SEPARATOR) + XSDDATATYPE_SEPARATOR.length());
					
					// all data should start with quotes 
					if (data.length() > 0 && (!data.startsWith(QUOTES) || !data.endsWith(QUOTES))) {
						return false;
					}				
				
					// check datatype part
					if (datatype.length() > 0 && datatype.startsWith(OPENINGTAG) && datatype.endsWith(CLOSINGTAG)) {
						datatype = datatype.substring(1, datatype.lastIndexOf(CLOSINGTAG));
						if (this.isURL(datatype) && datatype.contains("#")) { // simply check if there is a URL
							return true; // take into account that we do not check if defined datatype is available
						}
					} else { // some prefix:element formatted datatype
						return this.isNSPrefixFormat(datatype);
					}
				}
			}
		}
			
		return false;		
	}
	
	/**
	 * Determines if the element is an XML-Schema datatype 
	 * @param element the element to check
	 * @return <code>true</code> if the element is an XML-Schema lang: <code>""@lang</code>
	 */
	public boolean isXMLSchemaLangLiteral(String element) {
		if (this.hasContent(element)) {
			// take care of CDATA
			String elementContent = element;
			if (this.isCDATA(elementContent)) {
				elementContent = this.getCDATAContent(elementContent);
			}
			
			if (this.hasContent(elementContent)) {
				if (elementContent.contains(XSDLANG_SEPARATOR)) { // seems to be a xml schema lang. lets check if true
					String data = elementContent.substring(0, elementContent.lastIndexOf(XSDLANG_SEPARATOR));
					String lang = elementContent.substring(elementContent.lastIndexOf(XSDLANG_SEPARATOR) + XSDLANG_SEPARATOR.length());
					
					// all data should start with quotes 
					if (data.length() > 0 && (!data.startsWith(QUOTES) || !data.endsWith(QUOTES))) {
						return false;
					}
					
					// check lang part
					if (lang.length() > 0) {
						return true;
					}
				}
			}
		}
		return false;		
	}
	
	/**
	 * Gets the content of a xml-schema 
	 * @param element the element to get the literal
	 * @return the content of the literal
	 */
	public String getXMLSchemaContent(String element) {
		if (this.hasContent(element)) {
			// take care of CDATA
			String elementContent = element;
			if (this.isCDATA(elementContent)) {
				elementContent = this.getCDATAContent(elementContent);
			}
			
			if (this.hasContent(elementContent)) {
				String data = elementContent;
				if (elementContent.contains(XSDLANG_SEPARATOR)) { // seems to be a xml schema lang. lets check if true
					data = elementContent.substring(0, elementContent.lastIndexOf(XSDLANG_SEPARATOR));
				} else if (elementContent.contains(XSDDATATYPE_SEPARATOR)) { // seems to be a xml schema datatype literal. lets check if true
					data = elementContent.substring(0, elementContent.lastIndexOf(XSDDATATYPE_SEPARATOR));
				}
					
				if (data.length() > 0) {
					if (data.startsWith(QUOTES) && data.endsWith(QUOTES)) {
						return data.substring(data.indexOf(QUOTES)+QUOTES.length(), data.lastIndexOf(QUOTES));
					} else { // simply return the data
						return data;
					}
				}
			}
		}
		return null;		
	}
	
	/**
	 * Gets the datatype of a xml-schema formatted pattern
	 * @param element the element to get the datatype
	 * @return the content of the datatype
	 */
	public String getXMLSchemaDatatype(String element) {
		if (this.hasContent(element)) {
			// take care of CDATA
			String elementContent = element;
			if (this.isCDATA(elementContent)) {
				elementContent = this.getCDATAContent(elementContent);
			}
			
			if (this.hasContent(elementContent)) {
				if (elementContent.contains(XSDDATATYPE_SEPARATOR)) { // seems to be a xml schema datatype literal. lets check if true
					String datatype = elementContent.substring(elementContent.lastIndexOf(XSDDATATYPE_SEPARATOR) + XSDDATATYPE_SEPARATOR.length());
					
					// check datatype part
					if (datatype.length() > 0 && datatype.startsWith(OPENINGTAG) && datatype.endsWith(CLOSINGTAG)) {
						datatype = datatype.substring(1, datatype.lastIndexOf(CLOSINGTAG));
						
						return datatype;

					} else if (this.isNSPrefixFormat(datatype)) {
						String datatypePrefix = datatype.substring(0, datatype.lastIndexOf(":"));
						String datatypeElement = datatype.substring(datatype.lastIndexOf(":")+1);
						
						if (datatypePrefix.equals(XSDRDFPREFIXDATATYPE_OPENINGTAG)) {
							StringBuilder sb = new StringBuilder();
							sb.append(XSDRDFDATATYPE_URL).append(datatypeElement);
							return sb.toString();
						} else {
							return datatype;
						}
					}
				}

			}
		}
		return "";		
	}
	
	/**
	 * Gets the language of a xml-schema formatted pattern
	 * @param element the element to get the lang
	 * @return the content of the lang
	 */
	public String getXMLSchemaLang(String element) {
		if (this.hasContent(element)) {
			// take care of CDATA
			String elementContent = element;
			if (this.isCDATA(elementContent)) {
				elementContent = this.getCDATAContent(elementContent);
			}
			
			if (this.hasContent(elementContent)) {
				if (elementContent.contains(XSDLANG_SEPARATOR)) { // seems to be a xml schema lang. lets check if true
					String lang = elementContent.substring(elementContent.lastIndexOf(XSDLANG_SEPARATOR) + XSDLANG_SEPARATOR.length());
					
					// check lang part
					if (lang.length() > 0) {
						return lang;
					}

				}

			}
		}
		return "";		
	}
	
	/**
	 * Determines if an element is defined in base to the rdf-schema dataypes
	 * @param element the element to check
	 * @return <code>true</code> if the element is an RDF-Schema datatype
	 */
	public boolean isRDFSchemaDatatypeLiteral(String element) {
		if (this.isXMLSchemaDatatypeLiteral(element)) {
			String datatype = this.getXMLSchemaDatatype(element);
			if (datatype.startsWith(XSDRDFPREFIXDATATYPE_OPENINGTAG) || 
					datatype.startsWith(XSDRDFDATATYPE_URL)) {
				return true;
			}
		}
		
		return false;
	}

	/**
	 * Encloses the incoming parameter in CDATA
	 * @param element a element to enclose in CDATA
	 * @return the incoming param enclosed in CDATA
	 */
	public String encloseInCDATA(String element) {
		StringBuilder sb = new StringBuilder();
		sb.append(CDATA_OPENINGTAG);
		
		if (this.hasContent(element)) {
			if (this.isCDATA(element)) { // if it was already enclosed in CDATA, return the value
				return element;
			} else {
				sb.append(element);
			}
		}
		
		sb.append(CDATA_CLOSINGTAG);
		
		return sb.toString();
	}

	/**
	 * Gets the element defining an ontology resource
	 * @param element the url in what is defined the element
	 * @return the latest part of the element
	 */
	public String getURLFormatElement(String element) {
		if (this.hasContent(element)) {
			// take care of CDATA
			String elementContent = element;
			if (this.isCDATA(elementContent)) {
				elementContent = this.getCDATAContent(elementContent);
			}
			
			if (this.hasContent(elementContent)) {
				if (elementContent.contains(URLRESOURCE_DESCRIPTOR)) { // seems to be a xml schema lang. lets check if true
					String resource = elementContent.substring(elementContent.lastIndexOf(URLRESOURCE_DESCRIPTOR) + URLRESOURCE_DESCRIPTOR.length());
					
					// check lang part
					if (resource.length() > 0) {
						return resource;
					}

				}

			}
		}
		return null;		
	}
}
