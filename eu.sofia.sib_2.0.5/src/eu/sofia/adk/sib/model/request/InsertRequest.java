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
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.shared.SyntaxError;

import eu.sofia.adk.sib.exception.EncodingNotSupportedException;
import eu.sofia.adk.sib.exception.SSAPSyntaxException;
import eu.sofia.adk.sib.exception.TransformationException;
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
 * This class is a request for executing an INSERT REQUEST
 * SSAP Message in the SIB. The triples included in the parameter
 * <i>triples</i> will be asserted in the SIB. The assertion is
 * atomic, that is, if there are multiple triples, all of them will 
 * be asserted before any other triples will be asserted or any queries made.
 * The triples must be serialized as RDF-XML or the M3 internal format, RDF-M3.
 * There may be multiple instances of parameter <i>triples</i>.
 * The parameter <i>confirm</i> determines whether the SIB sends a confirmation 
 * for the request. If <i>TRUE</i>, a confirmation is sent, if <i>FALSE</i>,
 * a confirmation is not sent. Parameter is optional, default value is <i>TRUE</i>.
 * 
 * @author Fran Ruiz, ESI
 * @see eu.sofia.adk.sib.model.request.NotifiableRequest
 * 
 */
public class InsertRequest extends NotifiableRequest {

	/** The logger */
	private static Logger logger;
	
	/**
	 * Constructor for synchronous INSERT requests
	 * @param request the SSAP Message request containing the INSERT Request
	 * @param sib the current SIB
	 */
	public InsertRequest(SSAPMessageRequest request, SIB sib) {
    	super(request, sib);

    	// Inits the logger
    	logger = Logger.getLogger(InsertRequest.class);	
	}
	
	/**
	 * Gets the request type
	 * @return the TransactionType associated to this request (INSERT)
	 */
	@Override
	public TransactionType getRequestType() {
		return SSAPMessage.TransactionType.INSERT;
	}
	
	/**
	 * Process a single INSERT transaction.
	 * The INSERT CONFIRM message is sent as a confirmation for INSERT REQUEST 
	 * message. If confirmation was requested by node, the node must handle 
	 * the confirmation message. Parameter status contains a status code
	 * reporting the success of the operation (see description of status codes).
	 * 
	 * The parameter bNodes is a list of URIs assigned by the SIB for each bNode 
	 * instance. The parameter bNodes is returned only if the corresponding 
	 * INSERT REQUEST contained any triples having subject or object type equal 
	 * to bNode.
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
			SSAPMessageParameter requestParam = request.getParameter(SSAPMessageParameter.NameAttribute.INSERTGRAPH);
			SSAPMessageParameter encodingParam = request.getEncodingParameter();

			if (requestParam == null) {
				SSAPMessageParameter param = new SSAPMessageStatusParameter(
						SSAPMessageStatusParameter.SIBStatus.STATUS_KP_MESSAGE_INCOMPLETE);
				
				response.addParameter(param);
			} else {
				String insertGraph = requestParam.getContent();
				TypeAttribute encoding = null;
				
				if (requestParam instanceof SSAPMessageRDFParameter) {
					SSAPMessageRDFParameter rdfParam = (SSAPMessageRDFParameter) requestParam;
					encoding = rdfParam.getType();
				}
				
				if (encodingParam != null && encoding == null) {
					encoding = TypeAttribute.findValue(encodingParam.getContent());
				}
				
				try {
					if (encoding == null || insertGraph == null) {
						throw new SSAPSyntaxException("Insert graph or encoding is null");
					}
					
					// Creates a new model
					// UPDATE: Now DL is not sufficient for our needs. We will start using OWL-Full 
					OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null);
					//OntModel model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM, null);

					String insertContent = null;
					String modelEncoding = null;
				
					insertContent = EncodingFactory.getInstance().getTransformer(encoding).toRDFXML(insertGraph);
					if (insertContent != null) {
						modelEncoding = "RDF/XML";
					} else {
						insertContent = EncodingFactory.getInstance().getTransformer(encoding).toNTriples(insertGraph);
						modelEncoding = "N-TRIPLE";
					}
					
					// Transformation of the String to a InputStream
					ByteArrayInputStream is = new ByteArrayInputStream(insertContent.getBytes()); 
					
					model.read(is, null, modelEncoding);
						
					// Closes the input stream
					if (is != null) { 
						is.close(); 
					}
					
					// New functionality: BNode detection+creation of new bnodes resources
					HashMap<String, String> bNodeList = sib.getSemanticModel().renameBNodes2NamedResources(model, sib.getName());
					
					boolean modelAdded = sib.getSemanticModel().addModel(request.getNodeId(), model);
					
					if (modelAdded) {
						// Notifies the model
						sib.getViewerListenerManager().addModel(request.getNodeId(), model);
						
						//@SuppressWarnings("unused")
						//String bNodes = sib.getSemanticModel().getBNodes(model);
						//SIBManager.getInstance().getSemanticModel().getBNodes2(model);
						SSAPMessageParameter param = new SSAPMessageStatusParameter(
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
						SSAPMessageParameter param = new SSAPMessageStatusParameter(
								SSAPMessageStatusParameter.SIBStatus.STATUS_ERROR);
						response.addParameter(param);
					}
					
				} catch (SyntaxError ex) {
					ex.printStackTrace();
					SSAPMessageParameter statusParam = new SSAPMessageStatusParameter(
							SSAPMessageStatusParameter.SIBStatus.STATUS_KP_MESSAGE_SYNTAX);
					
					response.addParameter(statusParam);							
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
		
		if (confirm) { // if confirmation is required, send
			return response;
		} else {
			return null;
		}
	}
}
