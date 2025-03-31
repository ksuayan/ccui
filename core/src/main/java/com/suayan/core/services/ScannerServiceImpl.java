package com.suayan.core.services;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import org.apache.sling.jcr.api.SlingRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.suayan.core.extractor.ExtractRule;
import com.suayan.core.extractor.ExtractRulesParser;
import com.suayan.core.extractor.ExtractType;
import com.suayan.core.extractor.JcrReader;
import com.suayan.core.extractor.JsonExtractor;
import com.suayan.core.utils.Utils;

/**
 * An actual implementation of the ScannerService interface.
 * 
 * @author Kyo Suayan.
 *
 */
@Component(immediate = true)
public class ScannerServiceImpl implements ScannerService {

  @Reference
  SlingRepository repository;

  @Reference
  private JcrReader jcrReader;

  private final Logger log = LoggerFactory.getLogger(this.getClass());
  // The directory where JSON Query definitions are located
  private static final String CONFIG_PATH = "apps/ccui/extractor/";
  private Session session = null;
  private boolean recurseEnabled = false;
  private final ExtractRule defaultRule = new ExtractRule(null, ExtractType.ALL);

  @Activate
  public void init() {
    try {
      this.session = this.repository.loginService("ccui-root-read-user", null);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  public boolean isRecurseEnabled() {
    return recurseEnabled;
  }

  public void setRecurseEnabled(boolean recurseEnabled) {
    this.recurseEnabled = recurseEnabled;
  }

  /**
   * Given either a JCR-SQL2 or xpath query, return a list of
   * JCR Paths that match that query.
   * 
   */
  public List<String> queryForJcrPaths(String query, String language) {
    List<String> paths = null;
    NodeIterator nodeIter = this.getNodeIteratorFromJcrQuery(query, language);
    if (nodeIter != null) {
      paths = this.getPaths(nodeIter);
    }
    return paths;
  }

  /**
   * Retrieve PageTitles for given sqlQuery.
   */
  public List<JSONObject> queryForPageTitles(String query, String language) {
    List<JSONObject> pages = null;
    NodeIterator nodeIter = this.getNodeIteratorFromJcrQuery(query, language);
    if (nodeIter != null) {
      pages = this.getPageTitles(nodeIter);
    }
    return pages;
  }

  /**
   * Read a JSON config file in the JCR tree and build an ExtractRulesParser
   * instance. Read the query from the JSON file, execute it and iterate over each
   * node found while applying all ExtractRules defined in ExtractRulesParser.
   * 
   */
  public List<JSONObject> queryByJsonConfig(String path, Map<String, String[]> params) {
    List<JSONObject> pages = null;
    String jsonPath = CONFIG_PATH + path + ".json";
    jcrReader.setFilepath(jsonPath);
    String jsonConfig = jcrReader.readToString();
    try {
      JSONObject jsonObj = new JSONObject(jsonConfig);
      pages = queryByJsonObject(jsonObj, params);
    } catch (Exception e) {
      log.error(">>> Exception: " + e.getMessage());
    }
    return pages;
  }

  /**
   * Allow queries with provided JSONObject representation of query config. Core
   * extracted from queryByJsonConfif(path,params);
   * 
   * @param jsonObj
   * @param params
   * @return
   */
  public List<JSONObject> queryByJsonObject(JSONObject jsonObj, Map<String, String[]> params) {
    List<JSONObject> pages = null;
    List<ExtractRule> parsedRules = null;
    ExtractRulesParser ruleParser = null;
    try {
      String query = this.compileQuery(jsonObj.getString("query"), params);
      String queryLanguage = Query.JCR_SQL2;
      if (jsonObj.has("language")) {
        queryLanguage = jsonObj.getString("language");
      }
      boolean recurse = false;
      if (jsonObj.has("recurse")) {
        recurse = jsonObj.getBoolean("recurse");
        log.info("recurse enabled");
      }
      this.setRecurseEnabled(recurse);
      NodeIterator it = this.getNodeIteratorFromJcrQuery(query, queryLanguage);
      if (jsonObj.has("rules")) {
        JSONArray jsonRules = jsonObj.getJSONArray("rules");
        if (jsonRules.length() > 0) {
          ruleParser = new ExtractRulesParser(jsonRules);
          parsedRules = ruleParser.parseExtractRules();
          pages = this.getExtractedPages(it, parsedRules);
        } else {
          pages = getDefaultExtract(it);
        }
      } else {
        pages = getDefaultExtract(it);
      }
    } catch (Exception e) {
      log.error(">>> Exception: " + e.getMessage());
    }
    return pages;
  }

  /**
   * Replace placeholders in query with map values using {key} as
   * substring to look for.
   * 
   * @param query
   * @param params
   * @return
   */
  private String compileQuery(String query, Map<String, String[]> params) {
    if (query == null)
      return null;

    if (params == null)
      return query;

    String result = query;
    for (Map.Entry<String, String[]> entry : params.entrySet()) {
      String token = Pattern.quote("{" + entry.getKey() + "}");
      String[] values = entry.getValue();
      log.info("token: " + token + " value:" + values[0]);
      result = result.replaceAll(token, Matcher.quoteReplacement(values[0]));
    }
    log.info(">> compiled: " + result);
    return result;
  }

public List<JSONObject> queryForJsonObjects(String query, String queryLanguage) {
   List<JSONObject> jsonNodes = new ArrayList<>();
   NodeIterator nodeIter = this.getNodeIteratorFromJcrQuery(query, queryLanguage);
    
    if (nodeIter != null) {
        while (nodeIter.hasNext()) {
            try {
                Node node = nodeIter.nextNode();
                ObjectNode jsonNode = Utils.convertNodeToJson(node);
                jsonNodes.add(Utils.toJsonObject(jsonNode));
            } catch (RepositoryException e) {
                log.error("Error processing node: " + e.getMessage(), e);
            }
        }
    }
    
    return jsonNodes;
}

  /**
   * Simple wrapper that returns a NodeIterator from a SQL query.
   * @return
   */
  private NodeIterator getNodeIteratorFromJcrQuery(String statement, String language) {
    NodeIterator nodeIter = null;
    try {
      this.init();
      // Obtain the query manager for the session ...
      QueryManager queryManager = session.getWorkspace().getQueryManager();
      Query query = queryManager.createQuery(statement, language);
      // Execute the query and get the results ...
      QueryResult results = query.execute();
      // Iterate over the nodes in the results ...
      nodeIter = results.getNodes();
      // session.logout();
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return nodeIter;
  }

  /**
   * Return a list of paths from a given NodeIterator.
   * 
   * @param nodeIter
   * @return
   */
  private List<String> getPaths(NodeIterator nodeIter) {
    if (nodeIter == null) {
      return null;
    }
    List<String> paths = new ArrayList<String>();
    try {
      while (nodeIter.hasNext()) {
        Node node = nodeIter.nextNode();
        paths.add(node.getPath());
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return paths;
  }

  /**
   * Retrieve PageTitles for given sqlQuery.
   */
  private List<JSONObject> getPageTitles(NodeIterator it) {
    if (it == null) {
      return null;
    }
    List<JSONObject> jsonList = new ArrayList<JSONObject>();
    try {
      JsonExtractor jsonExtractor = new JsonExtractor(session);
      jsonExtractor.addRule(new ExtractRule("title", ExtractType.TITLE));
      jsonExtractor.addRule(new ExtractRule("path", ExtractType.PATH));
      while (it.hasNext()) {
        Node node = it.nextNode();
        JSONObject jsonObject = new JSONObject();
        jsonObject = jsonExtractor.getJsonObject(node, jsonObject);
        jsonList.add(jsonObject);
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return jsonList;
  }

  private List<JSONObject> getDefaultExtract(NodeIterator it) {
    if (it == null) {
      return null;
    }
    List<JSONObject> jsonList = new ArrayList<JSONObject>();
    try {
      JsonExtractor jsonExtractor = new JsonExtractor(session);
      List<ExtractRule> parsedRules = new ArrayList<ExtractRule>();
      parsedRules.add(defaultRule);
      jsonExtractor.addRules(parsedRules);
      while (it.hasNext()) {
        Node node = it.nextNode();
        jsonList.add(Utils.toJsonObject(jsonExtractor.getJsonObject(node.getPath())));
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return jsonList;
  }

  /**
   * Process the nodes and apply extract rules using instantiated JsonExtractor.
   * @param it
   * @param rules
   * @return
   */
  private List<JSONObject> getExtractedPages(NodeIterator it, List<ExtractRule> rules) {
    if (it == null) {
      return null;
    }
    List<JSONObject> jsonList = new ArrayList<JSONObject>();
    try {
      JsonExtractor jsonExtractor = new JsonExtractor(session);
      jsonExtractor.addRules(rules);
      while (it.hasNext()) {
        Node node = it.nextNode();
        JSONObject jsonObject = new JSONObject();
        jsonObject = jsonExtractor.getJsonObject(node, jsonObject);
        jsonList.add(jsonObject);
      }
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return jsonList;
  }

  public String getJsonString(Node node) {
    ObjectMapper objectMapper = new ObjectMapper();
    ObjectNode jsonNode = objectMapper.createObjectNode();

    try {
      // Convert the JCR Node to JSON
      convertNodeToJson(node, jsonNode);
    } catch (RepositoryException e) {
      log.error(e.getMessage(), e);
    }

    // Convert ObjectNode to String
    StringWriter stringWriter = new StringWriter();
    try {
      objectMapper.writeValue(stringWriter, jsonNode);
    } catch (Exception e) {
      log.error(e.getMessage(), e);
    }

    return stringWriter.toString();
  }

  private void convertNodeToJson(Node node, ObjectNode jsonNode) throws RepositoryException {
    // Add properties of the node to the JSON object
    if (node.hasProperties()) {
      javax.jcr.PropertyIterator properties = node.getProperties();
      while (properties.hasNext()) {
        javax.jcr.Property property = properties.nextProperty();
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
    }

    // Optionally, handle child nodes if needed
    if (node.hasNodes()) {
      javax.jcr.NodeIterator childNodes = node.getNodes();
      while (childNodes.hasNext()) {
        javax.jcr.Node childNode = childNodes.nextNode();
        ObjectNode childJsonNode = jsonNode.putObject(childNode.getName());
        convertNodeToJson(childNode, childJsonNode);
      }
    }
  }

}