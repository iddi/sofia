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

package eu.sofia.adk.osgi.commands.data;

import java.util.ArrayList;
import java.util.List;

import eu.sofia.adk.gateway.service.IGateway;
import eu.sofia.adk.sib.service.ISIB;

/**
 * This is a data class used by the Semantic Information Broker View. 
 * The internal viewer structure of the view uses this data class internally
 * to manage the elements on display. 
 * 
 * @author Cristina López, cristina.lopez@esi.es, ESI
 *
 */
public class Element{
	
	/**
	 * SIB server instance
	 */
	private ISIB sib;
	
	/**
	 * List of gateways of the SIB
	 */
	private List<IGateway> gateways= new ArrayList<IGateway>();
	
	/**
	 * List of extensions
	 */
	//TODO
	
	/**
	 * Default Constructor
	 */
	public Element (){		
	}
	
	/**
	 * Constructor
	 * @param sib
	 */
	public Element (ISIB sib){
		this.sib=sib;
	}
	
	/**
	 * Retrieves the SIB elements
	 * @return
	 */
	public ISIB getSib() {
		return sib;
	}
	
	/**
	 * Sets the sib element
	 * @param sib
	 */
	public void setSib(ISIB sib) {
		this.sib = sib;
	}
	
	/**
	 * Retrieves the list of Gateways of the Sib element
	 * @return
	 */
	public List<IGateway> getGateways() {
		return gateways;
	}
	
	/**
	 * Sets the list of Gateways of the Sib element
	 * @param gateways
	 */
	public void setGateways(List<IGateway> gateways) {
		this.gateways = gateways;
	}
	
}
