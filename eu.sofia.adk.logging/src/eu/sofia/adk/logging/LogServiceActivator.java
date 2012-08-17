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

package eu.sofia.adk.logging;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/** 
 * The activator class for the Logging Service in the ADK.
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
 * @author Aitor Aldazabal, aitor.aldazabal@tecnalia.com, Tecnalia
 * 
 */
public class LogServiceActivator implements BundleActivator {
	
	/** The plug-in id */
	public static final String PLUGIN_ID = "eu.sofia.adk.logging";
	
	/** The Bundle Context */
	private static BundleContext bc;
	
	/** The Service Tracker associated to the log service */
	private ServiceTracker logServiceTracker;
	
	/** Prevents for getting the service while stopping */ 
	private ReadWriteLock logService_lock;
	
	/** The shared instance */
	private static LogServiceActivator plugin;
	
	/** OSGi logging service */
	private LogService logService;
	
	/**
	 * Constructor
	 */
	public LogServiceActivator() {
		plugin = this;
		this.logService_lock = new ReentrantReadWriteLock();
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
	public void start(BundleContext context) throws Exception {
		bc = context;
		ServiceTrackerCustomizer logSTC = createLogTrackerCustomizer();
		logServiceTracker = new ServiceTracker(context, LogService.class.getName(), logSTC);
		logServiceTracker.open();
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
		this.logServiceTracker.close();
	}
	
	/**
	 * Gets the log service instance
	 * @return the log service instance
	 */
	public static LogServiceActivator getInstance() {
		if(plugin == null) {
			plugin = new LogServiceActivator();
		}
		return plugin;
	}
	/**
	 * 
	 * @return
	 */
	public LogService getLogService () {
		return this.logService;
	}
	/**
	 * 
	 * @param level
	 * @param message
	 */
	public void log(int level, String message) {
		logService_lock.readLock().lock();
		if(this.getLogService()!=null){
			this.getLogService().log(level, message);
		}
		logService_lock.readLock().unlock();
	}
	/**
	 * 
	 * @param level
	 * @param message
	 * @param t
	 */
	public void log(int level, String message, Throwable t) {
		logService_lock.readLock().lock();
		if(this.getLogService()!=null){
			this.getLogService().log(level, message, t);
		}
		logService_lock.readLock().unlock();
	}
	/**
	 * 
	 * @param sr
	 * @param level
	 * @param message
	 */
	public void log(ServiceReference sr, int level, String message) {
		logService_lock.readLock().lock();
		if(this.getLogService()!=null){
			this.getLogService().log(sr, level, message);
		}
		logService_lock.readLock().unlock();
	}
	/**
	 * 
	 * @param sr
	 * @param level
	 * @param message
	 * @param t
	 */
	public void log(ServiceReference sr, int level, String message, Throwable t) {
		logService_lock.readLock().lock();
		if(this.getLogService()!=null){
			this.getLogService().log(sr, level, message, t);
		}
		logService_lock.readLock().unlock();
	}

	/**
	 * Gets the Service Tracker Customizer associated to the OSGi Log Service
	 * @return the ServiceTrackerCustomizer for the OSGi Log Service 
	 */
	private ServiceTrackerCustomizer createLogTrackerCustomizer() {
		return new ServiceTrackerCustomizer() {
			public Object addingService(ServiceReference reference) {
				Object service = bc.getService(reference);
				synchronized (LogServiceActivator.this) {
					if (LogServiceActivator.this.logService == null) {
						LogServiceActivator.this.logService = (LogService) service;
						LogServiceActivator.this.bind();
					}
				}
				return service;
			}

			public void modifiedService(ServiceReference reference, Object service) {
				// No service property modifications to handle 
			}		
		
			public void removedService(ServiceReference reference, Object service) {
				synchronized (LogServiceActivator.this) {
					if (service != LogServiceActivator.this.logService) {
						return;
					}
					LogServiceActivator.this.unbind();
					LogServiceActivator.this.bind();
				}
			}
		};
	}
	
	/**
	 * Binds a service obtained from the ServiceTracker
	 */
	private void bind() {
		if (logService == null) {
			logService = (LogService) logServiceTracker.getService();
		}
	}
	
	/**
	 * Unbinds the log service
	 */
	private void unbind() {
		if (logService != null) {
			logService = null;
		}
	}
	
}
