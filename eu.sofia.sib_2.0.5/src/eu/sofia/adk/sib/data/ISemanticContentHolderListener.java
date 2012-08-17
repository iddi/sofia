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

package eu.sofia.adk.sib.data;

import java.util.Collection;

/**
 * This interface determines the set of methods needed to be implemented by
 * the classes responsible of the semantic content
 * 
 * @author Fran Ruiz, fran.ruiz@tecnalia.com, ESI
 */
public interface ISemanticContentHolderListener {

	/**
	 * Notifies the initialization of the triple set
	 * @param triples the initial triple set
	 */
	public void initTriples(Collection<Triple> triples, String nodeId);
	
	/**
	 * Adds a collection of triples to the current set of triples
	 * @param triples the set of triples to be added
	 * @param nodeId 
	 */
	public void addTriples(Collection<Triple> triples, String nodeId);

	/**
	 * Removes a collection of triples from the current set of triples
	 * @param triples the set of triples to be removed
	 */
	public void removeTriples(Collection<Triple> triples, String nodeId);

	/**
	 * Resets the content of the SIB
	 */
	public void reset();	
}
