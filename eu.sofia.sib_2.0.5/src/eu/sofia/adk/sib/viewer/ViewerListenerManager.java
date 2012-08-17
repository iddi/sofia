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

package eu.sofia.adk.sib.viewer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Statement;

import eu.sofia.adk.sib.data.ISemanticContentHolderListener;
import eu.sofia.adk.sib.data.ISessionHolderListener;
import eu.sofia.adk.sib.data.ISubscriptionHolderListener;
import eu.sofia.adk.sib.data.Session;
import eu.sofia.adk.sib.data.Subscription;
import eu.sofia.adk.sib.data.Triple;
import eu.sofia.adk.sib.data.util.TripleUtil;

/**
 * This class contains the listeners to the data holders. The data holder
 * will be notified about changes in the Semantic Information Broker data,
 * including active sessions, subscriptions and individuals inside the
 * semantic data warehouse. 
 * 
 * @author Fran Ruiz, fran.ruiz@tecnalia.com, ESI
 *
 */
public class ViewerListenerManager {

	/** The collection of triples listeners */
	protected Collection<ISemanticContentHolderListener> triplesListeners = null;
	
	/** The collection of session listeners */
	protected Collection<ISessionHolderListener> sessionListeners = null;
	
	/** The collection of subscription listeners */
	protected Collection<ISubscriptionHolderListener> subscriptionListeners = null;
	
	/** The logger */
	private static Logger logger;
	
	/**
	 * Constructor
	 */
	public ViewerListenerManager() {
		sessionListeners = new ArrayList<ISessionHolderListener>();
		subscriptionListeners = new ArrayList<ISubscriptionHolderListener>();
		triplesListeners = new ArrayList<ISemanticContentHolderListener>();
		logger = Logger.getLogger(ViewerListenerManager.class);
	}
  
	/**
	 * Adds a listener to the list of listeners
	 * @param listener a DataHolderListener
	 */
    public void addListener(ISemanticContentHolderListener listener) {
       	logger.debug("Adding the semantic content listener...");
    	triplesListeners.add(listener);
    }

	/**
	 * Adds a listener to the list of listeners
	 * @param listener a DataHolderListener
	 */
    public void addListener(ISessionHolderListener listener) {
       	sessionListeners.add(listener);
    }

	/**
	 * Adds a listener to the list of listeners
	 * @param listener a DataHolderListener
	 */
    public void addListener(ISubscriptionHolderListener listener) {
       	subscriptionListeners.add(listener);
    }

    /**
     * Removes a listener from the set of listeners
     * @param listener a DataHolderListener
     */
    public boolean removeListener(ISemanticContentHolderListener listener) {
       	return triplesListeners.remove(listener);
    }

    /**
     * Removes a listener from the set of listeners
     * @param listener a DataHolderListener
     */
    public boolean removeListener(ISessionHolderListener listener) {
       	return sessionListeners.remove(listener);
    }

    /**
     * Removes a listener from the set of listeners
     * @param listener a DataHolderListener
     */
    public boolean removeListener(ISubscriptionHolderListener listener) {
       	return subscriptionListeners.remove(listener);
    }    

    /**
     * Notifies the session addition
     * @param session the added Session object
     */
	public void addSession(Session session) {
		if (sessionListeners.size() > 0) {
			//logger.info("There are no session listeners.");
		//} else {
			for(ISessionHolderListener listener : sessionListeners) {
				listener.addSession(session);
			}
		}		
	}

    /**
     * Notifies the session removal
     * @param session the removed Session object
     */
	public void removeSession(Session session) {
		if (sessionListeners.size() > 0) {
			//logger.info("There are no session listeners.");
		//} else {
			for(ISessionHolderListener listener : sessionListeners) {
				listener.removeSession(session);
			}
		}		
	}

	/**
	 * Notifies a new model addition to the current model
	 * @param nodeId 
	 * @param model a OntModel
	 */
	public void addModel(String nodeId, OntModel model) {
		if (triplesListeners.size() > 0) {
			//logger.info("There are no semantic model listeners.");
		//} else {
			Collection<Triple> triples = TripleUtil.obtainTriples(model);
			for(ISemanticContentHolderListener listener : triplesListeners) {
				listener.addTriples(triples, nodeId);
			}
		}	
	}
	
	/**
	 * Notifies a new model removal
	 * @param nsMap 
	 * @param model a OntModel
	 */
	public void removeStatements(String nodeId, Collection<Statement> statements, Map<String, String> nsMap) {
		if (triplesListeners.size() > 0) {
			//logger.info("There are no semantic model listeners.");
		//} else {
			Collection<Triple> triples = TripleUtil.obtainTriples(statements, nsMap);
			for(ISemanticContentHolderListener listener : triplesListeners) {
				listener.removeTriples(triples, nodeId);
			}
		}	
	}
	

	public void removeModel(String nodeId, OntModel removedModel) {
		if (triplesListeners.size() > 0) {
			//logger.info("There are no semantic model listeners.");
		//} else {
			Collection<Triple> triples = TripleUtil.obtainTriples(removedModel);
			for(ISemanticContentHolderListener listener : triplesListeners) {
				listener.removeTriples(triples, nodeId);
			}
		}	
	}		

	/**
	 * Notifies a model update in the current model
	 * @param insertModel the model to insert
	 * @param removalModel the model to remove
	 */
	public void updateModel(String nodeId, OntModel insertModel, OntModel removalModel) {
		if (triplesListeners.size() > 0) {
			//logger.info("There are no semantic model listeners.");
		//} else {
			Collection<Triple> insertTriples = TripleUtil.obtainTriples(insertModel);
			Collection<Triple> removeTriples = TripleUtil.obtainTriples(removalModel);
			for(ISemanticContentHolderListener listener : triplesListeners) {
				listener.removeTriples(removeTriples, nodeId);
				listener.addTriples(insertTriples, nodeId);
			}
		}	
	}

	/**
	 * Adds a subscription to the current model
	 * @param subscription the Subscription
	 */
	public void addSubscription(Subscription subscription, String nodeId) {
		if (subscriptionListeners.size() > 0) {
			//logger.info("There are no subscription listeners.");
		//} else {
			for(ISubscriptionHolderListener listener : subscriptionListeners) {
				listener.addSubscription(subscription);
			}
		}
	}

	/**
	 * Removes a subscription to the current model
	 * @param subscriptionId the Subscription identifier
	 */
	public void removeSubscription(Long subscriptionId, String nodeId) {
		if (subscriptionListeners.size() > 0) {
			//logger.info("There are no subscription listeners.");
		//} else {
			for(ISubscriptionHolderListener listener : subscriptionListeners) {
				listener.removeSubscription(subscriptionId);
			}
		}
	}
	
	/**
	 * Removes a subscription from the current model
	 * @param subscriptionIds the list of subscription identifier to remove
	 */
	public void removeSubscription(Collection<Long> subscriptionIds, String nodeId) {
		if (subscriptionListeners.size() > 0) {
			//logger.info("There are no subscription listeners.");
		//} else {
			for(ISubscriptionHolderListener listener : subscriptionListeners) {
				listener.removeSubscriptions(subscriptionIds);
			}
		}
	}
	
	/**
	 * Updates a subscription in the current model
	 * @param subscriptions a list of subscriptions
	 */
	public void updateSubscriptions(Collection<Subscription> subscriptions) {
		if (subscriptionListeners.size() > 0) {
			//logger.info("There are no subscription listeners.");
		//} else {
			for(ISubscriptionHolderListener listener : subscriptionListeners) {
				listener.updateSubscriptions(subscriptions);
			}
		}
	}
	
	/**
	 * Resets the content of the semantic model
	 */
	public void resetModel() {
		if (triplesListeners.size() > 0) {
			//logger.info("There are no triples listeners.");
		//} else {
			for(ISemanticContentHolderListener listener : triplesListeners) {
				listener.reset();
			}
		}
	}	

	
	/**
	 * Resets the content of the semantic model
	 */
	public void resetSessions() {
		if (sessionListeners.size() > 0) {
			//logger.info("There are no session listeners.");
		//} else {
			for(ISessionHolderListener listener : sessionListeners) {
				listener.reset();
			}
		}
	}		
	
	/**
	 * Resets the content of a subscription in the current model
	 */
	public void resetSubscriptions() {
		if (subscriptionListeners.size() > 0) {
			//logger.info("There are no subscription listeners.");
		//} else {
			for(ISubscriptionHolderListener listener : subscriptionListeners) {
				listener.reset();
			}			

		}
	}	

}
