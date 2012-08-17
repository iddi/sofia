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
import eu.sofia.adk.ssapmessage.parameter.SSAPMessageBNodesParameter;
import eu.sofia.adk.ssapmessage.parameter.SSAPMessageParameter;
import eu.sofia.adk.ssapmessage.parameter.SSAPMessageRDFParameter;
import eu.sofia.adk.ssapmessage.parameter.SSAPMessageRDFParameter.TypeAttribute;
import eu.sofia.adk.ssapmessage.parameter.SSAPMessageStatusParameter;

/**
 * The triples included in the parameter <i>inserted_triples</i> will be asserted 
 * in the SIB and triples included in the parameter <i>removed_triples</i> will be 
 * removed from the SIB . The assertion and removal are atomic, that is, 
 * if there are multiple triples, all of them will be asserted and removed before 
 * any other triples will be asserted or any queries made. The triples must be 
 * serialized as RDF-XML or the M3 internal format, RDF-M3. 
 * There may be multiple instances of parameter <i>triples</i>. 
 * 
 * The parameter <i>confirm</i> determines whether the SIB sends a confirmation 
 * for the request. If <i>TRUE</i>, a confirmation is sent, if <i>FALSE</i>, a 
 * confirmation is not sent. Parameter is optional, default value is <i>FALSE</i>.
 * 
 * @author Fran Ruiz, ESI
 * @see eu.sofia.adk.sib.model.request.Request
 * 
 */
public class UpdateRequest extends NotifiableRequest {

	/** The logger */
	private static Logger logger;
	
	/**
	 * Constructor for synchronous insert requests
	 * @param message the SSAP Message request containing the UPDATE Request
	 * @param sib the current SIB
	 */
	public UpdateRequest(SSAPMessageRequest message, SIB sib) {
    	super(message, sib);

    	// Inits the logger
    	logger = Logger.getLogger(UpdateRequest.class);
	}
	
	/**
	 * Gets the request type
	 * @return the TransactionType associated to this request (UPDATE)
	 */
	@Override
	public TransactionType getRequestType() {
		return SSAPMessage.TransactionType.UPDATE;
	}
	
	/**
	 * Process a single UPDATE transaction.
	 * The <i>UPDATE CONFIRM</i> message is sent as a confirmation for 
	 * <i>UPDATE REQUEST</i> message. If confirmation was requested by node, 
	 * the node must handle the confirmation message. Parameter status contains 
	 * a status code reporting the success of the operation.
	 * 
	 * The parameter <b>bNodes</b> is a list of URIs assigned by the SIB 
	 * for each bNode instance. The parameter <i>bNodes</i> is returned only 
	 * if the <i>inserted_triples</i> parameter of the corresponding 
	 * <i>UPDATE REQUEST</i> contained any triples having subject or object type 
	 * equal to <i>bNode</i>.
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

			// this parameter will get the error params
			SSAPMessageStatusParameter param = null;
			
			SSAPMessageParameter requestInsertParam = request.getParameter(SSAPMessageParameter.NameAttribute.INSERTGRAPH);
			SSAPMessageParameter requestRemoveParam = request.getParameter(SSAPMessageParameter.NameAttribute.REMOVEGRAPH);
			SSAPMessageParameter encodingParam = request.getEncodingParameter();
			
			if (requestInsertParam == null || requestRemoveParam == null) {
				param = new SSAPMessageStatusParameter(
						SSAPMessageStatusParameter.SIBStatus.STATUS_KP_MESSAGE_INCOMPLETE);
			} else {
				String insertGraph = requestInsertParam.getContent();
				String removeGraph = requestRemoveParam.getContent();
				TypeAttribute encodingInsert = null;
				TypeAttribute encodingRemove = null;
				
				if (requestInsertParam instanceof SSAPMessageRDFParameter) {
					SSAPMessageRDFParameter rdfParam = (SSAPMessageRDFParameter) requestInsertParam;
					encodingInsert = rdfParam.getType();
				}
				if (requestRemoveParam instanceof SSAPMessageRDFParameter) {
					SSAPMessageRDFParameter rdfParam = (SSAPMessageRDFParameter) requestRemoveParam;
					encodingRemove = rdfParam.getType();
				}
				
				if (encodingParam != null && encodingInsert == null) {
					encodingInsert = TypeAttribute.findValue(encodingParam.getContent());
				}

				if (encodingParam != null && encodingRemove == null) {
					encodingRemove = TypeAttribute.findValue(encodingParam.getContent());
				}
				
				try {
					if (encodingRemove == null || removeGraph == null) {
						throw new SSAPSyntaxException("Remove graph or encoding is null");
					} 
					
					if (encodingInsert == null || insertGraph == null) {
						throw new SSAPSyntaxException("Insert graph or encoding is null");
					}
				
					// Creates a new model
					// UPDATE: Now DL is not sufficient for our needs. We will start using OWL-Full 
					OntModel insertModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);
					//OntModel insertModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, null);

					String insertContent = null;
					String insertModelEncoding = null;
				
					insertContent = EncodingFactory.getInstance().getTransformer(encodingInsert).toRDFXML(insertGraph);
					if (insertContent != null) {
						insertModelEncoding = "RDF/XML";
					} else {
						insertContent = EncodingFactory.getInstance().getTransformer(encodingInsert).toNTriples(insertGraph);
						insertModelEncoding = "N-TRIPLE";
					}
					
					// Transformation of the String to a InputStream
					ByteArrayInputStream insertIS = new ByteArrayInputStream(insertContent.getBytes()); 
					
					insertModel.read(insertIS, null, insertModelEncoding);
						
					// Closes the input stream
					if (insertIS != null) { 
						insertIS.close(); 
					}					
					
					// Creates a new model for removing
					// UPDATE: Now DL is not sufficient for our needs. We will start using OWL-Full 
					OntModel removalModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);					
					//OntModel removalModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, null);

					String removeContent = null;
					String removeModelEncoding = null;
				
					/** support for wildcards */
					try {
						String newGraph = checkWildcards(encodingRemove, removeGraph);
						if (newGraph != null && !newGraph.equals("")) {
							removeGraph = newGraph;
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					removeContent = EncodingFactory.getInstance().getTransformer(encodingRemove).toRDFXML(removeGraph);
					if (removeContent != null) {
						removeModelEncoding = "RDF/XML";
					} else {
						removeContent = EncodingFactory.getInstance().getTransformer(encodingRemove).toNTriples(removeGraph);
						removeModelEncoding = "N-TRIPLE";
					}
					
					// Transformation of the String to a InputStream
					ByteArrayInputStream removeIS = new ByteArrayInputStream(removeContent.getBytes()); 
					
					removalModel.read(removeIS, null, removeModelEncoding);
						
					// Closes the input stream
					if (removeIS != null) { 
						removeIS.close(); 
					}					
					
					// New functionality: BNode detection+creation of new bnodes resources
					HashMap<String, String> bNodeList = sib.getSemanticModel().renameBNodes2NamedResources(insertModel, sib.getName());
					
					// Perform the update
					boolean modelUpdated = sib.getSemanticModel().updateModel(request.getNodeId(), insertModel, removalModel);
					
					if (modelUpdated) {
						// Notifies the model
						sib.getViewerListenerManager().updateModel(request.getNodeId(), insertModel, removalModel);

						param = new SSAPMessageStatusParameter(
								SSAPMessageStatusParameter.SIBStatus.STATUS_OK);
						response.addParameter(param);
						
						SSAPMessageParameter bNodesParam;
						if (bNodeList.size() > 0) {
							bNodesParam = new SSAPMessageBNodesParameter(bNodeList);
						} else {
							bNodesParam = new SSAPMessageBNodesParameter(null);
						}
							
						response.addParameter(bNodesParam);									
					} else {
						param = new SSAPMessageStatusParameter(
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

					param = new SSAPMessageStatusParameter(
							SSAPMessageStatusParameter.SIBStatus.STATUS_KP_MESSAGE_SYNTAX);
					
					response.addParameter(param);
				} catch (SSAPSyntaxException ex) {
					ex.printStackTrace();
					SSAPMessageParameter statusParam = new SSAPMessageStatusParameter(
							SSAPMessageStatusParameter.SIBStatus.STATUS_KP_MESSAGE_SYNTAX);
					
					response.addParameter(statusParam);	
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
