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

package eu.sofia.adk.gateway.tcpip.core;

/**
 * IConnectionListener.java
 *
 * @author Raul Otaolea, Raul.Otaolea@tecnalia.com, Tecnalia-Tecnalia
 */

public interface IConnectionListener {
	public void connectionCreated(ClientConnection connection);
	public void connectionDestroyed(ClientConnection connection);
}
