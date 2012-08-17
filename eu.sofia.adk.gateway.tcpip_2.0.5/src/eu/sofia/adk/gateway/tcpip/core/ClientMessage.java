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
 * ClientMessage.java
 *
 * @author Raul Otaolea, Raul.Otaolea@tecnalia.com, Tecnalia-Tecnalia
 */

public class ClientMessage {
	private static long idGenerator;
	
	private long id;
	private long timeStamp;
	private byte[] payload;
	
	public ClientMessage() {
		// No need to synchronize because ++ is a one cycle operation.
		id = idGenerator++;
		timeStamp = System.nanoTime();
	}
	
	public ClientMessage(byte[] payload) {
		this();
		this.payload = payload;
	}

	public long getId() {
		return id;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public int getPayloadSize() {
		if(payload == null) {
			return 0;
		} else {
			return payload.length;
		}
	}

	public byte[] getPayload() {
		return payload;
	}
	
	public String toString() {
		StringBuilder sb = new StringBuilder();
		try {
			sb.append("[gw]id: ");
			sb.append(id);
			sb.append(", timestamp: ");
			sb.append(timeStamp);
			sb.append(", len: ");
			sb.append(payload.length);
			sb.append(", msg: ");
			sb.append(new String(payload));
		} catch(Exception e) {
			sb.append("with error: " + e.toString());
		}
		return sb.toString();
	}
}
