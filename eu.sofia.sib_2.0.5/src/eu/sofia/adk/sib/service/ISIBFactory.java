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

package eu.sofia.adk.sib.service;

/**
 * The ISIBMgt interface exposes the methods related to the general management
 * of a server
 * 
 * @author Cristina López, ESI, cristina.lopez@tecnalia.com
 */

import java.util.Hashtable;

import eu.sofia.adk.sib.exception.SIBException;

/**
 * The ISIBManager interface exposes the methods that allows the creation
 * of new SIB instances, destroying, starting and stopping.
 */
public interface ISIBFactory {
	
	/**
	 * Creates a new SIB instance
	 * @param prop the set of parameters
	 * @return an unique SIB identifier obtained from the properties
	 * @throws SIBException when the SIB cannot be created
	 */
	public int create(String name, Hashtable<String, Object> prop) throws SIBException;
	
	/**
	 * Destroys a SIB instance
	 * @param id the SIB identifier
	 * @throws SIBException when the SIB instance cannot be destroyed
	 */
	public void destroy(int id) throws SIBException;
	
	/**
	 * Gets a SIB for a given identifier
	 * @param id the SIB identifier
	 * @return a ISIB representing a SIB
	 * @throws SIBException when a SIB instance cannot be obtained
	 */
	public ISIB getSIB(int id) throws SIBException;
	
}
