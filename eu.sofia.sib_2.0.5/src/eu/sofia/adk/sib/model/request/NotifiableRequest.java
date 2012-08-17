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

package eu.sofia.adk.sib.model.request;

import eu.sofia.adk.sib.ssap.SIB;
import eu.sofia.adk.ssapmessage.SSAPMessageRequest;

/**
 * This abstract class is the responsible for checking the subscriptions
 * and create as many indications as needed. This abstract class must
 * be extended for all requests that modifies the semantic model content.
 * 
 * @author Fran Ruiz, fran.ruiz@tecnalia.com, ESI
 * @author Raul Otaolea, raul.otaolea@tecnalia.com, ESI 
 * @see eu.sofia.adk.sib.model.request.Request
 *
 */
public abstract class NotifiableRequest extends Request {

	/**
	 * The default constructor
	 * @param request the SSAP Message request
	 * 
	 */
	public NotifiableRequest(SSAPMessageRequest request, SIB sib) {
		super(request, sib);
	}

	/**
	 * Executes the operation indicated in the SSAPMessageRequest.
	 * After doing this, checks the subscriptions and generates as
	 * many indications as needed.
	 */
	@Override
	public void run() {
		super.run();
		sib.getSubscriptionManager().checkSubscriptions(sib);
	}
}
