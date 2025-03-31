package com.suayan.core.services;

import java.util.List;

import com.suayan.core.sling.SlingQueryStatement;

/**
 *
 * A set of reusable helper methods for populating Parent beans.
 * The idea is to use this service in a more declarative manner rather than
 * having each bean implement retrieval and instantiation.
 *
 * We define common patterns for retrieving Child entities based on
 * properties defined within the Parent entity.
 *
 * @author webwork@suayan.com
 *
 */
public interface EntityBuilderService {
    <T> T toItem(String path, Class<T> type);
    <T> List<T> toList(List<String> paths, Class<T> type);
    <T> List<T> query(SlingQueryStatement query, Class<T> type);
    List<String> queryForPaths(SlingQueryStatement query) throws Exception;
    List<String> queryForPathsUsingSelector(SlingQueryStatement sqs, String selector) throws Exception;
    <T> List<T> queryByProperty(String rootPath, String propertyName, String value, Class<T> type);
    <T> List<T> queryByTag(String rootPath, String tagName, Class<T> type);
    boolean pathHasProperty(String path, String property);
    String getResourceProperty(String path, String property);
    String[] getResourceMultiProperty(String path, String property); 
    List<String> getPropertyFromComponents(String path, List<String> componentResourceTypes, String propertyName);    
}