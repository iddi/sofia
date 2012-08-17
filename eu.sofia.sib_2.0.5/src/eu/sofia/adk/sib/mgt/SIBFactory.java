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

package eu.sofia.adk.sib.mgt;

import java.util.HashMap;
import java.util.Hashtable;

import org.apache.log4j.Logger;
import org.osgi.framework.ServiceRegistration;

import eu.sofia.adk.sib.SIBActivator;
import eu.sofia.adk.sib.exception.SIBException;
import eu.sofia.adk.sib.service.ISIB;
import eu.sofia.adk.sib.service.ISIBFactory;
import eu.sofia.adk.sib.service.IViewer;
import eu.sofia.adk.sib.ssap.SIB;
import eu.sofia.adk.sib.viewer.Viewer;

/**
 * This class implements the Server Management Interface
 * 
 * @author Cristina López, ESI, cristina.lopez@tecnalia.com
 */
public class SIBFactory implements ISIBFactory {

	/** The list of created SIBs */
	private HashMap<Integer,SIBRegistration> servers;
	
	/** The Logger */
	private static Logger logger;
	
	/**
	 * The Constructor 
	 */
	public SIBFactory() {
		servers = new HashMap<Integer, SIBRegistration>();
		logger = Logger.getLogger(SIBFactory.class);
	}
	
	/**
	 * Creates a new SIB instance
	 * @param props the set of parameters
	 * @return an unique SIB identifier obtained from the properties
	 * @throws SIBException when the SIB cannot be created
	 */
	@SuppressWarnings("rawtypes")
	public int create(String name, Hashtable<String, Object> props) throws SIBException {
		
		try{
			// Creates a new SIB
			SIB sib = new SIB(name);
			
			// Add name property to the SIB.
			Hashtable<String, Object> sibProperties = new Hashtable<String, Object>();
			if(props != null) {
				sibProperties.putAll(props);
			}
			sibProperties.put(sib.getName(), sib.getId());
			
			// Registers the SIB Service.
			ServiceRegistration sr1 = SIBActivator.getBundleContext().registerService(ISIB.class.getName(), sib, sibProperties);
			logger.debug("SIB server registered.");
			
			// Add new property (same as in SIB registration)
			Hashtable<String, Object> viewerProps = new Hashtable<String, Object>();
			viewerProps.putAll(sibProperties); 
			viewerProps.put(sib.getName(), "Viewer");

			// Registers the Viewer services
			Viewer viewService = new Viewer(sib);
			sib.addViewerListener(viewService); // adds the viewer to the listeners
			ServiceRegistration sr2 = SIBActivator.getBundleContext().registerService(IViewer.class.getName(), viewService, viewerProps);	
			logger.debug("Viewer service registered.");
			
			// Add to cache.
			SIBRegistration sr = new SIBRegistration();
			sr.setSIB(sib);
			sr.setSIBRegistration(sr1);
			sr.setViewerRegistration(sr2);
			
			servers.put(sib.getId(), sr);

			logger.debug("Created and registered new instance of a SIB server");
			
			return sib.getId();
		} catch(Exception e){
			logger.error("Cannot create a new instance of the SIB: " + e.getMessage(), e.getCause());
			e.printStackTrace();
			throw new SIBException(e.getMessage(), e.getCause());
		}
	}

	/**
	 * Destroys a SIB instance
	 * @param sibId the SIB identifier
	 * @throws SIBException when the SIB instance cannot be destroyed
	 */
	public void destroy(int sibId) throws SIBException {
		try {
			SIBRegistration sr = servers.get(sibId);
			if(sr.getSIB().isRunning()) {
				sr.stop();
			} 
			sr.unregister();
			logger.debug("SIB services unregistered.");
			
			servers.remove(sibId);
			logger.debug("SIB server deleted");
		} catch(Exception e) {
			logger.error("Cannot destroy a SIB instance: " + e.getMessage(), e.getCause());
			e.printStackTrace();
			throw new SIBException(e.getMessage(), e.getCause());
		}
	}

	/**
	 * Gets a SIB for a given identifier
	 * @param sibId the SIB identifier
	 * @return a ISIB representing a SIB
	 * @throws SIBException 
	 * @throws SIBException when a SIB instance cannot be obtained
	 */
	public ISIB getSIB(int sibId) throws SIBException{
		ISIB _return = null;
		try{
			_return = servers.get(sibId).getSIB();
		} catch(Exception e) {
			logger.error("Cannot get a SIB identified by " + sibId +": " + e.getMessage(), e.getCause());
			e.printStackTrace();
			throw new SIBException(e.getMessage(), e.getCause());
		}
		return _return;
	}
}



