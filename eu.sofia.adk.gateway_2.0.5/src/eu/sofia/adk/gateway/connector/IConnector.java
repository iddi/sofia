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
 
package eu.sofia.adk.gateway.connector;

import eu.sofia.adk.gateway.exception.GatewayException;
import eu.sofia.adk.ssapmessage.SSAPMessage;

/**
 * The interface that defines the set of methods to be implemented by the connector.
 * 
 * @author Raul Otaolea, Raul.Otaolea@tecnalia.com, Tecnalia
 */
public interface IConnector {

	/**
	 * Notifies that the connector receives data from a Knowledge Processor.
	 * @param stream an array of bytes
	 */
	public void fireMessageReceived(byte[] stream);
	
	/**
	 * Receives a SSAP Message response from the SIB.
	 * @param response the SSAPMessageResponse
	 */
	public void messageReceivedFromSIB(SSAPMessage response);

	/**
	 * Fires a connection error to ...
	 * @param ex a GatewayException 
	 */
	public void fireConnectionError(GatewayException ex);	

	/**
	 * Fires a connection timeout to ...
	 */
	public void fireConnectionTimeout();

	/**
	 * Fires a connection closed error to ...
	 */
	public void fireConnectionClosed();
	
	/**
	 * Send to KP a byte array SSAP message.
	 * @param a byte array representing a SSAP xml message.
	 */
	public void sendToKP(byte[] ba);
}
