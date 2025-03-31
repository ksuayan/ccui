package com.suayan.core.extractor;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Given extract rules in JSONArray format,
 * return a list of extract rules (List<ExtractRule>).
 * 
 * The following rule types are supported:
 * 
 * Basic Types
 * -----------------------
 * title: simple jcr:title
 * path: jcr path for node.
 * name: name of node
 * 
 * Customizable Types
 * ----------------------
 * property: jcr property
 * multi-property: multi-value jcr property
 * object-list: create a JSON Array of anonymous objects
 * object-map: create an associative map of JSONObjects using node name as key
 * 
 * @author Kyo Suayan
 *
 */
public class ExtractRulesParser {

    private JSONArray rules = null;

    public ExtractRulesParser(JSONArray rules) {
        this.rules = rules;
    }

    public List<ExtractRule> parseExtractRules() {
        List<ExtractRule> parsedRules = new ArrayList<ExtractRule>();
        if (rules == null) {
            return parsedRules;
        }        
        int size = rules.length();
        for (int i=0; i<size; i++) {
            try {
                JSONObject rule = rules.getJSONObject(i);
                ExtractRule parsedRule = convert(rule);
                if (parsedRule!=null) {
                    parsedRules.add(parsedRule);                    
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return parsedRules;
    }
    
    private ExtractRule convert(JSONObject rule) {
        if (rule == null) {
            return null;
        }
        ExtractRule parsedRule = null;
        try {
            String type = rule.getString("type").toLowerCase().trim();
            String targetProperty = getJsonProperty(rule,"targetProperty");
            String sourceSubpath;
            String sourceProperty;

            switch (type) {
                case "property":  // Handle "ExtractType.PROPERTY"
                    sourceSubpath = getJsonProperty(rule, "sourceSubpath");
                    sourceProperty = getJsonProperty(rule, "sourceProperty");
                    parsedRule = new ExtractRule(targetProperty, ExtractType.PROPERTY, sourceSubpath, sourceProperty);

                    break;
                case "multi-property":  // Handle "ExtractType.MULTI_PROPERTY"
                    sourceSubpath = getJsonProperty(rule, "sourceSubpath");
                    sourceProperty = getJsonProperty(rule, "sourceProperty");
                    parsedRule = new ExtractRule(targetProperty, ExtractType.MULTI_PROPERTY, sourceSubpath, sourceProperty);

                    break;
                case "object-list": // Handle "ExtractType.OBJECT_LIST"
                    sourceSubpath = getJsonProperty(rule, "sourceSubpath");
                    parsedRule = new ExtractRule(targetProperty, ExtractType.OBJECT_LIST, sourceSubpath, null);

                    break;
                case "object-map": // Handle "ExtractType.OBJECT_MAP"
                    sourceSubpath = getJsonProperty(rule, "sourceSubpath");
                    parsedRule = new ExtractRule(targetProperty, ExtractType.OBJECT_MAP, sourceSubpath, null);

                    break;
                case "title": // ExtractType.TITLE
                    parsedRule = new ExtractRule(targetProperty, ExtractType.TITLE);

                    break;
                case "name": // ExtractType.NAME
                    parsedRule = new ExtractRule(targetProperty, ExtractType.NAME);

                    break;
                case "path": // ExtractType.PATH
                    parsedRule = new ExtractRule(targetProperty, ExtractType.PATH);
                    break;
                case "property-from-children":  // Handle "ExtractType.PROPERTY_FROM_CHILDREN"
                    sourceSubpath = getJsonProperty(rule, "sourceSubpath");
                    sourceProperty = getJsonProperty(rule, "sourceProperty");
                    parsedRule = new ExtractRule(targetProperty, ExtractType.PROPERTY_FROM_CHILDREN, sourceSubpath, sourceProperty);

                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return parsedRule;
    }
    
    private String getJsonProperty(JSONObject jsonObj, String key) {
        if (jsonObj == null) {
            return "";
        }
        String value = "";
        try {
            value = jsonObj.getString(key);
        } catch (JSONException ignored) {
        }
        return value;
    }   
}