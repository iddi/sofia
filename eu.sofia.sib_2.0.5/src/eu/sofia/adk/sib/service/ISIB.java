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

import eu.sofia.adk.sib.exception.SIBException;
import eu.sofia.adk.ssapmessage.SSAPMessageRequest;


/**
 * The ISIB interface exposes the SSAP protocol operations to communicate
 * the Smart Applications and the SIB.
 * 
 * @author Fran Ruiz, ESI
 */
public interface ISIB {

	/**
	 * Starts a SIB instance, publishing the available services
	 * @param id the SIB identifier
	 * @throws SIBException when the SIB cannot start
	 */
	public void start() throws SIBException;
	
	/**
	 * Stops a SIB instance, unpublishing the available services
	 * @param id the SIB identifier
	 * @throws SIBException if the SIB cannot start.
	 */
	public void stop() throws SIBException;
	
	/**
	 * Indicates if a SIB is running
	 */
	public boolean isRunning();

	/**
	 * Indicates if a SIB is running
	 */
	public boolean isStopped();
	
	/**
	 * Gets the Smart Space ID
	 * @return the name for the smart space covered by the SIB
	 */
	public String getName();

	/**
	 * Gets the numeric identifier.
	 * @return the identifier for the smart space covered by the SIB
	 */
	public int getId();

	/**
	 * Connects the smart application to this SIB.
	 * This SIB has full information about the joined nodes.
	 * @param proxy the reference for reaching to the smart application (node)
	 * @param message the SSAPMessage corresponding to the JOIN transaction
	 * @throws SIBException if the SIB find an error.
	 */
	public void connect(SSAPMessageRequest message) throws SIBException, NullPointerException;
	
	/**
	 * Disconnects the smart application from this SIB.
	 * Removes all this information from the SIB
	 * @param message the SSAPMessage corresponding to the LEAVE transaction
	 * @throws SIBException if the SIB find an error.
	 */
	public void disconnect(SSAPMessageRequest message) throws SIBException, NullPointerException;
	
	/**
	 * Process the SSAPMessages to interoperate semantically with the SIB.
	 * @param message the SSAPMessage
	 * @throws SIBException if the SIB find an error.
	 */
	public void process(SSAPMessageRequest message) throws SIBException, NullPointerException;
	
	/**
	 * Disconnects a KP from this SIB.
	 * Removes all this information from the SIB
	 * @param kpID the KP identifier
	 * @param errorDesc Error description
	 * @throws SIBException if the SIB find an error.
	 */
	public void disconnect(String kpID, String errorDesc) throws SIBException;
	
	/**
	 * Resets the information associated to a SIB
	 */
	public void reset();		
	
	/**
	 * Adds a listener to the SIB state changes (running, stopped).
	 */
	public void addStateListener(ISIBStateListener listener);		

	/**
	 * Removes a listener to the SIB state changes (running, stopped).
	 */
	public void removeStateListener(ISIBStateListener listener);		
	
}
