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

package eu.sofia.adk.common.sax;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

/**
 * The resource utility deals with the internal and remote resources for using
 * it during the plugin.  
 * 
 * @author Fran Ruiz, Fran.Ruiz@tecnalia.com, Tecnalia
 *
 */
public class ResourceUtil {

	/** The constants */
	private static String BUNDLERESOURCE_SCHEME = "bundleresource://";
	private static String HTTPS_SCHEME = "https";
	private static String HTTP_SCHEME = "http";
	private static String FILE_SCHEME = "file";
	private static String FTP_SCHEME = "ftp";
	

	/**
	 * Gets the InputStream associated to the resource path specified in the parameter
	 * @param resource a resource. this resource can be:<ul><li>a HTTP/HTTPS resource</li><li>a FTP resource</li><li>a bundle internal resource</li><li>a system path resource</li></ul>
	 * @return the InputStream associated to the resource
	 * @throws NullPointerException when the resource cannot be opened
	 */
	public static InputStream getInputStream(String resource) throws NullPointerException {
		
		InputStream inputStream = null;
		
		try {
			if (hasScheme(resource)) {
				if (isHTTPSResource(resource) || isHTTPResource(resource)) {
					inputStream = getRemoteStream(new URI(resource));
				} else if (isBundleResource(resource)) {
					inputStream = getBundleStream(resource);
				} else if (isFileResource(resource)) {
					inputStream = getFileStream(resource);
				} else {
					System.out.println("Unsupported scheme " + resource);
				}
			} else {
				inputStream = getBundleStream(resource);
			}
		} catch (URISyntaxException e) {
//			logger.error("Invalid URI syntax (" + resource + "): " + e.getMessage());
			e.printStackTrace();
		}

		if (inputStream == null) {
			throw new NullPointerException("Cannot get stream from the given resource (" + resource + ")");
		}
		
		return inputStream;
	}
	
	/**
	 * Gets the remote input stream for a given external resource
	 * @param uriResource the URI resource to be opened
	 * @return the InputStream corresponding to that remote resource
	 */
	private static InputStream getRemoteStream(URI uriResource) {
		
		InputStream inputStream = null;
		
		try {
			if (uriResource != null) {
				
				//logger.debug("The Scheme is <" + uriResource.getScheme() + ">");
				
				if (uriResource.getScheme().equals(HTTP_SCHEME)) {
					//logger.debug("Opening http connection...");
					HttpURLConnection httpCon = (HttpURLConnection) uriResource.toURL().openConnection();  
					
					//logger.info("Connecting to the remote location ("+uriResource.toString()+")...");
			
					httpCon.connect();  
					
					//logger.info("Getting stream...");
					
					inputStream = httpCon.getInputStream();
				} else if (uriResource.getScheme().equals(HTTPS_SCHEME)) {
					//logger.debug("Opening https connection...");
					HttpsURLConnection httpsCon = (HttpsURLConnection) uriResource.toURL().openConnection();  
					
					//logger.info("Setting hostname verifier...");
					
					httpsCon.setHostnameVerifier(new HostnameVerifier() { 
						@Override
						public boolean verify(String hostname, SSLSession arg1) {
							return true;
						}  
					});  
			
					//logger.info("Connecting to the remote location ("+uriResource.toString()+")...");
			
					httpsCon.connect();  
					
					//logger.info("Getting stream...");
					
					inputStream = httpsCon.getInputStream();
				} else {
					
				}
			}
			
		} catch (IOException ex) {
			//logger.error("Cannot get input stream from " + uriResource.toString());
			ex.printStackTrace();
		} catch (Exception ex) {
			//logger.error("Error on getting the remote stream for " + uriResource.toString());
			ex.printStackTrace();
		}

		// Returns the input stream
		return inputStream; 
	}	
	
	/**
	 * Gets the input stream for a given bundle resource
	 * @param resource the bundle resource to be opened
	 * @return the InputStream corresponding to that remote resource
	 */
	private static InputStream getBundleStream(String resource) {

		//logger.debug("Getting bundle stream for " + resource);
		
		InputStream inputStream = null;
		try {
			if (resource != null && !resource.equals("")) {
				inputStream = ResourceUtil.class.getClassLoader().getResourceAsStream(resource);
			}
		} catch (Exception ex) {
			//logger.error("Cannot get bundle stream from " + resource);
			ex.printStackTrace();
		} 

		// Returns the input stream
		return inputStream; 
	}	
	
	/**
	 * Gets the input stream for a given system-path resource
	 * @param resource a path pointing to the resource to be opened
	 * @return the InputStream corresponding to that remote resource
	 */
	private static InputStream getFileStream(String resource) {

		////logger.debug("Getting file stream for " + resource);
		
		InputStream inputStream = null;
		try {
			if (resource != null && !resource.equals("")) {
				inputStream = ResourceUtil.class.getClassLoader().getResourceAsStream(resource);
			}
		} catch (Exception ex) {
			//logger.error("Cannot get bundle stream from " + resource);
			ex.printStackTrace();
		} 

		// Returns the input stream
		return inputStream; 
	}		
	
	/**
	 * Gets the bundle resource URL
	 * @param filename a filename referred to a plugin internal file
	 * @return a URL with the bundle resource url (bundleresource://...)
	 */
	public static URL getBundleResourceURL(String filename) {
		URL urlResource = null;
		
		try {
			if (filename != null && !filename.equals("")) {
				if (filename.startsWith(BUNDLERESOURCE_SCHEME)) {
					urlResource = new URL(filename);
				} else {
					urlResource = ResourceUtil.class.getClassLoader().getResource(filename);
				}
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return urlResource;
	}
	
	/**
	 * Determines if the resource is a bundle resource
	 * @param resource a string with a resource location
	 * @return <code>true</code> if the resource belongs to the bundle
	 */
	public static boolean isBundleResource(String resource) {
		return checkScheme(resource, BUNDLERESOURCE_SCHEME);
	}
	
	/**
	 * Determines if the resource has a file scheme
	 * @param resource a string with a resource location
	 * @return <code>true</code> if the resource is a local file
	 */
	public static boolean isFileResource(String resource) {
		return checkScheme(resource, FILE_SCHEME);
	}
	
	/**
	 * Determines if the resource has a http scheme
	 * @param resource a string with a resource location
	 * @return <code>true</code> if the resource has a http scheme
	 */
	public static boolean isHTTPResource(String resource) {
		return checkScheme(resource, HTTP_SCHEME);
	}	
	
	/**
	 * Determines if the resource has a https scheme
	 * @param resource a string with a resource location
	 * @return <code>true</code> if the resource has a https scheme
	 */
	public static boolean isHTTPSResource(String resource) {
		return checkScheme(resource, HTTPS_SCHEME);
	}	
	
	/**
	 * Determines if the resource has a ftp scheme
	 * @param resource a string with a resource location
	 * @return <code>true</code> if the resource has a ftp scheme
	 */
	public static boolean isFTPResource(String resource) {
		return checkScheme(resource, FTP_SCHEME);
	}
	
	/**
	 * Checks a scheme for a given resource
	 * @param resource a String containing a resource
	 * @param scheme the scheme to check
	 * @return <code>true</code> if the resource has the scheme
	 */
	private static boolean checkScheme(String resource, String scheme) {
		if (resource != null && !resource.equals("")) {
			if (resource.startsWith(scheme)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Determines if the resource is a bundle resource
	 * @param resource a string with a resource location
	 * @return <code>true</code> if the resource belongs to the bundle
	 */
	public static boolean isValidURL(String resource) {
		try {
			@SuppressWarnings("unused")
			URL testURL = new URL(resource);
			return true;
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Determines if a given resource has an scheme
	 * @param resource a String resource
	 * @return <code>true</code> if the resource contains a valid schema
	 */
	public static boolean hasScheme(String resource) {
		try {
			URI uri = new URI(resource);
			if (uri.isAbsolute()) {
				return true;
			}
		} catch (URISyntaxException e) {
			return false;
		}
		
		return false;
	}
	
}
