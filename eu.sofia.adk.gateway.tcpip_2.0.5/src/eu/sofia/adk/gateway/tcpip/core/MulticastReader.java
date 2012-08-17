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

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.Properties;

/**
 * MulticastReader.java
 *
 * @author Raul Otaolea, Raul.Otaolea@tecnalia.com, Tecnalia-Tecnalia
 */

public class MulticastReader extends Thread {

	private static final int PORT = 5555;
	private static final String MULTICAST_GROUP = "235.0.0.1";
	private static final String KP_REQUEST = "<KP>ping</KP>";
	
	/** Logger */

	private volatile boolean running;
	private InetAddress group;
	private MulticastSocket ms;
	private byte[] buffer;
	private String sibName;
	private Properties properties;
	
	public MulticastReader(String sibName, Properties properties) {
		this.sibName = sibName;
		this.properties = properties;
	}
	
	private boolean init() {
		try {
			buffer = new byte[128];
			group = InetAddress.getByName(MulticastReader.MULTICAST_GROUP);
			ms = new MulticastSocket(MulticastReader.PORT);
			ms.joinGroup(group);
			return true;
		} catch(Exception e) {
			return false;
		}
	}
	public void run() {
		System.out.println("MulticastReader started");
		running = init();
		while (running) {
			try {
				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
				ms.receive(packet);
				
				byte[] tmp = new byte[packet.getLength()];
				System.arraycopy(packet.getData(), 0, tmp, 0, tmp.length);
				String received = new String(tmp);
				System.out.println(received);
	
				if(received.equals(KP_REQUEST)) {
					tmp = getSIBPropertiesXML();
				} else {
					tmp = "Sorry, but you are not probably allowed in this multicast port...".getBytes();
				}
	
				// send the response to the client at "address" and "port"
				InetAddress address = packet.getAddress();
				int clientPort = packet.getPort();
				packet = new DatagramPacket(tmp, tmp.length, address, clientPort);
				ms.send(packet);
			} catch(Exception e) {
				
			}
		}
		
		try {
			ms.leaveGroup(group);
			ms.close();
		} catch(Exception e) {
		}
		System.out.println("MulticastReader finished");
	}
	
	public void shutdown() {
		running = false;
		ms.close();
	}
	
	@SuppressWarnings("unchecked")
	private byte[] getSIBPropertiesXML() {
		StringBuilder sb = new StringBuilder();
		sb.append("<sib>");
		sb.append("<name>");
		sb.append(sibName);
		sb.append("</name>");
		
		// Add the available IPs.
		ArrayList<String> ipAddresses = (ArrayList<String>)this.properties.get(TCPIPGatewayConfiguration.ADDRESSES);
		if(ipAddresses != null) {
			sb.append("<parameter name=\"" + TCPIPGatewayConfiguration.ADDRESSES + "\">");
			StringBuilder sb2 = new StringBuilder();
			for(String ip : ipAddresses) {
				sb2.append(ip);
				sb2.append(",");
			}
			sb.append(sb2.substring(0, sb2.length()-1));
			sb.append("</parameter>");
		}
		
		// Add the default IP.
		String defaultIP = this.properties.getProperty(TCPIPGatewayConfiguration.DEFAULT_IPADDRESS);
		if(defaultIP != null) {
			sb.append("<parameter name=\""+ TCPIPGatewayConfiguration.DEFAULT_IPADDRESS +"\">");
			sb.append(defaultIP);
			sb.append("</parameter>");
		}
		
		// Add the port.
		try {
			int port = (Integer)this.properties.get(TCPIPGatewayConfiguration.PORT);
			if(port != 0) {
				sb.append("<parameter name=\""+ TCPIPGatewayConfiguration.PORT +"\">");
				sb.append(port);
				sb.append("</parameter>");
			}
		} catch(NumberFormatException nfe) {	
		}
		
		sb.append("</sib>");
		return sb.toString().getBytes();
	}
}
