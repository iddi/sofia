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

package eu.sofia.adk.sib.model.subscription;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;

import org.apache.log4j.Logger;

import eu.sofia.adk.sib.data.Subscription;
import eu.sofia.adk.sib.exception.EncodingNotSupportedException;
import eu.sofia.adk.sib.exception.RDFM3ParseException;
import eu.sofia.adk.sib.exception.SemanticModelException;
import eu.sofia.adk.sib.exception.TransformationException;
import eu.sofia.adk.sib.model.query.SOFIAResultSet;
import eu.sofia.adk.sib.model.query.SOFIAResultSetFormatter;
import eu.sofia.adk.sib.model.session.SIBSession;
import eu.sofia.adk.sib.protocol.m3.M3Parser;
import eu.sofia.adk.sib.protocol.m3.RDFM3;
import eu.sofia.adk.sib.ssap.SIB;
import eu.sofia.adk.sib.util.EncodingFactory;
import eu.sofia.adk.ssapmessage.SSAPMessageIndication;
import eu.sofia.adk.ssapmessage.parameter.SSAPMessageParameter;
import eu.sofia.adk.ssapmessage.parameter.SSAPMessageRDFParameter.TypeAttribute;

/**
 * The Subscription Manager controls the current subscriptions to the semantic
 * data. In this class is stored the queries, as well as subscription data, 
 * including the subscription identifier and the smart application data. 
 * 
 * @author Fran Ruiz, ESI
 *
 */
public class SubscriptionManager {

	/** The logger */
	private static Logger logger;
	
	/** The subscription identifier */
	private long subscriptionId = 0;

	/** The current subscribed entities */
	private LinkedHashMap<String, Subscriber> subscribers;
	
	/**
	 * Constructor of the Semantic Information Broker
	 */
	public SubscriptionManager() {
		logger = Logger.getLogger(SubscriptionManager.class);
		subscribers = new LinkedHashMap<String, Subscriber>();
	}
	
	public long createNewSubscriptionId() {
		return subscriptionId++;
	}

	/**
	 * Adds a query to the subscription list
	 * @param query The sparql query
	 * @param originalQuery the original query
	 * @param originalEncoding the original encoding
	 * @param session The SIBSession of joined node
	 * @return the subscription identifier
	 */
	public long addSubscription(String query, String originalQuery, TypeAttribute originalEncoding, SIBSession session, SOFIAResultSet queryResult) {
		
		// Checks the current subscribed triples state for triple parameter
		if (subscribers.containsKey(query)) {

//			boolean already = false;

			Subscriber subscription = subscribers.get(query);

//			if (subscription.getOriginalEncoding().equals(originalEncoding) && 
//					subscription.getSubscribedKP(session.getNodeId()) != null) {
//				already = true;
//			}
//			
//			if (!already) {
				subscription.addSubscribedKP(new SubscribedKP(++subscriptionId, session));
//			}
		} else { // if it is not a previously subscribed query, add it
			// Create a new entry for the new query
			Subscriber subscriber = new Subscriber(originalEncoding, originalQuery, queryResult);
			subscriber.addSubscribedKP(new SubscribedKP(++subscriptionId, session));
			subscribers.put(query, subscriber);
		}
		
		return subscriptionId;
	}

	/**
	 * Adds a query to the subscription list
	 * @param query The sparql query
	 * @param originalQuery the original query
	 * @param originalEncoding the original encoding
	 * @param session The SIBSession of joined node
	 * @return the subscription identifier
	 */
	public long addSubscription(String query, String originalQuery, TypeAttribute originalEncoding, SIBSession session, SOFIAResultSet queryResult, long subscriptionId) {
		
		// Checks the current subscribed triples state for triple parameter
		if (subscribers.containsKey(query)) {

			Subscriber subscription = subscribers.get(query);
			subscription.addSubscribedKP(new SubscribedKP(subscriptionId, session));
		} else { // if it is not a previously subscribed query, add it
			// Create a new entry for the new query
			Subscriber subscriber = new Subscriber(originalEncoding, originalQuery, queryResult);
			subscriber.addSubscribedKP(new SubscribedKP(subscriptionId, session));
			subscribers.put(query, subscriber);
		}
		
		return subscriptionId;
	}	
	
	/**
	 * Removes all subscriptions made for a triple 
	 * @param subscriptionId The subscription identifier
	 * @param nodeId The node identifier
	 */
	public boolean removeSubscription(long subscriptionId, String nodeId) {

		boolean removed = false;
		ArrayList<String> removeQueries = new ArrayList<String>();
		
		for (String query : subscribers.keySet()) {
			Subscriber subscriber = subscribers.get(query);
			
			SubscribedKP subscribedKP = subscriber.getSubscribedKP(nodeId);
			
			if (subscribedKP != null && subscribedKP.getSubscriptionId() == subscriptionId) { // this is the subscribed kp to remove
				subscriber.removeSubscribedKP(subscribedKP);
				removed = true;
			}
			
			if (!subscriber.hasSubscribedKPs()) {
				removeQueries.add(query);
			}
			
		} // end subscribed triples
		
		for (String removeQuery : removeQueries) {
			subscribers.remove(removeQuery);
		}
		
		return removed;
	}
	
	/**
	 * Removes all subscriptions associated to a node 
	 * @param nodeId The node identifier
	 * @return the set of subscriptionId
	 */
	public Collection<Long> removeSubscriptions(String nodeId) {

		ArrayList<Long> removed = new ArrayList<Long>();
		ArrayList<String> removedQueries = new ArrayList<String>();
		
		for (String query : subscribers.keySet()) {
			Subscriber subscriber = subscribers.get(query);
			
			ArrayList<SubscribedKP> subscribedKPs = new ArrayList<SubscribedKP>();
			subscribedKPs.addAll(subscriber.listSubscribedKPs());
			
			for(SubscribedKP subscribedKP : subscribedKPs) {
				if (subscribedKP.getSession().getNodeId().equals(nodeId)) {
					subscriber.removeSubscribedKP(subscribedKP);
					removed.add(subscribedKP.getSubscriptionId());
				}
			}
			
			if (!subscriber.hasSubscribedKPs()) {
				removedQueries.add(query);
			}
		} // end subscribed triples
		
		for (String query : removedQueries) {
			subscribers.remove(query);
		}	
		
		return removed;
	}
	
	/**
	 * Checks the subscriptions in the database
	 */
	public void checkSubscriptions(SIB sib) {
		
		ArrayList<Subscription> changedSubscriptions = new ArrayList<Subscription>();
		
		for (String query : subscribers.keySet()) {
			Subscriber subscriber = subscribers.get(query);
			
			try {
				SOFIAResultSet currentResultSet = sib.getSemanticModel().query(query);
				SOFIAResultSet previousResultSet = subscriber.getResults();
				// If it is not initialized (impossible)
				if (previousResultSet == null) {
					previousResultSet = currentResultSet;
					logger.error("Some error occurred during the subscription checking...");
				} else {
					// Checks the obsolete and new results
					SOFIAResultSet obsoleteResults = previousResultSet.getObsoleteResults(currentResultSet);
					// logger.debug("OBSOLETE RESULTS");
					// logger.debug("======================================================");
					// logger.debug(SOFIAResultSetFormatter.asXMLString(obsoleteResults));
					
					SOFIAResultSet newResults = previousResultSet.getNewResults(currentResultSet);
					//logger.debug("NEW RESULTS");
					//logger.debug("======================================================");
					//logger.debug(SOFIAResultSetFormatter.asXMLString(newResults));
	
					if (subscriber.hasSubscribedKPs() && 
							(obsoleteResults.size() > 0 || newResults.size() > 0)) {
						sendIndicationResults(subscriber, newResults, obsoleteResults, sib);
						
						// Transforms into a format to be send to the attached viewers
						String previousXMLResults = SOFIAResultSetFormatter.asXMLString(previousResultSet);
						String currentXMLResults = SOFIAResultSetFormatter.asXMLString(currentResultSet);
						if (!previousXMLResults.equals(currentXMLResults)) {
							
							// notifies the listeners
							for (SubscribedKP subscribedKP : subscriber.listSubscribedKPs()) {
								Subscriber viewerSubscriber = subscribers.get(query);
								String originalQuery = viewerSubscriber.getOriginalQuery();
								try {
									String formattedResults = EncodingFactory.getInstance().getTransformer(viewerSubscriber.getOriginalEncoding()).toOriginalEncoding(originalQuery, currentResultSet);
									Subscription subscription = new Subscription(subscribedKP.getSubscriptionId(),
											subscribedKP.getSession().getNodeId(),
											toSortString(originalQuery), 
											toSortString(formattedResults));
									changedSubscriptions.add(subscription);
								} catch (TransformationException e) {
									e.printStackTrace();
								} catch (EncodingNotSupportedException e) {
									e.printStackTrace();
								}
								
							}
						}
					}
					// Updates the results set with updated data
					subscriber.updateResultSet(currentResultSet);
				}
			} catch (SemanticModelException ex) {
				ex.printStackTrace();
				logger.error("Cannot query to the current semantic model due to " + ex.getMessage());
				return;
			}
		}
		
		if (changedSubscriptions.size() > 0) { // notify the subscription listeners
			sib.getViewerListenerManager().updateSubscriptions(changedSubscriptions);
		}
	}

	/**
	 * Creates the indication message and queues it into the processing queue
	 * @param subscribedNodes a Vector of subscribed nodes
	 * @param newResults a SOFIAResultSetFormatter with the new results
	 * @param obsoleteResults a SOFIAResultSetFormatter with the obsolete results
	 */
	private void sendIndicationResults(Subscriber subscription,
			SOFIAResultSet newResults, SOFIAResultSet obsoleteResults, SIB sib) {
		
		String formattedNewResults = null;
		String formattedObsoleteResults = null;
		
		// The indication response 
		SSAPMessageIndication message;		
		
		try {

			formattedNewResults = EncodingFactory.getInstance().getTransformer(subscription.getOriginalEncoding()).toOriginalEncoding(subscription.getOriginalQuery(), newResults);
			formattedObsoleteResults = EncodingFactory.getInstance().getTransformer(subscription.getOriginalEncoding()).toOriginalEncoding(subscription.getOriginalQuery(), obsoleteResults);

//			if (newResults.size() > 0) {
//				formattedNewResults = EncodingFactory.getInstance().getTransformer(subscription.getOriginalEncoding()).toOriginalEncoding(subscription.getOriginalQuery(), newResults);
//			} else {
//				//formattedNewResults = "";
//				// for compatibility with current Smart-M3 implementation
//				formattedNewResults = TRIPLELIST_OPENINGTAG + TRIPLELIST_CLOSINGTAG + CR;
//
//			}
//			
//			if (obsoleteResults.size() > 0) {
//				formattedObsoleteResults = EncodingFactory.getInstance().getTransformer(subscription.getOriginalEncoding()).toOriginalEncoding(subscription.getOriginalQuery(), obsoleteResults);
//			} else {
//				//formattedObsoleteResults = "";
//				// for compatibility with current Smart-M3 implementation
//				formattedObsoleteResults = TRIPLELIST_OPENINGTAG + TRIPLELIST_CLOSINGTAG + CR;
//			}		
			
		} catch (EncodingNotSupportedException ex) {
			ex.printStackTrace();
		} catch (TransformationException ex) {
			ex.printStackTrace();
		}
		
		for (SubscribedKP nodeSubscription : subscription.listSubscribedKPs()) {
			// The indication response 
			message = new SSAPMessageIndication(
					nodeSubscription.getSession().getNodeId(), 
					sib.getName(), 
					nodeSubscription.getSubscriptionId());		

			
			SSAPMessageParameter newResultsParam = new SSAPMessageParameter(
					SSAPMessageParameter.NameAttribute.NEWRESULTS,
					formattedNewResults);
				
			message.addParameter(newResultsParam);
			
			SSAPMessageParameter obsoleteResultsParam = new SSAPMessageParameter(
					SSAPMessageParameter.NameAttribute.OBSOLETERESULTS,
					formattedObsoleteResults);
			message.addParameter(obsoleteResultsParam);

			SIBSession session = nodeSubscription.getSession();
			
			// Sends the message to the Smart Application
			Indication indication = new Indication(message, session);
			
			sib.getRequestedCommandExecutor().execute(indication);
		}
	}

	/**
	 * Get the subscriptions that are currently on the SIB
	 * @return a Hashtable containing the subscriptions 
	 */
	public Collection<Subscription> getAllSubscriptions(SIB sib) {
//		ArrayList<Subscription> subscriptionList = new ArrayList<Subscription>();
//		
//		for (String query : subscriptions.keySet()) {
//			// For each query it is necessary a result
//			SOFIAResultSet result = sib.getSemanticModel().query(query);
//			String queryResult = SOFIAResultSetFormatter.asXMLString(result);
//			
//			for (Subscription smartAppSubscription : subscriptions.get(query)) {
//				SubscriptionViewer subscription = new SubscriptionViewer();
//				subscription.setQuery(query);
//				subscription.setResult(queryResult);
//				subscription.setNodeId(smartAppSubscription.getSession().getNodeId());
//				subscription.setSubscriptionId(smartAppSubscription.getSubscriptionId());
//				subscriptionList.add(subscription);
//			}
//		}
//
//		return subscriptionList;
		return null;
	}

	/**
	 * Gets a short representation of data
	 * @param data the data to be processed
	 * @return the sort form of data
	 */
	public String toSortString(String data) {
		try {
			// Parses the input stream 
			M3Parser parser = new M3Parser();
			RDFM3 m3 = parser.parse(data);
			
			return m3.toSortString();
			
		} catch (RDFM3ParseException ex) {
			// do nothing
		} catch (Exception ex) {
			// do nothing
		}
		
		return data;
	}
}
