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

package eu.sofia.adk.gateway.connector.impl;

import eu.sofia.adk.gateway.connector.IConnector;
import eu.sofia.adk.gateway.exception.GatewayException;
import eu.sofia.adk.gateway.parser.SSAPMessageParseException;
import eu.sofia.adk.gateway.parser.SSAPMessageParser;
import eu.sofia.adk.gateway.session.ISIBCommunication;
import eu.sofia.adk.gateway.session.impl.Session;
import eu.sofia.adk.ssapmessage.SSAPMessage;
import eu.sofia.adk.ssapmessage.SSAPMessageRequest;

/**
 * The connector that stores the reference of the internal connection of the KP
 * as well as possible errors that can arise during the presentation.
 * 
 * @author Raul Otaolea, Raul.Otaolea@tecnalia.com, Tecnalia
 * @author Fran Ruiz, Fran.Ruiz@tecnalia.com, Tecnalia
 *
 */
public abstract class Connector implements IConnector {

	/** The session reference */
	private ISIBCommunication session;

	/** Indicates if the connection is persistent */
	private boolean persistent;
	
	public abstract void close();
	
	/**
	 * Establish if this connection is persistent.
	 * @param persistent
	 * 		Indicates if this connection is persistent. If so, it will notify to session 
	 * 		when the connection is closed, timeout or error. 
	 */
	public void setPersistent(boolean persistent) {
		this.persistent = persistent;
	}
	
	/**
	 * Gets if the connection is persistent or not.
	 * @return <code>yes</code> if it is persistent, <code>false</code> otherwise.
	 */
	public boolean isPersistent() {
		return this.persistent;
	}
	
	@Override
	/**
	 * This is the method that the connector must invoke when a new message represented
	 * in a byte array arrives.
	 */
	public void fireMessageReceived(byte[] stream) {
		if(session != null) {
			SSAPMessageRequest request = null;
			try {
//				System.out.println("Gateway parsing:\n" + new String(stream));
				request = SSAPMessageParser.parse(stream);
//				System.out.println("Parsed:\n" + request.toString());
				session.sendToSIB(request);
			} catch (SSAPMessageParseException e) {
				System.err.println("Error while parsing:\n" + new String(stream));
				e.printStackTrace();
				//TODO [ROM] Send and invalid SSAPMessageFormat to the client.
			}
		}
	}

	/**
	 * Gets the received message from the SIB and pass it to the connector in order
	 * to send it to the KP.
	 */
	@Override
	public void messageReceivedFromSIB(SSAPMessage response) {
		sendToKP(response.toByteArray());
	}

	/**
	 * Fires a connection timeout when the connection is closed
	 */
	@Override
	public void fireConnectionClosed() {
		if(session != null) {
			session.connectionClosed();
		}
	}

	/**
	 * Fires a connection timeout to 
	 * @param ex a GatewayException 
	 */
	@Override
	public void fireConnectionError(GatewayException ex) {
		if(session != null) {
			session.connectionError(ex.getMessage());
		}
	}

	/**
	 * Fires a connection timeout when no response is received from the KP
	 */
	@Override
	public void fireConnectionTimeout() {
		if(session != null) {
			session.connectionTimeout();
		}
	}

	/**
	 * Associates the connection to a SIBSession
	 * @param sibSession a SIBSession
	 */
	public void setSIBSession(Session session) {
		this.session = session;
	}
	
}
