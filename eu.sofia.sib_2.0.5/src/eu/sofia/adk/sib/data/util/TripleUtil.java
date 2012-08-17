/*
 *    Copyright 2010 European Software Institute
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *    
 *      http://www.apache.org/licenses/LICENSE-2.0
 *    
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *    
 */
package eu.sofia.adk.sib.data.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import eu.sofia.adk.sib.data.Triple;
import eu.sofia.adk.sib.model.semantic.util.IRIUtil;

/**
 * The triple util is used for getting the information about the model and transformation
 * in a Triple structure. 
 * 
 * @author Fran Ruiz, fran.ruiz@tecnalia.com, Tecnalia
 *
 */
public class TripleUtil {

	private static int anonRef = 1;
	private static HashMap<String,Integer> anonMap = new HashMap<String,Integer>();
	
	/**
	 * This method obtains the individual triples from the Jena OntModel
	 * 
	 * @param model the model
	 * @return a collection of triples contained in the model
	 */
	public static Collection<Triple> obtainTriples(OntModel model) {
		StmtIterator stmtIterator = model.listStatements();
		
		return obtainTriples(stmtIterator.toSet(), model.getNsPrefixMap());		
	}
	
	/**
	 * This method obtains the individual triples from the Jena OntModel
	 * 
	 * @param model the model
	 * @return a collection of triples contained in the model
	 */
	public static Collection<Triple> obtainTriples(Collection<Statement> statements, Map<String,String> nsMap) {
		ArrayList<Triple> triples = new ArrayList<Triple>();
		
		for (Statement stmt : statements) {
			Resource  subject   = stmt.getSubject();     // get the subject
		    Property  predicate = stmt.getPredicate();   // get the predicate
		    RDFNode   object    = stmt.getObject();      // get the object
			
		    StringBuilder sbSubject = new StringBuilder();
		    StringBuilder sbPredicate = new StringBuilder();
		    StringBuilder sbObject = new StringBuilder();

	    	if (subject.isAnon()) {
	    		if (anonMap.get(subject.toString()) == null) {
	    			anonMap.put(subject.toString(), anonRef++);
	    		}
	    
    			sbSubject.append("_:anon").append(anonMap.get(subject.toString()));
	    		
	    	} else {
	    		sbSubject.append(IRIUtil.getShortName(subject.toString(), nsMap));
	    	}
		    
	    	sbPredicate.append(IRIUtil.getShortName(predicate.toString(), nsMap));
		    
		    if (object instanceof Resource) {
		    	if (object.isAnon()) {
		    		if (anonMap.get(object.toString()) == null) {
		    			anonMap.put(object.toString(), anonRef++);
		    		}
		    
	    			sbObject.append("_:anon").append(anonMap.get(object.toString()));
		    		
		    	} else {
		    		sbObject.append(IRIUtil.getShortName(object.toString(), nsMap));
		    	}
		    } else {
		         // object is a literal
		    	sbObject.append(IRIUtil.getLiteralShortName(object.toString()));
		    }

			Triple triple= new Triple(sbSubject.toString(),
					sbPredicate.toString(), sbObject.toString());
		
			triples.add(triple);
		}

		return triples;

	}	
}
