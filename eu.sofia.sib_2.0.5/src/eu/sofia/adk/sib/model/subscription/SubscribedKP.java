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

package eu.sofia.adk.sib.model.subscription;

import eu.sofia.adk.sib.model.session.SIBSession;

/**
 * This class contains the Smart Application-related information about the 
 * subscription and session.
 * 
 * @author Fran Ruiz, ESI
 * 
 */
public class SubscribedKP {

	/*
	 * Private variables
	 */
	private long subscriptionId;
	private SIBSession session;
	
	/**
	 * Constructor
	 * @param subscriptionId The subscription identifier
	 * @param session The SIBSession
	 */
	public SubscribedKP(long subscriptionId, SIBSession session) {
		this.subscriptionId = subscriptionId;
		this.session = session;
	}
	
	/**
	 * Sets the subscription identifier
	 * @param subscriptionId The subscription identifier
	 */
	public void setSubscriptionId(long subscriptionId) {
		this.subscriptionId = subscriptionId;
	}
	
	/**
	 * Gets the subscription identifier
	 * @return The subscription identifier
	 */
	public long getSubscriptionId() {
		return subscriptionId;
	}
	
	/**
	 * Sets the Smart Application Session
	 * @param session SIBSession containing the connection information to the Smart Application
	 */
	public void setSession(SIBSession session) {
		this.session = session;
	}
	
	/**
	 * Gets the Smart Application Session
	 * @return SIBSession containing connection information to the Smart Application
	 */
	public SIBSession getSession() {
		return session;
	}
	
	
}
