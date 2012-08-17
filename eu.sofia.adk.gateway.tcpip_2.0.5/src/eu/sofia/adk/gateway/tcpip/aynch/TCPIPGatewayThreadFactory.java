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

package eu.sofia.adk.gateway.tcpip.aynch;

import java.util.concurrent.ThreadFactory;

/**
 * TCPIPGatewayThreadGroup.java
 * 
 * A thread factory to centralize <code>Thread</code> creation.
 * @author Raul Otaolea, Raul.Otaolea@tecnalia.com, Tecnalia-Tecnalia
  */
public class TCPIPGatewayThreadFactory implements ThreadFactory {

	private String name;
	private ThreadGroup tg;
	private long id;
	
	public TCPIPGatewayThreadFactory(String name, ThreadGroup tg) {
		this.name = name;
		this.tg = tg;
	}
	
	public Thread newThread(Runnable r) {
		id++;
		return new Thread(tg, r, name + id);
	}

}
