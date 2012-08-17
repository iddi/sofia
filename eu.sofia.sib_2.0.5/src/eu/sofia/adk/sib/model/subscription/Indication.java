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

import org.apache.log4j.Logger;

import eu.sofia.adk.sib.model.session.SIBSession;
import eu.sofia.adk.ssapmessage.SSAPMessageIndication;

/**
 * Sends the indication to subscribed node
 * 
 * @author Fran Ruiz, fran.ruiz@tecnalia.com, ESI
 * @author Raul Otaolea, raul.otaolea@tecnalia.com, ESI
 */
public class Indication implements Runnable {

	/** The logger */
	private static Logger logger;

	/** The SIBSession */
	protected SIBSession session;
	
	/** The response message */
	protected SSAPMessageIndication indication;
	
	/**
	 * Creates a new indication sending request
	 * @param indication the INDICATION SSAPMessage
	 * @param session the session
	 */
	public Indication(SSAPMessageIndication indication, SIBSession session) {
		logger = Logger.getLogger(Indication.class);
		this.session = session;
		this.indication = indication;
	}

	/**
	 * Method needed to be executed by ExecutorService.
	 * @see java.util.concurrent.Callable
	 * @throws Exception When the request cannot be executed
	 */
	@Override
	public void run() {
		try {
			if (indication != null) {
				logger.debug("<" + indication.getTransactionId() + ">::SIB Sending::" +
						indication.getTransactionType() + " " + indication.getMessageType());
				logger.debug(indication.toString());
				// Sends the message to the Smart Application

				long idSubscription = Long.parseLong(indication.getSubscriptionIdParameter().getContent());
				session.getSubscriptionProxy(idSubscription).sendMessage(indication);

			}
			// Sends the message to the Smart Application

			
		} catch (Exception ex) {
			logger.error("Cannot process the request. Caused by " + ex.getMessage());
			logger.error("Stack trace:\n");
			ex.printStackTrace();
		}
	}
}
