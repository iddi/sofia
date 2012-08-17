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

import eu.sofia.adk.sib.model.session.SIBSession;
import eu.sofia.adk.sib.ssap.SIB;
import eu.sofia.adk.ssapmessage.SSAPMessage;
import eu.sofia.adk.ssapmessage.SSAPMessage.TransactionType;
import eu.sofia.adk.ssapmessage.SSAPMessageRequest;
import eu.sofia.adk.ssapmessage.SSAPMessageResponse;
import eu.sofia.adk.ssapmessage.parameter.SSAPMessageParameter;
import eu.sofia.adk.ssapmessage.parameter.SSAPMessageStatusParameter;

/**
 * 
 * This abstract class implements the Runnable interface.
 * This classes are called by the ExecutorService 
 * defined in the SIB. 
 * 
 * @author Fran Ruiz, fran.ruiz@tecnalia.com, ESI
 * @see java.lang.Runnable
 * @see eu.sofia.adk.ssapmessage.SSAPMessage
 *
 */
public abstract class Request implements Runnable {
	
	/** The SIB */
	protected SIB sib;
	
	/** The SIBSession */
	protected SIBSession session;
	
	/** The response message */
	protected SSAPMessageRequest request;

	/** The response message */
	protected SSAPMessageResponse response;

	/** The logger */
	private static Logger logger;
	
	/**
	 * Process the request
	 * @return A SSAPMessage with the response
	 */
	public abstract SSAPMessageResponse processRequest();

	/**
	 * Constructor of a request. A request must contain at least the 
	 * node identifier, the smart space id (for response purposes), the
	 * transaction id and the set of parameters. 
	 */
	public Request(SSAPMessageRequest request, SIB sib) {
    	// Inits the logger
    	logger = Logger.getLogger(Request.class);
    	
    	if (request != null) {
    		this.setRequest(request);
    	}
    	
    	// The session is set to null
    	this.setSession(null);
    	
    	this.sib = sib;
	}	
	
	/**
	 * Method needed to be executed by ExecutorService.
	 * @see java.util.concurrent.Callable
	 * @throws Exception When the request cannot be executed
	 */
	@Override
	public void run() {
		try {
			logger.info("<" + request.getTransactionId() + ">::SIB Received::" +
					request.getTransactionType() + " " + request.getMessageType());
			logger.info("Received from KP:\n"+request);
			
			// Process the request
			response = processRequest();
			
			logger.info("Sending to KP:\n" + response);
			
			if (response != null) {
				logger.debug("<" + response.getTransactionId() + ">::SIB Sending::" +
						response.getTransactionType() + " " + response.getMessageType() + "::" +
						response.getStatusParameter().getStatus().getValue());
				
				// Sends the message to the Smart Application
				request.getClientProxy().sendMessage(response);
			}
			
			// This notifies in changes of data to the data holders
			//notifyDataHolders();
			
		} catch (Exception ex) {
			logger.error("Cannot process the request. Caused by " + ex.getMessage());
			logger.error("Stack trace:\n");
			ex.printStackTrace();
			response = new SSAPMessageResponse(
					request.getNodeId(), request.getSpaceId(), 
					request.getTransactionId(), request.getTransactionType());
			
			SSAPMessageParameter param = new SSAPMessageStatusParameter(
					SSAPMessageStatusParameter.SIBStatus.STATUS_ERROR);
			
			response.addParameter(param);
			this.request.getClientProxy().sendMessage(response); 
		}
	}

	/**
	 * Gets the request type
	 * @return A SIBSession with connection parameters to KP
	 */
	public abstract TransactionType getRequestType();

	/**
	 * Sets the session
	 * @param session The SIBSession
	 */
	public void setSession(SIBSession session) {
		this.session = session;
	}

	/**
	 * Gets the session
	 * @return A SIBSession with connection parameters to KP
	 */
	public SIBSession getSession() {
		return session;
	}

	/**
	 * Sets the request
	 * @param request a request SSAPMessage
	 */
	public void setRequest(SSAPMessageRequest request) {
		this.request = request;
	}

	/**
	 * Gets the request SSAPMessage
	 * @return a SSAPMessage
	 */
	public SSAPMessage getRequest() {
		return request;
	}	
	
	
	/**
	 * Gets the smart space identifier associated to this request
	 * @return a String with the smart space identifier
	 */
	public String getSmartSpaceId() {
		return request.getSpaceId();
	}

	/**
	 * Gets the node identifier (smart application identifier) 
	 * @return a String with the node identifier
	 */
	public String getNodeId() {
		return request.getNodeId();
	}

	/**
	 * Gets the transaction identifier 
	 * @return a long with the transaction identifier
	 */
	public long getTransactionId() {
		return request.getTransactionId();
	}
	
	/**
	 * Gets the SSAP parameters associated to this request 
	 * @return a vector of SSAP parameters
	 */
	public Collection<SSAPMessageParameter> getParameters() {
		return request.listParameters();
	}	
}
