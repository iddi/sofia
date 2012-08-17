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

package eu.sofia.adk.gateway;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import eu.sofia.adk.gateway.exception.GatewayException;
import eu.sofia.adk.gateway.service.IGateway;
import eu.sofia.adk.gateway.service.IGatewayFactory;
import eu.sofia.adk.sib.service.ISIB;

/**
 * A gateway manager is a service responsible for creating and destroying instances of gateway.
 * There is a manager per gateway types (one for TCP/IP, one for Bluetooth, etc.)  
 * @author Raul Otaolea, Raul.Otaolea@tecnalia.com, Tecnalia-Tecnalia 
 */
public abstract class AbstractGatewayFactory implements IGatewayFactory, ServiceTrackerCustomizer {

	/** A list containing all gateway instances of this type */
	protected Map<Integer, AbstractGateway> instances;
	
	/** A list with the OSGi service registrations **/
	private Map<Integer, ServiceRegistration> registrations;
	
	/** Declares a bundle context */
	private BundleContext bc;

	/**
	 * Creates a new instance of a gateway manager.
	 * @param bc the bundle context
	 */
	public AbstractGatewayFactory(BundleContext bc) {
		this.bc = bc;
		this.instances = new HashMap<Integer, AbstractGateway>();
		this.registrations = new HashMap<Integer, ServiceRegistration>();
	}
	
	/**
	 * Creates a new instance of a gateway. This method must be implemented by gateway managers
	 * in order to create new gateway instances.
	 * @param name				the name of the new gateway.
	 * @param sib				the SIB this new gateway will be attached to.
	 * @param properties		the properties of this gateway.
	 * @return					a new gateway
	 * @throws GatewayException	if the gateway can not be created.
	 */
	public abstract AbstractGateway createNewInstance(String name, ISIB sib, Properties properties) throws GatewayException;
	
	@Override
	public Object addingService(ServiceReference reference) {
		// Nothing to do.
		return null;
	}

	@Override
	public void modifiedService(ServiceReference reference, Object serviceObject) {
		// Obtains the SIB
		Object obj = bc.getService(reference);
		
		if (obj instanceof ISIB) {
			ISIB sib = (ISIB) bc.getService(reference);
			for(AbstractGateway ag: instances.values()) {
				ag.setSIB(sib);
			}
		}
	}

	@Override
	public void removedService(ServiceReference reference, Object service) {
		// Obtains the SIB
		Object obj = bc.getService(reference);
		
		if (obj instanceof ISIB) {
			ISIB sib = (ISIB) bc.getService(reference);
			Iterator<AbstractGateway> iterator = instances.values().iterator();
			while(iterator.hasNext()) {
				AbstractGateway ag = iterator.next();
				if(ag.getSIB().equals(sib)) {
					iterator.remove();
					try {
						ag.stop();
					} catch(GatewayException ge) {
						System.err.println("Could not stop gateway [" + ag.getName() + "]: " + ge.getMessage());
					} catch(Exception e) {
						e.printStackTrace();
					}
					ServiceRegistration sr = this.registrations.remove(ag.getId());
					if(sr != null) {
						sr.unregister();
					}
				}
			}
		}
	}
	
	/**
	 * Creates a new instance of a gateway
	 * @param name				the name of the new gateway.
	 * @param sib				the SIB this new gateway will be attached to.
	 * @param properties		the properties of this gateway.
	 * @return					a new gateway
	 * @throws GatewayException	if the gateway can not be created.
	 */
	public int create(String name, ISIB sib, Properties properties) throws GatewayException {
		
		// First create the instance.
		AbstractGateway gw = createNewInstance(name, sib, properties);
		
		// Then register it.
		ServiceRegistration service = bc.registerService(IGateway.class.getName(), gw, properties);

		this.instances.put(gw.getId(), gw);
		this.registrations.put(gw.getId(), service);

		return gw.getId();
	}

	/**
	 * Destroys an instance of a gateway
	 * @param ag				the gateway reference to be destroyed.
	 * @throws GatewayException	if the gateway can not be destroyed.
	 */	
	@Override
	public void destroy(int id) throws GatewayException {
		AbstractGateway ag = instances.remove(id);
		if(ag != null) {
			try {
				// Unregister from OSGi
				ServiceRegistration sr = this.registrations.remove(ag.getId());
				if(sr != null) {
					sr.unregister();
				}
				// Stop the gateway.
				ag.stop();
				ag = null;
			} catch(GatewayException ge) {
				throw new GatewayException(ge.getMessage(), ge);
			} catch(Exception e) {
				throw new GatewayException(e.getMessage(), e);
			}
		} else {
			throw new GatewayException("The gateway with id=" + id + "does no exist");
		}
	}
	
	/**
	 * Shutdowns the manager. This method stops and remove all gateway instances of the manager.
	 */
	public void shutdown() {
		// Unregister all from OSGi.
		for(ServiceRegistration sr : registrations.values()) {
			sr.unregister();
		}
		
		// Stop all gateways.
		for(AbstractGateway ag: instances.values()) {
			try {
				ag.stop();
			} catch(GatewayException ge) {
				System.err.println("Could not stop gateway [" + ag.getName() + "]: " + ge.getMessage());
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		// Clear caches.
		this.instances.clear();
		this.registrations.clear();
	}
	
	@Override
	public AbstractGateway getGateway(int id) {
		return this.instances.get(new Integer(id));
	}
	
	@Override
	public Collection<Integer> listGateways() {
		return instances.keySet();
	}
}
