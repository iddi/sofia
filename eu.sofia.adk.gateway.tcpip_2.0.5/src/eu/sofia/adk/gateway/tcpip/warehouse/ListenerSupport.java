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

package eu.sofia.adk.gateway.tcpip.warehouse;

import java.util.ArrayList;
import java.util.List;

/**
 * ListenerSupport.java
 * @author Raul Otaolea, Raul.Otaolea@tecnalia.com, Tecnalia-Tecnalia 
 */
public abstract class ListenerSupport<E> {
    
    protected List<E> listeners = null; 
    
    public ListenerSupport() {
        listeners = new ArrayList<E>( );
    }
  
    public void addListener(E l) {
        if(l == null) {
            throw new IllegalArgumentException("El listener no puede ser null");
        }
        synchronized(listeners) {
        	listeners.add(l);
        }
    }

    public void removeListener(E l) {
    	synchronized(listeners) {
    		listeners.remove(l);
    	}
    }
    
}
