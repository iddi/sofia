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

import org.apache.log4j.Logger;

import eu.sofia.adk.sib.ssap.SIB;

/**
 * The <code>ViewerRequest</code> extends the runnable to 
 * implement the listening adding/removing and the consult
 * of the SIB content.
 *  
 * @author Fran Ruiz, fran.ruiz@tecnalia.com, ESI
 *
 */
public abstract class ViewerRequest implements Runnable {
	
	/** The logger */
	private static Logger logger;
	
	protected boolean addListener = false;
	
	protected SIB sib;
	
	public ViewerRequest(SIB sib) {
    	// Inits the logger
    	logger = Logger.getLogger(ViewerRequest.class);
    	this.sib = sib;
	}

	/**
	 * Process the request
	 */
	public abstract void processRequest();	
	
	/**
	 * The runnable method
	 */
	@Override
	public void run() {
		try {
			// Process the viewer request
			processRequest();
		} catch (Exception ex) {
			logger.error("Cannot process the request. Caused by " + ex.getMessage());
			logger.error("Stack trace:\n");
			ex.printStackTrace();
		}
	}

	/**
	 * Set the addition to listener
	 * @param addListener
	 */
	public void setAddListener(boolean addListener) {
		this.addListener = addListener;
	}
	
	/**
	 * Sets the removal of the listener
	 * @param removeListener
	 */
	public void setRemoveListener(boolean removeListener) {
		this.addListener = !removeListener;
	}	

}
