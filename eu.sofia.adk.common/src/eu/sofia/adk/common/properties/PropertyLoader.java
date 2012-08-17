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

package eu.sofia.adk.common.properties;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * This class loads the SOFIA properties
 *  
 * @author Fran Ruiz, Fran.Ruiz@tecnalia.com, Tecnalia
 */
public class PropertyLoader {
	
	private Properties sofiaProps;
	private static final String DEFAULT_PROPS = "/sofia.properties"; 

	/**
	 * Constructor of PropertyLoader.
	 */
    private PropertyLoader() {
		sofiaProps = new Properties();
		this.init();
    }
	
    /**
     * Initializes the SofiaProperties class, loading the 
     * properties data.
     */
	public void init() {
		try {
			sofiaProps.load(getClass().getResourceAsStream(DEFAULT_PROPS));
		} catch (FileNotFoundException e) {
			System.out.println("The indicated file " + DEFAULT_PROPS + " does not exist");
			System.out.println(e.toString());
		} catch (IOException e) {
			System.out.println("There were problems with opening of the properties file " + DEFAULT_PROPS);
			System.out.println(e.toString());
		}
	}
	
	/**
	 * Gets property from the Sofia Properties
	 * @param key The key
	 * @return A string with the property value
	 */
	public String getProperty(String key) {
		return sofiaProps.getProperty(key);
	}

    /**
     * SingletonHolder is loaded on the first execution of SofiaProperties.getInstance() 
     * or the first access to SingletonHolder.INSTANCE, not before.
     */
    private static class SingletonHolder { 
    	private static final PropertyLoader INSTANCE = new PropertyLoader();
    }
  
    /**
     * Gets the instance of the SofiaProperties singleton
     * @return The SofiaProperties singleton
     */
    public static PropertyLoader getInstance() {
      return SingletonHolder.INSTANCE;
    }
}
