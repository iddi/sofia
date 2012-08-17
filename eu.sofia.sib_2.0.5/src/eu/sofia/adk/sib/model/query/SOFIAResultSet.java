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

package eu.sofia.adk.sib.model.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.engine.binding.Binding;

/**
 * This class is responsible for containing the result set obtained
 * after executing a query.
 * 
 * @author Fran Ruiz, fran.ruiz@tecnalia.com, ESI
 * @author Raul Otaolea, raul.otaolea@tecnalia.com, ESI
 *
 */
public class SOFIAResultSet implements ResultSet {

	/** The collection of solutions */
	private Collection<QuerySolution> solutions = new ArrayList<QuerySolution>();
	
	/** The collection of solutions */
	private Iterator<QuerySolution> iterator = null;
	
	private int rowNumber = 0;
	
	private Model model;
	
	private List<String> varNames = new ArrayList<String>();
	
	/**
	 * The constructor
	 * @param results the list of QuerySolutions
	 */
	public SOFIAResultSet(ResultSet results) {
		try {
			if (results != null) {
				model = results.getResourceModel();
			
				while (results.hasNext()) {
					QuerySolution sol = results.next();
					solutions.add(sol);
				}
			
				iterator = solutions.iterator();
			
				varNames = results.getResultVars();
			} else {
				model = null;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	/**
	 * Adds a solution to the model
	 * @param solution a QuerySolution containing a single result
	 * @return <code>true</code> if it is successfully added
	 */
	public boolean add(QuerySolution solution) {
		boolean added = solutions.add(solution);
		rowNumber = 0;
		iterator = solutions.iterator();
		return added;
	}
	
	/**
	 * Removes a solution from the model
	 * @param solution a QuerySolution containing a single result
	 * @return <code>true</code> if it is successfully added
	 */
	public boolean remove(QuerySolution solution) {
		boolean removed = solutions.remove(solution);
		rowNumber = 0;
		iterator = solutions.iterator();
		return removed;
	}
	
	/**
	 * Move back to the start of the iterator for this instance of results of a query.
	 */
	public void reset() {
		iterator = solutions.iterator();
		rowNumber = 0;
	}
	
	/**
	 * Return the number of solutions
	 */
	public int size() {
		return solutions.size();
	}
	
	/**
	 * Gets the query solutions
	 * @return the set of solutions
	 */
	public Collection<QuerySolution> getQuerySolutions() {
		return solutions;
	}
	
	/**
	 * Gets the query solutions
	 * @return the set of solutions
	 */
	private Collection<String> getVarNames() {
		return varNames;
	}
	
	/**
	 * Sets the var names
	 * @param varNames the var names
	 */
	private void setVarNames(Collection<String> varNames) {
		this.varNames.addAll(varNames);
	}
	
	/**
	 * Gets the model associated to this result set
	 * @return the JENA model
	 * @see Model
	 */
	@Override
	public Model getResourceModel() {
		return model;
	}

	/**
	 * Gets the result var names
	 * @return a list of string with the var names
	 */
	@Override
	public List<String> getResultVars() {
		return varNames;
	}

	/**
	 * Gets the row number
	 * @return the row number
	 */
	@Override
	public int getRowNumber() {
		return rowNumber;
	}

	/**
	 * Determines if there are more solutions
	 * @return <code>true</code> if there are more solutions
	 */
	@Override
	public boolean hasNext() {
		return iterator.hasNext();
	}

	/**
	 * Gets the next query solution
	 * @return the QuerySolution
	 * @see SOFIAResultSet#nextSolution()
	 */
	@Override
	public QuerySolution next() {
		rowNumber++;
		return iterator.next();
	}

	/**
	 * Gets the next binding
	 */
	@Override
	public Binding nextBinding() {
		return null;
	}

	/**
	 * Gets the next solution
	 * @return a QuerySolution containin the next result
	 */
	@Override
	public QuerySolution nextSolution() {
		return iterator.next();
	}

	/**
	 * Removes the iterator
	 */
	@Override
	public void remove() {
		iterator = null;
	}

	/**
	 * Gets the obsolete result sets
	 * @param result the result obtained after an indication
	 * @return a SOFIAResultSet with the obsolete results
	 */
	public SOFIAResultSet getObsoleteResults(SOFIAResultSet result) {
		// Creates a new void SOFIAResultSet
		SOFIAResultSet returnResult = new SOFIAResultSet(null);
		
		// Adds the var names
		returnResult.setVarNames(this.getVarNames());
		
		for (QuerySolution solution : this.getQuerySolutions()) {
			
			if (!result.contains(solution)) {
				returnResult.add(solution);
			}
		}

		return returnResult;
	}

	/**
	 * Gets the new result sets
	 * @param result the result obtained after an indication
	 * @return a SOFIAResultSet with the obsolete results
	 */
	public SOFIAResultSet getNewResults(SOFIAResultSet result) {
		// Creates a new void SOFIAResultSet
		SOFIAResultSet returnResult = new SOFIAResultSet(null);
		
		// Adds the var names
		returnResult.setVarNames(result.getVarNames());
		
		for (QuerySolution solution : result.getQuerySolutions()) {

			if (!this.contains(solution)) {
				returnResult.add(solution);
			}
		}
		
		return returnResult;
	}

	/**
	 * Determines if the current solution contains a determinate solution
	 * @param solution the QuerySolution to obtain
	 * @return <code>true</code> if the solution set contains the given solution
	 */
	private boolean contains(QuerySolution solution) {
		
		// For each solution
		for (QuerySolution currentSolution : this.getQuerySolutions()) {

			boolean equals = true;
			
			// Compare one-to-one the var obtained RDFNodes 
			for (String var : varNames) {
				RDFNode node1 = solution.get(var);
				RDFNode node2 = currentSolution.get(var);
				
				if (node1 != null) {
					if (!node1.equals(node2)) {
						equals = false;
					}
				} else {
					if (node1 == null && node2 != null) {
						equals = false;
					}
				}
			}
			
			// after all, if we found equality, return true value
			if (equals) {
				return true;
			}
		}
			
		return false;
	}
	
	/**
	 * Returns the String representation of the result set
	 * @return a String with the representation of the result set
	 */
	@Override
	public String toString() {
		
		StringBuffer sb = new StringBuffer();
		
		// For each solution
		for (QuerySolution currentSolution : solutions) {
			sb.append(currentSolution.toString());
		}			
		return sb.toString();
	}	
}
