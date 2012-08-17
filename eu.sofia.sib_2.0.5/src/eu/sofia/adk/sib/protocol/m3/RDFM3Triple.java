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

/**
 * Represents a triple defined in a M3 format
 * 
 * @author Fran Ruiz, Fran.Ruiz@tecnalia.com, Tecnalia - Software Systems Engineering
 */
public class RDFM3Triple {

	/**
	 * Determines the possible element types allowed for attributes
	 * in subject and object of an RDF-M3 triple
	 */
	public enum TypeAttribute {
		URI			("URI"),
		LITERAL		("literal"),
		BNODE		("bNode");

		/** The attribute value */
		private final String type;
		
		/**
		 * Constructor of AttributeType
		 * @param type the type
		 */
		TypeAttribute(String type) {
			this.type = type;
		}
		
		/**
		 * Obtains the value
		 * @return the value of element type
		 */
		public String getType() {
			return type;
		}
		
		/**
		 * Finds a determinate type attribute
		 * @param v the value to find
		 * @return the associated entity
		 */
		public static TypeAttribute findType(String v) {
			for(TypeAttribute ta : values()) {
				if(ta.getType().equalsIgnoreCase(v)) {
					return ta;
				}
			}
			return null;
		}
	}
	
	/** Triple values */
	private String subject;
	private TypeAttribute subjectType;
	private String predicate;
	private String object;
	private TypeAttribute objectType;
	
	/** Useful tags */
	private static final String TRIPLE_OPENINGTAG = "<triple>";
	private static final String TRIPLE_CLOSINGTAG = "</triple>";
	private static final String SUBJECT_OPENINGTAG = "<subject";
	private static final String SUBJECT_CLOSINGTAG = "</subject>";
	private static final String PREDICATE_OPENINGTAG = "<predicate>";
	private static final String PREDICATE_CLOSINGTAG = "</predicate>";
	private static final String OBJECT_OPENINGTAG = "<object";
	private static final String OBJECT_CLOSINGTAG = "</object>";
	private static final String TYPE_ATTRIBUTE = "type='";
		
	private static final String CR = "\n";
	
	public static final String WILDCARD = RDFM3Util.WILDCARD;
	
	/** The utility for processing content */
	private RDFM3Util m3Util = new RDFM3Util();
	
	/**
	 * Constructor
	 * @param subject the subject element content
	 * @param predicate the predicate element content
	 * @param object the object element content
	 */
	public RDFM3Triple(String subject, String predicate, String object) {
		if (m3Util.isURL(subject)) {
			this.subjectType = TypeAttribute.URI;
		} else if (m3Util.isBNode(subject)) {
			this.subjectType = TypeAttribute.BNODE;
		} else {
			this.subjectType = null;
		}
		this.subject = subject;
		
		this.predicate = predicate;

		if (m3Util.isURL(object)) {
			this.objectType = TypeAttribute.URI;
			this.object = object;
		} else if (m3Util.isBNode(object)) {
			this.objectType = TypeAttribute.BNODE;
			this.object = object;
		} else if (m3Util.isWildcard(object)) {
			this.objectType = null;
			this.object = object;
		} else if (m3Util.isCDATA(object)) {
			this.objectType = TypeAttribute.LITERAL;
			this.object = object;
		} else if (m3Util.isNSPrefixFormat(object)) {
			this.objectType = TypeAttribute.URI;
			this.object = object;
		} else { // literal 
			this.objectType = TypeAttribute.LITERAL;
			this.object = m3Util.encloseInCDATA(object);
		}		
	}

	/**
	 * Constructor
	 * @param subject the subject element content
	 * @param subjectType the subject type
	 * @param predicate the predicate element content
	 * @param object the object element content
	 * @param objectType the subject type
	 */	
	public RDFM3Triple(String subject, TypeAttribute subjectType,
			String predicate,
			String object, TypeAttribute objectType) {
		// note that no checking if done for this constructor
		this.subject = subject;
		if (subjectType != null) {
			this.setSubjectType(subjectType);
		}
		this.predicate = predicate;
		
		// literal checking and CDATA enclosing
		String objectToSet = object;
		if (objectType != null && objectType == TypeAttribute.LITERAL && !m3Util.isCDATA(object)) {
			objectToSet = m3Util.encloseInCDATA(object);
		}
		this.object = objectToSet;
		
		if (objectType != null) {
			this.setObjectType(objectType);
		}
	}	
	
	/**
	 * Gets the subject of the current triple
	 * @return the content of the triple subject
	 */
	public String getSubject() {
		return subject;
	}
	
	/**
	 * Determines if the subject type is a URL
	 * @return <code>true</code> if the subject type is set to 'URI' or if the subject content
	 */
	public boolean isSubjectTypeURL() {
		if (this.getSubjectType() != null) { // the subject type is set
			if (this.getSubjectType() == TypeAttribute.URI) {
				return true;
			}
		}
		return false; // otherwise, return false
	}
	
	/**
	 * Determines if the subject is a URL
	 * @return <code>true</code> if the subject type is set to 'URI' or if the subject content
	 */
	public boolean isSubjectURL() {
		return m3Util.isURL(subject);
	}	
	
	
	/**
	 * Determines if the subject is has a wildcard pattern
	 * @return <code>true</code> if the subject type has a wildcard pattern (e.g. <code>'sib:any'</code>)
	 */
	public boolean isSubjectWildcard() {
		return m3Util.isWildcard(subject);
	}		
	
	/**
	 * Determines if the subject is a URL
	 * @return <code>true</code> if the subject type is set to 'URI' or if the subjectchu content matches with a bnode pattern
	 */
	public boolean isSubjectTypeBNode() {
		if (this.getSubjectType() != null) { // the subject type is set
			if (this.getSubjectType() == TypeAttribute.BNODE) {
				return true;
			}
		}
		return false; // otherwise, return false		return false; // otherwise, return false
	}	
	
	/**
	 * Determines if the subject is a URL
	 * @return <code>true</code> if the subject type is set to 'URI' or if the subjectchu content matches with a bnode pattern
	 */
	public boolean isSubjectBNode() {
		return m3Util.isBNode(subject);
	}		
	
	/**
	 * Determines if the subject has a namespace prefix format
	 * @return <code>true</code> if the subject type has a namespace prefix format.
	 * note that namespace prefix format does not says that the prefix 
	 */	
	public boolean isSubjectNsPrefixFormat() {
		return m3Util.isNSPrefixFormat(subject);
	}

	/**
	 * Gets the subject prefix. the subject should be in namespace prefix format (e.g. <code>prefix:element</code>. 
	 * @return the prefix corresponding to the subject
	 */	
	public String getSubjectPrefix() {
		return m3Util.getNSPrefixFormatPrefix(subject);
	}	

	/**
	 * Gets the subject prefix. the subject should be in namespace prefix format (e.g. <code>prefix:element</code>. 
	 * @return the prefix corresponding to the subject
	 */	
	public String getSubjectElement() {
		if (m3Util.isURL(subject)) {
			return m3Util.getURLFormatElement(subject);
		} else if (m3Util.isNSPrefixFormat(subject)) {
			return m3Util.getNSPrefixFormatElement(subject);
		}
		return null;
	}

	/**
	 * Gets the subject type of the current triple
	 * @return the subject type. if it is not set, returns <code>null</code>
	 */
	public TypeAttribute getSubjectType() {
		return subjectType;
	}

	/**
	 * Set the subject type of the current triple. note that only bNode and URI types are allowed
	 * @param type the type attribute to set to the subject
	 */
	public boolean setSubjectType(TypeAttribute type) {
		if (type != null && (type == TypeAttribute.BNODE || type == TypeAttribute.URI)) {
			this.subjectType = type;
			return true;
		}
		return false;
	}		
	
	/**
	 * Gets the predicate of the current triple
	 * @return the content of the triple predicate
	 */
	public String getPredicate() {
		return predicate;
	}		
	
	/**
	 * Determines if the predicate is a URL
	 * @return if the predicate content matches with a URL format
	 */
	public boolean isPredicateURL() {
		return m3Util.isURL(predicate);
	}
	
	/**
	 * Determines if the predicate is has a wildcard pattern
	 * @return <code>true</code> if the predicate type has a wildcard pattern (e.g. <code>'sib:any'</code>)
	 */
	public boolean isPredicateWildcard() {
		return m3Util.isWildcard(predicate);
	}		

	/**
	 * Determines if the predicate has a namespace prefix format
	 * @return <code>true</code> if the predicate has a namespace prefix format.
	 * note that namespace prefix format does not says that the prefix 
	 */	
	public boolean isPredicateNsPrefixFormat() {
		return m3Util.isNSPrefixFormat(predicate);
	}

	/**
	 * Gets the predicate prefix. the predicate should be in namespace prefix format (e.g. <code>prefix:element</code>. 
	 * @return the prefix corresponding to the predicate
	 */	
	public String getPredicatePrefix() {
		return m3Util.getNSPrefixFormatPrefix(predicate);
	}	

	/**
	 * Gets the predicate prefix. the predicate should be in namespace prefix format (e.g. <code>prefix:element</code>. 
	 * @return the prefix corresponding to the predicate
	 */	
	public String getPredicateElement() {
		if (m3Util.isURL(predicate)) {
			return m3Util.getURLFormatElement(predicate);
		} else if (m3Util.isNSPrefixFormat(predicate)) {
			return m3Util.getNSPrefixFormatElement(predicate);
		}
		return null;
	}		
	
	/**
	 * Gets the object of the current triple
	 * @return the content of the triple object
	 */
	public String getObject() {
		return object;
	}		
	
	/**
	 * Determines if the object is a URL
	 * @return <code>true</code> if the object type is set to 'URI' or if the object content
	 */
	public boolean isObjectTypeURL() {
		if (this.getObjectType() != null) { // the subject type is set
			if (this.getObjectType() == TypeAttribute.URI) {
				return true;
			}
		}
		return false; // otherwise, return false
	}	
	
	/**
	 * Determines if the object is a URL
	 * @return <code>true</code> if the object type is set to 'URI' or if the object content
	 */
	public boolean isObjectURL() {
		return m3Util.isURL(object);
	}
	
	/**
	 * Determines if the object is has a wildcard pattern
	 * @return <code>true</code> if the object has a wildcard pattern (e.g. <code>'sib:any'</code>)
	 */
	public boolean isObjectWildcard() {
		return m3Util.isWildcard(object);
	}		
	
	/**
	 * Determines if the object type is a URL
	 * @return <code>true</code> if the object type is set to 'bNode' or if the object content matches with a bnode pattern
	 */
	public boolean isObjectTypeBNode() {
		if (this.getObjectType() != null) { // the subject type is set
			if (this.getObjectType() == TypeAttribute.BNODE) {
				return true;
			}
		}
		return false; // otherwise, return false
	}	
	
	/**
	 * Determines if the object is a URL
	 * @return <code>true</code> if the object type is set to 'bNode' or if the object content matches with a bnode pattern
	 */
	public boolean isObjectBNode() {
		return m3Util.isBNode(object);
	}		
	
	/**
	 * Determines if the object has a namespace prefix format
	 * @return <code>true</code> if the object type has a namespace prefix format.
	 * note that namespace prefix format does not says that the prefix 
	 */	
	public boolean isObjectNsPrefixFormat() {
		return m3Util.isNSPrefixFormat(object);
	}

	/**
	 * Gets the object prefix. the object should be in namespace prefix format (e.g. <code>prefix:element</code>. 
	 * @return the prefix corresponding to the object
	 */	
	public String getObjectPrefix() {
		return m3Util.getNSPrefixFormatPrefix(object);
	}	

	/**
	 * Gets the object element. the object should be in namespace prefix format (e.g. <code>prefix:element</code>. 
	 * @return the prefix corresponding to the object
	 */	
	public String getObjectElement() {
		if (m3Util.isURL(object)) {
			return m3Util.getURLFormatElement(object);
		} else if (m3Util.isNSPrefixFormat(object)) {
			return m3Util.getNSPrefixFormatElement(object);
		}
		return null;
	}		
	
	/**
	 * Determines if the object is a literal
	 * @return <code>true</code> if the object type is set to 'literal'
	 */
	public boolean isObjectTypeLiteral() {
		if (this.getObjectType() != null) { // the subject type is set
			if (this.getObjectType() == TypeAttribute.LITERAL) {
				return true;
			}
		}
		return false; // otherwise, return false
	}	
	
	/**
	 * Determines if the object is a literal
	 * @return <code>true</code> if the object type is set to 'literal'
	 */
	public boolean isObjectLiteral() {
		return m3Util.isCDATA(object);
	}
	
	/**
	 * Gets the content of an object literal
	 * @return the content of the object
	 */
	public String getObjectContent() {
		return m3Util.getCDATAContent(object); // the object literal should always be enclosed between CDATA
	}	
	
	/**
	 * Gets the content of an object literal
	 * @return the content of the object
	 */
	public String getObjectDataContent() {
		String content = m3Util.getCDATAContent(object); // the object literal should always be enclosed between CDATA
		if (m3Util.hasContent(content)) {
			/**
			 * Three possible cases
			 * 1. Raw data (e.g., a number or a simple string)
			 * 2. Data with reference to the datatype (e.g. "something"^^xsd:string)
			 * 3. Data with reference to lang (e.g. "something"@lang)
			 */
			if (m3Util.isXMLSchemaFormatLiteral(content)) {
				return m3Util.getXMLSchemaContent(content);
			} else {
				return content;
			}
		} else {
			return object;
		}
	}
	
	
	/**
	 * Gets the datatye of an object literal
	 * @return the datatype of the object
	 */
	public String getObjectDatatype() {
		if (m3Util.hasContent(object)) {
			if (m3Util.isXMLSchemaFormatLiteral(object)) {
				return m3Util.getXMLSchemaDatatype(object);
			}
		}
		return "";
	}	
	
	/**
	 * Gets the lang of an object literal
	 * @return the lang of the object
	 */
	public String getObjectLang() {
		if (m3Util.hasContent(object)) {
			if (m3Util.isXMLSchemaFormatLiteral(object)) {
				return m3Util.getXMLSchemaLang(object);
			}
		}
		return "";
	}		
	
	/**
	 * Gets the object type of the current triple
	 * @return the object type. if it is not set, returns <code>null</code>
	 */
	public TypeAttribute getObjectType() {
		return objectType;
	}
	
	/**
	 * Set the subject type of the current triple. note that only bNode and URI types are allowed
	 * @param type the type attribute to set to the subject
	 */
	public boolean setObjectType(TypeAttribute type) {
		if (type != null && (type == TypeAttribute.BNODE || type == TypeAttribute.URI || type == TypeAttribute.LITERAL)) {
			this.objectType = type;
			return true;
		}
		return false;
	}	
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(TRIPLE_OPENINGTAG).append(CR);
		sb.append(SUBJECT_OPENINGTAG);
		if (subjectType != null) {
			sb.append(" ").append(TYPE_ATTRIBUTE);
			sb.append(subjectType.getType());
			sb.append("'").append(">");
		} else {
			sb.append(">");
		}
		sb.append(this.getSubject());
		sb.append(SUBJECT_CLOSINGTAG).append(CR);
		sb.append(PREDICATE_OPENINGTAG).append(predicate).append(PREDICATE_CLOSINGTAG).append(CR);
		sb.append(OBJECT_OPENINGTAG);
		if (objectType != null) {
			sb.append(" ").append(TYPE_ATTRIBUTE);
			sb.append(objectType.getType());
			sb.append("'").append(">");
		} else {
			sb.append(">");
		}
		sb.append(this.getObject());
		sb.append(OBJECT_CLOSINGTAG).append(CR);

		sb.append(TRIPLE_CLOSINGTAG).append(CR);
		
		return sb.toString();
	}
	
	/**
	 * Returns if the datatype has ns-prefix format
	 * @param datatype a datatype in <code>prefix:element</code> format
	 * @return the prefix corresponding to the input parameter. <code>null</code> if not found
	 */
	protected boolean isNSPrefixFormat(String element) {
		if (m3Util.hasContent(element)) {
			 return m3Util.isNSPrefixFormat(element);
		} 
		
		return false;
	}	
	
	/**
	 * Returns the datatype prefix (if exists)
	 * @param datatype a datatype in <code>prefix:element</code> format
	 * @return the prefix corresponding to the input parameter. <code>null</code> if not found
	 */
	protected String getNSPrefixFormatPrefix(String element) {
		if (m3Util.hasContent(element)) {
			if (m3Util.isNSPrefixFormat(element)) {
				return m3Util.getNSPrefixFormatPrefix(element);
			}
		}
		return null;
	}
	
	/**
	 * Returns the datatype prefix (if exists)
	 * @param datatype a datatype in <code>prefix:element</code> format
	 * @return the prefix corresponding to the input param. <code>null</code> if not found
	 */
	protected String getNSPrefixFormatElement(String element) {
		if (m3Util.hasContent(element)) {
			if (m3Util.isNSPrefixFormat(element)) {
				return m3Util.getNSPrefixFormatElement(element);
			}
		}
		return null;
	}

	protected boolean isURL(String element) {
		if (m3Util.hasContent(element)) {
			 return m3Util.isURL(element);
		} 
		
		return false;
	}

	/**
	 * Determines if the triple has at least one wildcard in
	 * subject, predicate or object
	 * @return <code>true</code> if the subject, predicate or object are wildcards
	 */
	public boolean containsWildcards() {
		return isSubjectWildcard() || isPredicateWildcard() || isObjectWildcard();
	}	
}
