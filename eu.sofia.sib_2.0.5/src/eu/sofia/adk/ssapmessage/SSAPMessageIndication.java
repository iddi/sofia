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

import eu.sofia.adk.ssapmessage.parameter.SSAPMessageParameter;

/**
 * The specialized SSAPMessage for indications
 * 
 * @author Fran Ruiz, fran.ruiz@tecnalia.com, ESI
 */
public class SSAPMessageIndication extends SSAPMessage {

	/**
	 * Creates a new indication message
	 * @param nodeId the node identifier
	 * @param spaceId the space id
	 * @param subscriptionId the subscription id
	 */
	public SSAPMessageIndication(String nodeId, String spaceId, long subscriptionId) {
		super();
		setNodeId(nodeId);
		setSpaceId(spaceId);
		setTransactionType(SSAPMessage.TransactionType.SUBSCRIBE);
		setMessageType(MessageType.INDICATION);
		setTransactionId(subscriptionId);
		
		SSAPMessageParameter subscriptionIdParam = new SSAPMessageParameter(
				SSAPMessageParameter.NameAttribute.SUBSCRIPTIONID,
				new Long(subscriptionId).toString());
		
		addParameter(subscriptionIdParam);
	}
}
