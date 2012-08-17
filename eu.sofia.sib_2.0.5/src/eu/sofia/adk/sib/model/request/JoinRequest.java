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

import eu.sofia.adk.sib.data.Session;
import eu.sofia.adk.sib.model.session.SIBSession;
import eu.sofia.adk.sib.ssap.SIB;
import eu.sofia.adk.ssapmessage.SSAPMessage;
import eu.sofia.adk.ssapmessage.SSAPMessage.TransactionType;
import eu.sofia.adk.ssapmessage.SSAPMessageRequest;
import eu.sofia.adk.ssapmessage.SSAPMessageResponse;
import eu.sofia.adk.ssapmessage.parameter.SSAPMessageParameter;
import eu.sofia.adk.ssapmessage.parameter.SSAPMessageStatusParameter;

/**
 * This class is a request for executing an JOIN REQUEST
 * If the credentials given in the <i>credentials</i> parameter are accepted 
 * by the SIB, the node will become a member of  the smart space. 
 * The format of credentials is not currently defined, 
 * and the join procedure may change in the future.
 * 
 * @author Fran Ruiz, ESI
 * @see eu.sofia.adk.sib.model.request.Request
 * 
 */
public class JoinRequest extends Request {

	/** The logger */
	private static Logger logger;
	
	/**
	 * Constructor for synchronous join requests
	 * @param proxy the proxy associated to the KP
	 * @param request the SSAP Message request containing the JOIN Request
	 * @param sib the current SIB
	 */
	public JoinRequest(SSAPMessageRequest request, SIB sib) {
		super(request, sib);

    	// Inits the logger
    	logger = Logger.getLogger(InsertRequest.class);	
		
    	if (request != null) {
	    	if (request.getNodeId() != null) {
				// Create the session with the information of the client proxy
				SIBSession session = new SIBSession(request.getNodeId());
				
				super.setSession(session);
	    	}
    	} else {
    		
    	}
	}

	/**
	 * Gets the request type
	 * @return the TransactionType associated to this request (JOIN)
	 */
	@Override
	public TransactionType getRequestType() {
		return SSAPMessage.TransactionType.JOIN;
	}
	
	
	/**
	 * Process a single JOIN transaction.
	 * The parameter <i>status</i> will signal the success of the join operation. 
	 * If joining was successful, status will be <i>sib:Status_OK</i>, 
	 * any other value signals an error in joining. 
	 * A reason for failure may be that the node can not be accepted as 
	 * a member because of lacking credentials, or a technical error in the SIB.
	 * 
	 * @return SSAPMessageResponse with the response after processing the request. 
	 */
	public SSAPMessageResponse processRequest() {

		logger.debug("[" + sib.getName() + "] (" + request.getTransactionId()+ ") " +
				request.getTransactionType().getType() + " " + request.getMessageType().getType());
		
		// The response
		SSAPMessageResponse response = null;
		
		
		/***************************************************************************************
		 *
		 * Modification made by AVK 06/15/2011 To avoid KPs not being able to reconnect when
		 * connection is interrupted (unplugged or whatever the reason) and they try to re-connect 
		 * It was checking if the session already existed, which it did, and thus didn't allow
		 * to reconnect. Although it works, we don't recommend it since this is a very nasty
		 * feature (Not removig a KP when dropped and allowing to reconnect...)
		 * 
		 */
		// Adds the node to joined nodes
		//boolean nodeInserted = 
		//	sib.getSessionManager().addNode(request.getNodeId(), session);
		
		SIBSession cur = sib.getSessionManager().getSession(request.getNodeId());
		
		
		if (cur==null){
			sib.getSessionManager().addSession(request.getNodeId(), session);
		}
		else{
			this.session=cur;
			logger.debug("[" + sib.getName() + "] Already exists");
		}
			
		
		//if (nodeInserted) {
		sib.getViewerListenerManager().addSession(new Session(request.getNodeId()));
			
		// Creates the SSAP Response Message
		response = new SSAPMessageResponse(
					request.getNodeId(), request.getSpaceId(), 
					request.getTransactionId(), request.getTransactionType());
			
		SSAPMessageParameter param = new SSAPMessageStatusParameter(
					SSAPMessageStatusParameter.SIBStatus.STATUS_OK);
			
		response.addParameter(param);

//		} else {
//			logger.info("Cannot join the node");
//			
//			response = new SSAPMessageResponse(
//					request.getNodeId(), request.getSpaceId(), 
//					request.getTransactionId(), request.getTransactionType());
//			
//			SSAPMessageParameter param = new SSAPMessageStatusParameter(
//					SSAPMessageStatusParameter.SIBStatus.STATUS_KP_REQUEST);
//			
//			response.addParameter(param);
//		}
			
		/***************************************************************************************
		*
		* End of Modification made by AVK 06/15/2011 To avoid KPs not being able to reconnect when
		* connection is interrupted (unplugged or whatever the reason) and they try to re-connect 
		* It was checking if the session already existed, which it did, and thus didn't allow
		* to reconnect. Although it works, we don't recommend it since this is a very nasty
		* feature (Not removig a KP when dropped and allowing to reconnect...)
		* 
		*/
		
		return response;
	}

}
