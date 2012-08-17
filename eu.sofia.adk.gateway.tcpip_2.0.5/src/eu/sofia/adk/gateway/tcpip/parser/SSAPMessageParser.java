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

package eu.sofia.adk.gateway.tcpip.parser;

import java.util.Vector;

import eu.sofia.adk.gateway.tcpip.core.ClientConnectionAttachment;

/**
 * SSAPMessageParser.java
 * 
 * This class is responsible for detecting a SSAP message from the received string
 * of bytes. A SSAP message starts with the tag <SSAP_message> and
 * ends with </SSAP_message>
 * 
 * @author Raul Otaolea, Raul.Otaolea@tecnalia.com, Tecnalia-Tecnalia
 */

public class SSAPMessageParser extends ClientConnectionAttachment {
	
	private final static String START_TOKEN = "<SSAP_MESSAGE>";
	private final static String END_TOKEN = "</SSAP_MESSAGE>";
	
	private Vector<Integer> indexes = new Vector<Integer>();
	
	public SSAPMessageParser() {
	}
	
	@Override
	public int[] parseMessages(byte[] buffer) {
		
		String s;
		try {
			s = new String(buffer).toUpperCase();
		} catch(Exception e) {
			return null;
		}
		
		indexes.clear();
		int fromIndex = 0;
		int pos = 0;
		while(pos != -1) {
			pos = s.indexOf(SSAPMessageParser.START_TOKEN, fromIndex);
			if(pos != -1) {			
				// Start token found.
				fromIndex = pos;
				pos = s.indexOf(SSAPMessageParser.END_TOKEN, fromIndex + SSAPMessageParser.START_TOKEN.length());
				if(pos != -1) {
					indexes.add(fromIndex);
					indexes.add(pos + SSAPMessageParser.END_TOKEN.length()-1);
					fromIndex = pos + SSAPMessageParser.END_TOKEN.length();
				}
			}
		}
		
		if(indexes.size() == 0) {
			return null;
		} else {
			int[] idx = new int[indexes.size()];
			for(int i = 0; i < indexes.size(); i++) {
				idx[i] = indexes.elementAt(i);
			}
			return idx;
		}
	}

}
