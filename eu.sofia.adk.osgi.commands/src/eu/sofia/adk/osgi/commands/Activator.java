/*******************************************************************************
 * Copyright (c) 2009,2011 Tecnalia Research and Innovation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Cristina López (Fundacion European Software Institute) - initial API, implementation and documentation
 *******************************************************************************/ 

package eu.sofia.adk.osgi.commands;

import java.util.ArrayList;

import org.eclipse.core.runtime.Plugin;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import eu.sofia.adk.gateway.service.IGateway;
import eu.sofia.adk.gateway.service.IGatewayFactory;
import eu.sofia.adk.osgi.commands.data.Element;
import eu.sofia.adk.osgi.commands.provider.SofiaCommandProvider;
import eu.sofia.adk.sib.service.ISIB;
import eu.sofia.adk.sib.service.ISIBFactory;


/**
 * This Activator class prepares the plug-in to work both with the manager of the SIB
 * and the manager of the Gateway.
 * 
 * This plug-in extends the OsGi default available commands with a collection of commands
 * that will be available in the scope of Sofia to execute operations over SIB servers
 * and Gateways.
 * 
 * @author Cristina López, cristina.lopez@esi.es, ESI 
 */
public class Activator extends Plugin {

	/**
	 * Plug-in Id
	 */
	public static final String PLUGIN_ID = "eu.sofia.adk.osgi.commands";

	/**
	 * Shared Activator Instance
	 */
	private static Activator plugin;
	
	/**
	 * Command Provider service property
	 */
	private CommandProvider service; 
	
	/**
	 * ISIB Factory service tracker customizer
	 */
	private ServerManagerServiceTrackerCustomizer mgtstc;	
	/**
	 * Gateway Factory service tracker customizer
	 */
	private GatewayManagerServiceTrackerCustomizer gwmgtstc;
	
	/**
	 * SIB service tracker customizer
	 */
	private SIBServiceTrackerCustomizer sibstc;
	
	/**
	 * Gateway service tracker customizer
	 */
	private GatewayServiceTrackerCustomizer gwstc;
	
	/**
	 * List of SIB instances
	 */
	private ArrayList<Element> sibList= new ArrayList<Element>();
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/**
	 * Retrieves the ISIB Factory service tracker customizer
	 * @return ServerManagerServiceTrackerCustomizer
	 */
	public ServerManagerServiceTrackerCustomizer getMgtstc() {
		return mgtstc;
	}

	/**
	 * Retrieves the IGateway factory service tracker customizer
	 * @return GatewayManagerServiceTrackerCustomizer
	 */
	public GatewayManagerServiceTrackerCustomizer getgwmgtstc() {
		return gwmgtstc;
	}
	
	/**
	 * Retrieves the SIB service tracker customizer
	 * @return SIBServiceTrackerCustomizer
	 */
	public SIBServiceTrackerCustomizer getSibstc() {
		return sibstc;
	}
	
	/**
	 * Retrieves the Gateway service tracker customizer
	 * @return
	 */
	public GatewayServiceTrackerCustomizer getGwstc() {
		return gwstc;
	}

	/**
	 * Retrieves the list of SIB instances
	 * @return
	 */
	public ArrayList<Element> getSibList() {
		return sibList;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	//@SuppressWarnings({"rawtypes", "unchecked"})
	//@SuppressWarnings({"rawtypes"})
	public void start(BundleContext context) throws Exception {        
		plugin= this;
		

		
		//Obtains the SIB manager service
		mgtstc= new ServerManagerServiceTrackerCustomizer(context);
		ServiceTracker serverTracker = new ServiceTracker(context, ISIBFactory.class.getName(), mgtstc);
		serverTracker.open();
		
		//Obtains the Generic Gateway manager service
		gwmgtstc= new GatewayManagerServiceTrackerCustomizer(context);
		ServiceTracker tcpipGwTracker = new ServiceTracker(context, IGatewayFactory.class.getName(), gwmgtstc);
		tcpipGwTracker.open();	
		
		//Obtains the registered SIB instances
		sibstc= new SIBServiceTrackerCustomizer(context);
		ServiceTracker sibTracker = new ServiceTracker(context, ISIB.class.getName(), sibstc);
		sibTracker.open();
		
		//Obtains the registered Gateway instances
		gwstc= new GatewayServiceTrackerCustomizer(context);
		ServiceTracker gwTracker = new ServiceTracker(context, IGateway.class.getName(), gwstc);
		gwTracker.open();
		
		// register the service   
		service = new SofiaCommandProvider();
		context.registerService(CommandProvider.class.getName(), service, null);
		} 

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {        
		service = null;  
		mgtstc=null;
		gwmgtstc=null;
		sibstc=null;
		gwstc=null;
		
		plugin=null;
		} 
	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

}
