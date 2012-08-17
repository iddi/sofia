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

package eu.sofia.adk.sib;

import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.helpers.Loader;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;

import eu.sofia.adk.sib.mgt.SIBFactory;
import eu.sofia.adk.sib.service.ISIBFactory;

/** 
 * The activator class for the Semantic Information Broker (SIB).
 * 
 * A plug-in subclasses this class and overrides the appropriate life cycle 
 * methods in order to react to the life cycle requests automatically issued 
 * by the platform. For compatibility reasons, the methods called for those 
 * life cycle events vary, please see the "Constructors and life cycle methods" 
 * section below.
 * 
 *  Conceptually, the plug-in runtime class represents the entire plug-in 
 *  rather than an implementation of any one particular extension the plug-in 
 *  declares. A plug-in is not required to explicitly specify a plug-in 
 *  runtime class; if none is specified, the plug-in will be given a default 
 *  plug-in runtime object that ignores all life cycle requests 
 *  (it still provides access to the corresponding plug-in descriptor).
 *  
 *  In the case of more complex plug-ins, it may be desirable to define a 
 *  concrete subclass of Plugin. However, just subclassing Plugin is not 
 *  sufficient. The name of the class must be explicitly configured in the 
 *  plug-in's manifest (plugin.xml) file with the class attribute of the 
 *  <code><plugin></code> element markup.
 *  
 *  Instances of plug-in runtime classes are automatically created by the 
 *  platform in the course of plug-in activation. For compatibility reasons, 
 *  the constructor used to create plug-in instances varies, please see the 
 *  "Constructors and life cycle methods" section below.
 *  
 *  The concept of bundles underlies plug-ins. However it is safe to regard 
 *  plug-ins and bundles as synonyms. 
 * 
 * @author Fran Ruiz, fran.ruiz@tecnalia.com, ESI
 */
public class SIBActivator extends Plugin {

	/** The plug-in id */
	public static final String PLUGIN_ID = "eu.sofia.adk.sib";

	/** The shared instance */
	@SuppressWarnings("unused")
	private static SIBActivator plugin;
	
	/** The bundle context */
	private static BundleContext bc;
	
	/** The logger */
	private static Logger logger;
	
	/**
	 * Creates the plug-in runtime object.
	 * 
	 * Note that instances of plug-in runtime classes are automatically 
	 * created by the platform in the course of plug-in activation. 
	 */	
	public SIBActivator() {
		try {
			URL url = Loader.getResource("log4j.properties");
			PropertyConfigurator.configure(url);
			logger = Logger.getLogger(SIBActivator.class);
			System.out.println ("[" + url.toString() + "] Logger initialized (adk-sib_tue).");
		} catch (Exception e) {
			BasicConfigurator.configure();
			logger = Logger.getLogger(SIBActivator.class);
			System.out.println ("Excepci�n al inicializar el log: " + e.toString());

			BasicConfigurator.configure();
			logger.info("Activates the SIB bundle");
		}
	}

	/**
	 * Starts up this plug-in.
	 * 
	 * This method overrides the Plugin class in subclasses that need to do 
	 * something when this plug-in is started. The start class calls the
	 * <code>super()</code> method to ensure that any system requirements 
	 * can be met.
	 * 
	 *  If this method throws an exception, it is taken as an indication that 
	 *  plug-in initialization has failed; as a result, the plug-in will not 
	 *  be activated; moreover, the plug-in will be marked as disabled and 
	 *  ineligible for activation for the duration.
	 *  
	 *  Plug-in startup code should be robust. In the event of a startup 
	 *  failure, the plug-in's shutdown method will be invoked automatically, 
	 *  in an attempt to close open files, etc.
	 *  
	 *  <code>Note 1</code>: This method is automatically invoked by the 
	 *  platform the first time any code in the plug-in is executed.
	 *  
	 *  <code>Note 2</code>: This method is intended to perform simple 
	 *  initialization of the plug-in environment. The platform may terminate 
	 *  initializers that do not complete in a timely fashion.
	 *  
	 *  <code>Note 3</code>: The class loader typically has monitors acquired 
	 *  during invocation of this method. It is strongly recommended that this 
	 *  method avoid synchronized blocks or other thread locking mechanisms, 
	 *  as this would lead to deadlock vulnerability.
	 *  
	 *  <code>Note 4</code>: The supplied bundle context represents the plug-in 
	 *  to the OSGi framework. For security reasons, it is strongly recommended 
	 *  that this object should not be divulged.
	 *  
	 *  <code>Note 5</code>: This method and the stop(BundleContext) may be 
	 *  called from separate threads, but the OSGi framework ensures that both 
	 *  methods will not be called simultaneously.
	 *  
	 *  <strong>Clients must never explicitly call this method.</strong>
	 *   
	 * @see org.eclipse.core.runtime.Plugin#start(BundleContext)
	 */
	@SuppressWarnings("static-access")
	public void start(BundleContext context) throws Exception {
		super.start(context);
		
		plugin = this;
		
		this.bc = context;
		
		try{
			SIBFactory sibManager = new SIBFactory();
			Dictionary<String,String> dict = new Hashtable<String,String>();
			dict.put("SIBMgt", "Semantic Information Broker Management");
			context.registerService(ISIBFactory.class.getName(), sibManager, dict);
			logger.debug("SIB Management services registered.");
		} catch(Exception ex){
			logger.error("Cannot start the SIB services: " + ex.getMessage(), ex);
			ex.printStackTrace();
		}
	}

	/**
	 * Stops this plug-in.
	 * 
	 * This method overrides the Plugin class in subclasses that need to do 
	 * something when this plug-in is shut down. The stop class calls the
	 * <code>super()</code> method to ensure that any system requirements 
	 * can be met.
	 * 
	 * Plug-in shutdown code should be robust. In particular, this method 
	 * should always make an effort to shut down the plug-in. 
	 * Furthermore, the code should not assume that the plug-in was started 
	 * successfully, as this method will be invoked in the event of a failure 
	 * during startup.
	 * 
	 * <code>Note 1</code>: If a plug-in has been automatically started, 
	 * this method will be automatically invoked by the platform when the 
	 * platform is shut down.
	 * 
	 * <code>Note 2<code>: This method is intended to perform simple 
	 * termination of the plug-in environment. The platform may terminate 
	 * invocations that do not complete in a timely fashion.
	 * 
	 * <code>Note 3</code>: The supplied bundle context represents the plug-in 
	 * to the OSGi framework. For security reasons, it is strongly recommended 
	 * that this object should not be divulged.
	 * 
	 * <code>Note 4</code>: This method and the start(BundleContext) may be 
	 * called from separate threads, but the OSGi framework ensures that both 
	 * methods will not be called simultaneously.
	 * 
	 * <strong>Clients must never explicitly call this method.</strong>
	 * 
	 * @see org.eclipse.core.runtime.Plugin#stop(BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Gets the current plugin bundle context
	 * @return the bundle context
	 * @see org.osgi.framework.BundleContext
	 */
	public static BundleContext getBundleContext() {
		return bc;
	}

	/**
	 * Gets the current logger
	 * @return the logger
	 * @see org.apache.log4j.Logger
	 */
	public static Logger getLogger() {
		return logger;
	}

	/**
	 * Sets te logger
	 * @param logger the Logger
	 * @see org.apache.log4j.Logger
	 */
	public static void setLogger(Logger logger) {
		SIBActivator.logger = logger;
	}
	
	
	
}
