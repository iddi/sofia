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

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import eu.sofia.adk.sib.service.ISIB;
import eu.sofia.adk.osgi.commands.data.Element;

/**
 * This class is a service tracker customizer that detects the SIB server instances that
 * have been registered or unregistered in the OSGI Framework.
 * 
 * @author Cristina López, cristina.lopez@esi.es, ESI
 *
 */
//@SuppressWarnings({"rawtypes", "unchecked"})
public class SIBServiceTrackerCustomizer implements ServiceTrackerCustomizer {

	/**
	 * SIB service
	 */
	protected ISIB asib = null;
	/**
	 * Bundle Context
	 */
	private BundleContext bc;
		
	/**
	 * Default Constructor
	 * @param bc Bundle Context
	 */
	public SIBServiceTrackerCustomizer(BundleContext bc){
		this.bc = bc;
	}
	
	@Override
	/*
	 * (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer
	 */
	public Object addingService(ServiceReference reference) {

		// Obtains the SIB
		Object obj = bc.getService(reference);

		if (obj instanceof ISIB) {
			asib = (ISIB) bc.getService(reference);			
			Element sib= new Element(asib);	
			Activator.getDefault().getSibList().add(sib);
			
			
			return asib;
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
		// Obtains the SIB
		Object obj = bc.getService(reference);
		
		if (obj instanceof ISIB) {
			asib = (ISIB) bc.getService(reference);
			Activator.getDefault().getSibList().remove(serviceObject);
			Element sib= new Element(asib);	
			Activator.getDefault().getSibList().add(sib);

		}

	}

	@Override
	/*
	 * (non-Javadoc)
	 * @see org.osgi.util.tracker.ServiceTrackerCustomizer
	 */
	public void removedService(ServiceReference reference, Object service) {
		
		Object obj = bc.getService(reference);
		
		if (obj instanceof ISIB) {
			asib = (ISIB) bc.getService(reference);
			Element sib=null;
			
			int i=0;
			boolean found=false;
			while(i<Activator.getDefault().getSibList().size() && found==false){
				Element current= Activator.getDefault().getSibList().get(i);
				if (current.getSib()==asib){
					found=true;
					sib=current;
				}
					i++;
			}
			Activator.getDefault().getSibList().remove(sib);

		}
	}
}
