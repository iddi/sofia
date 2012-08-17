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

package eu.sofia.adk.common;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/** 
 * The activator class for the common part of the set of plugins.
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
 * @author Fran Ruiz, Fran.Ruiz@tecnalia.com, Tecnalia
 */
public class CommonActivator implements BundleActivator {

	/** The plug-in id */
	public static final String PLUGIN_ID = "eu.sofia.adk.common";
	
	/**
	 * Creates the plug-in runtime object.
	 * 
	 * Note that instances of plug-in runtime classes are automatically 
	 * created by the platform in the course of plug-in activation. 
	 */	
	public CommonActivator() {
	
	}
	
	/**
	 * Called when this bundle is started so the Framework can perform the 
	 * bundle-specific activities necessary to start this bundle. 
	 * <p>This method can be used to register services or to allocate any 
	 * resources that this bundle needs.<br/>
	 * This method must complete and return to its caller in a timely manner.</p>
	 * 
	 * @param context the execution context of the bundle being started
	 * @throws Exception if this method throws an exception, this bundle is marked as stopped and the framework will remove this bundle
	 */
	public void start(BundleContext context) throws Exception {
	}

	/**
	 * Called when this bundle is stopped so the Framework can perform the 
	 * bundle-specific activities necessary to stop the bundle. 
	 * <p>In general, this method should undo the work that the 
	 * <code>BundleActivator.start</code> method started. <br/>There should be 
	 * no active threads that were started by this bundle when this bundle 
	 * returns. A stopped bundle must not call any Framework objects.</p>
	 * <p>This method must complete and return to its caller in a timely manner.</p>
	 * 
	 * @param context the execution context of the bundle being stopped
	 * @throws Exception if this method throws an exception, this bundle is still marked as stopped and the framework will remove this bundle the bundle's listeners, unregister all services registered by the bundle and release all services used by the bundle.
	 */
	public void stop(BundleContext context) throws Exception {
	}
}
