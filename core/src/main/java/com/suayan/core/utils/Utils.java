package com.suayan.core.utils;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.api.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class Utils {

	private static final Logger log = LoggerFactory.getLogger(Utils.class);
	public static final int NODE_LIMIT = 20;
    
    private Utils() {        
    }
    
    public static <T> boolean contains( final T[] array, final T v ) {
        for ( final T e : array )
            if ( e == v || v != null && v.equals( e ) )
                return true;
        return false;
    }
    
    /**
     * Check if a value v startsWith() an element in an array
     * @param array
     * @param v
     * @return
     */
    public static boolean startsWith( final String[] array, final String v ) {
        for ( final String e : array )
            if ( v != null && v.startsWith(e) )
                return true;
        return false;
    }

    /**
     * Simple string join. 
     * @param values
     * @param joinStr
     * @return
     */
    public static String join(List<String>values, String joinStr ) {
        StringBuilder sb = new StringBuilder();
        if (values == null||values.size()==0) {
            return "";
        }
        for (int i=0,n=values.size(); i<n; i++) {
            String value = values.get(i);
            if (value!=null) {
                sb.append(value);
                if (joinStr!=null && i<n-1)
                    sb.append(joinStr);
            }
        }
        return sb.toString();
    }
    
    /**
     * Extracted from StringFormatFunctions.
     * @param phone
     * @return
     */
    public static String formatPhone(String phone) {
        String formattedPhone = Utils.normalizeToLocalNumber(phone);
        if (formattedPhone.length() == 10) {
            formattedPhone = String.format("%s-%s-%s",
                formattedPhone.substring(0, 3),
                formattedPhone.substring(3, 6),
                formattedPhone.substring(6, 10));
            return formattedPhone;
        } else if (formattedPhone.length() == 11) {
            formattedPhone = String.format("%s-%s-%s-%s",
                    formattedPhone.substring(0, 1),
                    formattedPhone.substring(1, 4),
                    formattedPhone.substring(4, 7),
                    formattedPhone.substring(7, 11));
            return formattedPhone;
        } else {
            return phone;
        }        
    }

    /**
     * Extracted from StringFormatFunctions.
     * @param phone
     * @return
     */
    public static String formatPhoneWithParens(String phone) {
        String formattedPhone = Utils.normalizeToLocalNumber(phone);
        if (formattedPhone.length() == 10) {
            formattedPhone = String.format("(%s) %s-%s",
                    formattedPhone.substring(0, 3),
                    formattedPhone.substring(3, 6),
                    formattedPhone.substring(6, 10));
            return formattedPhone;
        } else if (formattedPhone.length() == 11) {
            formattedPhone = String.format("+%s (%s) %s-%s",
                    formattedPhone.substring(0, 1),
                    formattedPhone.substring(1, 4),
                    formattedPhone.substring(4, 7),
                    formattedPhone.substring(7, 11));
            return formattedPhone;
        } else {
            return phone;
        }
    }
    
    /**
     * Normalize a phone number by stripping out special characters 
     * and removing "1" if it's longer than 10-digits.
     */
    public static String normalizeToLocalNumber (String phone) {
        if (phone==null) return null;
        String usNumber = phone.replaceAll("[^a-zA-Z0-9]+","");
        if (usNumber.length()==10)
            return usNumber;
        else if (usNumber.length()==11 && usNumber.startsWith("1"))
            return usNumber;
        else
            return phone;
    }
    
    /**
     * Fix improperly spaced semicolons.
     * @param text
     * @return
     */
    public static String fixSemicolons(String text) {
    	return text.replaceAll("(\\w+):(\\w+)", "$1: $2");
    }
    
    /**
     * return truncated string with ellipsis.
     * @param text
     * @param limit
     * @return
     */
    public static String ellipsis(String text, int limit) {
        if (text==null) {
            return null;
        }    
        String trimmed = text.trim();
        String truncated = trimmed.substring(0, Math.min(trimmed.length(), limit));
        if (trimmed.length()>limit) {
            return truncated + "...";
        }
        return truncated;
    }
    
    /**
     * Remap a dictionary to a JSONObject.
     * @param dictionary
     * @return
     */
    public static JSONObject toJSONObject(Dictionary<String, String> dictionary) {        
        if (dictionary==null) {
            return null;
        }        
        JSONObject jsonObj = new JSONObject();
        for (Enumeration<String> e = dictionary.keys(); e.hasMoreElements();) {
            String key = e.nextElement().toString(); 
             try {
                jsonObj.put(key, dictionary.get(key));
            } catch (JSONException jse) {
                log.error(jse.getMessage());
            }
         }
        return jsonObj;
    }
    
    /**
     * Put the value into the JSONObject if it's not null.
     * @param jso
     * @param key
     * @param value
     * @return
     */
    public static JSONObject checkedPut(JSONObject jso, String key, String value) {
        if (jso == null) 
            return jso;
        if (value!=null) {
            try {
                jso.put(key, value);
            } catch (JSONException e) {
            }
        }
        return jso;
    }
    
    /**
     * Attach a list to a jsonObject.
     * @param jso
     * @param key
     * @param list
     * @return
     */
    public static JSONObject checkedPutArray(JSONObject jso, String key, List<String> list) {
        if (jso==null || list==null)
            return jso;
        if (list.size()>0) {
            JSONArray jlist = new JSONArray();
            for (String value : list) {
                jlist.put(value);
            }
            try {
                jso.put(key, jlist);
            } catch (JSONException e) {
            }
        }
        return jso;
    }
    
    /**
     * Create a JSONArray from relPath with excluded attributes.
     * @param node
     * @param relPath
     * @param excludes
     * @return
     */
    public static JSONArray getArrayFromNodes(Node node, String relPath, String[] excludes){
    	JSONArray jsa = null;
    	NodeIterator ni;
		try {
			ni = node.getNode(relPath).getNodes();
	    	if (ni!= null && ni.getSize()>0) {
	    		int count = 0;
	    		jsa = new JSONArray();
	    		while(ni.hasNext() && count<NODE_LIMIT ){
	    			Node child = ni.nextNode();
	    			JSONObject obj = new JSONObject();
	    			obj = attachProperties(obj, child, excludes);
	    			jsa.put(obj);
	    		}
	    	}
		} catch (RepositoryException e) {
		}
    	return jsa;
    }

    /**
     * Given a set of keys, concatenate the properties of the JSONObject obj with joinStr.
     * @param obj
     * @param keys
     * @param joinStr
     * @return
     */
    public static String concatProperties(JSONObject obj, String[] keys, String joinStr ) {
    	StringBuilder sb = new StringBuilder();
    	if (obj == null) {
    		return "";
    	}
    	for (int i=0,n=keys.length; i<n; i++) {
    		try {
        		String value = obj.getString(keys[i]);
        		if (value!=null) {
        			sb.append(value);
            		if (joinStr!=null)
            			sb.append(joinStr);
        		}
    		} catch (JSONException e) {
    		}
    	}
    	return sb.toString();
    }

    /**
     * Attach JSON attributes from node's relative path
     * @param obj
     * @param node
     * @param relPath
     * @return
     */
    public static JSONObject attachSubProperties(JSONObject obj, Node node, String relPath, String[] excludes ) {
    	try {
        	Node childNode = node.getNode(relPath);
    		obj = attachProperties(obj, childNode, excludes);
    	} catch (RepositoryException e) {
		}
    	return obj;
    }
    
    /**
     * JSONObject decorator that attaches node properties to a json object.
     * @param obj
     * @param node
     * @param excludes
     * @return
     */
    public static JSONObject attachProperties(JSONObject obj, Node node, String[] excludes) {
    	try {
        	PropertyIterator pi = node.getProperties();
        	if (pi!=null && pi.getSize()>0) {
        		while (pi.hasNext()){
        			javax.jcr.Property p = pi.nextProperty();
        			String propName = p.getName();
        			if (!Utils.startsWith(excludes, propName)) {
               			if (p.isMultiple()) {
            				obj.put(propName, getMultiPropertyArray(p));
            			} else {
            				obj.put(propName, p.getValue().getString());
            			}
        			}
        		}
        	}    		
		} catch (RepositoryException e) {
		} catch (JSONException e) {
		} 
    	return obj;
    }
    
    /**
     * Retrieve multi-prop values as JSONArray.
     * @param resourceResolver
     * @param path
     * @param subpath
     * @param propertyName
     * @return
     */
    public static JSONArray getMultiPropertyJsonArray(ResourceResolver resourceResolver, 
            String path, String subpath, String propertyName) {
        
        JSONArray jsonArray = null;        
        Node node = Utils.getNode(resourceResolver, path, subpath);        
        if (node == null) {
            log.info(">>> node not found ... ["+path+"]["+subpath+"]["+propertyName+"]");
            return null;
        }
        try {
            Property prop = node.getProperty(propertyName);
            jsonArray = new JSONArray();            
            if (prop!=null) {
                if (prop.isMultiple()) {
                    Value[] valueList = prop.getValues();                    
                    for (int i=0, n=valueList.length; i<n; i++) {
                        JSONObject jsonObj = new JSONObject(valueList[i].getString());
                        jsonArray.put(i, jsonObj);
                    }                   
                } else {
                    JSONObject jsonObj = new JSONObject(prop.getString());
                    jsonArray.put(0, jsonObj);
                }
            } 
        } catch (JSONException e) {
        } catch (ValueFormatException e) {
        } catch (IllegalStateException e) {
        } catch (PathNotFoundException e) {
        } catch (RepositoryException e) {
        }
        return jsonArray;
    }
    
    /**
     * Return a json array from a multi-value property
     * @param p
     * @return
     */
    public static JSONArray getMultiPropertyArray(javax.jcr.Property p) {
    	JSONArray jsa = null;
    	Value[] values;
		try {
            jsa = new JSONArray();
            if(p.isMultiple()){
                values = p.getValues();
                for (int i=0,n=values.length; i<n; i++){
                    jsa.put(values[i].getString());
                }
            } else {
               jsa.put(p.getString());
            }
		} catch (ValueFormatException e) {
		} catch (RepositoryException e) {
		}
    	return jsa;
    }
    
    /**
     * Retrieve property prop from Node selected via path.
     * @param path
     * @param prop
     * @param session
     * @return
     */
    public static String getProperty(String path, String prop, Session session) {
    	String result = null;
        try {
            Node node = session.getNode(path);
            if (node != null) {
                Property property = node.getProperty(prop);
                if (property != null) {
                    return property.getValue().getString();
                }
            }
        } catch (RepositoryException e) {
        }
        return result;
    }
    
    /**
     * Retrieve property from node with given subpath (key).
     * @param node
     * @param subpath
     * @return
     */
    public static String getPropertyString(Node node, String subpath) {
    	if (node == null)
    		return null;
    	
    	String value = null;    	
    	try {
			value = node.getProperty(subpath).getValue().getString();
		} catch (ValueFormatException e) {
		} catch (IllegalStateException e) {
		} catch (PathNotFoundException e) {
		} catch (RepositoryException e) {
		}
    	return value;
    }

    /**
     * Retrieve multi-properties as ArrayList from node with given subpath (key).
     * @param node
     * @param subpath
     * @return
     */
    public static ArrayList<String> getPropertyStringArray(Node node, String subpath) {
    	if (node == null)
    		return null;
    	ArrayList<String> values = null;
    	try {
    		Property prop = node.getProperty(subpath);
    		if (prop!=null) {
    			if (prop.isMultiple()) {
    	    		Value[] valueList = prop.getValues();
        			values = new ArrayList<String>(valueList.length);
        			for (int i=0, n=valueList.length; i<n; i++) {
        				values.add(valueList[i].getString());
        			}
    			} else {
    				values = new ArrayList<String>();
    				values.add(prop.getString());
    			}
    		}    		
		} catch (ValueFormatException e) {
		} catch (IllegalStateException e) {
		} catch (PathNotFoundException e) {
		} catch (RepositoryException e) {
		}
    	return values;
    }
    
    public static Node getNode(String path, Session session) {
    	Node node = null;
        try {
            node = session.getNode(path);
            if (node!=null) {
            	return node;
            }
        } catch(RepositoryException re) {
        }
        return node;
    }
    
    public static String[] getAttributesFromJsonMulti(ResourceResolver resourceResolver, 
            String path, String subpath, String propName, String jsonAttribute) {
        
        if (subpath == null || propName == null || jsonAttribute == null)
            return new String[]{null};

        Node node = Utils.getNode(resourceResolver, path, subpath);
        if (node==null)
            return new String[]{null};

        ArrayList<String> propArray = Utils.getPropertyStringArray(node, propName);
        if (propArray == null) {
            return new String[]{null};
        }
        int numProps = propArray.size();
        String[] values = new String[numProps];
        for (int i = 0; i < numProps; i++) {
            String propStr = propArray.get(i);
            if (propStr != null && propStr.trim().length() > 0) {
                try {
                    JSONObject jsonObj = new JSONObject(propStr);
                    if (jsonObj != null) {
                        values[i] = jsonObj.getString(jsonAttribute);
                    }
                } catch (JSONException e) {
                    values[i] = null;
                }
            }
        }
        return values;
    }
    
    /**
     * Get Node relative to this resource's node.
     * @param subpath
     * @return
     */
    public static Node getNode(ResourceResolver resourceResolver, String path, String subpath) {
        String resourcePath = "";
        final String JCR_CONTENT = "/jcr:content";

        if (path.endsWith(JCR_CONTENT)) {
            resourcePath = path + "/" + subpath;
        } else if (path.endsWith(JCR_CONTENT + "/")) {
            resourcePath = path + subpath;
        } else {
            resourcePath = path + JCR_CONTENT + "/" + subpath;
        }

        return Utils.getNode(resourceResolver, resourcePath);
    }
    
    /**
     * Overloaded.
     * @param resourceResolver
     * @param resourcePath
     * @return
     */
    public static Node getNode(ResourceResolver resourceResolver, String resourcePath) {
        Resource resource = resourceResolver.getResource(resourcePath);
        if (resource != null)
            return resource.adaptTo(Node.class);
        else
            return null;
    }
    
    
    /**
     * Create an ArrayList of Text list given a subpath and a selected property name.
     * @param subpath
     * @param propNames
     * @return
     */
    public static ArrayList<String> getCompositeTextList(ResourceResolver resourceResolver, String path, String subpath, String[] propNames) {
        
        if (propNames == null || propNames.length == 0)
            return null;
        
        ArrayList<String> textList = new ArrayList<String>();
        try {
            Node node = Utils.getNode(resourceResolver, path, subpath);
            if (node != null) {
                NodeIterator ni = node.getNodes();
                
                while (ni.hasNext()) {
                    Node subnode = (Node) ni.next();
                    StringBuffer sb = new StringBuffer();
                    
                    // concatenate selected properties values.
                    for (int i=0,n=propNames.length; i<n; i++) {                        
                        String value = subnode.getProperty(propNames[i]).getValue().getString();
                        if (value!=null && value.trim().length()>0) {
                            sb.append(value);
                            if (i+1<n)
                                sb.append(", ");                            
                        }
                    }
                    textList.add(Utils.fixSemicolons(sb.toString()));
                }               
            }
        } catch (RepositoryException re) {
        }
        return textList;
    }

    /**
     * Get the pipe separated string from the string array
     *
     * @param resourceResolver resource resolver
     * @param path path of the node
     * @param subPath subPath from the node
     * @param propertyName propertyName on the subPath
     *
     * @return pipe separated string from the string array
     */
    public static String getPipeSeparatedStringArray(ResourceResolver resourceResolver, String path, String subPath, String propertyName) {
        String pipeSeparatedString = "";
        List<String> stringList = getStringArray(resourceResolver, path, subPath, propertyName);
        
        if(stringList != null && stringList.size() > 0){
            pipeSeparatedString = StringUtils.join(stringList, "|");
        }

        return pipeSeparatedString;
    }
    

    /**
     * Get the required string array as list of strings
     * 
     * @param resourceResolver resource resolver
     * @param path path of the node
     * @param subPath subPath from the node
     * @param propertyName propertyName on the subPath
     *                     
     * @return list of strings from the string array
     */
    public static List<String> getStringArray(ResourceResolver resourceResolver, String path, String subPath, String propertyName) {
        List<String> stringList = new ArrayList<String>();
        Node node = Utils.getNode(resourceResolver, path);
        if (node == null) {
            log.info(">>> node not found ... ["+path+"]["+propertyName+"]");
            return null;
        }
        try {
            Property prop = node.getProperty(subPath +"/"+propertyName);
            if (prop!=null) {
                if (prop.isMultiple()) {
                    Value[] valueList = prop.getValues();
                    for (int i=0, n=valueList.length; i<n; i++) {
                        stringList.add(valueList[i].getString());
                    }
                } else {
                    stringList.add(prop.getString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return stringList;
    }

    /**
     * Collects the given property from under the given subNodePath of parentNode
     * Example: collects the value of property from clinics
     *
     * @param parentNode parent node
     * @param subNodePath sub node path
     * @param propertyName property name
     *
     * @return list of property values from each of the child node under the subNodePath
     */
    public static ArrayList<String> getSamePropertyFromAllSubNodes(Node parentNode, String subNodePath, String propertyName) {
        ArrayList<String> propertyValues = new ArrayList<String>();

        try {
            if(parentNode.hasNode(subNodePath) && parentNode.getNode(subNodePath).hasNodes()){
                Node subNode = parentNode.getNode(subNodePath);
                NodeIterator nodeIterator = subNode.getNodes();
                
                while (nodeIterator.hasNext()){
                    Node childNode = nodeIterator.nextNode();
                    if(childNode.hasProperty(propertyName)){
                        propertyValues.add(childNode.getProperty(propertyName).getString());
                    }
                }
            }
        } catch (RepositoryException e) {
            log.error(e.getMessage());
        }

        return propertyValues;
    }

    /**
     * Retrieve jcr:title of the page in given path.
     *
     * @param path path of the page
     * @param session jcr session
     * @return title of the page in given path
     */
    public static String getTitle(String path, Session session) {
        String title = Utils.getProperty(path+"/jcr:content", "jcr:title", session);
        if (path==null || title == null)
            return "";
        return title;
    }

    /**
     * Pass in a current page and a list of template types you are looking for, this will look up the path tree and check
     * the template type and if it matches the one you are looking for it will return that parent path.
     * This is useful to check from subpages to get the path to what the parent page is.
     * @param currentPage
     * @param templateList
     * @return
     */
    public static String getPagePathMatchingTemplate(Page currentPage, List<String> templateList){
        int partsOfPath = currentPage.getPath().split("/").length - 2;
        for(int i=partsOfPath; i>2 ; i--){
            Page myPage = currentPage.getAbsoluteParent(i);
            if(myPage != null){
                ValueMap myValueMap = myPage.getProperties();
                String myTemplate = myValueMap.get("cq:template", "");

                for(String template: templateList){
                    if(myTemplate.toLowerCase().contains(template.toLowerCase())){
                        return myPage.getPath();
                    }
                }
            }
        }

        //no match
        return "";
    }
    
    /**
     * Build email body from http request.
     *
     * @param req
     * @return email body
     */
    public static String formatRequestParams(SlingHttpServletRequest req) {
        // implement simple field filtering.
        final String[] exemption = {":formid", ":formstart", "_charset_", "Submit", "g-recaptcha-response", ":cq_csrf_token", "mode"};
        StringBuffer sb = new StringBuffer();
        @SuppressWarnings("unchecked")
        Enumeration<String> params = (Enumeration<String>) req.getParameterNames();
        while (params.hasMoreElements()) {
            String paramName = params.nextElement();            
            if (!Utils.contains(exemption, paramName)) {
                boolean hasValue = false;                
                String fieldNameText = "<br/><b>"+paramName+":</b><br/>\n";
                StringBuffer fieldValueText = new StringBuffer();
                String[] paramValues = req.getParameterValues(paramName);
                for (int i=0; i<paramValues.length; i++) {
                    if (StringUtils.isNotBlank(paramValues[i]) && !paramValues[i].equalsIgnoreCase("Select Option")) {
                        hasValue = true;
                        fieldValueText.append("\t" + paramValues[i]).append("<br/>\n");
                    }
                }
                if (hasValue) {
                    sb.append(fieldNameText).append(fieldValueText);
                }
            }
        }
        return sb.toString();
    }
    
    /**
     * Parse comma delimited list of email addresses.
     *
     * @param emails
     * @return
     */
    public static String[] parseEmailList(String emails) {
        String[] list = {};
        if (StringUtils.isBlank(emails)) {
            return list;
        }
        list = emails.split(",");
        for (int i=0, n=list.length; i<n; i++) {
            list[i] = list[i].trim();
        }
        return list;
    }
    
    /**
     * Convert an array of strings to javax.util.List<String>.
     * 
     * @param props
     * @return
     */
    public static List<String> toList(String[] props) {
        if (props == null) {
            return null;
        }
        return new ArrayList<String>(Arrays.asList(props));
    }

    /**
     * Combine 2 JSONArrays and return it.
     * 
     * @param destination
     * @param additional
     * @return
     */
    public static JSONArray append(JSONArray destination, JSONArray additional) {        
        JSONArray newList = destination;
        if (newList.equals(null)) {
            newList = new JSONArray();
        }
        // short-circuit return newList if 2nd param is empty
        if (additional.equals(null)||additional.length()==0){
            return newList;
        }

        for (int i=0,n=additional.length(); i<n; i++) {
            try {
                newList.put(additional.get(i));
            } catch (JSONException e) {
            }
        }        
        return newList;
    }

	/**
	 *
	 * @param text text to split into list
	 * @param separator separator used to split the text
	 * @return list of String
	 */
    public static List<String> splitToList(String text, String separator) {
      String[] array = text.split(separator);
      return toList(array);
    }
  

    // Method to convert Node to ObjectNode with rejected keys
    public static ObjectNode convertNodeToJson(Node node, Set<String> rejectedKeys) throws RepositoryException {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode jsonNode = objectMapper.createObjectNode();
        // Add properties of the node to the JSON object, excluding rejected keys
        if (node.hasProperties()) {
              PropertyIterator properties = node.getProperties();
              while (properties.hasNext()) {
                  javax.jcr.Property property = properties.nextProperty();
                  String name = property.getName();
                  if (!rejectedKeys.contains(name)) { // Check if the property is not in rejected keys
                      addPropertyToJson(jsonNode, property);
                  }
              }
          }  
          return jsonNode;
      }
  
    // Overloaded method to convert Node to ObjectNode without rejected keys
    public static ObjectNode convertNodeToJson(Node node) throws RepositoryException {
        return convertNodeToJson(node, Collections.emptySet()); // Call the other method with an empty set
    }
  
    // Helper method to add property to ObjectNode
    private static void addPropertyToJson(ObjectNode jsonNode, javax.jcr.Property property) throws RepositoryException {
      String name = property.getName();
      switch (property.getType()) {
        case javax.jcr.PropertyType.STRING:
          jsonNode.put(name, property.getString());
          break;
        case javax.jcr.PropertyType.BOOLEAN:
          jsonNode.put(name, property.getBoolean());
          break;
        case javax.jcr.PropertyType.LONG:
          jsonNode.put(name, property.getLong());
          break;
        case javax.jcr.PropertyType.DOUBLE:
          jsonNode.put(name, property.getDouble());
          break;
        case javax.jcr.PropertyType.DATE:
          jsonNode.put(name, property.getDate().getTimeInMillis());
          break;
        // Handle other property types as needed
        default:
          break;
      }
    }
    
    /**
     * Convert ObjectNode to JSONObject.
     * @param objectNode
     * @return
     */
    public static JSONObject toJsonObject(ObjectNode objectNode) {
      try {
          return new JSONObject(objectNode.toString());
        } catch (JSONException e) {
          log.trace("Failed to convert ObjectNode to JSONObject", e);
          return new JSONObject(); // or return null based on your preference
        }
    }

}
