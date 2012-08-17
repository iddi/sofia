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

package eu.sofia.adk.gateway.tcpip;

import java.util.Properties;

import org.osgi.framework.BundleContext;

import eu.sofia.adk.gateway.AbstractGateway;
import eu.sofia.adk.gateway.AbstractGatewayFactory;
import eu.sofia.adk.gateway.exception.GatewayException;
import eu.sofia.adk.gateway.tcpip.core.TCPIPGateway;
import eu.sofia.adk.gateway.tcpip.core.TCPIPGatewayConfiguration;
import eu.sofia.adk.sib.service.ISIB;

/**
 * TCPIPManager.java
 * 
 * The TCP/IP manager responsible for creating and destroying tcp/ip gateways.
 * @author Raul Otaolea, Raul.Otaolea@tecnalia.com, Tecnalia-Tecnalia
 */
public class TCPIPGatewayFactory extends AbstractGatewayFactory {

	/** The name of this Gateway */
	private static final String TYPE = "TCP/IP";
	
	
	public TCPIPGatewayFactory(BundleContext bc) {
		super(bc);	
	}
	
	public AbstractGateway createNewInstance(String name, ISIB sib, Properties properties) throws GatewayException {
		try {
			if(checkPort(properties.get(TCPIPGatewayConfiguration.PORT))) {
				TCPIPGateway gw = new TCPIPGateway(name, getType(), sib, properties);
				System.out.println("Created a new TCP/IP Gateway instance [" + gw.getName() + "]");
				return gw;
			} else {
				throw new GatewayException("Can not create a tcp/ip gateway instance.");
			}
		} catch(Exception e) {
			throw new GatewayException(e.getMessage(), e);
		}
	}

	public void destroy(int id) throws GatewayException {
		super.destroy(id);
		System.out.println("Destroyed TCP/IP instance Gateway ["+id+"]");
	}
	
	public String getType() {
		return TYPE;
	}
	
	/**
	 * @return The default properties of the TCP/IP gateway.
	 */
	public Properties getConfigurableProperties() {
		return TCPIPGatewayConfiguration.getDefaultProperties();
	}

	private boolean checkPort(Object sport) throws GatewayException {
		int port = TCPIPGatewayConfiguration.DEFAULT_PORT; 
		if(sport != null) {
			if(sport instanceof String) {
				try {
					port = Integer.parseInt((String)sport);
				} catch(Exception e) {
					throw new NumberFormatException("Parameter 'PORT' is not valid");
				}
			} else if(sport instanceof Integer) {
				port = (Integer)sport;
			} else {
				throw new NumberFormatException("Parameter 'PORT' is not valid");
			}
		}
		// Check if there is another gateway running in the same port.
		for(AbstractGateway gw : instances.values()) {
			TCPIPGateway tgw = (TCPIPGateway)gw;
			if(port == tgw.getConfig().getPort()) {
				throw new GatewayException("Port " + port + " is already used by gateway " + gw.getName());
			}
		}

		return true;
	}
}
