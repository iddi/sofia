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

package eu.sofia.adk.gateway.session;

import eu.sofia.adk.ssapmessage.SSAPMessageRequest;

/**
 * The interface that defines the set of methods to be implemented by the connector.
 * 
 * @author Raul Otaolea, Raul.Otaolea@tecnalia.com, Tecnalia
 */
public interface ISIBCommunication {

	/**
	 * Sends an SSAPMessageRequest to the SIB
	 * @param request a SSAPMessageRequest
	 */
	public void sendToSIB(SSAPMessageRequest request);
	
	/**
	 * Notifies about a connection error.
	 * @param errorMsg the message associated to the error
	 */
	public void connectionError(String errorMsg);

	/**
	 * Notifies about a connection error.
	 */
	public void connectionTimeout();	

	/**
	 * Notifies about a connection error.
	 */
	public void connectionClosed();	
}
