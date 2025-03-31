package com.suayan.core.sling;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SlingQueryStatement {

    private final Logger log = LoggerFactory.getLogger(this.getClass());
    private String queryStatement;
    private String queryLanguage;
    private Map<String,String> params = null;

    /**
     * Do not assume params will always be defined.
     * Some queries are not parameterized.
     * 
     * @param statement
     * @param language
     */
    public SlingQueryStatement(String statement, String language) {
        this.queryStatement = statement;
        this.queryLanguage = language;
    }

    public SlingQueryStatement(String statement, String language, Map<String, String> params) {
        this.queryStatement = statement;
        this.queryLanguage = language;
        this.params = params;
    }

    /**
     * @return the queryStatement
     */
    public String getQueryStatement() {
        return queryStatement;
    }
    /**
     * @param queryStatement the queryStatement to set
     */
    public void setQueryStatement(String queryStatement) {
        this.queryStatement = queryStatement;
    }
    /**
     * @return the queryLanguage
     */
    public String getQueryLanguage() {
        return queryLanguage;
    }
    /**
     * @param queryLanguage the queryLanguage to set
     */
    public void setQueryLanguage(String queryLanguage) {
        this.queryLanguage = queryLanguage;
    }
    /**
     * @return the params
     */
    public Map<String, String> getParams() {
        return params;
    }
    /**
     * @param params the params to set
     */
    public void setParams(Map<String, String> params) {
        this.params = params;
    }
    /**
     * Replace placeholders in query with map values using {key} as
     * substring to look for.
     *
     * @param query
     * @param params
     * @return
     */
    public String compile() {
        if (StringUtils.isBlank(queryStatement))
            return null;
        if (params == null)
            return this.queryStatement;
        String result = queryStatement;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String token = Pattern.quote("{"+entry.getKey()+"}");
            String value = entry.getValue();
            // log.trace("token: " + token + " value:" + value);
           result = result.replaceAll(token, Matcher.quoteReplacement(value));
        }
        // log.trace(">> compiled: " + result);
        return result;
    }

    public void addParam(String name, String value) {
        if (params == null) {
            params = new HashMap<String,String>();
        }
        if (name!=null) {
            params.put(name, value);
        }
    }
}