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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.net.*;

/**
 * TCPIPGatewayConfiguration.java
 * 
 * @author Raul Otaolea, Raul.Otaolea@tecnalia.com, Tecnalia-Tecnalia
 */
public class TCPIPGatewayConfiguration {

	/** client buffer size in bytes */
	public static final int BUFFER_SIZE = 32768;

	/** client buffer grow size in bytes */
	public static final int BUFFER_GROW = 100;

	/** port the server listens on */
	public static final String PORT = "PORT";

	/** size of ByteBuffer for reading/writing from channels */
	public static final String NET_BUFFER_SIZE = "NET_BUFFER_SIZE";

	/** interval to sleep between attempts to write to a channel. */
	public static final String CHANNEL_WRITE_SLEEP = "CHANNEL_WRITE_SLEEP";

	/** number of worker threads for EventWriter */
	public static final String EVENT_WRITER_WORKERS = "EVENT_WRITER_WORKERS";

	/** default number of workers for GameControllers */
	public static final String CONTROLLER_WORKERS = "DEFAULT_CONTROLLER_WORKERS";

	/** default size of the write queue */
	public static final String WRITE_QUEUE_SIZE = "WRITE_QUEUE_SIZE";

	/** default sleep for reader channels */
	public static final String CHANNEL_READER_SLEEP = "CHANNEL_READER_SLEEP";

	// Added by AVK to include a list of network adapters and their ips
	/** default sleep for reader channels */
	public static final String NETWORK_ADAPTER = "NETWORK_ADAPTER";
	
	/** array of IPs to bind with the serversockets  */
	public static final String ADDRESSES = "ADDRESSES";
	
	/** the default IP for the KP  */
	public static final String DEFAULT_IPADDRESS = "DEFAULT_IPADDRESS";
	

	/** The TCP/IP Gateway parameter default values */
	public static final int DEFAULT_PORT = 23000;
	public static final int DEFAULT_NET_BUFFER_SIZE = 512;
	public static final long DEFAULT_CHANNEL_WRITE_SLEEP = 10;
	public static final int DEFAULT_EVENT_WRITER_WORKERS = 5;
	public static final int DEFAULT_CONTROLLER_WORKERS = 5;
	public static final int DEFAULT_WRITE_QUEUE_SIZE = 100;
	public static final long DEFAULT_CHANNEL_READER_SLEEP = 30;

	/** Properties */
	private Properties properties;

	public TCPIPGatewayConfiguration() {
		this.properties = TCPIPGatewayConfiguration.getDefaultProperties();
	}

	public void setProperties(Properties properties) {
		if (properties == null) {
			return;
		}
		for (Object key : properties.keySet()) {
			Object value = properties.get(key);
			this.properties.put(key, value);
		}
	}

	public int getInt(Object key) {
		if (check(key)) {
			Object value = properties.get(key);
			if (value instanceof String) {
				return Integer.parseInt((String) value);
			} else if (value instanceof Integer) {
				return ((Integer) value).intValue();
			} else if (value instanceof Long) {
				return ((Integer) value).intValue();
			} else {
				throw new NumberFormatException("Key " + key
						+ " can not be converted into int");
			}
		} else {
			throw new NumberFormatException("Key " + key
					+ " can not be converted into int");
		}
	}

	public long getLong(Object key) {
		if (check(key)) {
			Object value = properties.get(key);
			if (value instanceof String) {
				return Long.parseLong((String) value);
			} else if (value instanceof Long) {
				return ((Long) value).longValue();
			} else {
				throw new NumberFormatException("Key " + key
						+ " can not be converted into long");
			}
		} else {
			throw new NumberFormatException("Key " + key
					+ " can not be converted into long");
		}
	}

	private boolean check(Object key) {
		if (key == null) {
			throw new NullPointerException();
		}
		Object value = properties.get(key);
		if (value == null) {
			throw new NumberFormatException(key + " is null");
		}
		return true;
	}

	public Properties getProperties() {
		return properties;
	}

	public static Properties getDefaultProperties() {
		Properties properties = new Properties();
		// This map will hold pairs <adapterName, list of addresses>
		HashMap<String, ArrayList<String>> mappings = new HashMap<String, ArrayList<String>>();
		properties.put(TCPIPGatewayConfiguration.PORT, TCPIPGatewayConfiguration.DEFAULT_PORT);
		// Addon by AVK to include pairs, networkadaptername-ip in the properties
		Enumeration<NetworkInterface> nets = null;
		boolean valid = false;
		boolean defaultIp = false;
		try 
		{
			nets = NetworkInterface.getNetworkInterfaces();
		} 
		catch (SocketException e) 
		{

			e.printStackTrace();
		}
		for (NetworkInterface netint : Collections.list(nets)) 
		{
			// Check if the adapter name contains the *blue* sequence, which
			// probably
			// means it is a Bluetooth adapter that we don't want to show
			valid = false;

			if (!netint.getDisplayName().toUpperCase().contains("BLUE")) 
			{
				// list of addresses for an adapter
				ArrayList<String> addresses = new ArrayList<String>();

				Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
				for (InetAddress inetAddress : Collections.list(inetAddresses)) 
				{
					if (validIP(inetAddress.toString().substring(1)))
					{
						//check that the given ip is valid
						addresses.add(inetAddress.toString());
						valid=true;
					}
				
				}
				if (valid)
				{
					mappings.put(netint.getName(), addresses);
					//Set the ADDRESSES field, which is required when the SIB is started via command line, otherwise ADDRESSES is setup using the Wizard,
					//thus, we have to initialize it here too if starting with the command line
					if (!defaultIp)
					{
						ArrayList<String> ipAddresses = new ArrayList<String>();
						// remove the slash from each ip in the addresses arrayList
						for( String ip : addresses) {
							ipAddresses.add(ip.substring(1));
						}
						
						properties.put(TCPIPGatewayConfiguration.ADDRESSES, ipAddresses);
						defaultIp=true;
					}
				}
			}

		}
		properties.put(TCPIPGatewayConfiguration.NETWORK_ADAPTER, mappings);
		// End of addon by AVK to include pairs, networkadaptername-ip in the properties.
		properties.put(TCPIPGatewayConfiguration.NET_BUFFER_SIZE, TCPIPGatewayConfiguration.DEFAULT_NET_BUFFER_SIZE);
		properties.put(TCPIPGatewayConfiguration.CHANNEL_WRITE_SLEEP, TCPIPGatewayConfiguration.DEFAULT_CHANNEL_WRITE_SLEEP);
		properties.put(TCPIPGatewayConfiguration.EVENT_WRITER_WORKERS, TCPIPGatewayConfiguration.DEFAULT_EVENT_WRITER_WORKERS);
		properties.put(TCPIPGatewayConfiguration.CONTROLLER_WORKERS, TCPIPGatewayConfiguration.DEFAULT_CONTROLLER_WORKERS);
		properties.put(TCPIPGatewayConfiguration.WRITE_QUEUE_SIZE, TCPIPGatewayConfiguration.DEFAULT_WRITE_QUEUE_SIZE);
		properties.put(TCPIPGatewayConfiguration.CHANNEL_READER_SLEEP, TCPIPGatewayConfiguration.DEFAULT_CHANNEL_READER_SLEEP);

		return properties;
	}

	/**
	 * @return The port in which this server is bind.
	 */
	public int getPort() {
		return this.getInt(TCPIPGatewayConfiguration.PORT);
	}

	@SuppressWarnings("unchecked")
	public ArrayList<String> getAddresses() {
		return (ArrayList<String>) this.properties.get(TCPIPGatewayConfiguration.ADDRESSES);
	}
	
	//Method that checks if a valid ipv4 address is passed. Added by AVK 06/13/2011 
	public static boolean validIP (String ip) 
	{
	    try 
	    {
	        if (ip == null || ip.isEmpty()) 
	        {
	            return false;
	        }

	        String[] parts = ip.split( "\\." );
	        if ( parts.length != 4 ) 
	        {
	            return false;
	        }

	        for ( String s : parts ) 
	        {
	            int i = Integer.parseInt( s );
	            if ( (i < 0) || (i > 255) ) 
	            {
	                return false;
	            }
	        }

	        return true;
	    } 
	    catch (NumberFormatException nfe) 
	    {
	        return false;
	    }
	}
}
