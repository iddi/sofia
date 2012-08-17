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

package eu.sofia.adk.sib.model.request;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

import eu.sofia.adk.sib.exception.EncodingNotSupportedException;
import eu.sofia.adk.sib.exception.RDFM3ParseException;
import eu.sofia.adk.sib.exception.SSAPSyntaxException;
import eu.sofia.adk.sib.exception.SemanticModelException;
import eu.sofia.adk.sib.exception.TransformationException;
import eu.sofia.adk.sib.model.query.SOFIAResultSet;
import eu.sofia.adk.sib.protocol.m3.M3Parser;
import eu.sofia.adk.sib.protocol.m3.RDFM3;
import eu.sofia.adk.sib.protocol.m3.RDFM3Triple;
import eu.sofia.adk.sib.ssap.SIB;
import eu.sofia.adk.sib.util.EncodingFactory;
import eu.sofia.adk.ssapmessage.SSAPMessage;
import eu.sofia.adk.ssapmessage.SSAPMessage.TransactionType;
import eu.sofia.adk.ssapmessage.SSAPMessageRequest;
import eu.sofia.adk.ssapmessage.SSAPMessageResponse;
import eu.sofia.adk.ssapmessage.parameter.SSAPMessageParameter;
import eu.sofia.adk.ssapmessage.parameter.SSAPMessageRDFParameter;
import eu.sofia.adk.ssapmessage.parameter.SSAPMessageRDFParameter.TypeAttribute;
import eu.sofia.adk.ssapmessage.parameter.SSAPMessageStatusParameter;

/**
 * This class implements a SUBSCRIPTION REQUEST.
 * A <i>QUERY REQUEST</i> causes a query to be performed in the SIB.
 * The <i>&lt;query_type&gt;</i> can be the name of any supported query language, 
 * currently supported values are <i>RDF-M3</i>, meaning a triple template 
 * (with return value as <i>RDF-M3</i>), <i>WQL-X</i> for different <i>wql</i>
 * (wilbur query language) queries.
 * 
 * The format of <i>QUERY_STRING</i> and return values depend on the type of 
 * the query.
 * 
 * RDF-M3 template queries contain a description of a triple in RDF-M3, 
 * possibly containing a <i>sib:any</i> value as a subject, object, predicate, 
 * or any combination of these. The <i>sib:any</i> value is interpreted as a 
 * wildcard, matching any URI or literal.
 * 
 * @author Fran Ruiz
 * @see eu.sofia.adk.sib.model.request.Request
 */
public class QueryRequest extends Request {

	/** The logger */
	private static Logger logger;
	
	/**
	 * Constructor for synchronous QUERY requests
	 * @param request the SSAP Message request containing the QUERY Request
	 * @param sib the current SIB
	 */
	public QueryRequest(SSAPMessageRequest request, SIB sib) {
    	super(request, sib);

    	// Inits the logger
    	logger = Logger.getLogger(QueryRequest.class);	
	}
	
	/**
	 * Gets the request type
 	 * @return the TransactionType associated to this request (QUERY)
	 */
	@Override
	public TransactionType getRequestType() {
		return SSAPMessage.TransactionType.QUERY;
	}
	
	/**
	 * Process a single QUERY transaction.
	 * The QUERY CONFIRM message is sent as a confirmation for QUERY REQUEST 
	 * message. The node must handle the confirmation message. Parameter status 
	 * contains a status code reporting the success of the operation 
	 * (see description of status codes).
	 * 
	 * The parameter results contains the response obtained from the semantic
	 * manager.
	 * 
	 * @return SSAPMessageResponse with the response after processing the request. 
	 */
	public SSAPMessageResponse processRequest() {

		logger.debug("[" + sib.getName() + "] (" + request.getTransactionId()+ ") " +
				request.getTransactionType().getType() + " " + request.getMessageType().getType());

		// The response
		response = new SSAPMessageResponse(
				request.getNodeId(), request.getSpaceId(), 
				request.getTransactionId(), request.getTransactionType());

		session = sib.getSessionManager().getSession(request.getNodeId());
		
		if (session != null) { // if the session is not null
			SSAPMessageParameter queryParam = request.getParameter(SSAPMessageParameter.NameAttribute.QUERY);
			SSAPMessageParameter encodingParam = request.getEncodingParameter();
			
			if (queryParam == null) {
				SSAPMessageParameter param = new SSAPMessageStatusParameter(
						SSAPMessageStatusParameter.SIBStatus.STATUS_KP_MESSAGE_INCOMPLETE);
				
				response.addParameter(param);
			// REMOVED DUE TO INCOMPATIBILITY WITH PYTHON KP
			//} else if (requestParam instanceof SSAPMessageRDFParameter) {
			//	SSAPMessageRDFParameter rdfParam = (SSAPMessageRDFParameter) requestParam;
			} else {
				String query = queryParam.getContent();
				TypeAttribute encoding = null;
				
				if (queryParam instanceof SSAPMessageRDFParameter) {
					SSAPMessageRDFParameter rdfParam = (SSAPMessageRDFParameter) queryParam;
					encoding = rdfParam.getType();
				} 
				
				if (encodingParam != null && encoding == null) {
					encoding = TypeAttribute.findValue(encodingParam.getContent());
				}
				
				try {

					if (encoding == null || query == null) {
						throw new SSAPSyntaxException("Query or encoding is null");
					}
					
					String returnData = new String();
					if (encoding == TypeAttribute.RDFM3) {
						// allow the multiple query
						HashMap<String,String> sparqlQueries = new HashMap<String,String>();
						sparqlQueries.putAll(EncodingFactory.getInstance().getTransformer(encoding).toSPARQL(query));
						
						returnData = obtainQueryResults(sparqlQueries, query);
					} else {
						// if not rdf-m3 format, only one query is available
						String sparqlQuery = (String) EncodingFactory.getInstance().getTransformer(encoding).toSPARQL(query).toString();
						
						SOFIAResultSet results = sib.getSemanticModel().query(sparqlQuery);
						
						returnData = EncodingFactory.getInstance().getTransformer(encoding).toOriginalEncoding(query, results);
					}
					
					// Construct now the results parameter with obtained results
					SSAPMessageParameter param = new SSAPMessageParameter(
							SSAPMessageParameter.NameAttribute.RESULTS,
							returnData);

					response.addParameter(param);
					
					SSAPMessageParameter statusParam = new SSAPMessageStatusParameter(
							SSAPMessageStatusParameter.SIBStatus.STATUS_OK);
					
					response.addParameter(statusParam);					
				} catch (EncodingNotSupportedException ex) {
					ex.printStackTrace();
					SSAPMessageParameter statusParam = new SSAPMessageStatusParameter(
							SSAPMessageStatusParameter.SIBStatus.STATUS_NOT_IMPLEMENTED);
					
					response.addParameter(statusParam);					
				} catch (TransformationException ex) {
					ex.printStackTrace();
					SSAPMessageParameter statusParam = new SSAPMessageStatusParameter(
							SSAPMessageStatusParameter.SIBStatus.STATUS_KP_MESSAGE_SYNTAX);
					
					response.addParameter(statusParam);	
				} catch (SemanticModelException ex) {
					ex.printStackTrace();
					SSAPMessageParameter statusParam = new SSAPMessageStatusParameter(
							SSAPMessageStatusParameter.SIBStatus.STATUS_ERROR);
					
					response.addParameter(statusParam);	
				} catch (SSAPSyntaxException ex) {
					ex.printStackTrace();
					SSAPMessageParameter param = new SSAPMessageStatusParameter(
							SSAPMessageStatusParameter.SIBStatus.STATUS_KP_MESSAGE_SYNTAX);
					
					response.addParameter(param);					
				}
			}
				
		}
		
		return response;		
	}
	
	private String obtainQueryResults(HashMap<String,String> sparqlQueries, String query) throws TransformationException, SemanticModelException {
		
		try {
			M3Parser parser = new M3Parser();
			RDFM3 queryM3 = parser.parse(query);
			
			ArrayList<RDFM3Triple> resultTriples = new ArrayList<RDFM3Triple>();
			
			for (String sparqlQuery : sparqlQueries.keySet()) {
				SOFIAResultSet results = sib.getSemanticModel().query(sparqlQuery);
			
				String originalQuery = sparqlQueries.get(sparqlQuery);
				String queryResults = EncodingFactory.getInstance().getTransformer(TypeAttribute.RDFM3).toOriginalEncoding(originalQuery, results);
				
				RDFM3 m3Results = parser.parse(queryResults);
				
				if (m3Results.hasTriples()) {
					resultTriples.addAll(m3Results.listTriples());
				}
			}
			
			// add wildcardresults to m3
			RDFM3 m3 = new RDFM3();
			m3.addNamespaces(queryM3.listNamespaces());
			m3.addTriples(resultTriples);
			
			return m3.toString();
			
		} catch (RDFM3ParseException ex) {
			ex.printStackTrace();
			throw new TransformationException("Cannot parse due to " + ex.getMessage());
		} catch (TransformationException ex) {
			ex.printStackTrace();
			throw new TransformationException("Transformation from RDF-M3 into SPARQL was not successful");
		} catch (EncodingNotSupportedException ex) {
			ex.printStackTrace();
			throw new TransformationException("Encoding not supported: " + ex.getMessage());
		}			
	}	
}
