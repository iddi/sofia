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

package eu.sofia.adk.sib.model.semantic;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.topbraid.spin.inference.DefaultSPINRuleComparator;
import org.topbraid.spin.inference.SPINRuleComparator;
import org.topbraid.spin.system.SPINModuleRegistry;
import org.topbraid.spin.util.CommandWrapper;
import org.topbraid.spin.util.SPINQueryFinder;
import org.topbraid.spin.util.SystemTriples;
import org.topbraid.spin.vocabulary.SPIN;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.compose.MultiUnion;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.shared.ReificationStyle;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.LocationMapper;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import com.hp.hpl.jena.vocabulary.RDF;

import eu.sofia.adk.sib.data.Triple;
import eu.sofia.adk.sib.data.util.TripleUtil;
import eu.sofia.adk.sib.exception.SemanticModelException;
import eu.sofia.adk.sib.model.query.SOFIAResultSet;


/*Added by Gerrit*/
import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.graph.compose.MultiUnion;
import com.hp.hpl.jena.shared.ReificationStyle;
import org.topbraid.spin.constraints.ConstraintViolation;
import org.topbraid.spin.constraints.SPINConstraints;
import org.topbraid.spin.inference.SPINInferences;
import org.topbraid.spin.statistics.SPINStatistics;
import org.topbraid.spin.system.SPINLabels;
import org.topbraid.spin.system.SPINModuleRegistry;
import org.topbraid.spin.inference.SPINRuleComparator;
import org.topbraid.spin.inference.DefaultSPINRuleComparator;
import org.topbraid.spin.util.CommandWrapper;
import org.topbraid.spin.util.JenaUtil;
import org.topbraid.spin.util.SPINQueryFinder;
import org.topbraid.spin.util.SystemTriples;
import org.topbraid.spin.vocabulary.SPIN;
import java.util.List;
import com.hp.hpl.jena.util.LocationMapper;
import org.topbraid.spin.functions.SPINFunctions;

/**
 * This class contains the Jena Model containing all the ontology information
 * stored in a Jena Ontology Model. It also provides the methods to 
 * creating, adding, removing and querying about the model.
 *   
 * @author Fran Ruiz, fran.ruiz@tecnalia.com, ESI
 */
public class SemanticModel {

	/** Creates the basic variables for the model */
	private OntModel model;
	
	/*Added by Gerrit*/
	private Model newTriples;
	//private OntModel cleanupModel;
	private Map<CommandWrapper, Map<String,RDFNode>> initialTemplateBindings;
	private Map<Resource,List<CommandWrapper>> cls2Query;
	private Map<Resource,List<CommandWrapper>> cls2Constructor;
	private SPINRuleComparator comparator;
	
	/** The logger */
	private static Logger logger;
	
	/** The list of models for each nodeId **/
	private HashMap<String, OntModel> smartAppModels;
	
	/** The namespaces */
	private HashMap<String,String> nsPrefixes;
	
	/** BNodes mapping */
	private HashMap<String,String> bNodesMapping;
	
	private static final String DEFAULT_NAMESPACE = "http://emb1.esilab.org/sofia/ontology/";
	private static int bNodeSequential = 1;
	
    /**
     * The constructor that inits the Semantic Model for the SIB
     */
	public SemanticModel() {
		// Inits the logging
		logger = Logger.getLogger(SemanticModel.class);
		
		// Creates a void model
		// we use a simple OWL-Full stored in memory with transitive class hierarchy inference
		//model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_TRANS_INF); /*Removed by Gerrit*/
		//model = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM_TRANS_INF);
		
		/*Added by Gerrit*/
		//Set up local location for ontologies while developing
	/*	LocationMapper lm= new LocationMapper("location-mapping.ttl");
		LocationMapper.setGlobalLocationMapper(lm);
		FileManager.get().setLocationMapper(lm); */
		
		// Load domain model with imports
		logger.debug("Loading domain ontology...");
		model = loadModelWithImports("https://raw.github.com/iddi/sofia/master/nl.tue.id.sofia.ontologies/src/InteractionEvents.owl");
		//model = loadModelWithImports("/home/gerrit/code/TBCFreeWorkspace/ontologies/InteractionEvents.owl","https://raw.github.com/iddi/sofia/master/nl.tue.id.sofia.ontologies/src/InteractionEvents.owl");
		//model = loadModelWithImports("/home/gerrit/code/TBCFreeWorkspace/ontologies/BondingDevice5.owl","https://raw.github.com/iddi/sofia/master/nl.tue.id.sofia.ontologies/src/BondingDevice.owl");
		//model = loadModelWithImports("https://raw.github.com/iddi/sofia/master/nl.tue.id.sofia.ontologies/src/BondingDevice.owl");
		
		
		//Load Pellet reasoner (Gerrit)
		//model = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC); 
		//org.mindswap.pellet.PelletOptions.USE_CONTINUOUS_RULES=true;
		
		//Load SemCon ontology into model (Gerrit)
		//model.read("https://raw.github.com/iddi/sofia/master/nl.tue.id.sofia.ontologies/src/SemanticInteraction.owl");
		//model.read("https://raw.github.com/iddi/sofia/master/nl.tue.id.sofia.ontologies/src/SemanticConnections.owl");
		//model.read("https://raw.github.com/iddi/sofia/master/nl.tue.id.sofia.ontologies/src/BondingDevice.owl");
				
		// Load OWL RL library from the web
		logger.debug("Loading OWL RL ontology...");		
		OntModel owlrlModel = loadModelWithImports("http://topbraid.org/spin/owlrl-all");
		
		logger.debug("Loading our SPIN functions..");	
		//SIB crashes with java.net.ConnectException (Connection refused) if the server is offline	
		OntModel sifuncModel = loadModelWithImports("https://raw.github.com/iddi/sofia/master/nl.tue.id.sofia.ontologies/src/semint-functions.spin.rdf");
		
		SPINModuleRegistry.get().init(); // Initialize system functions and templates
		//SPINFunctions.init(); //Registers the spif functions in spin.functions.jar
		
		// Create and add Model for inferred triples
		newTriples = ModelFactory.createDefaultModel(ReificationStyle.Minimal);
		model.addSubModel(newTriples); 
		SPINModuleRegistry.get().registerAll(owlrlModel, null);
		SPINModuleRegistry.get().registerAll(sifuncModel, null); //For our own SPIN functions
		SPINModuleRegistry.get().registerAll(model, null); //Register SPIN functions in the ontology itself
		
		/*
		//Create cleanup model and load newTriples into it too
		logger.debug("Loading cleanup ontology..");		
		cleanupModel = loadModelWithImports("/home/gerrit/code/TBCFreeWorkspace/ontologies/cleanup.owl","https://raw.github.com/iddi/sofia/master/nl.tue.id.sofia.ontologies/src/cleanup.owl");
		cleanupModel.addSubModel(newTriples);
		SPINModuleRegistry.get().registerAll(cleanupModel, null);
		*/
		
		// Build one big union Model of everything
		MultiUnion multiUnion = new MultiUnion(new Graph[] {
			model.getGraph(),
			owlrlModel.getGraph(),
			sifuncModel.getGraph(),
			SystemTriples.getVocabularyModel().getGraph()
		});
		Model unionModel = ModelFactory.createModelForGraph(multiUnion);
		
		// Collect rules (and template calls) defined in OWL RL
		initialTemplateBindings = new HashMap<CommandWrapper, Map<String,RDFNode>>();
		cls2Query = SPINQueryFinder.getClass2QueryMap(unionModel, model, SPIN.rule, true, initialTemplateBindings, false);
		cls2Constructor = SPINQueryFinder.getClass2QueryMap(model, model, SPIN.constructor, true, initialTemplateBindings, false);
		comparator = new DefaultSPINRuleComparator(model); //determines order of execution of spin:rules
		
		//create directory to log models to file
		String directoryName = "logs/";
		File theDir = new File(directoryName);
		if (!theDir.exists()) {
			logger.debug("creating directory: " + directoryName);
		    theDir.mkdir();  
		}
		
		// Run all inferences
		runInferences();
		
		
		
		/*up to here*/
		
		smartAppModels = new HashMap<String, OntModel>();	
		nsPrefixes = new HashMap<String,String>();
		
		bNodesMapping = new HashMap<String,String>();
	}
	
	/**
	 * Gets the model associated to a node
	 * @param nodeId the node identifier
	 * @return the OntModel for the node
	 */
	public OntModel getModel(String nodeId) {
		logger.debug("Getting the model for the node " + nodeId);
		
		if (smartAppModels.containsKey(nodeId)) {
			OntModel previousModel = smartAppModels.get(nodeId);
			return previousModel;
		} else {
			return null;
		}
	}
	
	/**
	 * Queries if a node has an associated semantic model
	 * @param nodeId the node identifier
	 * @return <code>true</code> if the node has an associated model
	 */
	public boolean hasModel(String nodeId) {
		return smartAppModels.containsKey(nodeId);
	}	
	
	/**
	 * Adds a submodel to the current model
	 * @param nodeId the node identifier
	 * @param submodel the model to be included
	 * @return <code>true</code> if the model is successfully loaded
	 */
	public boolean addModel(String nodeId, OntModel submodel) {
		try {
			model.add(submodel);
			
			// Getting the namespace prefixes
			Map<String,String> map = submodel.getNsPrefixMap();
			
			// Set the namespace prefixes
			for (String key : map.keySet()) {
				String value = map.get(key);
				model.setNsPrefix(key, value);
			}
			
			// Getting the namespace prefixes
			Map<String,String> parentMap = model.getNsPrefixMap();
			
			// Set the namespace prefixes
			for (String key : parentMap.keySet()) {
				String value = parentMap.get(key);
				submodel.setNsPrefix(key, value);
			}			
			
			//model.write(System.out, "RDF/XML");
			
			if (smartAppModels.containsKey(nodeId)) {
				OntModel nodeModel = smartAppModels.get(nodeId);
				nodeModel.add(submodel);
			} else {
				smartAppModels.put(nodeId, submodel);
			}
		
			nsPrefixes.putAll(model.getNsPrefixMap());
			
			runInferences(); /* Added by Gerrit */
			
			return true;
		} catch (Exception ex) {
			logger.error("An error arised during addition of the model");
			ex.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Updates the submodel into the current model
	 * @param nodeId the node identifier
	 * @param insertModel the model to be included
	 * @param removalModel the model to be removed
	 * @return <code>true</code> if the model is successfully loaded
	 */
	public boolean updateModel(String nodeId, OntModel insertModel, OntModel removalModel) {
		model.remove(removalModel);
		
		model.add(insertModel);
		
		// Getting the namespace prefixes
		Map<String,String> map = removalModel.getNsPrefixMap();
		
		// Set the namespace prefixes
		for (String key : map.keySet()) {
			model.removeNsPrefix(key);
		}
		
		// Getting the namespace prefixes
		map = insertModel.getNsPrefixMap();
		
		// Set the namespace prefixes
		for (String key : map.keySet()) {
			String value = map.get(key);
			model.setNsPrefix(key, value);
		}
		
		// Getting the namespace prefixes
		Map<String,String> parentMap = model.getNsPrefixMap();
		
		// Set the namespace prefixes
		for (String key : parentMap.keySet()) {
			String value = parentMap.get(key);
			insertModel.setNsPrefix(key, value);
			removalModel.setNsPrefix(key, value);
		}		
		
		//model.write(System.out, "RDF/XML");
		
		if (smartAppModels.containsKey(nodeId)) {
			OntModel nodeModel = smartAppModels.get(nodeId);
			// TODO Triple statements like in the remove
			nodeModel.remove(removalModel);
			nodeModel.add(insertModel);
			smartAppModels.put(nodeId, nodeModel);
		} else {
			logger.error("There was a problem in updating the model, " +
					"because the model was not previously included.");
		}
		//smartAppModels.put(nodeId, submodel);
		nsPrefixes.putAll(model.getNsPrefixMap());		
		
		runInferences(); /* Added by Gerrit */
		
		return true;
	}	

	/**
	 * Removes a submodel from the current model
	 * @param nodeId the node identifier
	 * @param submodel the model to be removed
	 * @return <code>true</code> if the model is successfully removed
	 */
	public ArrayList<Statement> removeModel(String nodeId, OntModel submodel) {
		ArrayList<Statement> removeStatements = new ArrayList<Statement>();
		
		// Getting the namespace prefixes
		Map<String,String> parentMap = model.getNsPrefixMap();
		
		// Set the namespace prefixes
		for (String key : parentMap.keySet()) {
			String value = parentMap.get(key);
			submodel.setNsPrefix(key, value);
		}			
		
		StmtIterator it = submodel.listStatements();
		
		while (it.hasNext()) { // Adds to the remove statements the ones that are not rdf:type
			Statement stmt = it.next();
			logger.debug("Stmt ns:" + stmt.getPredicate().getNameSpace() + " id:" + stmt.getPredicate().getLocalName());
			if (!(stmt.getPredicate().equals(RDF.type))) {
				if (model.contains(stmt)) {
					logger.debug("Individual statement to remove: " + stmt.toString());
					removeStatements.add(stmt);
				}
			}
		}
		
		// Remove the statements
		model.remove(removeStatements);

		/** Remove the individuals that were in the incoming model as subject but do not have any other properties */
		it = submodel.listStatements();
		
		while (it.hasNext()) {
			Statement stmt = it.next();
			
			if (!removeStatements.contains(stmt)) {
				Resource subjectResource = stmt.getSubject();
	
				Resource objectResource = null;
				
				RDFNode object = stmt.getObject();
				
				if (object instanceof Resource) {
					objectResource = (Resource) object;
				}
				
				ExtendedIterator<Individual> individuals = model.listIndividuals();
				
				while (individuals.hasNext()) {
					Individual individual = individuals.next();
					
					if (individual.equals(subjectResource)) {
						// the individual should have at least one property [IndividualId, rdf:type, ClassId]
						if (!(individual.listProperties().toList().size() > 1)) {
							logger.debug("Deleting resource " + subjectResource.getLocalName());
							model.remove(stmt);
							removeStatements.add(stmt);
						}
					}
					
					if (individual.equals(objectResource)) {
						// the individual should have at least one property [IndividualId, rdf:type, ClassId]
						if (!(individual.listProperties().toList().size() > 1)) {
							StmtIterator objectStatements = model.listStatements(new SimpleSelector(objectResource, null, (RDFNode)null));
							
							while(objectStatements.hasNext()) {
								Statement objectStatement = objectStatements.next();
								model.remove(objectStatement);
								removeStatements.add(objectStatement);
							}
						}
					}
				}
			}
		}
		
		runInferences(); /* Added by Gerrit */
		
		// update the model associated to the kp
		if (smartAppModels.containsKey(nodeId)) {
			try {
				OntModel previousModel = smartAppModels.get(nodeId);
				previousModel.remove(removeStatements);
				smartAppModels.put(nodeId, previousModel);
			} catch (Exception ex) {
				logger.error("Cannot add model for node " + nodeId);
				logger.error("Reason: " + ex.toString());
			}
			logger.debug("The model for node " + nodeId + " is removed");
			
			nsPrefixes.putAll(model.getNsPrefixMap());			
			
			return removeStatements;
		} else { 
			logger.info("Cannot remove the model for node " + nodeId);
			logger.info("Reason: Node has no model");
			return new ArrayList<Statement>();
		}
	}
	
	/**
	 * Removes a submodel from the current model
	 * @param nodeId the node identifier
	 * @return the removed OntModel associated to the node
	 */
	public OntModel remove(String nodeId) {
		//logger.debug("Removes a submodel to the current model");
		
		if (smartAppModels.containsKey(nodeId)) {
			OntModel removedModel = null;

			try {
				// THE DATA IS NOW PERSISTENT
//				OntModel saModel = smartAppModels.get(nodeId);
//				
//				// note: only removes individuals
//				ExtendedIterator<Individual> it = saModel.listIndividuals();
//				
//				while (it.hasNext()) {
//					Individual individual = (Individual) it.next();
//					
//					Individual sharedIndividual = model.getIndividual(individual.getURI());
//					StmtIterator si = sharedIndividual.listProperties();
//
//					model.remove(si);
//				}
				
				// removes the node from the models
				removedModel = smartAppModels.remove(nodeId);
				
			} catch (Exception ex) {
				logger.error("Cannot remove model for node " + nodeId);
				logger.error("Reason: " + ex.toString());
				ex.printStackTrace();
			}
			
			//logger.debug("The model for node " + nodeId + " is removed");
			return removedModel;
		} else { 
			logger.info("Cannot remove the model for node " + nodeId);
			logger.info("Reason: Node has no model");
			return null;
		}
		
	}
	
	/**
	 * Queries about the SIB semantic model
	 * @param sparqlQuery the query to send to the semantic model
	 * @param encoding the encoding used in the query (RDF-XML, M3, WQL)
	 * @return the response
	 * @throws Exception 
	 */
	public SOFIAResultSet query(String sparqlQuery) throws SemanticModelException {
	
		SOFIAResultSet results = null;
		
		try {
			
			long startTime = System.currentTimeMillis();
			
			Query jenaQuery = QueryFactory.create(sparqlQuery);
			
			// Execute the query and obtain results
			QueryExecution queryExec = QueryExecutionFactory.create(jenaQuery, model);

			try {
				ResultSet resultSet = queryExec.execSelect();
				
				// here we have the solution in a list
				results = new SOFIAResultSet(resultSet);
				
				//String result = SOFIAResultSetFormatter.asXMLString(results);
				//logger.debug("Obtained the following result(XMLString): \n" + result);				
			} catch (Exception ex) {
				ex.printStackTrace();
				logger.error("The query cannot be executed");
				throw new SemanticModelException("Query cannot be executed due to " + ex.getMessage());
			} finally {
				queryExec.close();
			}
			
			long endTime = System.currentTimeMillis();
			long duration = (endTime - startTime);
			
			FileWriter fstream = new FileWriter("querytime.txt",true);
			BufferedWriter out = new BufferedWriter(fstream);
				
			out.write(duration + "\n");
			
			out.close();

			return results;
		} catch (SemanticModelException ex) {
			ex.printStackTrace();
			throw new SemanticModelException("Cannot execute query");
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new SemanticModelException("Cannot create query " + sparqlQuery);
		}
	}

	/**
	 * Gets the bNodes for a model
	 * @param model the OntModel
	 * @param sibName the SIB name
	 * @return a String with the blank nodes
	 */
	public HashMap<String, String> renameBNodes2NamedResources(OntModel model, String sibName) {
		HashMap<String, String> bNodes = new HashMap<String, String>();
		
		ArrayList<Statement> removedStatements = new ArrayList<Statement>();
		ArrayList<Statement> addedStatements = new ArrayList<Statement>();
		
		// list the statements in the Model
		StmtIterator iter = model.listStatements();

		// print out the predicate, subject and object of each statement
		while (iter.hasNext()) {
		    Statement stmt      = iter.nextStatement();  // get next statement
		    Resource  subject   = stmt.getSubject();     // get the subject
		    Property  predicate = stmt.getPredicate();   // get the predicate
		    RDFNode   object    = stmt.getObject();      // get the object
		    
		    boolean anonPresence = false;
		    
		    if (subject.isAnon()) {
		    	anonPresence = true;
		    	// Create a new named node
		    	AnonId anonId = subject.getId();
		    	if (bNodesMapping.get(anonId.getLabelString()) == null) {
		    		StringBuilder newAnonURI = new StringBuilder();
		    		newAnonURI.append(DEFAULT_NAMESPACE).append(sibName).append("#").append("_bNode").append(bNodeSequential++);
		    		bNodesMapping.put(anonId.getLabelString(), newAnonURI.toString());
		    		bNodes.put(anonId.getLabelString(), newAnonURI.toString());
		    		subject = model.createResource(newAnonURI.toString());
		    	} else {
		    		subject = model.getResource(bNodesMapping.get(anonId.getLabelString()));
		    		// bNode was already declared
		    		bNodes.put(anonId.getLabelString(), bNodesMapping.get(anonId.getLabelString()));		    		
		    	}
		    }
		    
		    if (predicate.isAnon()) {
		    	anonPresence = true;
		    	// Create a new named node
		    	AnonId anonId = predicate.getId();
		    	if (bNodesMapping.get(anonId.getLabelString()) == null) {
		    		StringBuilder newAnonURI = new StringBuilder();
		    		newAnonURI.append(DEFAULT_NAMESPACE).append(sibName).append("#").append("_bNodeProp").append(bNodeSequential++);
		    		bNodesMapping.put(anonId.getLabelString(), newAnonURI.toString());
		    		bNodes.put(anonId.getLabelString(), newAnonURI.toString());
		    		predicate = model.createProperty(newAnonURI.toString());
		    	} else {		    		
		    		predicate = model.getProperty(bNodesMapping.get(anonId.getLabelString()));
		    		// bNode was already declared
		    		bNodes.put(anonId.getLabelString(), bNodesMapping.get(anonId.getLabelString()));		    		
		    	}
		    }
		    
		    if (object.isAnon()) {
		    	anonPresence = true;
		    	// Create a new named node
		    	AnonId anonId = ((Resource)object).getId();
		    	if (bNodesMapping.get(anonId.getLabelString()) == null) {
		    		StringBuilder newAnonURI = new StringBuilder();
		    		newAnonURI.append(DEFAULT_NAMESPACE).append(sibName).append("#").append("_bNode").append(bNodeSequential++);
		    		bNodesMapping.put(anonId.getLabelString(), newAnonURI.toString());
		    		bNodes.put(anonId.getLabelString(), newAnonURI.toString());
		    		object = model.createResource(newAnonURI.toString());
		    	} else {
		    		object = model.getResource(bNodesMapping.get(anonId.getLabelString()));
		    		// bNode was already declared
		    		bNodes.put(anonId.getLabelString(), bNodesMapping.get(anonId.getLabelString()));
		    	}
		    }
		    
		    if (anonPresence) {
		    	Statement newStmt = model.createStatement(subject, predicate, object);
		    	addedStatements.add(newStmt);
		    	removedStatements.add(stmt);
		    }
		} 		
		
		model.add(addedStatements);
		model.remove(removedStatements);
		
		return bNodes;
	}
	
	/**
	 * Gets the collection of triples referred to the individuals
	 * @return the collection of triples of the model
	 */
	public Collection<Triple> getAllTriples() {

		logger.debug("Getting all the triples...");
		
		StmtIterator it = model.listStatements();
		
		return TripleUtil.obtainTriples(it.toList(), model.getNsPrefixMap());			

	}
	
	/*Added by Gerrit*/
	public void writeTriples() {
		
		try{
			
			 String timestamp = new Date().toString();
			 FileOutputStream fout=new FileOutputStream("logs/inferred_" + timestamp + ".rdf");
			 //newTriples.write(fout, "N-TRIPLE");
			 newTriples.write(fout);
			 fout.close();
			 
			 FileOutputStream fout2=new FileOutputStream("logs/model_" + timestamp + ".rdf");
			 model.write(fout2);
			 fout2.close();
			
			 /*
			StmtIterator iter = newTriples.listStatements();
			
			while(iter.hasNext()) {
				Statement s = iter.nextStatement();
				out.write(s.getSubject().toString()+", "+s.getPredicate().toString()+", "+s.getObject().toString() + "\n");
			}
			  out.close(); */
		  }catch (Exception e){
		  System.err.println("Error: " + e.getMessage());
		  }
	}
	
	/* Added by Gerrit */
	public void runInferences() {
		try{
			FileWriter fstream = new FileWriter("stats.txt",true);
			BufferedWriter out = new BufferedWriter(fstream);
			
			out.write(model.size() + ", ");
			// MODEL SIZE, INFERRED MODEL SIZE, INFERENCE DURATION, CONSTRAINT DETECTION DURATION
			
			long startTime = System.currentTimeMillis();
			newTriples.removeAll();
			SPINInferences.run(model, newTriples, cls2Query, cls2Constructor, initialTemplateBindings, null, null, false, SPIN.rule, comparator, null);
			logger.debug("INFERRED SPIN TRIPLES: " + newTriples.size());
			long endTime = System.currentTimeMillis();
			long duration = (endTime - startTime);
			
			out.write(newTriples.size() + ", ");
			
			out.write(duration + ", ");
			writeTriples();
			
			//SPINInferences.run(cleanupModel, newTriples, cls2Query, cls2Constructor, initialTemplateBindings, null, null, false, SPIN.rule, comparator, null);
			//logger.debug("After cleanup: " + newTriples.size());
			
			//writeNewTriples("cleanup.txt");
			
			// Run all constraints
			startTime = System.currentTimeMillis();
//			List<ConstraintViolation> cvs = SPINConstraints.check(model, null);
			endTime = System.currentTimeMillis();
			duration = (endTime - startTime);
			
			
			out.write( duration + "\n");
			
/*
			System.out.println("Constraint violations:");
			for(ConstraintViolation cv : cvs) {
				
				Property pred = cv.getPaths().iterator().next().getPredicate();
				System.out.println(cv.getMessage() + " - at " + SPINLabels.get().getLabel(cv.getRoot()) + ", getPath: " + SPINLabels.get().getLabel(pred) );//+ ",getFixes: " + cv.getFixes().toString()  );
			
				if(cv.getMessage().contains("Irreflexive property")) {
					
					Statement stmt = model.createStatement(cv.getRoot(), pred, cv.getRoot());
					logger.debug("Removing statement: " + SPINLabels.get().getLabel(stmt.getSubject()) + ", " + SPINLabels.get().getLabel(stmt.getPredicate()) + ", " + SPINLabels.get().getLabel(stmt.getResource()));
					model.remove(stmt); //remove from asserted model
					newTriples.remove(stmt); //and from inferred model
				}
			}
*/			
			out.close();
		}catch (Exception e){
			  System.err.println("Error: " + e.getMessage());
		}
	}
	
	/* Added by Gerrit */
	private static OntModel loadModelWithImports(String url) {
		Model baseModel = ModelFactory.createDefaultModel(ReificationStyle.Minimal);
		baseModel.read(url);
		return ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, baseModel);
	}
	
	/* Added by Gerrit */
	private static OntModel loadModelWithImports(String file, String uri) {
		Model baseModel = ModelFactory.createDefaultModel(ReificationStyle.Minimal);
		InputStream input = FileManager.get().open(file);
		baseModel.read(input,uri);
		return ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, baseModel);
	}
	
}



