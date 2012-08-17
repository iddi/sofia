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

import eu.sofia.adk.sib.data.Subscription;
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
 * This class implements a SUBSCRIPTION REQUEST
 * @author Fran Ruiz
 */
/**
 * This class is a request for executing an SUBSCRIPTION REQUEST
 * A <i>SUBSCRIBE REQUEST</i> message sets up a subscription in SIB. 
 * The subscription returns a response whenever the results of the given 
 * query change. The subscription is stopped with <i>UNSUBSCRIBE REQUEST</i> 
 * message. The SIB may send <i>SUBSCRIBE INDICATION</i> messages after the 
 * <i>UNSUBSCRIBE</i> message has been sent by Node until the
 * <i>UNSUBSCRIBE INDICATION</i> message is sent by SIB.
 * 
 * The <i>&lt;query_type&gt;</i> can be the name of any supported query language, 
 * currently supported values are <i>RDF-M3</i>, meaning a triple template 
 * (with return value as <i>RDF-M3</i>), <i>WQL-X</i> for different 
 * wql (wilbur query language) queries. <b>The format of QUERY_STRING and return 
 * values depend on the type of the query.</b>
 * <ul>
 * <li><i>RDF-M3</i> template queries contain a description of a triple in <i>RDF-M3</i>, 
 * possibly containing a <i>sib:any</i> value as a subject, object, predicate, 
 * or any combination of these. The <i>sib:any</i> value is interpreted as a 
 * wildcard, matching any URI or literal.</li>
 * <li><i>WQL-VALUES</i> query consist of a starting node and a path expression. 
 * The starting node can be either a URI or a literal.</li>
 * <li><i>WQL-RELATED</i> query consist of a starting node, a path expression 
 * and an end node. The start and end nodes can be either a URIs or a literals.</li>
 * <li><i>WQL-NODETYPES</i> query consists of a node. The node should be a URI.</li>
 * <li><i>WQL-ISTYPE</i> query consists of a start node and a type node. 
 * The nodes should be URIs.</li>
 * <li><i>WQL-ISSUBTYPE query consists of a subtype node and a supertype node. 
 * The nodes should be URIs.</li>
 * </ul>
 * 
 * @author Fran Ruiz, ESI
 * @see eu.sofia.adk.sib.model.request.Request
 * 
 */
public class SubscribeRequest extends Request {

	/** The logger */
	private static Logger logger;
	
	/**
	 * Constructor for synchronous subscribe requests
	 * @param message the SSAP Message request containing the SUBSCRIBE Request
	 * @param sib the current SIB
	 */
	public SubscribeRequest(SSAPMessageRequest message, SIB sib) {
    	super(message, sib);

    	// Inits the logger
    	logger = Logger.getLogger(SubscribeRequest.class);
	}
	
	/**
	 * Gets the request type
	 * @return the TransactionType associated to this request (SUBSCRIBE)
	 */
	@Override
	public TransactionType getRequestType() {
		return SSAPMessage.TransactionType.SUBSCRIBE;
	}
	
	
	/**
	 * Process a single SUBSCRIBE transaction.
	 * <i>SUBSCRIBE CONFIRM</i> is sent as a response for <i>SUBSCRIBE REQUEST</i>
	 * message. It confirms that the subscription has been properly set up and 
	 * returns an initial set of results matching the query given in 
	 * <i>SUBSCRIBE REQUEST</i>.
	 * 
	 * Parameter status contains a status code reporting the success of the 
	 * operation.
	 * 
	 * The parameter <i>subscription_id</i> contains an identifier for the 
	 * subscription. The identifier should be the same as the 
	 * <i>&lt;transaction_id&gt;</i> in the corresponding <i>SUBSCRIBE REQUEST</i>
	 * message.
	 * 
	 * The format of results parameter corresponding to parameter type 
	 * in SUBSCRIBE REQUEST.
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
				String subscriptionQuery = queryParam.getContent();
				TypeAttribute encoding = null;
				
				if (queryParam instanceof SSAPMessageRDFParameter) {
					SSAPMessageRDFParameter rdfParam = (SSAPMessageRDFParameter) queryParam;
					encoding = rdfParam.getType();
				}
				
				if (encodingParam != null && encoding == null) {
					encoding = TypeAttribute.findValue(encodingParam.getContent());
				}
				
				try {
					
					if (encoding == null || subscriptionQuery == null) {
						throw new SSAPSyntaxException("Query or encoding is null");
					}
					
					String returnData = new String();
					long subscriptionId = new Long(0);
					if (encoding == TypeAttribute.RDFM3) {
						// allow the multiple query
						HashMap<String,String> sparqlQueries = new HashMap<String,String>();
						sparqlQueries.putAll(EncodingFactory.getInstance().getTransformer(encoding).toSPARQL(subscriptionQuery));
						
						M3Parser parser = new M3Parser();
						RDFM3 queryM3 = parser.parse(subscriptionQuery);
						
						ArrayList<RDFM3Triple> resultTriples = new ArrayList<RDFM3Triple>();
						subscriptionId = sib.getSubscriptionManager().createNewSubscriptionId();
						
						for (String sparqlQuery : sparqlQueries.keySet()) {
							SOFIAResultSet results = sib.getSemanticModel().query(sparqlQuery);
						
							String originalQuery = sparqlQueries.get(sparqlQuery);
							String queryResults = EncodingFactory.getInstance().getTransformer(TypeAttribute.RDFM3).toOriginalEncoding(originalQuery, results);
							
							RDFM3 m3Results = parser.parse(queryResults);
							
							if (m3Results.hasTriples()) {
								resultTriples.addAll(m3Results.listTriples());
							}
							
							// Adds the subscription with the obtained model
							sib.getSubscriptionManager().addSubscription(sparqlQuery, originalQuery, encoding, session, results, subscriptionId);
							
						}
						
						// add wildcardresults to m3
						RDFM3 m3 = new RDFM3();
						m3.addNamespaces(queryM3.listNamespaces());
						m3.addTriples(resultTriples);
						
						returnData =  m3.toString();
						
					} else {
						// if not rdf-m3 format, only one query is available
						String sparqlQuery = (String) EncodingFactory.getInstance().getTransformer(encoding).toSPARQL(subscriptionQuery).toString();
						
						SOFIAResultSet results = sib.getSemanticModel().query(sparqlQuery);
						
						returnData = EncodingFactory.getInstance().getTransformer(encoding).toOriginalEncoding(subscriptionQuery, results);

						// Adds the subscription with the obtained model
						subscriptionId = sib.getSubscriptionManager().addSubscription(sparqlQuery, subscriptionQuery, encoding, session, results);
					}
					
					// Construct now the results parameter with obtained results
					SSAPMessageParameter param = new SSAPMessageParameter(
							SSAPMessageParameter.NameAttribute.RESULTS,
							returnData);					
					
					response.addParameter(param);						

					
					// TODO WRONG!!
//					String sparqlQuery = (String) EncodingFactory.getInstance().getTransformer(encoding).toSPARQL(subscriptionQuery).toString();
//	
//					SOFIAResultSet results = sib.getSemanticModel().query(sparqlQuery);
//					
//					String formattedResults = EncodingFactory.getInstance().getTransformer(encoding).toOriginalEncoding(subscriptionQuery, results);
//					
//					SSAPMessageParameter param = new SSAPMessageParameter(
//							SSAPMessageParameter.NameAttribute.RESULTS,
//							formattedResults);
//	
//					response.addParameter(param);				
//
//					// Adds the subscription with the obtained model
//					long subscriptionId = sib.getSubscriptionManager().addSubscription(sparqlQuery, subscriptionQuery, encoding, session, results);

					// Add the client proxy to the session.
					session.addSubscriptionProxy(subscriptionId, request.getClientProxy());
					
					sib.getViewerListenerManager().addSubscription(
							new Subscription(subscriptionId, 
									request.getNodeId(), 
									sib.getSubscriptionManager().toSortString(subscriptionQuery), 
									sib.getSubscriptionManager().toSortString(returnData)), request.getNodeId());
					
					SSAPMessageParameter statusParam = new SSAPMessageStatusParameter(
							SSAPMessageStatusParameter.SIBStatus.STATUS_OK);
					
					response.addParameter(statusParam);

					SSAPMessageParameter subscriptionIdParam = new SSAPMessageParameter(
							SSAPMessageParameter.NameAttribute.SUBSCRIPTIONID,
							new Long(subscriptionId).toString());
					
					response.addParameter(subscriptionIdParam);
					
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
				} catch (RDFM3ParseException e) {
					e.printStackTrace();
					SSAPMessageParameter param = new SSAPMessageStatusParameter(
							SSAPMessageStatusParameter.SIBStatus.STATUS_ERROR);
					response.addParameter(param);			
				}
			}

		}
		
		return response;
	}
}
