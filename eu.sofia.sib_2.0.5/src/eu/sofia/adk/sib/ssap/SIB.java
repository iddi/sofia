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

package eu.sofia.adk.sib.ssap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import eu.sofia.adk.sib.data.Session;
import eu.sofia.adk.sib.data.Subscription;
import eu.sofia.adk.sib.exception.MalformedSSAPMessageException;
import eu.sofia.adk.sib.exception.SIBException;
import eu.sofia.adk.sib.model.request.InsertRequest;
import eu.sofia.adk.sib.model.request.JoinRequest;
import eu.sofia.adk.sib.model.request.LeaveRequest;
import eu.sofia.adk.sib.model.request.QueryRequest;
import eu.sofia.adk.sib.model.request.RemoveRequestwWildcards;
import eu.sofia.adk.sib.model.request.Request;
import eu.sofia.adk.sib.model.request.SubscribeRequest;
import eu.sofia.adk.sib.model.request.UnsubscribeRequest;
import eu.sofia.adk.sib.model.request.UpdateRequest;
import eu.sofia.adk.sib.model.semantic.SemanticModel;
import eu.sofia.adk.sib.model.session.SessionManager;
import eu.sofia.adk.sib.model.subscription.SubscriptionManager;
import eu.sofia.adk.sib.service.ISIB;
import eu.sofia.adk.sib.service.ISIBStateListener;
import eu.sofia.adk.sib.util.SIBStateSupport;
import eu.sofia.adk.sib.viewer.Viewer;
import eu.sofia.adk.sib.viewer.ViewerListenerManager;
import eu.sofia.adk.ssapmessage.SSAPMessage.TransactionType;
import eu.sofia.adk.ssapmessage.SSAPMessageRequest;
import eu.sofia.adk.ssapmessage.SSAPMessageResponse;
import eu.sofia.adk.ssapmessage.parameter.SSAPMessageParameter;
import eu.sofia.adk.ssapmessage.parameter.SSAPMessageParameter.NameAttribute;
import eu.sofia.adk.ssapmessage.parameter.SSAPMessageStatusParameter;

/**
 * This class implements the SSAP Message Protocol calling to the 
 * associated actions in the SIB. Methods receive messages from 
 * Smart Applications in asynchronous mode. When messages are processed 
 * by the SIB, then a response is sent to the caller.
 *  
 * @author Jon Duran, ESI
 * @author Fran Ruiz, ESI
 */
public class SIB implements ISIB {

	/**
	 * Types of possible transactions with the SIB using the SSAP protocol.
	 */
	public enum SIBStatus {  
		RUNNING		(1),
		STOPPED		(0);
		
		private final int status;
		
		SIBStatus(int status) {
			this.status = status;
		}
		
		public int getStatus() {
			return status;
		}
		
		public boolean isRunning() {
			return (this == SIBStatus.RUNNING);
		}
		
		public boolean isStopped() {
			return (this == SIBStatus.STOPPED);
		}
	}	
	
	
	
	/** The logger */
	private static Logger logger;
	
	private SessionManager sessionManager;
	private SubscriptionManager subscriptionManager;
	private SemanticModel semanticModel;
	
	private ArrayList<Viewer> viewerListeners;	
	
	private ViewerListenerManager viewerListenerManager;
	
	/** The manager that generates the request messages to be processed */
	protected ExecutorService requestedCommandExecutor;

	/** SIB Identifier */
	private int id;
	
	/** SIB Identifier generator */
	private static int idGenerator;

	/** SIB Identifier generator latch to avoid concurrency*/
	private static Object uniqueId;
	
	/** SIB Name */
	private String name = null;	
	
	/** Indicates if the SIB is started **/
	private SIBStatus status;
	
	/** Manages ISIBStateListener listeners to notify when SIB starts/stops **/
	private SIBStateSupport support;
	
	static {
		uniqueId = new Object();
	}
	
	/**
	 * Constructor of the Semantic Information Broker
	 * @param name the SIB name
	 */
	public SIB(String name) throws SIBException {
		if (name != null && !name.equals("")) { // name
			this.name = name;
		} else {
			throw new SIBException("Invalid SIB name");
		}
		this.id = generateId();
		
		this.status = SIBStatus.STOPPED;
		
		logger = Logger.getLogger(SIB.class);
		this.support = new SIBStateSupport();
		this.init();
		this.initViewers();
	}
	
	/**
	 * Generate a unique id for the SIB.
	 */
	private static int generateId() {
		synchronized(uniqueId) {
			return ++idGenerator;
		}
	}

	/**
	 * Add a listener to see this SIB's state changes (running, stopped).
	 */
	@Override
	public void addStateListener(ISIBStateListener listener) {
		this.support.addListener(listener);
	}

	/**
	 * Removes a listener to see this SIB's state changes (running, stopped).
	 */
	@Override
	public void removeStateListener(ISIBStateListener listener) {
		this.support.removeListener(listener);
	}
	
	/**
	 * Initialize the SIB variables.
	 */
	private void init() {
    	// Inits the session manager, the subscription and the semantic model.
    	this.sessionManager = new SessionManager();
    	this.subscriptionManager = new SubscriptionManager();
    	this.semanticModel = new SemanticModel();		
		this.requestedCommandExecutor = Executors.newSingleThreadExecutor();
	}
	
	/**
	 * Initialize the SIB variables.
	 */
	private void initViewers() {
    	// Inits the session manager, the subscription and the semantic model.
    	this.viewerListeners = new ArrayList<Viewer>();
    	this.viewerListenerManager = new ViewerListenerManager();
	}	
	
	/**
	 * Gets the Smart Space ID
	 * @return the identifier for the smart space covered by the SIB
	 */
	public String getName() {		
		return name;
	}
	
	/**
	 * Gets the SIB internal id
	 * @return String containing the internal id
	 */
	public int getId(){
		return id;
	}
	
	/**
	 * Connects the smart application to this SIB.
	 * This SIB has full information about the joined nodes.
	 * @param proxy the reference for reaching to the smart application (node)
	 * @param message the SSAPMessage corresponding to the JOIN transaction
	 */
	public void connect(SSAPMessageRequest message) throws SIBException, NullPointerException {

		try {
			if(!this.isRunning()) {
				throw new SIBException("SIB is stopped");
			}
			
			if (message == null) { // the message cannot be null
				logger.error("Cannot connect due to the SSAP Message is null");
				throw new NullPointerException("SSAPMessage cannot be null");
			}
			
			if (message.getClientProxy() == null) { // the proxy cannot be null
				logger.error("Cannot connect due to the proxy is null");
				throw new NullPointerException("Client proxy cannot be null");
			}
			
			// Validate the message syntax
			validateSSAPMessageContent(message);
			
			if (!message.getTransactionType().equals(TransactionType.JOIN)) {
				throw new MalformedSSAPMessageException("SSAPMessage transaction is not a JOIN");
			}
			
			if (!message.getSpaceId().equals(getName())) {
				throw new SIBException("Invalid smart space id");
			}
			
			// Create the request and add it to the execution list
			Request joinRequest = new JoinRequest(message, this);
			
			// Submits the joinRequest, locked till it's served (FIFO)
			requestedCommandExecutor.submit(joinRequest);
		} catch (MalformedSSAPMessageException mex) {
			mex.printStackTrace();
			try {
				if (message.getClientProxy() != null) {
					SSAPMessageResponse response = new SSAPMessageResponse(
							message.getNodeId(), message.getSpaceId(), 
							message.getTransactionId(), message.getTransactionType());
					SSAPMessageParameter param = new SSAPMessageStatusParameter(
							SSAPMessageStatusParameter.SIBStatus.STATUS_KP_MESSAGE_SYNTAX);
					response.addParameter(param);
					message.getClientProxy().sendMessage(response);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logger.error("Error in execution");
			throw new SIBException(mex.getMessage(), mex.getCause());					
		} catch (NullPointerException npe) {
			logger.error("Null pointer exception: " + npe.getMessage());
			throw new NullPointerException(npe.getMessage());
		} catch (Exception ex) {
			logger.error("Error in execution");
			throw new SIBException(ex.getMessage());
		}
	}
	
	/**
	 * Disconnects the smart application from this SIB.
	 * Removes all this information from the SIB
	 * @param message the SSAPMessage corresponding to the LEAVE transaction
	 */
	public void disconnect(SSAPMessageRequest message) throws SIBException, NullPointerException {
		try {
			if(!this.isRunning()) {
				throw new SIBException("SIB is stopped");
			}
			if (message == null) { // the message cannot be null
				throw new NullPointerException("SSAPMessage cannot be null");
			}
			
			// Validate the message syntax
			validateSSAPMessageContent(message);
			
			if (!message.getTransactionType().equals(TransactionType.LEAVE)) {
				throw new MalformedSSAPMessageException("SSAPMessage transaction type is not a LEAVE");
			}
			
			if (!message.getSpaceId().equals(getName())) {
				throw new SIBException("Invalid smart space id");
			}
			
			// Create the leave request and add it to the execution list
			Request leaveRequest = new LeaveRequest(message, this);
			
			// Submits the leaveRequest
			requestedCommandExecutor.submit(leaveRequest);
		} catch (MalformedSSAPMessageException mex) {
			mex.printStackTrace();
			try {
				if (message.getClientProxy() != null) {
					SSAPMessageResponse response = new SSAPMessageResponse(
							message.getNodeId(), message.getSpaceId(), 
							message.getTransactionId(), message.getTransactionType());
					SSAPMessageParameter param = new SSAPMessageStatusParameter(
							SSAPMessageStatusParameter.SIBStatus.STATUS_KP_MESSAGE_SYNTAX);
					response.addParameter(param);
					message.getClientProxy().sendMessage(response);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logger.error("Error in execution");
			throw new SIBException(mex.getMessage(), mex.getCause());					
		} catch (NullPointerException npe) {
			logger.error("Null pointer exception: " + npe.getMessage());
			throw new NullPointerException(npe.getMessage());
		} catch (Exception ex) {
			logger.error("Error in execution");
			throw new SIBException(ex.getMessage());
		}
	}
	
	/**
	 * Process the SSAPMessages to interoperate semantically with the SIB.
	 * @param message the SSAPMessage
	 */
	public void process(SSAPMessageRequest message) throws SIBException, NullPointerException {
		try {
			if(!this.isRunning()) {
				throw new SIBException("SIB is stopped");
			}
	
			if (message == null) { // the message cannot be null
				throw new NullPointerException("SSAPMessage cannot be null");
			}
			
			// Validate the message syntax
			validateSSAPMessageContent(message);
			
			if (!message.getSpaceId().equals(getName())) {
				throw new SIBException("Invalid smart space id");
			}
			
			Request request;
				
			switch(message.getTransactionType()) {
				case INSERT:
					request = new InsertRequest(message, this);
					break;
				case REMOVE:
					request = new RemoveRequestwWildcards(message, this);
					break;
				case UPDATE:
					request = new UpdateRequest(message, this);
					break;
				case SUBSCRIBE:
					request = new SubscribeRequest(message, this);
					break;
				case UNSUBSCRIBE:
					request = new UnsubscribeRequest(message, this);
					break;
				case QUERY:
					request = new QueryRequest(message, this);
					break;
				default:
					request = null;
			}
				
			if (request == null) { // if the request has no values
				throw new MalformedSSAPMessageException("SSAPMessage transaction type is not adequate");
			} else {
				requestedCommandExecutor.submit(request);
			}
		} catch (MalformedSSAPMessageException mex) {
			mex.printStackTrace();
			try {
				if (message.getClientProxy() != null) {
					SSAPMessageResponse response = new SSAPMessageResponse(
							message.getNodeId(), message.getSpaceId(), 
							message.getTransactionId(), message.getTransactionType());
					SSAPMessageParameter param = new SSAPMessageStatusParameter(
							SSAPMessageStatusParameter.SIBStatus.STATUS_KP_MESSAGE_SYNTAX);
					response.addParameter(param);
					message.getClientProxy().sendMessage(response);
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			logger.error("Error in execution");
			throw new SIBException(mex.getMessage(), mex.getCause());					
		} catch (NullPointerException npe) {
			logger.error("Null pointer exception: " + npe.getMessage());
			throw new NullPointerException(npe.getMessage());
		} catch (Exception ex) {
			logger.error("Error in execution");
			throw new SIBException(ex.getMessage());
		}
	}

	/**
	 * Disconnects a given KP from the active sessions due to a 
	 * connection error or unexpected error in communication with the KP
	 * @param kpID the KP identifier
	 * @param errorDesc the 
	 */
	public void disconnect(String kpID, String errorDesc) throws SIBException {
		if(!this.isRunning()) {
			throw new SIBException("SIB is stopped");
		}

		logger.debug("Disconnecting " + kpID + " due to " + errorDesc);
		if (this.getSessionManager().removeNode(kpID) != null) {
			this.getViewerListenerManager().removeSession(new Session(kpID));

			// Removes the model
			this.getSemanticModel().remove(kpID);
			//OntModel removedModel = this.getSemanticModel().remove(kpID);
			//if (removedModel != null) {
			//	this.getViewerListenerManager().removeModel(removedModel);
			//}
			
			Collection<Subscription> subscriptions = this.getSubscriptionManager().getAllSubscriptions(this);
			
			if (subscriptions.size() > 0) {
				Collection<Long> subscriptionIds = new ArrayList<Long>();

				for (Subscription subscription : subscriptions) {
					subscriptionIds.add(subscription.getSubscriptionId());
				}	
				this.getViewerListenerManager().removeSubscription(subscriptionIds, kpID);
			}
		}
	}	

	/**
	 * Validates a SSAPMessageRequest for the given transaction type 
	 * @param message a SSAPMessageRequest
	 */
	private void validateSSAPMessageContent(SSAPMessageRequest message) 
			throws MalformedSSAPMessageException {
		try {
			if (message.getNodeId() == null) { // the node id is null
				throw new NullPointerException();
			}
			
			if (message.getSpaceId() == null) { // the smart space id is null
				throw new NullPointerException();
			}
			
			if (message.getTransactionType() == null) { // the transaction type is null
				throw new NullPointerException();
			}
			
			if (message.getMessageType() == null) { // the message type is null
				throw new NullPointerException();
			}
	
			// Validation over the transaction type
			switch(message.getTransactionType()) {
				case JOIN:
					if (message.getParameter(NameAttribute.CREDENTIALS) == null) {
						// TODO [FJR]: Define the credential parameter 
					}
					break;
				case LEAVE:
					if (message.getParameterSize() > 0) {
						throw new MalformedSSAPMessageException("SSAPMessage LEAVE Request cannot have associated parameters");
					}
					break;
				case INSERT:
					if (message.getParameterSize() == 0) {
						throw new MalformedSSAPMessageException("SSAPMessage INSERT Request has no parameters");
					} else if (message.getInsertGraphParameter() == null) {
						throw new MalformedSSAPMessageException("SSAPMessage INSERT Request does not contain INSERT-GRAPH parameter");
					} else if (message.getInsertGraphParameter().getContent() == null) {
						throw new MalformedSSAPMessageException("SSAPMessage INSERT Request INSERT-GRAPH parameter content is null");
					} 
	
//					// INSERT-GRAPH parameter
//					if (message.getInsertGraphParameter() instanceof SSAPMessageRDFParameter) {
//						SSAPMessageRDFParameter rdfParameter = (SSAPMessageRDFParameter) message.getInsertGraphParameter();
//						TypeAttribute ta = rdfParameter.getType();
//						if (!rdfParameter.getType().equals(TypeAttribute.RDFXML) &&
//								!rdfParameter.getType().equals(TypeAttribute.RDFM3)) {
//							throw new MalformedSSAPMessageException("SSAPMessage INSERT Request INSERT-GRAPH parameter TYPE attribute is invalid");
//						}
//					} else {
//						throw new MalformedSSAPMessageException("SSAPMessage INSERT Request INSERT-GRAPH parameter has not a type attribute");
//					}
					
					// CONFIRM parameter
					if (message.getParameterSize(NameAttribute.CONFIRM) == 0) {
						// do nothing
					} else if (message.getParameterSize(NameAttribute.CONFIRM) == 1) {
						if (message.getParameter(NameAttribute.CONFIRM).getContent() == null) {
							throw new MalformedSSAPMessageException("SSAPMessage INSERT Request CONFIRM parameter has not content");
						}
					} else {
						throw new MalformedSSAPMessageException("SSAPMessage INSERT Request CONFIRM parameter has more than one value");
					}
					break;
				case REMOVE:
					if (message.getParameterSize() == 0) {
						throw new MalformedSSAPMessageException("SSAPMessage REMOVE Request has no parameters");
					} else if (message.getRemoveGraphParameter() == null) {
						throw new MalformedSSAPMessageException("SSAPMessage REMOVE Request does not contain REMOVE-GRAPH parameter");
					} else if (message.getRemoveGraphParameter().getContent() == null) {
						throw new MalformedSSAPMessageException("SSAPMessage REMOVE Request REMOVE-GRAPH parameter content is null");
					} 
	
//					// REMOVE-GRAPH parameter
//					if (message.getRemoveGraphParameter() instanceof SSAPMessageRDFParameter) {
//						SSAPMessageRDFParameter rdfParameter = (SSAPMessageRDFParameter) message.getRemoveGraphParameter();
//						if (!rdfParameter.getType().equals(TypeAttribute.RDFXML) &&
//								!rdfParameter.getType().equals(TypeAttribute.RDFM3)) {
//							throw new MalformedSSAPMessageException("SSAPMessage REMOVE Request REMOVE-GRAPH parameter TYPE attribute is invalid");
//						}
//					} else {
//						throw new MalformedSSAPMessageException("SSAPMessage REMOVE Request REMOVE-GRAPH parameter has not a type attribute");
//					}
					
					// CONFIRM attribute
					if (message.getParameterSize(NameAttribute.CONFIRM) == 0) {
						// do nothing
					} else if (message.getParameterSize(NameAttribute.CONFIRM) == 1) {
						if (message.getParameter(NameAttribute.CONFIRM).getContent() == null) {
							throw new MalformedSSAPMessageException("SSAPMessage REMOVE Request CONFIRM parameter has not content");
						}
					} else {
						throw new MalformedSSAPMessageException("SSAPMessage REMOVE Request CONFIRM parameter has more than one value");
					}
					break;
				case UPDATE:
					if (message.getParameterSize() == 0) {
						throw new MalformedSSAPMessageException("SSAPMessage UPDATE Request has no parameters");
					} else if (message.getInsertGraphParameter() == null && 
							message.getRemoveGraphParameter() == null ) {
						throw new MalformedSSAPMessageException("SSAPMessage UPDATE Request does not contain INSERT-GRAPH or REMOVE-GRAPH parameters");
					} else if (message.getInsertGraphParameter() != null && 
							message.getInsertGraphParameter().getContent() == null) {
						throw new MalformedSSAPMessageException("SSAPMessage UPDATE Request INSERT-GRAPH parameter content is null");
					} else if (message.getRemoveGraphParameter() == null) {
						throw new MalformedSSAPMessageException("SSAPMessage REMOVE Request does not contain REMOVE-GRAPH parameter");
					} else if (message.getRemoveGraphParameter().getContent() == null) {
						throw new MalformedSSAPMessageException("SSAPMessage REMOVE Request REMOVE-GRAPH parameter content is null");
					} 
	
//					// INSERT-GRAPH parameter
//					if (message.getInsertGraphParameter() != null) {
//						if (message.getInsertGraphParameter().getContent() == null) {
//							throw new MalformedSSAPMessageException("SSAPMessage UPDATE Request INSERT-GRAPH parameter content is null");
//						} else if (message.getInsertGraphParameter() instanceof SSAPMessageRDFParameter) {
//							SSAPMessageRDFParameter rdfParameter = (SSAPMessageRDFParameter) message.getInsertGraphParameter();
//							if (!rdfParameter.getType().equals(TypeAttribute.RDFXML) &&
//									!rdfParameter.getType().equals(TypeAttribute.RDFM3)) {
//								throw new MalformedSSAPMessageException("SSAPMessage UPDATE Request INSERT-GRAPH parameter TYPE attribute is invalid");
//							}
//						} else {
//							throw new MalformedSSAPMessageException("SSAPMessage UPDATE Request INSERT-GRAPH parameter has not a type attribute");
//						}
//					}
//					
//					// REMOVE-GRAPH parameter
//					if (message.getRemoveGraphParameter() != null) {
//						if (message.getRemoveGraphParameter().getContent() == null) {
//							throw new MalformedSSAPMessageException("SSAPMessage UPDATE Request REMOVE-GRAPH parameter content is null");
//						} else if (message.getRemoveGraphParameter() instanceof SSAPMessageRDFParameter) {
//							SSAPMessageRDFParameter rdfParameter = (SSAPMessageRDFParameter) message.getRemoveGraphParameter();
//							if (!rdfParameter.getType().equals(TypeAttribute.RDFXML) &&
//									!rdfParameter.getType().equals(TypeAttribute.RDFM3)) {
//								throw new MalformedSSAPMessageException("SSAPMessage UPDATE Request REMOVE-GRAPH parameter TYPE attribute is invalid");
//							}
//						} else {
//							throw new MalformedSSAPMessageException("SSAPMessage UPDATE Request REMOVE-GRAPH parameter has not a type attribute");
//						}
//					}
					
					// CONFIRM attribute
					if (message.getParameterSize(NameAttribute.CONFIRM) == 0) {
						// do nothing
					} else if (message.getParameterSize(NameAttribute.CONFIRM) == 1) {
						if (message.getParameter(NameAttribute.CONFIRM).getContent() == null) {
							throw new MalformedSSAPMessageException("SSAPMessage INSERT Request CONFIRM parameter has not content");
						}
					} else {
						throw new MalformedSSAPMessageException("SSAPMessage UPDATE Request CONFIRM parameter has more than one value");
					}
					break;
				case SUBSCRIBE:
					if (message.getParameterSize() == 0) {
						throw new MalformedSSAPMessageException("SSAPMessage SUBSCRIBE Request has no parameters");
					} else if (message.getQueryParameter() == null) {
						throw new MalformedSSAPMessageException("SSAPMessage SUBSCRIBE Request does not contain QUERY parameter");
					} else if (message.getQueryParameter().getContent() == null) {
						throw new MalformedSSAPMessageException("SSAPMessage SUBSCRIBE Request QUERY parameter content is null");
					} 
	
//					if (message.getQueryParameter() instanceof SSAPMessageRDFParameter) {
//						SSAPMessageRDFParameter queryParam = (SSAPMessageRDFParameter) message.getQueryParameter();
//						if (queryParam.getType() == null) { // there should be something in param named type, encoding or format
//							if (message.getParameter(NameAttribute.FORMAT) == null && message.getParameter(NameAttribute.TYPE) == null 
//									&& message.getParameter(NameAttribute.ENCODING) == null) {
//								throw new MalformedSSAPMessageException("SSAPMessage SUBSCRIBE Request encoding format is null");
//							}
//						}
//					} else {
//						throw new MalformedSSAPMessageException("Incorrect message format in SSAPMessage SUBSCRIBE Request");
//					}
					
					break;
				case UNSUBSCRIBE:
					if (message.getParameterSize() == 0) {
						throw new MalformedSSAPMessageException("SSAPMessage UNSUBSCRIBE Request has no parameters");
					} else if (message.getSubscriptionIdParameter() == null) {
						throw new MalformedSSAPMessageException("SSAPMessage UNSUBSCRIBE Request does not contain SUBSCRIPTION-ID parameter");
					} else if (message.getSubscriptionIdParameter().getContent() == null) {
						throw new MalformedSSAPMessageException("SSAPMessage UNSUBSCRIBE Request SUBSCRIPTION-ID parameter content is null");
					} 
					break;
				case QUERY:
					if (message.getParameterSize() == 0) {
						throw new MalformedSSAPMessageException("SSAPMessage QUERY Request has no parameters");
					} else if (message.getQueryParameter() == null) {
						throw new MalformedSSAPMessageException("SSAPMessage QUERY Request does not contain QUERY parameter");
					} else if (message.getQueryParameter().getContent() == null) {
						throw new MalformedSSAPMessageException("SSAPMessage QUERY Request QUERY parameter content is null");
					} 
	
//					if (message.getQueryParameter() instanceof SSAPMessageRDFParameter) {
//						SSAPMessageRDFParameter queryParam = (SSAPMessageRDFParameter) message.getQueryParameter();
//						if (queryParam.getType() == null) { // there should be something in param named type, encoding or format
//							if (message.getParameter(NameAttribute.FORMAT) == null && message.getParameter(NameAttribute.TYPE) == null 
//									&& message.getParameter(NameAttribute.ENCODING) == null) {
//								throw new MalformedSSAPMessageException("SSAPMessage SUBSCRIBE Request encoding format is null");
//							}
//						}
//					} else {
//						throw new MalformedSSAPMessageException("Incorrect message format in SSAPMessage SUBSCRIBE Request");
//					}
				
					break;
				default:
					throw new MalformedSSAPMessageException("SSAPMessage Request is not a valid request");
			}
		} catch (NullPointerException npe) {
			npe.printStackTrace();
			throw new MalformedSSAPMessageException("SSAPMessage Request is not a valid request");
		} catch (MalformedSSAPMessageException ex) {
			ex.printStackTrace();
			throw new MalformedSSAPMessageException(ex.getMessage());
		}
	}

    /**
     * Gets the session manager
     * @return the session manager
     */
    public SessionManager getSessionManager() {
    	return sessionManager;
    }
    
    /**
     * Gets the subscription manager
     * @return the subscriptions manager
     */
    public SubscriptionManager getSubscriptionManager() {
    	return subscriptionManager;
    }
    
    /**
     * Gets the semantic model
     * @return the semantic model
     */
    public SemanticModel getSemanticModel() {
    	return semanticModel;
    }
    
    public ExecutorService getRequestedCommandExecutor() {
    	return requestedCommandExecutor;
    }

	/**
	 * Adds a viewer to the list of listeners
	 * @param viewer a Viewer
	 * @return <code>true</code> if the listener is correctly added
	 */
    public boolean addViewerListener(Viewer viewer) {
    	return viewerListeners.add(viewer);
	}

    /**
	 * Removes a viewer from the list of listeners
	 * @param viewer a Viewer
	 * @return <code>true</code> if the listener is correctly removed
	 */
    public boolean removeViewerListener(Viewer viewer) {
    	return viewerListeners.remove(viewer);
	}

	public ViewerListenerManager getViewerListenerManager() {
		return viewerListenerManager;
	}
	
	/**
	 * Resets the content of the SIB.
	 */
	public void reset() {
		this.sessionManager = null;
    	this.subscriptionManager = null;
    	this.semanticModel = null;
    	if(this.requestedCommandExecutor != null) {
    		this.requestedCommandExecutor.shutdown();
    		this.requestedCommandExecutor = null;
    	}
    	
    	this.getViewerListenerManager().resetSessions();
    	this.getViewerListenerManager().resetModel();
    	this.getViewerListenerManager().resetSubscriptions();
	}
	
	/**
	 * Starts a SIB instance, publishing the available services
	 * @param sibId the SIB identifier
	 * @throws SIBException when the SIB cannot start
	 */
	@Override
	public void start() throws SIBException {
		if(this.isRunning()) {
			return;
		}
		try{
			this.init();
			//this.initViewers();
			this.status = SIBStatus.RUNNING;
			logger.debug("SIB server started");
			this.support.fireSIBRunning(this);
		} catch(Exception e) {
			logger.error("Cannot start the SIB identified by " + this.getName() +": " + e.getMessage(), e.getCause());
			e.printStackTrace();
			throw new SIBException(e.getMessage(), e.getCause());
		}
	}

	/**
	 * Stops a SIB instance, unpublishing the available services
	 * @param sibId the SIB identifier
	 * @throws SIBException when the SIB cannot start
	 */	
	@Override
	public void stop() throws SIBException {
		if(!this.isRunning()) {
			return;
		}
		try {
			this.reset();
			this.status = SIBStatus.STOPPED;
			logger.debug("SIB server stopped");
			this.support.fireSIBStopped(this);
		} catch(Exception e) {
			logger.error("Cannot stop the SIB identified by " + this.getName() +": " + e.getMessage(), e.getCause());
			e.printStackTrace();
			throw new SIBException(e.getMessage(), e.getCause());
		}
	}

	@Override
	public boolean isRunning() {
		return this.status.isRunning();
	}
	
	@Override
	public boolean isStopped() {
		return this.status.isStopped();
	}
	
}
