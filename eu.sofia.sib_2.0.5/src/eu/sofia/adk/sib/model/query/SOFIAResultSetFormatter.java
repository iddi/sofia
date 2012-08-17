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

package eu.sofia.adk.sib.model.query;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;

import eu.sofia.adk.sib.exception.TransformationException;
import eu.sofia.adk.sib.protocol.m3.RDFM3;
import eu.sofia.adk.sib.protocol.m3.RDFM3Triple;
import eu.sofia.adk.sib.protocol.m3.RDFM3Triple.TypeAttribute;

/**
 * This class is responsible for the formatting of the results obtained
 * after a query. The output formats are compatible with the ones previously
 * defined in <code>ResultSetFormatter</code> plus the formats needed for
 * SOFIA: RDF-XML (SPARQL-RESULTS), N3 and WQL.
 * 
 * @author Fran Ruiz, fran.ruiz@tecnalia.com, ESI
 *
 */
public class SOFIAResultSetFormatter {

	/**
	 * Default constructor
	 */
	public SOFIAResultSetFormatter() {
		
	}
	
	/**
	 * Returns a String representation of the results in text format
	 * @param results a SOFIAResultSet object
	 * @return a String representing a SPARQL-results
	 */
	public static String asText(SOFIAResultSet results) {
		String result = null;
		try {
			result = ResultSetFormatter.asText(results);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return result;
	}	
	
	/**
	 * Returns a String representation of the results in SPARQL-results
	 * @param results a SOFIAResultSet object
	 * @return a String representing a SPARQL-results
	 */
	public static String asXMLString(SOFIAResultSet results) {
		StringBuilder result = new StringBuilder();
		try {
			results.reset();
			
			result.append(getCDATAOpeningTag());
			result.append(ResultSetFormatter.asXMLString(results));
			result.append(getCDATAClosingTag());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return result.toString();
	}
	
	private static String getCDATAOpeningTag() {
		return "<![CDATA[";
	}

	private static String getCDATAClosingTag() {
		return "]]>";
	}
	
	/**
	 * Returns a String representation of the results in RDF-M3
	 * @param results a SOFIAResultSet object
	 * @return a String representing a RDF-M3 result
	 * @throws TransformationException 
	 */
	public static String asRDFM3(SOFIAResultSet results, RDFM3 query) throws TransformationException {
		try {
			results.reset();

			RDFM3 m3result = new RDFM3();
			String subjectData = null;
			TypeAttribute subjectType = null;
			String predicateData = null;
			String objectData = null;
			TypeAttribute objectType = null;
			
			m3result.addNamespaces(query.listNamespaces());
			
		    while (results.hasNext()) {
		    	QuerySolution soln = results.nextSolution() ;
		    	if (((RDFM3Triple)query.listTriples().toArray()[0]).isSubjectWildcard()) {
		    		RDFNode subject = soln.get("subject");       // Get a result variable by name.
		    		if (subject.isAnon()) {
		    			Resource subjectAnon = (Resource) subject;
		    			subjectData = subjectAnon.getId().getLabelString();
		    			subjectType = TypeAttribute.BNODE;
		    		} else if (subject.isResource() || 
		    				subject.isURIResource()) {
		    			Resource subjectURI = (Resource) subject;
		    			// TODO FJR Data following URI should be named using namespace prefixes
		    			subjectData = subjectURI.toString();
		    			subjectType = TypeAttribute.URI;
		    		} else {
		    			System.err.println("ERROR: CANNOT PROCESS SUBJECT WITH " + subject.toString());
		    		}
		    	} else { // the query is obtained from the query request
		    		if (((RDFM3Triple)query.listTriples().toArray()[0]).isSubjectURL()) {
			    		// TODO Include namespaces prefixes if defined
			    		subjectData = ((RDFM3Triple)query.listTriples().toArray()[0]).getSubject();
		    			subjectType = TypeAttribute.URI;	 
		    		} else {
			    		subjectData = ((RDFM3Triple)query.listTriples().toArray()[0]).getSubject();
			    	}
		    	}
		    	
		    	if (((RDFM3Triple)query.listTriples().toArray()[0]).isPredicateWildcard()) {
		    		RDFNode predicate = soln.get("predicate");       // Get a result variable by name.
		    		if (predicate.isResource() || 
		    				predicate.isURIResource()) {
		    			Resource predicateURI = (Resource) predicate;
		    			// TODO FJR Data following URI should be named using namespace prefixes
		    			predicateData = predicateURI.toString();
		    		} else {
		    			// TODO FJR This can be cause of error
		    			System.err.println("ERROR: CANNOT PROCESS PREDICATE WITH " + predicate.toString());
		    		}
		    	} else {
		    		// TODO Include namespaces prefixes if defined
		    		predicateData = ((RDFM3Triple)query.listTriples().toArray()[0]).getPredicate();
		    	}
	
		    	if (((RDFM3Triple)query.listTriples().toArray()[0]).isObjectWildcard()) {
		    		RDFNode object = soln.get("object");
		    		if (object.isLiteral()) {
		    			Literal objectLiteral = (Literal) object;
		    			// TBD: FJR Literal could contain attributes, such as datatype (e.g. ^^xsd:string) or language (e.g. @en). Use literal.getDatatype() in order to obtain a valid RDFDatatype
		    			StringBuilder sb = new StringBuilder();
		    			sb.append(objectLiteral.getLexicalForm()); // to be configured
		    			objectData = sb.toString();
		    			objectType = TypeAttribute.LITERAL;
		    		} else if (object.isAnon()) {
		    			Resource objectAnon = (Resource) object;
		    			objectData = objectAnon.getId().getLabelString();
		    			objectType = TypeAttribute.BNODE;
		    		} else if (object.isResource() ||
		    				object.isURIResource()) {
		    			Resource objectURI = (Resource) object;
		    			// TODO FJR Data following URI should be named using namespace prefixes
		    			objectData = objectURI.toString();
		    			objectType = TypeAttribute.URI;
		    		} else {
		    			// TODO FJR This can be cause of error
		    			System.err.println("ERROR: CANNOT PROCESS OBJECT WITH " + object.toString());
		    		}
		    	} else { // the query is obtained from the query request
		    		if (((RDFM3Triple)query.listTriples().toArray()[0]).isObjectURL()) {
			    		// TODO Include namespaces prefixes if defined
			    		objectData = ((RDFM3Triple)query.listTriples().toArray()[0]).getObject();
			    		objectType = TypeAttribute.URI;
		    		} else 	if (((RDFM3Triple)query.listTriples().toArray()[0]).isObjectNsPrefixFormat()) {
				    		// TODO Include namespaces prefixes if defined
				    		objectData = ((RDFM3Triple)query.listTriples().toArray()[0]).getObject();
				    		objectType = TypeAttribute.URI;
			    	} else { // the data must be a CDATA object, due to it is for sure a literal
			    		objectData = ((RDFM3Triple)query.listTriples().toArray()[0]).getObjectContent();
			    		objectType = TypeAttribute.LITERAL;
			    	}
		    	}
		    	RDFM3Triple triple = new RDFM3Triple(subjectData, subjectType, predicateData, objectData, objectType);
		    	m3result.addTriple(triple);
		    }
	
			return m3result.toString();
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new TransformationException("Cannot process query formatting due to " + ex.getMessage());
		}
	}
	
	/**
	 * Returns a String representation of the results in WQL
	 * @param results a SOFIAResultSet object
	 * @return a String representing a WQL result set
	 */
	public static String asWQL(SOFIAResultSet results) {
		
		return "";
	}	
	
}
