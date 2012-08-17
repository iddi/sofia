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

package eu.sofia.adk.sib.model.session;

import java.util.HashMap;

import eu.sofia.adk.ssapmessage.proxy.ClientProxy;


/**
 * Class containing the Smart Application reference.
 * 
 * @author Fran Ruiz, fran.ruiz@tecnalia.com, ESI
 *
 */
public class SIBSession {

	/*
	 * Variable definition 
	 */
	private String nodeId;

	/** A map containing the proxy for each subscription */
	private HashMap<Long, ClientProxy> subscriptionProxy;
	
	/**
	 * Constructor.
	 */
	public SIBSession() {
		this.nodeId = null;
		this.subscriptionProxy = new HashMap<Long, ClientProxy>(); 
	}
	
	/**
	 * Constructor with parameters
	 * @param nodeId The node identifier
	 */
	public SIBSession(String nodeId) {
		this.setNodeId(nodeId);
		this.subscriptionProxy = new HashMap<Long, ClientProxy>(); 
	}

	/**
	 * Sets the node reference
	 * @param nodeId The node identification
	 */
	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	/**
	 * Gets the node reference
	 * @return A String with the node identification
	 */
	public String getNodeId() {
		return nodeId;
	}
	
	/**
	 * Add a proxy associated to a subscription
	 * @param Integer The id associated with the subscription
	 * @param The ClientProxy associated with the subscription request.
	 */
	public void addSubscriptionProxy(long id, ClientProxy proxy) {
		this.subscriptionProxy.put(id, proxy);
	}

	/**
	 * Remove a proxy associated to a subscription
	 * @param Integer The id associated with the subscription
	 * @return The ClientProxy associated with the subscription request.
	 */
	public ClientProxy removeSubscriptionProxy(long id) {
		return this.subscriptionProxy.remove(id);
	}
	
	/**
	 * Gets a proxy associated to a subscription
	 * @param Integer The id associated with the subscription
	 * @return The ClientProxy associated with the subscription request.
	 */
	public ClientProxy getSubscriptionProxy(long id) {
		return this.subscriptionProxy.get(id);
	}
}
