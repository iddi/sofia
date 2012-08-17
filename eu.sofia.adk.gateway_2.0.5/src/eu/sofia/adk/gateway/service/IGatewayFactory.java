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

import java.util.Collection;
import java.util.Properties;

import eu.sofia.adk.gateway.AbstractGateway;
import eu.sofia.adk.gateway.exception.GatewayException;
import eu.sofia.adk.sib.service.ISIB;

/**
 * This interface must be implemented by the gateways managers in order to manage them. 
 * @author Raul Otaolea, Raul.Otaolea@tecnalia.com, Tecnalia
 * @author Fran Ruiz, fran.ruiz@tecnalia.com, Tecnalia
 */
public interface IGatewayFactory {
	/**
	 * Creates a Gateway associated to a SIB
	 * @param name the gateway name
	 * @param sib the SIB to what the gateway is associated
	 * @param properties the properties associated to the gateway
	 * @return the identifier of created gateway
	 * @throws GatewayException
	 */
	public int create(String name, ISIB sib, Properties properties) throws GatewayException;
	
	/**
	 * Destroys a gateway identified by the parameter <code>id</code>
	 * @param id the gateway identifier
	 * @throws GatewayException when cannot destroy the gateway
	 */
	public void destroy(int id) throws GatewayException;
	
	/**
	 * Gets the gateway type
	 * @return the gateway type
	 */
	public String getType();
	
	/**
	 * Gets the default properties associated to the gateway type
	 * @return default properties
	 */
	public Properties getConfigurableProperties();
	
	/**
	 * Gets the gateway identified by the parameter
	 * @param id the gateway identifier
	 * @return the AbstractGateway associted to the gateway
	 */
	public AbstractGateway getGateway(int id);
	
	/**
	 * Gets the list of available gateways 
	 * @return the list of gateways
	 */
	public Collection<Integer> listGateways();
}
