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

package eu.sofia.adk.sib.data;

import java.util.Collection;


/**
 * This interface determines the set of methods needed to be implemented by
 * the classes responsible of the subscriptions
 * 
 * @author Fran Ruiz, fran.ruiz@tecnalia.com, ESI
 */
public interface ISubscriptionHolderListener {

	/**
	 * Performs an initialization of the set of subscriptions.
	 * @param subscriptions the collection of subscriptions 
	 */
	public void initSubscriptions(Collection<Subscription> subscriptions);

	/**
	 * Adds a subscription
	 * @param subscription the added subscription
	 */
	public void addSubscription(Subscription subscription);

	/**
	 * Removes a subscription
	 * @param subscriptionId the subscription identifier
	 */
	public void removeSubscription(Long subscriptionId);

	/**
	 * Removes a set of subscriptions
	 * @param subscriptionIds the set of subscription identifiers
	 */
	public void removeSubscriptions(Collection<Long> subscriptionIds);
	
	/**
	 * Updates a subscription set
	 * @param subscriptions a collection of subscriptions
	 */
	public void updateSubscriptions(Collection<Subscription> subscriptions);
	
	/**
	 * Resets the content of the SIB
	 */
	public void reset();	
}
