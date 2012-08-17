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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;

import eu.sofia.adk.sib.exception.EncodingNotSupportedException;
import eu.sofia.adk.sib.exception.RDFM3ParseException;
import eu.sofia.adk.sib.exception.SSAPSyntaxException;
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
 * This class is a request for executing an REMOVE REQUEST
 * The triples included in the parameter <i>triples</i> will be removed from the SIB. 
 * The assertion is atomic, that is, if there are multiple triples, all of them 
 * will be removed before any other triples will be asserted or any queries made.
 * The triples must be serialized as <i>RDF-XML</i> or the M3 internal format, 
 * <i>RDF-M3</i>. There may be multiple instances of parameter <i>triples</i>. 
 * Any subject or object of removed triples must not be of type <i>bNode</i>.
 * 
 * The parameter <i>confirm</i> determines whether the SIB sends a confirmation for 
 * the request. If <i>TRUE</i>, a confirmation is sent, if <i>FALSE</i>, a confirmation is 
 * not sent. Parameter is optional, default value is <i>TRUE</i>.
 * 
 * @author Fran Ruiz, ESI
 * @see eu.sofia.adk.sib.model.request.NotifiableRequest
 */
public class RemoveRequestwWildcards extends NotifiableRequest {

	/** The logger */
	private static Logger logger;
	
	/**
	 * Constructor for synchronous REMOVE requests
	 * @param message the SSAP Message request containing the REMOVE Request
	 * @param sib the current SIB
	 */
	public RemoveRequestwWildcards(SSAPMessageRequest message, SIB sib) {
    	super(message, sib);

    	// Inits the logger
    	logger = Logger.getLogger(RemoveRequestwWildcards.class);
	}
	
	/**
	 * Gets the request type
	 * @return the TransactionType associated to this request (REMOVE)
	 */
	@Override
	public TransactionType getRequestType() {
		return SSAPMessage.TransactionType.REMOVE;
	}
	
	
	/**
	 * Process a single REMOVE transaction.
	 * The <i>REMOVE CONFIRM</i> message is sent as a confirmation for 
	 * <i>REMOVE REQUEST</i> message. If confirmation was requested by node, 
	 * the node must handle the confirmation message. Parameter status contains 
	 * a status code reporting the success of the operation.
	 * 
	 * @return SSAPMessageResponse with the response after processing the request. <code>null</code> if no response is required by the KP
	 */
	public SSAPMessageResponse processRequest() {
		boolean confirm = false;
		
		logger.debug("[" + sib.getName() + "] (" + request.getTransactionId()+ ") " +
				request.getTransactionType().getType() + " " + request.getMessageType().getType());
		
		SSAPMessageParameter confirmParam = request.getParameter(SSAPMessageParameter.NameAttribute.CONFIRM);
		if (confirmParam != null && confirmParam.getContent().equals(SSAPMessageParameter.TRUE)) {
			confirm = true;
		}
		
		// The response
		response = new SSAPMessageResponse(
				request.getNodeId(), request.getSpaceId(), 
				request.getTransactionId(), request.getTransactionType());

		session = sib.getSessionManager().getSession(request.getNodeId());
		
		if (session != null) { // if the session is not null

			SSAPMessageParameter requestParam = request.getParameter(SSAPMessageParameter.NameAttribute.REMOVEGRAPH);
			SSAPMessageParameter encodingParam = request.getEncodingParameter();
			
			if (requestParam == null) {
				SSAPMessageParameter param = new SSAPMessageStatusParameter(
						SSAPMessageStatusParameter.SIBStatus.STATUS_KP_MESSAGE_INCOMPLETE);
				
				response.addParameter(param);
				
			} else {
				String removeGraph = requestParam.getContent();
				TypeAttribute encoding = null;
				
				if (requestParam instanceof SSAPMessageRDFParameter) {
					SSAPMessageRDFParameter rdfParam = (SSAPMessageRDFParameter) requestParam;
					encoding = rdfParam.getType();
				}
				
				if (encodingParam != null && encoding == null) {
					encoding = TypeAttribute.findValue(encodingParam.getContent());
				}
				
				try {
					if (encoding == null || removeGraph == null) {
						throw new SSAPSyntaxException("Remove graph or encoding is null");
					}
					
					// Creates a new model
					OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);

					String removeContent = null;
					String modelEncoding = null;

					// TODO support for wildcards
					try {
						String newGraph = checkWildcards(encoding, removeGraph);
						System.out.println(newGraph);
						if (newGraph != null && !newGraph.equals("")) {
							removeGraph = newGraph;
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					removeContent = EncodingFactory.getInstance().getTransformer(encoding).toRDFXML(removeGraph);
					if (removeContent != null) {
						modelEncoding = "RDF/XML";
					} else {
						removeContent = EncodingFactory.getInstance().getTransformer(encoding).toNTriples(removeGraph);
						modelEncoding = "N-TRIPLE";
					}
					
					// Transformation of the String to a InputStream
					ByteArrayInputStream is = new ByteArrayInputStream(removeContent.getBytes()); 
					
					model.read(is, null, modelEncoding);
						
					// Closes the input stream
					if (is != null) { 
						is.close(); 
					}					
					
					ArrayList<Statement> removedStatements = sib.getSemanticModel().removeModel(request.getNodeId(), model);				
					
					if (removedStatements.size() > 0) {
						// Notifies the model
						sib.getViewerListenerManager().removeStatements(request.getNodeId(), removedStatements, model.getNsPrefixMap());

						SSAPMessageParameter param = new SSAPMessageStatusParameter(
								SSAPMessageStatusParameter.SIBStatus.STATUS_OK);
						response.addParameter(param);
					} else {
						SSAPMessageParameter param = new SSAPMessageStatusParameter(
								SSAPMessageStatusParameter.SIBStatus.STATUS_ERROR);
						response.addParameter(param);
					}
					
				} catch (TransformationException ex) {
					ex.printStackTrace();
					SSAPMessageParameter statusParam = new SSAPMessageStatusParameter(
							SSAPMessageStatusParameter.SIBStatus.STATUS_KP_MESSAGE_SYNTAX);
					
					response.addParameter(statusParam);	
				} catch (EncodingNotSupportedException ex) {
					ex.printStackTrace();
					SSAPMessageParameter statusParam = new SSAPMessageStatusParameter(
							SSAPMessageStatusParameter.SIBStatus.STATUS_NOT_IMPLEMENTED);
					
					response.addParameter(statusParam);							
				} catch (IOException e) { 
					logger.error("Unable to close input stream while retrieving rdf");

					SSAPMessageParameter param = new SSAPMessageStatusParameter(
							SSAPMessageStatusParameter.SIBStatus.STATUS_KP_MESSAGE_SYNTAX);
					
					response.addParameter(param);
				} catch (SSAPSyntaxException ex) {
					ex.printStackTrace();
					SSAPMessageParameter param = new SSAPMessageStatusParameter(
							SSAPMessageStatusParameter.SIBStatus.STATUS_KP_MESSAGE_SYNTAX);
					
					response.addParameter(param);	
				} 
			}
		}
		
		if (confirm) {
			return response;
		} else {
			return null;
		}
	}

	private String checkWildcards(TypeAttribute encoding, String s) throws Exception {
		
		ArrayList<RDFM3Triple> wildcardResults = new ArrayList<RDFM3Triple>();
		
		try {
			if (encoding != TypeAttribute.RDFM3) {
				return null;
			}
			
			// Parses the input string
			M3Parser parser = new M3Parser();
			RDFM3 m3 = parser.parse(s);
			
			// if the m3 contains wildcards
			if (!m3.containsWildcards()) {
				return null;
			}
			ArrayList<RDFM3Triple> wildcardTriples = new ArrayList<RDFM3Triple>();
			wildcardTriples.addAll(m3.getTriplescontainingWildcards());
			m3.removeTriples(wildcardTriples);
			
			// create the wildcards RDFM3
			RDFM3 wildcardM3 = new RDFM3();	
			wildcardM3.addNamespaces(m3.listNamespaces());
			wildcardM3.addTriples(wildcardTriples);
			
			HashMap<String,String> sparqlQueries = new HashMap<String,String>();
			sparqlQueries.putAll(EncodingFactory.getInstance().getTransformer(TypeAttribute.RDFM3).toSPARQL(wildcardM3.toString()));
			
			for (String sparqlQuery : sparqlQueries.keySet()) {
				SOFIAResultSet results = sib.getSemanticModel().query(sparqlQuery);
			
				// back to RDF-M3 format
				String originalQuery = sparqlQueries.get(sparqlQuery);
				String queryResults = EncodingFactory.getInstance().getTransformer(encoding).toOriginalEncoding(originalQuery, results);
				
				RDFM3 m3Results = parser.parse(queryResults);
				
				if (m3Results.hasTriples()) {
					wildcardResults.addAll(m3Results.listTriples());
				}
			}
			
			// add wildcardresults to m3
			m3.addTriples(wildcardResults);

			return m3.toString();

		} catch (RDFM3ParseException ex) {
			ex.printStackTrace();
			throw new Exception("Cannot parse due to " + ex.getMessage());
		} catch (TransformationException ex) {
			ex.printStackTrace();
			throw new Exception("Transformation from RDF-M3 into SPARQL was not successful");
		} catch (EncodingNotSupportedException ex) {
			ex.printStackTrace();
			throw new Exception("Encoding not supported: " + ex.getMessage());
		}			
	}
}
