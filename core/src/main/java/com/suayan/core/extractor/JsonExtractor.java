package com.suayan.core.extractor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.suayan.core.utils.Utils;

/**
 * Extract a JSONObject from a Node with a list of ExtractRules.
 * 
 * A JsonExtractor object is initialized with a @javax.jcr.Session object.
 * You can then programmatically provide it with a List of ExtractRules
 * that will be used to process each JCR Node in getJsonObject().
 * 
 * @author Kyo Suayan
 *
 */
public class JsonExtractor {

    private List<ExtractRule> rules = null;
    private Session session = null;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public JsonExtractor(Session session) {
        super();
        this.session = session;
        this.rules = new ArrayList<ExtractRule>();
    }
    
    /**
     * Add an ExtractRule to this extractor's list of rules.
     * @param rule
     */
    public void addRule(ExtractRule rule) {
        this.rules.add(rule);
    }

    /**
     * Add a list of ExtractRules.
     * @param parsedRules
     */
    public void addRules(List<ExtractRule> parsedRules) {
        if (parsedRules!=null) {
            rules.addAll(parsedRules);
        }
    }
    
    /**
     * Apply all applicable rules to this node.
     * Pass in the destination JSONObject
     * and return the updated version.
     * 
     * @param node
     * @param jsonObject
     * @return
     */
    public JSONObject getJsonObject(Node node, JSONObject jsonObject) {
        if (node == null)
            return null;
        int size = this.rules.size();
        if (size > 0) {
            for (ExtractRule rule : this.rules) {
                jsonObject = this.processRule(node, rule, jsonObject);
            }
        }
        return jsonObject;
    }

    /**
     * Apply the ExtractRule to the node.
     * @param node
     * @param rule
     * @param jsonObject
     * @return
     */
    private JSONObject processRule(Node node, ExtractRule rule, JSONObject jsonObject) {

        String sourceProperty = rule.getSourceProperty();
        String sourceSubpath = rule.getSourceSubpath();        
        String targetProperty = rule.getTargetProperty();
        try {
            String nodePath = node.getPath();
            String nodeName = node.getName();
            String fullPath = nodePath + sourceSubpath;           
            switch (rule.getExtractType()) {
                case PROPERTY:
                    jsonObject.put(targetProperty, getProperty(fullPath, sourceProperty));
                    break;
                case MULTI_PROPERTY:
                    jsonObject.put(targetProperty, getMultiProperty(fullPath, sourceProperty));
                    break;
                case PROPERTY_FROM_CHILDREN:
                    jsonObject.put(targetProperty, getPropertyFromChildren(node, sourceSubpath, sourceProperty));
                    break;
                case OBJECT_LIST:
                    jsonObject.put(targetProperty, getJsonArray(fullPath));
                    break;
                case OBJECT_MAP:
                    jsonObject.put(targetProperty, getJsonMap(fullPath));
                    break;
                case TITLE:
                    jsonObject.put(targetProperty, getProperty(fullPath, "jcr:title"));
                    break;
                case NAME:
                    jsonObject.put(targetProperty, nodeName);
                    break;
                case PATH:
                    jsonObject.put(targetProperty, nodePath);
                    break;
                case ALL:                    
                    JSONObject tempJson = getJsonMap(fullPath);
                    if (tempJson != null && tempJson.has("jcr:content")) {
                        jsonObject = tempJson.getJSONObject("jcr:content");
                    }
                    break;
            }
        } catch (Exception e) {
            log.error(">>> error: " + e.getMessage());
        }
        return jsonObject;
    }
       
    private String getProperty(String path, String prop) {
        String result = "";
        try {
            Node node = session.getNode(path);
            if (node != null) {
                Property p = node.getProperty(prop);
                if (p != null) {
                    return p.getValue().getString().replaceAll("\"", "").trim();
                }
            }
        } catch (RepositoryException ignored) {
        }
        return result;
    }
    
    private String getMultiProperty(String path, String propertyName) {
        if (path == null)
            return null;

        String result = null;
        try {
            Node node = session.getNode(path);
            if (node != null) {
                Property prop = node.getProperty(propertyName);
                if (prop != null) {
                    if (prop.isMultiple()) {
                        Value[] valueList = prop.getValues();
                        StringBuilder values = new StringBuilder();
                        for (Value eachValue : valueList) {
                            values.append(eachValue.getString()).append(" | ");
                        }
                        result = values.toString().replaceAll(" \\| $", "").trim();

                    } else {
                        result = prop.getString();
                    }
                }                           
            }
        } catch (IllegalStateException | RepositoryException ignored) {
        }
        return result;
    }

    /**
     * Used to fetch value of a property from the child nodes
     *
     * @param sourceNode source node
     * @param subPath subpath to the child nodes
     * @param propertyName name of the property
     * @return value (Pipe Separated) of same property from all the children under the subPath node.
     */
    private String getPropertyFromChildren(Node sourceNode, String subPath, String propertyName) {
        if (subPath == null)
            return null;
        String value = null;
        try {
            if (sourceNode != null) {
                if(subPath.startsWith("/"))
                    subPath = subPath.replaceFirst("/","");

                value = StringUtils.join(Utils.getSamePropertyFromAllSubNodes(sourceNode, subPath, propertyName), " | ");
            }
        } catch (IllegalStateException ignored) {
        }
        return value;
    }
    
    /**
     * Get a JSONArray of child nodes from a path.
     * @param path
     * @return
     */
    private JSONArray getJsonArray(String path) {
        if (path == null)
            return null;        
        JSONArray values = null;
        try {
            Node node = session.getNode(path);
            NodeIterator it = node.getNodes();
            if (it!=null) {
                values = new JSONArray();
                while (it.hasNext()) {
                    Node childNode = (Node) it.next();
                    JSONObject jsonObj = Utils.toJsonObject(getJsonObject(childNode.getPath()));
                    values.put(jsonObj);
                }                
            }
        } catch (IllegalStateException | RepositoryException ignored) {
        }
        return values;
    }
    
    /**
     * Get a JSONArray of child nodes from a path
     * using node names as key.
     * @param path
     * @return
     */
    private JSONObject getJsonMap(String path) {
        if (path == null)
            return null;        
        JSONObject values = new JSONObject();
        try {
            Node node = session.getNode(path);
            NodeIterator it = node.getNodes();
            if (it!=null) {
                while (it.hasNext()) {
                    Node childNode = (Node) it.next();
                    JSONObject jsonObj = Utils.toJsonObject(getJsonObject(childNode.getPath()));
                    values.put(childNode.getName(), jsonObj);
                }
            }
        } catch (JSONException | RepositoryException | IllegalStateException ignored) {
        }
        return values;
    }
    
    /**
     * Use JsonJcrNode to perform deep traversal of JCR tree for path.
     * @param path
     * @return
     */
    public ObjectNode getJsonObject(String path) {
        if (path == null) {
            return null;
        }
        ObjectNode jsonObj = null;
        try {
            Node node = session.getNode(path);
            // Convert the JCR Node to ObjectNode, excluding rejected keys
            jsonObj = Utils.convertNodeToJson(node, getRejectedKeys());
            // Traverse child nodes
            NodeIterator it = node.getNodes();
            if (it != null) {
                while (it.hasNext()) {
                    Node childNode = it.nextNode();
                    ObjectNode childJson = getJsonObject(childNode.getPath());
                    jsonObj.set(childNode.getName(), childJson); // Use set to add child ObjectNode
                }
            }
        } catch (RepositoryException e) {
            log.error("Error retrieving node at path: " + path, e);
        }
        return jsonObj;
    }

    private Set<String> getRejectedKeys() {
        Set<String> rejects = new HashSet<String>();
        rejects.add("jcr:path");
        rejects.add("jcr:name");
        rejects.add("jcr:primaryType");
        rejects.add("jcr:created");        
        rejects.add("jcr:createdBy");
        return rejects;
   }
}