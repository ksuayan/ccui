package com.suayan.core.services;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;

/**
 * The ScannerService interface defines basic services for retrieving
 * either a list of JCRPaths or JSONObjects from a SQL or xpath Query.
 * 
 * A query may likewise be executed given a json config file that exists
 * under /apps/ccui/extractor. This file comprise of extract rules that define
 * mappings from the JCR Node (sourcepath) and Property (sourceproperty)
 * and the desired top level property name.
 * 
 * @author Kyo Suayan
 *
 */
public interface ScannerService {
	/**
	 * Just return the list of paths.
	 * @param query
	 * @return
	 */
	public abstract List<String> queryForJcrPaths(String query, String queryLanguage);
	/**
	 * Return a list of name/title pairs.
	 * @param query
	 * @param  queryLanguage
	 * @return
	 */
	public List<JSONObject> queryForPageTitles(String query, String queryLanguage);
	/**
	 * Pass in a JCR path of a JSON config file.
	 * @param configPath
	 * @return
	 */
	public List<JSONObject> queryByJsonConfig(String configPath, Map<String,String[]> params);
	
	/**
	 * Perform query based an actual JSON query object.
	 * @return
	 */
	public List<JSONObject> queryByJsonObject(JSONObject jsonObj, Map<String,String[]> params);
	/**
	 * Recursive.
	 * @param query
	 * @return
	 */
	public abstract List<JSONObject> queryForJsonObjects(String query, String queryLanguage);	
	/**
	 *
	 * @return
	 */
	public boolean isRecurseEnabled();
	/**
	 * 
	 * @param recurseEnabled
	 */
	public void setRecurseEnabled(boolean recurseEnabled);
}