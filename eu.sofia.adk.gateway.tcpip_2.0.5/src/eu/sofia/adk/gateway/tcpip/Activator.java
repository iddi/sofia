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

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import eu.sofia.adk.gateway.service.IGatewayFactory;
import eu.sofia.adk.sib.service.ISIB;

/**
 * TCP/IP gateway manager to connect KP with tcp/ip connectors with the SIB.
 * @author Raul Otaolea, Raul.Otaolea@tecnalia.com, Tecnalia-Tecnalia
 */
public class Activator implements BundleActivator {


	
	/** The manager responsible for creating/destroying TCP/IP Gateway instances */
	private TCPIPGatewayFactory manager;

	public Activator() {

	}
	
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		Properties props = new Properties();
		props.put("Gateway", "TCP/IP");

		manager = new TCPIPGatewayFactory(context);
		context.registerService(IGatewayFactory.class.getName(), manager, props);
		System.out.println("Manager for TCP/IP registered");
		ServiceTracker tracker = new ServiceTracker(context, ISIB.class.getName(), manager);
		tracker.open();
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		// stop the server.
		manager.shutdown();
		manager = null;
		System.out.println("Manager for TCP/IP unregistered");
	}

}
