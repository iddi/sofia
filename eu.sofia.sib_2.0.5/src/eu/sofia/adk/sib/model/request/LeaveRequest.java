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

import java.util.Collection;

import org.apache.log4j.Logger;

import eu.sofia.adk.sib.data.Session;
import eu.sofia.adk.sib.ssap.SIB;
import eu.sofia.adk.ssapmessage.SSAPMessage;
import eu.sofia.adk.ssapmessage.SSAPMessage.TransactionType;
import eu.sofia.adk.ssapmessage.SSAPMessageRequest;
import eu.sofia.adk.ssapmessage.SSAPMessageResponse;
import eu.sofia.adk.ssapmessage.parameter.SSAPMessageParameter;
import eu.sofia.adk.ssapmessage.parameter.SSAPMessageStatusParameter;

/**
 * This class is a request for executing an LEAVE REQUEST
 * <i>LEAVE REQUEST</i> notifies the SIB that the node is going to leave 
 * the smart space.
 * 
 * @author Fran Ruiz, ESI
 * @see eu.sofia.adk.sib.model.request.NotifiableRequest
 * 
 */
public class LeaveRequest extends NotifiableRequest {

	/** The logger */
	private static Logger logger;

	/**
	 * Constructor for synchronous LEAVE requests
	 * @param request the SSAP Message request containing the LEAVE Request
	 * @param sib the current SIB
	 */
	public LeaveRequest(SSAPMessageRequest request, SIB sib) {
		
		super(request, sib);

		// Inits the logger
    	logger = Logger.getLogger(LeaveRequest.class);	
		
	}

	/**
	 * Gets the request type
	 * @return the TransactionType associated to this request (LEAVE)
	 */
	@Override
	public TransactionType getRequestType() {
		return SSAPMessage.TransactionType.LEAVE;
	}
	
	/**
	 * Process a single LEAVE transaction.
	 * <i>LEAVE CONFIRM</i> confirms the leaving of the smart space to the node. 
	 * The smart space will not communicate with the node after <i>LEAVE CONFIRM</i>
	 * message (until another <i>JOIN REQUEST</i>).
	 * 
	 * @return SSAPMessageResponse with the response after processing the request. 
	 */
	public SSAPMessageResponse processRequest() {
		
		logger.debug("[" + sib.getName() + "] (" + request.getTransactionId()+ ") " +
				request.getTransactionType().getType() + " " + request.getMessageType().getType());
		
		
		// Prepare the response SSAP Message
		// Creates the SSAP Response Message
		response = new SSAPMessageResponse(
				request.getNodeId(), request.getSpaceId(), 
				request.getTransactionId(), request.getTransactionType());
		
		session = sib.getSessionManager().removeNode(request.getNodeId());
		
		if (session == null) { // The node wasn't associated to this SIB
			logger.error("Node " + request.getNodeId() + " was not associated to this SIB");
			
			SSAPMessageParameter param = new SSAPMessageStatusParameter(
					SSAPMessageStatusParameter.SIBStatus.STATUS_KP_MESSAGE_SYNTAX);
			
			response.addParameter(param);
		} else { // The node was successfully leave the SIB
			// Removes the model
			sib.getSemanticModel().remove(request.getNodeId());
			//OntModel removedModel = sib.getSemanticModel().remove(request.getNodeId());
			//if (removedModel != null) {
			//	sib.getViewerListenerManager().removeModel(removedModel);
			//}

			// Removes the associated subscriptions
			Collection<Long> subscriptionIds = sib.getSubscriptionManager().removeSubscriptions(request.getNodeId());
			
			if (subscriptionIds.size() > 0) {
				sib.getViewerListenerManager().removeSubscription(subscriptionIds, request.getNodeId());
			}

			sib.getViewerListenerManager().removeSession(new Session(request.getNodeId()));

			
			//logger.debug("Node " + request.getNodeId() + " has successfully leave the SIB");

			SSAPMessageParameter param = new SSAPMessageStatusParameter(
					SSAPMessageStatusParameter.SIBStatus.STATUS_OK);
			
			response.addParameter(param);
			
		}
		
		return response;
	}
}
