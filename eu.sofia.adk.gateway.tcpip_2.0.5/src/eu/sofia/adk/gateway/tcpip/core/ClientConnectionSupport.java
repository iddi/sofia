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

package eu.sofia.adk.gateway.tcpip.core;

import eu.sofia.adk.gateway.tcpip.listener.ConnectionListener;
import eu.sofia.adk.gateway.tcpip.warehouse.ListenerSupport;

/**
 * ClientConnectionSupport.java
 *
 * @author Raul Otaolea, Raul.Otaolea@tecnalia.com, Tecnalia-Tecnalia
 */
public class ClientConnectionSupport extends ListenerSupport<ConnectionListener> {
	
	public ClientConnectionSupport() {
		super();
	}

	public void fireConnectionClosed(ClientConnection client) {
		synchronized(listeners) {
			for(ConnectionListener listener : listeners) {
				listener.connectionClosed(client);
			}
		}
	}

	public void fireConnectionError(int idError, String errorDescription, ClientConnection client) {
		synchronized(listeners) {
			for(ConnectionListener listener : listeners) {
				listener.connectionError(idError, errorDescription, client);
			}
		}
	}

	public void fireConnectionTimeout(ClientConnection client) {
		synchronized(listeners) {
			for(ConnectionListener listener : listeners) {
				listener.connectionTimeout(client);
			}
		}
	}

	public void fireMessageReceived(ClientMessage message, ClientConnection client) {
		synchronized(listeners) {
			for(ConnectionListener listener : listeners) {
				listener.messageReceived(message, client);
			}
		}
	}
	
}
