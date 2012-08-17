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

import org.apache.log4j.Logger;

import eu.sofia.adk.sib.ssap.SIB;
import eu.sofia.adk.ssapmessage.SSAPMessage;
import eu.sofia.adk.ssapmessage.SSAPMessage.TransactionType;
import eu.sofia.adk.ssapmessage.SSAPMessageRequest;
import eu.sofia.adk.ssapmessage.SSAPMessageResponse;
import eu.sofia.adk.ssapmessage.parameter.SSAPMessageParameter;
import eu.sofia.adk.ssapmessage.parameter.SSAPMessageStatusParameter;

/**
 * This class implements a UNSUBSCRIBE REQUEST
 * Stops a <i>SUBSCRIBE</i> transaction which has a subscription id stated in
 * <i>subscription_id</i> parameter. If confirmation is requested, 
 * no <i>SUBSCRIBE INDICATION</i> messages are received after 
 * <i>UNSUBSCRIBE CONFIRMATION</i> message.
 * 
 * However, <i>SUBSCRIBE INDICATION</i> messages may be received after sending
 * <i>UNSUBSCRIBE REQUEST</i> message but before receiving the confirmation.
 * 
 * @author Fran Ruiz, ESI
 * @see eu.sofia.adk.sib.model.request.Request
 * 
 */
public class UnsubscribeRequest extends Request {

	/** The logger */
	private static Logger logger;
	
	/**
	 * Constructor for synchronous UNSUBSCRIBE requests
	 * @param message the SSAP Message request containing the UNSUBSCRIBE Request
	 * @param sib the current SIB
	 */
	public UnsubscribeRequest(SSAPMessageRequest message, SIB sib) {
    	super(message, sib);

    	// Inits the logger
    	logger = Logger.getLogger(UnsubscribeRequest.class);
	}
	
	/**
	 * Gets the request type
 	 * @return the TransactionType associated to this request (UNSUBSCRIBE)
	 */
	@Override
	public TransactionType getRequestType() {
		return SSAPMessage.TransactionType.UNSUBSCRIBE;
	}
	
	/**
	 * Process a single UNSUBSCRIBE transaction.
	 * <i>UNSUBSCRIBE CONFIRM</i> message is used to confirm the stopping of 
	 * subscribe transaction. Parameter <i>status</i> contains a status code 
	 * reporting the success of the operation.
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
			SSAPMessageParameter requestParam = request.getParameter(SSAPMessageParameter.NameAttribute.SUBSCRIPTIONID);
			
			if (requestParam == null) {
				logger.error("Message incomplete");
				SSAPMessageParameter param = new SSAPMessageStatusParameter(
						SSAPMessageStatusParameter.SIBStatus.STATUS_KP_MESSAGE_INCOMPLETE);
				
				response.addParameter(param);
				
			} else {
				long subscriptionId = new Long(requestParam.getContent());

				boolean removed = sib.getSubscriptionManager().removeSubscription(
						subscriptionId, request.getNodeId());
				//logger.debug("Removed the subscription for subscription identifier " + subscriptionId);

				if (removed) {
					// Removes it from the listener list
					sib.getViewerListenerManager().removeSubscription(subscriptionId, request.getNodeId());
					
					// Removes the client proxy from the session.
					session.removeSubscriptionProxy(subscriptionId);
					
					SSAPMessageParameter statusParam = new SSAPMessageStatusParameter(
						SSAPMessageStatusParameter.SIBStatus.STATUS_OK);
				
					response.addParameter(statusParam);
				} else {
					SSAPMessageParameter statusParam = new SSAPMessageStatusParameter(
							SSAPMessageStatusParameter.SIBStatus.STATUS_ERROR);
					
					response.addParameter(statusParam);
				}

				SSAPMessageParameter subscriptionIdParam = new SSAPMessageParameter(
						SSAPMessageParameter.NameAttribute.SUBSCRIPTIONID,
						new Long(subscriptionId).toString());
				
				response.addParameter(subscriptionIdParam);
			}

		}
		
		return response;		
		
	}
}
