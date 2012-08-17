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

package eu.sofia.adk.sib.service;

import eu.sofia.adk.sib.data.ISemanticContentHolderListener;
import eu.sofia.adk.sib.data.ISessionHolderListener;
import eu.sofia.adk.sib.data.ISubscriptionHolderListener;

/**
 * The IViewer interface exposes the information of the SIB server to be displayed
 * 
 * @author Cristina López, ESI
 */

public interface IViewer {

	/**
	 * Attach the Session Listener to the SIB.
	 * This session listener is listening to the changes of sessions in the
	 * SIB.
	 * @param listener a listener that implements the ISessionHolderListener methods
	 */
	public void addListener(ISessionHolderListener listener);	
	
	/**
	 * Detach the Session listener from the SIB. 
	 * Detach the listener from the SIB.
	 * @param listener a listener that implements the ISessionHolderListener methods
	 */
	public void removeListener(ISessionHolderListener listener);	

	/**
	 * Attach the Subscription Listener to the SIB.
	 * This subscription listener is listening to the changes of subscribed 
	 * queries in the SIB.
	 * @param listener a listener that implements the ISubscriptionHolderListener methods
	 */
	public void addListener(ISubscriptionHolderListener listener);	
	
	/**
	 * Detach the Subscription listener from the SIB. 
	 * Detach the listener from the SIB.
	 * @param listener a listener that implements the ISubscriptionHolderListener methods
	 */
	public void removeListener(ISubscriptionHolderListener listener);		

	/**
	 * Attach the Semantic Content Listener to the SIB.
	 * The semantic content listener is listening to the changes of the semantic
	 * model that is in the SIB. The most easy way to view this information
	 * is using the data expressed in triples.
	 * @param listener a listener that implements the ISemanticContentHolderListener methods
	 */
	public void addListener(ISemanticContentHolderListener listener);	
	
	/**
	 * Detach the semantic content listener from the SIB. 
	 * Detach the listener from the SIB.
	 * @param listener a listener that implements the ISemanticContentHolderListener methods
	 */
	public void removeListener(ISemanticContentHolderListener listener);		

}
