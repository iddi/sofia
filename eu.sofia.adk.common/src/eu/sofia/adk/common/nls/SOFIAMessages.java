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

package eu.sofia.adk.common.nls;

import org.eclipse.osgi.util.NLS;

import eu.sofia.adk.common.properties.PropertyLoader;

/**
 * Gets the internationalization messages.
 * 
 * @author Fran Ruiz, Fran.Ruiz@tecnalia.com, Tecnalia
 */
public class SOFIAMessages extends NLS {

	/** The NLS Property key reference */
	private static final String NLS_KEY = "nlsreference";
	
	/** The Bundle Name */
	private static final String SOFIA_BUNDLE = PropertyLoader.getInstance().getProperty(NLS_KEY);

	/** Messages for the Wizard */
	public static String Wizard_windowTitle;
	
	/** Messages for the New Smart App Project Page */
	public static String NewModelLayerPage_projectLabel;
	public static String NewModelLayerPage_projectBrowseLabel;
	public static String NewModelLayerPage_packageLabel;
	public static String NewModelLayerPage_windowTitle;
	public static String NewModelLayerPage_pageTitle;
	public static String NewModelLayerPage_pageDescription;
	public static String NewModelLayerPage_configurationGroupTitle;
	public static String NewModelLayerPage_executionContextLabel;
	public static String NewModelLayerPage_executionContextLabel_PRODUCER;
	public static String NewModelLayerPage_executionContextLabel_PRODUCER_desc;
	public static String NewModelLayerPage_executionContextLabel_PROSUMER;
	public static String NewModelLayerPage_executionContextLabel_PROSUMER_desc;
	public static String NewModelLayerPage_executionContextLabel_CONSUMER;
	public static String NewModelLayerPage_executionContextLabel_CONSUMER_desc;
	
	/** Validation Messages for the New Smart App Project Page */
	public static String NewModelLayerPage_validationProjectNotSpecified;
	public static String NewModelLayerPage_validationPackageNotSpecified;
	public static String NewModelLayerPage_validationDefaultMessage;
	public static String NewModelLayerPage_validationSmartEnvironmentNotSelected;
	
	/** Error Messages for the New Smart App Project Page */
	public static String NewModelLayerPage_errorInvalidProjectIdentifier;
	public static String NewModelLayerPage_errorInvalidPackageIdentifier;
	public static String NewModelLayerPage_errorProjectNotExist;
	
	
	/** Messages for the Project Selection Dialog */
	public static String ProjectSelectionDialog_windowTitle;
	public static String ProjectSelectionDialog_windowDescription;

	/** Messages for the ADK Wizard */
	public static String ADKWizard_windowTitle;
	
	/** Messages for the New Smart App Project Page */
	public static String NewSmartAppProjectPage_projectLabel;
	public static String NewSmartAppProjectPage_packageLabel;
	public static String NewSmartAppProjectPage_windowTitle;
	public static String NewSmartAppProjectPage_pageTitle;
	public static String NewSmartAppProjectPage_pageDescription;
	public static String NewSmartAppProjectPage_configurationGroupTitle;
	public static String NewSmartAppProjectPage_executionContextLabel;
	public static String NewSmartAppProjectPage_executionContextLabel_PRODUCER;
	public static String NewSmartAppProjectPage_executionContextLabel_PRODUCER_desc;
	public static String NewSmartAppProjectPage_executionContextLabel_PROSUMER;
	public static String NewSmartAppProjectPage_executionContextLabel_PROSUMER_desc;
	public static String NewSmartAppProjectPage_executionContextLabel_CONSUMER;
	public static String NewSmartAppProjectPage_executionContextLabel_CONSUMER_desc;
	public static String NewSmartAppProjectPage_configuration_smart;
	public static String NewSmartAppProjectPage_configuration_direct;
	
	/** Validation Messages for the New Smart App Project Page */
	public static String NewSmartAppProjectPage_validationProjectNotSpecified;
	public static String NewSmartAppProjectPage_validationPackageNotSpecified;
	public static String NewSmartAppProjectPage_validationDefaultMessage;
	public static String NewSmartAppProjectPage_validationSmartEnvironmentNotSelected;
	
	/** Error Messages for the New Smart App Project Page */
	public static String NewSmartAppProjectPage_errorInvalidProjectIdentifier;
	public static String NewSmartAppProjectPage_errorInvalidPackageIdentifier;
	public static String NewSmartAppProjectPage_errorProjectAlreadyExists;
	
	
	/** Messages for the Project Selection Area */
	public static String ProjectSelectionArea_groupTitle;
	public static String ProjectSelectionArea_browseLabel;
	public static String ProjectSelectionArea_defaultTitle;
	public static String ProjectSelectionArea_locationLabel;
	public static String ProjectSelectionArea_directoryDialogFolder;

	/** Validation Messages for the Project Selection Area */
	public static String ProjectSelectionArea_validationLocationNotSpecified;
	
	/** Error Messages for the Projet Selection Area */
	public static String ProjectSelectionArea_errorInvalidProjectLocation;
	public static String ProjectSelectionArea_errorPathAlreadySelected;
	
	/** Messages for the Smart Environment Selection Area */
	public static String SmartEnvironmentSelectionArea_groupTitle;
	public static String SmartEnvironmentSelectionArea_treeLabel;
	public static String SmartEnvironmentSelectionArea_defaultSmartEnvironment;
	public static String SmartEnvironmentSelectionArea_userSmartEnvironment;
	
	/** Messages for the User defined Ontology Selection Area */
	public static String UserOntologySelectionArea_ontologyLabel;
	public static String UserOntologySelectionArea_browseLabel;
	public static String UserOntologySelectionArea_fileDialogOntology;
	
	/** Error Messages for the user-ontology selection area */
	public static String UserOntologySelectionArea_errorInvalidOntology;
	
	/** Messages for the Connector and Platform Page */
	public static String OntologyConceptSelectionPage_windowTitle;
	public static String OntologyConceptSelectionPage_producer_pageTitle;
	public static String OntologyConceptSelectionPage_producer_pageDescription;
	public static String OntologyConceptSelectionPage_consumer_pageTitle;
	public static String OntologyConceptSelectionPage_consumer_pageDescription;

	/** Validation Messages for the Connector and Platform Page */
	public static String OntologyConceptSelectionPage_validationDefaultMessage;
	public static String OntologyConceptSelectionPage_validationConsumerConceptsNotSelected;
	public static String OntologyConceptSelectionPage_validationProducerConceptsNotSelected;

	/** Messages for the Ontology Selection Area */
	public static String OntologySelectionArea_producerLabel;
	public static String OntologySelectionArea_consumerLabel;
	public static String OntologySelectionArea_selectAll;
	public static String OntologySelectionArea_deselectAll;
	public static String OntologySelectionArea_expand;	
	public static String OntologySelectionArea_collapse;	
	
	/** Messages for the Connector and Platform Page */
	public static String ConnectorPlatformPage_windowTitle;
	public static String ConnectorPlatformPage_pageTitle;
	public static String ConnectorPlatformPage_pageDescription;

	/** Validation Messages for the Connector and Platform Page */
	public static String ConnectorPlatformPage_validationDefaultMessage;
	public static String ConnectorPlatformPage_validationPlatformNotSelected;
	public static String ConnectorPlatformPage_validationConnectorNotSelected;

	/** Messages for the Platform Selection Area */
	public static String PlatformSelectionArea_label;
	
	/** Messages for the Connection Selection Area */
	public static String ConnectorSelectionArea_label;

	/** Messages for the Python details */
	public static String PythonDetails_groupTitle;
	public static String PythonDetails_pyTypeGroup;
	public static String PythonDetails_pyGrammarLabel;
	public static String PythonDetails_pyInterpreterLabel;
	
	/** Messages for the New Smart Modeller Project Page */
	public static String NewSmartModellerProjectPage_projectLabel;
	public static String NewSmartModellerProjectPage_packageLabel;
	public static String NewSmartModellerProjectPage_windowTitle;
	public static String NewSmartModellerProjectPage_pageTitle;
	public static String NewSmartModellerProjectPage_pageDescription;
	
	/** Validation Messages for the New Smart Modeller Project Page */
	public static String NewSmartModellerProjectPage_validationProjectNotSpecified;
	public static String NewSmartModellerProjectPage_validationPackageNotSpecified;
	public static String NewSmartModellerProjectPage_validationDefaultMessage;
	public static String NewSmartModellerProjectPage_validationSmartEnvironmentNotSelected;
	
	/** Error Messages for the New Smart Modeller Project Page */
	public static String NewSmartModellerProjectPage_errorInvalidProjectIdentifier;
	public static String NewSmartModellerProjectPage_errorInvalidPackageIdentifier;
	public static String NewSmartModellerProjectPage_errorProjectAlreadyExists;
	
	/** Messages for the Smart Environment Components Page */  
	public static String SmartEnvironmentComponentPage_windowTitle;
	public static String SmartEnvironmentComponentPage_pageTitle;
	public static String SmartEnvironmentComponentPage_pageDescription;
	public static String SmartEnvironmentComponentPage_runningSIBsGroupTitle; 
	public static String SmartEnvironmentComponentPage_newSIBsGroupTitle; 
	public static String SmartEnvironmentComponentPage_newKPsGroupTitle; 
	public static String SmartEnvironmentComponentPage_addNewModelElementButtonLabel;
	public static String SmartEnvironmentComponentPage_editModelElementButtonLabel;
	public static String SmartEnvironmentComponentPage_removeModelElementButtonLabel;
	public static String SmartEnvironmentComponentPage_viewModelElementButtonLabel;
	
	/** Validation Messages for the Smart Environment Component Page */
	public static String SmartEnvironmentComponentPage_validationDefaultMessage;
	public static String SmartEnvironmentComponentPage_idGroupTitle;
	public static String SmartEnvironmentComponentPage_elementPropertiesGroupTitle;
	public static String SmartEnvironmentComponentPage_idTextLabel;
	public static String SmartEnvironmentComponentPage_sibGroupTitle;
	public static String SmartEnvironmentComponentPage_sibIdLabel;
	public static String SmartEnvironmentComponentPage_sibNameLabel;
	public static String SmartEnvironmentComponentPage_sibIPLabel;
	public static String SmartEnvironmentComponentPage_sibPortLabel;
	public static String SmartEnvironmentComponentPage_kpGroupTitle;
	public static String SmartEnvironmentComponentPage_kpNameLabel;
	public static String SmartEnvironmentComponentPage_kpLangLabel;
	public static String SmartEnvironmentComponentPage_kpImplLabel;
	public static String SmartEnvironmentComponentPage_okLabel;
	public static String SmartEnvironmentComponentPage_cancelLabel;
	public static String SmartEnvironmentComponentPage_keyColumnModelElementLabel;
	public static String SmartEnvironmentComponentPage_valueColumnModelElementLabel;
	public static String SmartEnvironmentComponentPage_errorVoidIdentifier;
	public static String NewSmartModellerProjectPage_emfNameLabel;
	public static String NewSmartModellerProjectPage_gmfNameLabel;
	public static String NewSmartModellerProjectPage_modelGroupLabel;
	
	static {
		// load messages from the bundle file
		NLS.initializeMessages(SOFIA_BUNDLE, SOFIAMessages.class);
	}
}
