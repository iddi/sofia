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

package eu.sofia.adk.sib.viewer;

import org.apache.log4j.Logger;

import eu.sofia.adk.sib.data.ISemanticContentHolderListener;
import eu.sofia.adk.sib.data.ISessionHolderListener;
import eu.sofia.adk.sib.data.ISubscriptionHolderListener;
import eu.sofia.adk.sib.service.IViewer;
import eu.sofia.adk.sib.ssap.SIB;
import eu.sofia.adk.sib.viewer.request.SemanticContentListenerRequest;
import eu.sofia.adk.sib.viewer.request.SessionListenerRequest;
import eu.sofia.adk.sib.viewer.request.SubscriptionListenerRequest;
import eu.sofia.adk.sib.viewer.request.ViewerRequest;

/**
 * This class implements the IViewer interface, which access to some information of the server 
 * (triplets, subscriptions and sessions) and visualizes it in a view created 
 * for this purpose.
 *  
 * @author Cristina López, ESI
 * @author Fran Ruiz, fran.ruiz@tecnalia.com, ESI
 */
public class Viewer implements IViewer {

	/** The logger */
	private static Logger logger;
	
	private SIB sib;
	
	public Viewer(SIB sib) {
		logger = Logger.getLogger(Viewer.class);
		this.sib = sib;
	}
	
	/**
	 * Attach the Session Listener to the SIB.
	 * This session listener is listening to the changes of sessions in the
	 * SIB.
	 * @param listener a listener that implements the ISessionHolderListener methods
	 */
	@Override
	public void addListener(ISessionHolderListener listener) {
		logger.debug("Attaching session listener to the SIB");
		ViewerRequest request = new SessionListenerRequest(listener, sib);
		request.setAddListener(true);
		
		sib.getRequestedCommandExecutor().execute(request);
	}

	/**
	 * Detach the Session listener from the SIB. 
	 * Detach the listener from the SIB.
	 * @param listener a listener that implements the ISessionHolderListener methods
	 */
	@Override
	public void addListener(ISubscriptionHolderListener listener) {
		logger.debug("Attaching subscription listener to the SIB");
		ViewerRequest request = new SubscriptionListenerRequest(listener, sib);
		request.setAddListener(true);
		
		sib.getRequestedCommandExecutor().execute(request);
	}

	/**
	 * Attach the Subscription Listener to the SIB.
	 * This subscription listener is listening to the changes of subscribed 
	 * queries in the SIB.
	 * @param listener a listener that implements the ISubscriptionHolderListener methods
	 */
	@Override
	public void addListener(ISemanticContentHolderListener listener) {
		logger.debug("Attaching triple listener to the SIB");
		ViewerRequest request = new SemanticContentListenerRequest(listener, sib);
		request.setAddListener(true);
		
		sib.getRequestedCommandExecutor().execute(request);

	}

	/**
	 * Detach the Subscription listener from the SIB. 
	 * Detach the listener from the SIB.
	 * @param listener a listener that implements the ISubscriptionHolderListener methods
	 */
	@Override
	public void removeListener(ISessionHolderListener listener) {
		logger.debug("Detaching session listener to the SIB");
		ViewerRequest request = new SessionListenerRequest(listener, sib);
		request.setRemoveListener(true);
		
		sib.getRequestedCommandExecutor().execute(request);
	}

	/**
	 * Attach the Semantic Content Listener to the SIB.
	 * The semantic content listener is listening to the changes of the semantic
	 * model that is in the SIB. The most easy way to view this information
	 * is using the data expressed in triples.
	 * @param listener a listener that implements the ISemanticContentHolderListener methods
	 */
	@Override
	public void removeListener(ISubscriptionHolderListener listener) {
		logger.debug("Detaching subscription listener to the SIB");
		ViewerRequest request = new SubscriptionListenerRequest(listener, sib);
		request.setRemoveListener(true);
		
		sib.getRequestedCommandExecutor().execute(request);
	}

	/**
	 * Detach the semantic content listener from the SIB. 
	 * Detach the listener from the SIB.
	 * @param listener a listener that implements the ISemanticContentHolderListener methods
	 */
	@Override
	public void removeListener(ISemanticContentHolderListener listener) {
		logger.debug("Detaching triple listener to the SIB");
		ViewerRequest request = new SemanticContentListenerRequest(listener, sib);
		request.setRemoveListener(true);
		
		sib.getRequestedCommandExecutor().execute(request);
	}	
}
