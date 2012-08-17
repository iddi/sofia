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

/**
 * The SSAP specialized for responses
 * 
 * @author Fran Ruiz, fran.ruiz@tecnalia.com, ESI
 */
public class SSAPMessageResponse extends SSAPMessage {

	public SSAPMessageResponse(String nodeId, String spaceId, long transactionId, 
			TransactionType tranType) {
		super();
		setNodeId(nodeId);
		setSpaceId(spaceId);
		setTransactionType(tranType);
		setMessageType(MessageType.CONFIRM);
		setTransactionId(transactionId);
	}

	public SSAPMessageResponse(String nodeId, String spaceId, long transactionId, 
			TransactionType tranType, Collection<SSAPMessageParameter> parameters) {
		super();
		setNodeId(nodeId);
		setSpaceId(spaceId);
		setTransactionType(tranType);
		setMessageType(MessageType.CONFIRM);
		setTransactionId(transactionId);
		this.addParameters(parameters);
	}		
	
	public SSAPMessageResponse() {
		super();
	}
}
