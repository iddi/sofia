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

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * ClientConnectionAttachment.java
 *
 * This class is used as the attachment for each channel registered
 * with the Selector.<p>It holds the temporary incoming data and checks 
 * the completeness of each message.
 *
 * @author Raul Otaolea, Raul.Otaolea@tecnalia.com, Tecnalia-Tecnalia
 */
public abstract class ClientConnectionAttachment {
    
    /** Temporary buffer before it is converted into a message. */
    private byte[] buffer;
        
    /** List of ClientMessages already decoded. */
    private LinkedList<ClientMessage> decodedMessages;

	private int alloc;
	private int used;
	
	public ClientConnectionAttachment() {
		// [ROM] TODO: Modify hard coded values with values read from TCPIPServer.properties.
		buffer = new byte[TCPIPGatewayConfiguration.BUFFER_SIZE];
		alloc = TCPIPGatewayConfiguration.BUFFER_SIZE;
		used = 0;

		decodedMessages = new LinkedList<ClientMessage>();
	}
	
	/** Check if there are enough bytes to build message(s). */
	public boolean checkForMessages() throws MessageParseException {
		int[] indexes = parseMessages(buffer);
		if(indexes == null) {
			return false;
		} else {
			if(indexes.length % 2 != 0) {
				throw new MessageParseException("The length of the indexes is no even.");
			}
			try {
				// Create client messages.
				for(int i = 0; i < indexes.length; i+=2) {
					int messageSize = indexes[i+1] - indexes[i] + 1;
					byte[] payload = new byte[messageSize];
					System.arraycopy(buffer, indexes[i], payload, 0, messageSize);
					ClientMessage cm = new ClientMessage(payload);
					decodedMessages.add(cm);
				}
				// Compact the buffer.
				int remaining = used - indexes[indexes.length-1] - 1;
				int newLength = remaining;
				if(newLength < TCPIPGatewayConfiguration.BUFFER_SIZE) {
					newLength = TCPIPGatewayConfiguration.BUFFER_SIZE;
				}
				
				byte[] newBuffer = new byte[newLength];
				System.arraycopy(buffer, indexes[indexes.length-1]+1, newBuffer, 0, remaining);
				buffer = newBuffer;
				alloc = newLength;
				used = remaining;
				return true;
			} catch(Exception e) {
				throw new MessageParseException(e.toString());
			}
		}	
	}
	
	/**
	 * 
	 * @return int[] Returns pairs of indexes indicating the start index (included) 
	 * and the last index (included) of the detected messages.
	 */
	public abstract int[] parseMessages(byte[] buffer);
		
	/**
	 * Indicates if there are pending messages.
	 * @return
	 */
	public boolean hasMessages() {
		return decodedMessages.size() > 0;
	}

	/**
	 * This method removes the messages returned each time is called.
	 * @return Iterator with the parsed messages.
	 */
	public Iterator<ClientMessage> getMessages() {
		if(hasMessages()) {
			LinkedList<ClientMessage> messages = new LinkedList<ClientMessage>();
			Iterator<ClientMessage> i = decodedMessages.iterator();
			while(i.hasNext()) {
				ClientMessage cm = i.next();
				i.remove();
				messages.add(cm);
			}
			return messages.iterator();
		} else {
			return null;
		}
	}
	
	/**
	 * Clear this array
	 */
	public void reset() { 
		used = 0; 
	}

	/**
	 * Returns the lenght of this array
	 */
	public int length() { 
		return used; 
	}
	
	/**
	 * Add a ByteArray to this array
	 * @param b ByteArray to add
	 */

	public void add(ByteBuffer bb) {
		bb.flip();
		if(used+bb.limit() >= alloc) {
			grow(bb.limit() + TCPIPGatewayConfiguration.BUFFER_GROW);
		}
		
		bb.get(buffer, used, bb.limit());
		used += bb.limit();
	}

	private void grow(int newGap) {
		alloc += newGap;
		byte [] n = new byte[alloc];
		System.arraycopy(buffer, 0, n, 0, used);
		buffer = n;
	}
}
