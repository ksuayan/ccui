package com.suayan.core.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.version.VersionException;
import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = Servlet.class,
	immediate = true,
	property = {
		Constants.SERVICE_DESCRIPTION + "=Page Creator Servlet",
		Constants.SERVICE_VENDOR+"=CCUI",
		"sling.servlet.methods="+HttpConstants.METHOD_POST,
		"sling.servlet.extensions=json",
		"sling.servlet.paths=/bin/api/page"
	},
	configurationPid = "com.suayan.core.servlets.PageCreatorServlet"
)

public class PageCreatorServlet extends SlingAllMethodsServlet {
    private static final long serialVersionUID = 98343512463434495L;
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Reference
    private ResourceResolverFactory resolverFactory;

    private ResourceResolver jcrResourceResolver;
    private Session jcrContentSession;
    private boolean enableOverwrite = false;

    private static String[] DATE_FORMATS = {
        "EEE MMM dd yyyy HH:mm:ss 'GMT'Z", // Format: "Wed Jan 25 2023 00:00:00 GMT+0530"
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",    // Format: "2024-12-06T04:00:00.000-08:00"
        "yyyy-MM-dd'T'HH:mm:ssXXX",         // Format: "2024-12-06T04:00:00-08:00"
        "yyyy-MM-dd'T'HH:mm:ss.SSSZ",       // Format: "2024-12-06T04:00:00.000-0800"
        "yyyy-MM-dd'T'HH:mm:ssZ",           // Format: "2024-12-06T04:00:00-0800"
        "yyyy-MM-dd HH:mm:ss",               // Format: "2024-12-06 04:00:00"
        "yyyy-MM-dd"                        // Format: "2024-12-06"
    };


    // Define known date properties
    private static final Set<String> KNOWN_DATE_PROPERTIES = new HashSet<String>() {
    	private static final long serialVersionUID = 1L;
	{
        add("cq:lastModified");
        add("cq:lastReplicated");
        add("jcr:lastModified");
        add("jcr:created");
        add("pretitleEndDate");
        add("pretitleStartDate");
        add("pageDate");
        add("publicationDate");
    }};

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        String postData = getPostData(request);
        JSONObject jsonPostData = null;
        try {
        	
            Map<String, Object> params = new HashMap<String, Object>();
            params.put(ResourceResolverFactory.SUBSERVICE, "shc-core-content-user");
            jcrResourceResolver = resolverFactory.getServiceResourceResolver(params);
            jcrContentSession = jcrResourceResolver.adaptTo(Session.class);
            
            jsonPostData = new JSONObject(postData);
            JSONObject jcrJsonObject = getChildJson("jcr", jsonPostData);
            JSONObject orderedKeys = getChildJson("orderedKeys", jsonPostData);
            String path = jsonPostData.getString("path").trim();
            enableOverwrite = jsonPostData.getBoolean("overwrite");
            createNodeHierarchy(path, jcrJsonObject, orderedKeys);
            printJsonResponse(response, jcrJsonObject);
        } catch (JSONException | LoginException e) {
            throw new IOException("Error parsing JSON request string");
		}
    }

    private void printJsonResponse(SlingHttpServletResponse response, JSONObject jsonObject) {
        response.setContentType("application/json");
        PrintWriter out;
        try {
            out = response.getWriter();
            out.print(jsonObject);
            out.flush();
        } catch (IOException e) {
            log.error("IOException: response.getWriter()");
        }
    }

    private String getPostData(SlingHttpServletRequest request) {
        StringBuffer jb = new StringBuffer();
        String line = null;
        try {
            BufferedReader reader = request.getReader();
            while ((line = reader.readLine()) != null)
                jb.append(line);
        } catch (Exception e) {
        	log.error("getPostData()", e.getMessage());
        }
        return jb.toString();
    }

    private void createNodeHierarchy(String path, JSONObject jsonObject, JSONObject orderedKeys) {
        try {            
            if (jcrContentSession == null) {
                log.error("Failed to obtain JCR session.");
                return; // Exit if session is null
            }
    
            // Check if the node exists and handle accordingly
            Node context;
            if (jcrContentSession.nodeExists(path)) {
                context = jcrContentSession.getNode(path);
                if (!enableOverwrite) {
                    log.info("Node already exists at " + path + " and overwrite is disabled.");
                    return; // Exit if overwrite is not allowed
                } else {
                    // If overwrite is enabled, delete the existing jcr:content node
                    if (context.hasNode("jcr:content")) {
                        context.getNode("jcr:content").remove();
                        log.info("Existing node at " + path + "/jcr:content has been deleted.");
                        jcrContentSession.save(); // Save changes after deletion
                        jcrContentSession.refresh(true);
                    }
                }
            } else {
                // If the node does not exist, create it
                context = jcrContentSession.getRootNode().addNode(path.substring(path.lastIndexOf("/") + 1), "nt:unstructured");
                log.info("Creating new node at " + path);
            }
    
            // Create the jcr:content node with the correct primary type
            Node newJcrContent = context.addNode("jcr:content", "cq:PageContent");
            log.info("Creating new jcr:content node at " + newJcrContent.getPath());
            jcrContentSession.save();
            jcrContentSession.refresh(true);
            
            // Now traverse the JSON object to populate the new node
            createJCRObjects(newJcrContent, "/jcr:content", jsonObject, orderedKeys);
            createJCRAttributes(newJcrContent, "/jcr:content", jsonObject);
            jcrContentSession.save();
            jcrContentSession.refresh(true);
        } catch (RepositoryException e) {     
            log.error("createNodeHierarchy() RepositoryException: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    private JSONObject getChildJson(String key, JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        try {
            if (jsonObject.get(key) instanceof JSONObject) {
                return jsonObject.getJSONObject(key);
            }
        } catch (JSONException e) {
        }
        return null;
    }

    private Node checkNode(Node parent, String name, String primaryType) {
        if (parent == null) {
            return null;
        }
        Node childNode = null;
        try {
            if (parent.hasNode(name)) {
                return parent.getNode(name);
            }
            if (primaryType != null) {
                childNode = parent.addNode(name, primaryType);
            } else {
                childNode = parent.addNode(name);
            }
            jcrContentSession.save();
            jcrContentSession.refresh(true);
        } catch (ItemExistsException e) {
        } catch (PathNotFoundException e) {
        } catch (NoSuchNodeTypeException e) {
        } catch (LockException e) {
        } catch (VersionException e) {
        } catch (ConstraintViolationException e) {
        	e.printStackTrace();
        } catch (RepositoryException e) {
        	e.printStackTrace();
        }
        return childNode;

    }

    /*
     * JCR Node order is important in AEM although the JSON standard does not 
     * guarantee the order at which keys are returned. To augment
     * this we have an orderedKeys object which is a map of
     * arrays where the key is the JCR path and the values are 
     * JCR node names.
     */
    private void createJCRObjects(Node parent, String jcrPath, JSONObject jsonObject, JSONObject orderedKeys) {
    	
    	if (jcrPath != null && orderedKeys != null && orderedKeys.has(jcrPath)) {
    		try {
    			
				JSONArray keyArray = orderedKeys.getJSONArray(jcrPath);
                Set<String> orderedKeySet = new HashSet<>();

				for (int i = 0; i < keyArray.length(); i++) {
				    String key = keyArray.getString(i);
                    orderedKeySet.add(key);
		            try {
		                Object object = jsonObject.get(key);
		                if (object instanceof JSONObject) {
		                    JSONObject childJson = getChildJson(key, jsonObject);
		                    String primaryType = childJson.getString("jcr:primaryType");
		                    Node childNode = checkNode(parent, key, primaryType);
		                    // depth first traversal
		                    String newPath = jcrPath + "/" + key;
		                    createJCRObjects(childNode, newPath, childJson, orderedKeys);		                    
		                }
		            } catch (JSONException e) {
		                log.error("JSONExeption: " + e.getMessage());
		            }
                }
			} catch (JSONException e) {
				e.printStackTrace();
			}
        }
    }
    
    private void createJCRAttributes(Node parent, String jcrPath, JSONObject jsonObject) {
    	
        log.trace(String.format(">>> createJCRAttributes path: %s", jcrPath));

    	if (jcrPath != null) {
    		try {
    			
				
		        Iterator<?> keys = jsonObject.keys();
		        while (keys.hasNext()) {
		            String key = (String) keys.next();
		            key = key.trim();
		            

	                Object object = jsonObject.get(key);

                    if (object instanceof JSONArray) {

                        log.trace(String.format(">>> array: %s", key));
        	
                        // Handle JSONArray (elements are always strings)
                        JSONArray jsonArray = jsonObject.getJSONArray(key);
                        String[] valueArray = new String[jsonArray.length()]; // Initialize the String array	            
                        // Populate the String array with values from the JSONArray
                        for (int j = 0; j < jsonArray.length(); j++) {
                            valueArray[j] = jsonArray.getString(j); // Get the string value at index i
                        }
                        parent.setProperty(key, valueArray); // Set the property with a unique name	            
        

	                } else if (object instanceof String) {

                        log.trace(String.format(">>> string: %s", key));

	                    String value = jsonObject.getString(key);
	                    if (!key.equals("jcr:primaryType")) {
	                        if (KNOWN_DATE_PROPERTIES.contains(key)) {
	                            // Convert the string value to a JCR Date Value
	                            Value jcrDateValue = convertStringToJcrDate(value);
	                            if (jcrDateValue != null) {
	                                parent.setProperty(key, jcrDateValue);
	                            }
	                        } else {
	                            // Set the property as a regular string
	                            parent.setProperty(key, value);
	                        }

	                    }

	                } else if (object instanceof Boolean) {

                        log.trace(String.format(">>> boolean: %s", key));
	                    // Handle boolean values
	                    boolean booleanValue = jsonObject.getBoolean(key);
	                    parent.setProperty(key, booleanValue);

	                } else if (object instanceof JSONObject) {

                        log.trace(String.format(">>> object: %s", key));

	                    JSONObject childJson = getChildJson(key, jsonObject);
	                    Node childNode = parent.getNode(key);                  
	                    String newPath = jcrPath + "/" + key;

                        log.trace(String.format(">>> recursing at: %s", newPath));
	                    createJCRAttributes(childNode, newPath, childJson);
                    }

		        }

                jcrContentSession.save();
                jcrContentSession.refresh(true);

            } catch (JSONException e) {
				e.printStackTrace();
			} catch (AccessDeniedException e) {
				e.printStackTrace();
			} catch (ItemExistsException e) {
				e.printStackTrace();
			} catch (ReferentialIntegrityException e) {
				e.printStackTrace();
			} catch (ConstraintViolationException e) {
				e.printStackTrace();
			} catch (InvalidItemStateException e) {
				e.printStackTrace();
			} catch (VersionException e) {
				e.printStackTrace();
			} catch (LockException e) {
				e.printStackTrace();
			} catch (NoSuchNodeTypeException e) {
				e.printStackTrace();
			} catch (RepositoryException e) {
				e.printStackTrace();
			}
    		
    		log.trace(String.format(">>> exiting createJcrAttributes for : %s", jcrPath));
    	}
    }
    
    private Value convertStringToJcrDate(String dateString) {    
        for (String format : DATE_FORMATS) {
            try {
                // Try parsing with ZonedDateTime for timezone-aware dates
                ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateString, DateTimeFormatter.ofPattern(format, Locale.ENGLISH));
                // Convert ZonedDateTime to Calendar
                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(zonedDateTime.toInstant().toEpochMilli());
                // Get the ValueFactory from the session
                ValueFactory valueFactory = jcrContentSession.getValueFactory();
                // Create a JCR Date Value
                return valueFactory.createValue(calendar);
            } catch (DateTimeParseException e) {
                // Continue to the next format if parsing fails
            } catch (RepositoryException e) {
                log.error("Error creating JCR Value: " + e.getMessage(), e);
                return null; // Handle the error as needed
            }
        }
        log.error("Error parsing date string: " + dateString); // Log if no formats matched
        return null; // Handle the error as needed
    }    

    public static boolean isValidDate(String dateString) {
        for (String format : DATE_FORMATS) {
            try {
                // Try parsing with ZonedDateTime for timezone-aware dates
                ZonedDateTime.parse(dateString, DateTimeFormatter.ofPattern(format, Locale.ENGLISH));
                return true; // Parsing was successful
            } catch (DateTimeParseException e) {
                // Continue to the next format if parsing fails
            }
        }
        // If none of the formats matched, return false
        return false;
    }
}
