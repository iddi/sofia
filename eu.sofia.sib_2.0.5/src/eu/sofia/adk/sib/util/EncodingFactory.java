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

package eu.sofia.adk.sib.util;

import java.util.HashMap;

import eu.sofia.adk.sib.exception.EncodingNotSupportedException;
import eu.sofia.adk.sib.util.transformer.RDFM3Transformer;
import eu.sofia.adk.sib.util.transformer.RDFXMLTransformer;
import eu.sofia.adk.sib.util.transformer.Transformer;
import eu.sofia.adk.ssapmessage.parameter.SSAPMessageRDFParameter.TypeAttribute;

/**
 * This class is a factory that extracts the different possible 
 * encodings supported by the current Semantic Information Broker. 
 * The obtained objects allows transformations into the different
 * supported encodings. 
 * @author Fran Ruiz, fran.ruiz@tecnalia.com, Tecnalia
 * @author Raul Otaolea, raul.otaolea@tecnalia.com, Tecnalia
 *
 */
public class EncodingFactory {

    // volatile is needed so that multiple thread can reconcile the instance
    // semantics for volatile changed in Java 5.
    private volatile static EncodingFactory factory;
    
    private HashMap<TypeAttribute, Transformer> transformers;
 
    private EncodingFactory() {
    	transformers = new HashMap<TypeAttribute, Transformer>();
    	transformers.put(TypeAttribute.RDFXML, new RDFXMLTransformer());
    	transformers.put(TypeAttribute.SPARQL, new RDFXMLTransformer());
    	transformers.put(TypeAttribute.RDFM3, new RDFM3Transformer());
    }
 
    /**
     * 
     * @return
     */
    public static EncodingFactory getInstance() {
        // needed because once there is singleton available no need to acquire
        // monitor again & again as it is costly
        if(factory==null) {
            synchronized(EncodingFactory.class){
            // this is needed if two threads are waiting at the monitor at the
            // time when singleton was getting instantiated
                if(factory == null) {
                	factory = new EncodingFactory();
                }
            }
        }
        return factory;
    }
	
    /**
     * Gets the associated transformer
     * @param encoding the type attribute encoding
     * @return 
     * @throws EncodingNotSupportedException 
     */
	public Transformer getTransformer(TypeAttribute encoding) throws EncodingNotSupportedException {
		Transformer t = transformers.get(encoding);
		if (t == null) {
			throw new EncodingNotSupportedException(encoding);
		}
		
		return t;
	}
}
