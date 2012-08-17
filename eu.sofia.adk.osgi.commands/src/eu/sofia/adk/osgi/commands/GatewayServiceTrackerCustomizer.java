/*******************************************************************************
 * Copyright (c) 2009,2011 Tecnalia Research and Innovation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Cristina López (Fundacion European Software Institue))- initial API, implementation and documentation
 *******************************************************************************/ 


package eu.sofia.adk.osgi.commands;

import java.util.List;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import eu.sofia.adk.gateway.service.IGateway;
import eu.sofia.adk.sib.service.ISIB;
import eu.sofia.adk.osgi.commands.data.Element;

/**
 * This class is a service tracker customizer that detects if any Gateway instance
 * has been registered or unregistered in the OSGI Framework.
 * 
 * @author Cristina López, cristina.lopez@esi.es, ESI
 *
 */
//@SuppressWarnings({"rawtypes", "unchecked"})
public class GatewayServiceTrackerCustomizer implements ServiceTrackerCustomizer{

	/**
	 * Gatewat Element
	 */
	protected IGateway gw = null;
	/**
	 * Bundle Context
	 */
	private BundleContext bc;
	
	/**
	 * Constructor
	 * @param bc
	 */
	public GatewayServiceTrackerCustomizer(BundleContext bc) {
		this.bc=bc;
	}

	@Override
	/*
	 * (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer
	 */
	public Object addingService(ServiceReference reference) {
		// Obtains the Gateway
		Object obj = bc.getService(reference);

		if (obj instanceof IGateway) {
			gw = (IGateway) bc.getService(reference);			
			ISIB sib=gw.getSIB();

			int index= getElementIndex(sib);
			Element sibElement= Activator.getDefault().getSibList().get(index);
			sibElement.getGateways().add(gw);	
					
			return gw;
		} else {
			return null;
		}
	}

	@Override
	/*
	 * (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer
	 */
	public void modifiedService(ServiceReference reference, Object serviceObject) {
		// Obtains the Gateway
		Object obj = bc.getService(reference);

		if (obj instanceof IGateway) {
			gw = (IGateway) bc.getService(reference);			
			ISIB sib=gw.getSIB();
			
			int i=getElementIndex(sib);
			Element sibElement= Activator.getDefault().getSibList().get(i);
			sibElement.getGateways().remove((IGateway)serviceObject);
			sibElement.getGateways().add(gw);		
				
		} else {
		}
		
	}

	@Override
	/*
	 * (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer
	 */
	public void removedService(ServiceReference reference, Object serviceObject) {
		// Obtains the Gateway
		Object obj = bc.getService(reference);

		if (obj instanceof IGateway) {
			gw = (IGateway) bc.getService(reference);			
			ISIB sib=gw.getSIB();
			int i=getElementIndex(sib);
			Element sibElement= Activator.getDefault().getSibList().get(i);
			sibElement.getGateways().remove(gw);		

		}
		
	}
	
	/**
	 * Method that obtains the index of a certain Sib server in the list of Sib instances located in the plug-in Activator
	 * @param sib
	 * @return
	 */
	private int getElementIndex(ISIB sib){
		List<Element> elements= Activator.getDefault().getSibList();
		int i=0;
		boolean found=false;
		while (i<elements.size() && found==false){
			if (elements.get(i).getSib()==sib){
				found=true;
			}
			else{
				i++;
			}			
		}
		return i;
	}

}
