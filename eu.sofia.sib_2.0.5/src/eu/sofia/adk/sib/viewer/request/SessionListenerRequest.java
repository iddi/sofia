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

package eu.sofia.adk.sib.viewer.request;

import eu.sofia.adk.sib.data.ISessionHolderListener;
import eu.sofia.adk.sib.ssap.SIB;

/**
 * The <code>ViewerRequest</code> extends the runnable to 
 * implement the listening adding/removing and the consult
 * of the SIB content.
 *  
 * @author Fran Ruiz, fran.ruiz@tecnalia.com, ESI
 *
 */
public class SessionListenerRequest extends ViewerRequest {
	
	private ISessionHolderListener listener;
	
	public SessionListenerRequest(ISessionHolderListener listener, SIB sib) {
		super(sib);
    	this.listener = listener;
	}

	/**
	 * Process the request
	 */
	public void processRequest() {
		if (addListener) {
			sib.getViewerListenerManager().addListener(listener);
			listener.initSessions(sib.getSessionManager().getAllSessions());
		} else {
			sib.getViewerListenerManager().removeListener(listener);
		}
	}
}
