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

package eu.sofia.adk.gateway.session.impl;

import eu.sofia.adk.gateway.connector.impl.Connector;
import eu.sofia.adk.gateway.session.ISIBCommunication;
import eu.sofia.adk.sib.exception.SIBException;
import eu.sofia.adk.sib.service.ISIB;
import eu.sofia.adk.ssapmessage.SSAPMessage;
import eu.sofia.adk.ssapmessage.SSAPMessageRequest;
import eu.sofia.adk.ssapmessage.proxy.ClientProxy;
import eu.sofia.adk.ssapmessage.proxy.SIBMessageListener;

public class Session implements ISIBCommunication, SIBMessageListener {

	/** Different errors that are detected as abrupt disconnection of the SIB */
	private static final String CLOSED = "connection.closed";
	private static final String TIMEOUT_ERROR = "connection.timeout";

	/** ID generator */
	private static long nextId;

	/** The identifier of the connector session */
	private long id;

	/** The Connector */
	private Connector connector;
	
	/** The Client Proxy reference */
	private ClientProxy proxy;
	
	/** The SIB */
	private ISIB sib;
	
	/** KP name */
	private String kpName;
	
	/** Constructor */
	public Session() {
		proxy = new ClientProxy();
		this.id = getNextId();
	}
	
	private synchronized long getNextId() {
		return ++nextId;
	}
	
	public void setSIB (ISIB sib) {
		this.sib = sib;
	}
	
	/**
	 * Is called when the KP closes its connection. If the connection is persistent, this method
	 * notifies to the SIB that the connection has been closed. 
	 */
	@Override
	public void connectionClosed() {
		if(connector.isPersistent()) {
			try {
				this.getSIB().disconnect(kpName, CLOSED);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Sends to SIB a connection closed error. 
	 */
	@Override
	public void connectionError(String errorMsg) {
		if(connector.isPersistent()) {
			try {
				this.getSIB().disconnect(kpName, errorMsg);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Is called when the KP connection has a timeout. If the connection is persistent, 
	 * this method notifies to the SIB that the connection has a timeout. 
	 */
	@Override
	public void connectionTimeout() {
		if(connector.isPersistent()) {
			try {
				this.getSIB().disconnect(kpName, TIMEOUT_ERROR);
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Sends a message to the SIB.
	 * Gets the SIB reference and sends the SSAPMessage request. 
	 * @param request the SSAPMessage request
	 */
	@Override
	public void sendToSIB(SSAPMessageRequest request) {
		try {
			request.setClientProxy(this.proxy);
			if (request.getTransactionType() == SSAPMessage.TransactionType.JOIN) {
				this.kpName = request.getNodeId();
				this.getSIB().connect(request);
			} else if (request.getTransactionType() == SSAPMessage.TransactionType.LEAVE) {
				this.getSIB().disconnect(request);
			} else {
				this.getSIB().process(request);
			}
		} catch(SIBException e) {
			// TODO: Response with a proper SSAP message with the error code using 'connector'.
			e.printStackTrace();
		}

	}

	/**
	 * Sets the connector to this session
	 * @param connector the connector to set
	 */
	public void setConnector(Connector connector) {
		this.connector = connector;
		this.connector.setSIBSession(this);
		this.proxy.addSIBMessageListener(this);
	}

	/**
	 * @return the connector
	 */
	public Connector getConnector() {
		return connector;
	}

	/**
	 * @return the id
	 */
	public long getId() {
		return id;
	}

	private ISIB getSIB() {
		return sib;
	}

	@Override
	public void messageReceived(SSAPMessage message) {
		connector.messageReceivedFromSIB(message);
	}

	public void close() {
		if(connector != null) {
			connector.close();
		}
	}

	public String getKpName() {
		return kpName;
	}
}
