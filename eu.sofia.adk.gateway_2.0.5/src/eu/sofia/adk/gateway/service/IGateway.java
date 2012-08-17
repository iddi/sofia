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

package eu.sofia.adk.gateway.service;

import java.util.Properties;

import eu.sofia.adk.gateway.exception.GatewayException;
import eu.sofia.adk.sib.service.ISIB;

/**
 * This interface is for start/stop a gateway.
 * @author Raul Otaolea, Raul.Otaolea@tecnalia.com, Tecnalia
  */
public interface IGateway {
	
	/**
	 * Starts a gateway
	 * @throws GatewayException
	 */
	public void start() throws GatewayException;

	/**
	 * Stops a gateway
	 * @throws GatewayException
	 */
	public void stop() throws GatewayException;
	
	/**
	 * Retrieves the id of the gateway
	 */
	public int getId();
	
	/**
	 * Retrieves the state of a Gateway (STOPPED or RUNNING)
	 */
	public int getStatus();
	
	/**
	 * Gets if the gateway is running
	 * @return <code>true</code> if the gateway is running
	 */
	public boolean isRunning();
	
	/**
	 * Gets if the gateway is stopped
	 * @return <code>true</code> if the gateway is stopped
	 */
	public boolean isStopped();	
	
	/**
	 * Retrieves the SIB this gateway is attached to.
	 */
	public ISIB getSIB();
	
	/**
	 * Adds a listener to see gateway state changes (running, stopped).
	 */
	public void addStateListener(IGatewayStateListener listener);		

	/**
	 * Removes a listener to see gateway state changes (running, stopped).
	 */
	public void removeStateListener(IGatewayStateListener listener);		
	
	/**
	 * Retrieves the name of the Gateway
	 */
	public String getName();

	/**
	 * Retrieves the type of the Gateway.
	 */
	public String getType();
	
	/**
	 * Retrieves the Properties.
	 */
	public Properties getProperties();
	
	
}
