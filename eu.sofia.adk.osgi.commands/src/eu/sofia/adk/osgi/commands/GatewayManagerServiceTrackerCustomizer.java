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
/*

 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *    
 *      http://www.apache.org/licenses/LICENSE-2.0
 *    
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *    
 */

import java.util.HashMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import eu.sofia.adk.gateway.service.IGatewayFactory;

/**
 * This class is a service tracker customizer that detects if any Gateway Factory service
 * has been registered or unregistered in the OSGI Framework.
 * 
 * @author Cristina López, cristina.lopez@esi.es, ESI
 *
 */
//@SuppressWarnings({"rawtypes", "unchecked"})
public class GatewayManagerServiceTrackerCustomizer implements ServiceTrackerCustomizer {

	
	/**
	 * List containing the types of gateway available
	 */
	private HashMap<String, IGatewayFactory> gwMgtMap;
	
	/**
	 * Bundle Context
	 */
	private BundleContext bc;
	
	/**
	 * Constructor with one parameter 
	 * @param bc BundleContext
	 */
	public GatewayManagerServiceTrackerCustomizer(BundleContext bc){
		this.bc=bc;
		gwMgtMap= new HashMap<String,IGatewayFactory>();
	}

	/**
	 * Retrieves the map containing the types of gateway available
	 * @return
	 */
	public HashMap<String, IGatewayFactory> getGwMgtMap() {
		return gwMgtMap;
	}

	@Override
	/*
	 * (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer
	 */
	public Object addingService(ServiceReference reference) {
		Object obj = bc.getService(reference);
		if (obj instanceof IGatewayFactory){	
			String type= (String) reference.getProperty("Gateway");	
			IGatewayFactory gwMgt=(IGatewayFactory) bc.getService(reference);
			gwMgtMap.put(type, gwMgt);
		}
		return obj;
	}

	@Override
	/*
	 * (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer
	 */
	public void modifiedService(ServiceReference reference, Object service) {
		Object obj = bc.getService(reference);
		if (obj instanceof IGatewayFactory){
			String type= (String) reference.getProperty("Gateway");		
			IGatewayFactory gwMgt=(IGatewayFactory) bc.getService(reference);
			gwMgtMap.put(type, gwMgt);
		}
	}

	@Override
	/*
	 * (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer
	 */
	public void removedService(ServiceReference reference, Object service) {
		
		Object obj = bc.getService(reference);
		if (obj instanceof IGatewayFactory){
			String type= (String) reference.getProperty("Gateway");		
			gwMgtMap.remove(type);
		}
	}
	
}
