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

package eu.sofia.adk.ssapmessage;

import java.util.Collection;

import eu.sofia.adk.ssapmessage.parameter.SSAPMessageParameter;
import eu.sofia.adk.ssapmessage.proxy.ClientProxy;

/**
 * The specialized SSAPMessage for requests
 * 
 * @author Raul Otaolea
 */
public class SSAPMessageRequest extends SSAPMessage {

	/** the time of generation */
	private long sentTime;
	
	/** The proxy to send the response */
	private ClientProxy proxy;
	
	public SSAPMessageRequest(String nodeId, String spaceId, long transactionId, 
			TransactionType tranType) {
		super();
		setNodeId(nodeId);
		setSpaceId(spaceId);
		setTransactionType(tranType);
		setMessageType(MessageType.REQUEST);
		setTransactionId(transactionId);
	}

	public SSAPMessageRequest(String nodeId, String spaceId, long transactionId, 
			TransactionType tranType, Collection<SSAPMessageParameter> parameters) {
		super();
		setNodeId(nodeId);
		setSpaceId(spaceId);
		setTransactionType(tranType);
		setMessageType(MessageType.REQUEST);
		setTransactionId(transactionId);
		this.addParameters(parameters);
	}	
	
	/**
	 * Saves the time
	 */
	public void saveTime() {
		this.sentTime = System.currentTimeMillis();
	}
	
	/**
	 * Gets the time for this SSAPMessage
	 * @return the long representation of the time
	 */
	public long getSentTime() {
		return sentTime;
	}
	
	public void setClientProxy(ClientProxy proxy) {
		this.proxy = proxy;
	}
	
	public ClientProxy getClientProxy() {
		return this.proxy;
	}


	
}
