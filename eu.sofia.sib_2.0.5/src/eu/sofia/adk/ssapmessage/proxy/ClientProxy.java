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

package eu.sofia.adk.ssapmessage.proxy;

import eu.sofia.adk.ssapmessage.SSAPMessage;

/**
 * The connector between the SIB and the connected object. 
 * This will be surely belong to a KP or gateway.
 * 
 * @author Raul Otaolea, raul.otaolea@tecnalia.com, ESI
 *
 */
public class ClientProxy {
	
	private long id;
	
	/** The listener support */
	private SIBMessageSupport listeners = new SIBMessageSupport();
	
	public ClientProxy() {
		id = System.currentTimeMillis();
	}

	/**
	 * Gets the id of this proxy 
	 * @return id 
	 */
	public long getId() {
		return this.id;
	}
	
	/**
	 * Sends a message to the connected listeners 
	 * @param response the SSAPMessage response or indication
	 */
	public void sendMessage(SSAPMessage response) {
		listeners.fireMessageReceived(response);
	}
	
	/**
	 * Adds a listener to this proxy
	 * @param listener an object implementing the SIBMessageListener interface
	 */
	public void addSIBMessageListener(SIBMessageListener listener) {
		listeners.addListener(listener);
	}
	
	/**
	 * Removes a listener from the client proxy
	 * @param listener an object implementing the SIBMessageListener interface
	 */
	public void removeSIBMessageListener(SIBMessageListener listener) {
		listeners.removeListener(listener);
	}
}
