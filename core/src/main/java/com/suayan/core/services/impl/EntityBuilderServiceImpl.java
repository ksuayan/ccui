package com.suayan.core.services.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.RangeIterator;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.tagging.TagManager;
import com.suayan.core.services.EntityBuilderService;
import com.suayan.core.services.EntityBuilderServiceConfig;
import com.suayan.core.sling.SlingQueryStatement;

/**
 * A Sling based service implementation of the @EntityBuilderService interface.
 * 
 * Given a list of paths, return a List of entity instances.
 *
 * @author Kyo Suayan
 *
 */
@Component(service = {
		EntityBuilderService.class 
	}, 
	enabled = true, 
	immediate = true, 
	configurationPid = "com.suayan.com.core.services.impl.EntityBuilderServiceImpl", 
	property = {
				Constants.SERVICE_DESCRIPTION + "=CCUI Entity Builder Service" 
})
@Designate(ocd = EntityBuilderServiceConfig.class)
public class EntityBuilderServiceImpl implements EntityBuilderService {

	private static final String ROOT_READ_USER = "ccui-root-read-user";
	private static final Logger log = LoggerFactory.getLogger(EntityBuilderServiceImpl.class);
	private static Map<String, Object> params;
	private static final String FAILED_RESOLVER_MESSAGE = "Failed to acquire resourceResolver. ";
	private static final String XPATH = "xpath";

	@Reference
	Repository repository;

	@Reference
	SlingRepository slingRepository;

	@Reference
	private ResourceResolverFactory resourceResolverFactory;

	@Activate
	@Modified
	protected void activate(EntityBuilderServiceConfig ebsConfig) {
		log.info("CCUI EntityBuilder Service: Activated.");
		params = new HashMap<String, Object>();
		params.put(ResourceResolverFactory.SUBSERVICE, ROOT_READ_USER);
	}

	@Deactivate
	protected void deactivate(EntityBuilderServiceConfig ebsConfig) {
		log.info("CCUI Service: Deactivated.");
	}

	void setResourceResolverFactory(ResourceResolverFactory resourceResolverFactory) {
		this.resourceResolverFactory = resourceResolverFactory;
	}

	/**
	 * Converts a path and type to a bean of a certain type.
	 *
	 * @param path the path to the item
	 * @param type the type of the item
	 * @return a bean of the given type
	 */
	@Override
	public <T> T toItem(String path, Class<T> type) {
		ResourceResolver resourceResolver = null;
		T item = null;
		try {
			resourceResolver = resourceResolverFactory.getServiceResourceResolver(params);
			Resource resource = resourceResolver.getResource(path);

			if (resource != null) {
				item = resource.adaptTo(type);
			} else {
				log.info("null resource for path " + path);
			}
		} catch (LoginException e) {
			log.debug(FAILED_RESOLVER_MESSAGE + e.getMessage());
			log.debug("path: " + path);
			log.debug("params: " + params.toString());
			e.printStackTrace();
		} finally {
			close(resourceResolver);
		}
		return item;
	}

	/**
	 * Return a list of materialized beans based on target object type.
	 */
	@Override
	public <T> List<T> toList(List<String> paths, Class<T> type) {
		List<T> entityList = new ArrayList<>();

		if (paths == null || paths.isEmpty()) {
			return entityList;
		}

		ResourceResolver resourceResolver = null;
		try {
			resourceResolver = resourceResolverFactory.getServiceResourceResolver(params);
			for (String path : paths) {
				String resourcePath = path + "/" + JcrConstants.JCR_CONTENT;
				Resource resource = resourceResolver.getResource(resourcePath);

				if (resource != null) {
					T entity = resource.adaptTo(type);
					if (entity != null) {
						entityList.add(entity);
					}
				} else {
					log.debug("null resource for path " + resourcePath);
				}
			}
		} catch (LoginException e) {
			log.debug(FAILED_RESOLVER_MESSAGE + e.getMessage());
		} finally {
			close(resourceResolver);
		}
		return entityList;
	}

	/**
	 * Convenience method for querying nodes under a given rootNode matching the
	 * value for a given property.
	 */
	@Override
	public <T> List<T> queryByProperty(String rootPath, String propertyName, String value, Class<T> type) {
		String XPATH_QUERY = "{rootPath}//element(*,nt:unstructured)[{property}='{value}']";
		SlingQueryStatement sqs = new SlingQueryStatement(XPATH_QUERY, XPATH);
		sqs.addParam("value", value);
		sqs.addParam("rootPath", rootPath);
		sqs.addParam("property", propertyName);
		return query(sqs, type);
	}

	/**
	 * Based on nodes returned from a query, instantiate beans out of those nodes.
	 */
	@Override
	public <T> List<T> query(SlingQueryStatement sqs, Class<T> type) {
		List<T> entityList = new ArrayList<>();
		Session session = null;
		ResourceResolver resourceResolver = null;
		try {
			resourceResolver = resourceResolverFactory.getServiceResourceResolver(params);
			session = getSession(resourceResolver);
			if (session == null) {
				log.debug("entityBuilder's session was null.");
				return null;
			}

			NodeIterator it = getNodeIterator(session, sqs);
			while (it != null && it.hasNext()) {
				Node node = it.nextNode();
				log.trace("entityBuilder node found: " + node.getPath());
				Resource nodeResource = resourceResolver.getResource(node.getPath());
				if (nodeResource != null) {
					T entity = nodeResource.adaptTo(type);
					if (entity != null) {
						entityList.add(entity);
					}
				}
			}
		} catch (RepositoryException e) {
			log.debug("encountered exception with query", e);
		} catch (LoginException e) {
			log.debug(FAILED_RESOLVER_MESSAGE + e.getMessage());
		} finally {
			logout(session);
			close(resourceResolver);
		}
		return entityList;
	}

	/**
	 * Implement a query but create and close the session outside of this method.
	 * 
	 * @param <T>
	 * @param resourceResolver
	 * @param session
	 * @param sqs
	 * @param type
	 * @return
	 */
	private <T> List<T> internalQuery(ResourceResolver resourceResolver, Session session, SlingQueryStatement sqs,
			Class<T> type) {
		List<T> entityList = new ArrayList<>();
		try {
			if (session == null) {
				log.debug("entityBuilder's session was null.");
				return null;
			}
			NodeIterator it = getNodeIterator(session, sqs);
			if (it != null) {
				while (it != null && it.hasNext()) {
					Node node = it.nextNode();
					log.trace("entityBuilder node found: " + node.getPath());
					Resource nodeResource = resourceResolver.getResource(node.getPath());
					if (nodeResource != null) {
						T entity = nodeResource.adaptTo(type);
						if (entity != null) {
							entityList.add(entity);
						}
					}
				}				
			} else {
				log.trace("internalQuery's getNodeIterator() is null");
			}
		} catch (RepositoryException e) {
			log.debug("encountered exception with query: " + ExceptionUtils.getFullStackTrace(e));
		}
		return entityList;
	}

	public List<String> queryForPaths(SlingQueryStatement sqs) throws Exception {
		List<String> nodePaths = new ArrayList<>();

		Session session = null;
		try {
			session = this.slingRepository.loginService(ROOT_READ_USER, null);
			if (session == null) {
				throw new Exception("Unable to login session to obtain path");
			}
			NodeIterator it = getNodeIterator(session, sqs);
			while (it != null && it.hasNext()) {
				Node node = it.nextNode();
				nodePaths.add(node.getPath());
			}				
		} catch (RepositoryException e) {
			throw new Exception("Encountered exception with query", e);
		} finally {
			logout(session);
		}
		return nodePaths;
	}

    public List<String> queryForPathsUsingSelector(SlingQueryStatement sqs, String selector) throws Exception {
        List<String> nodePaths = new ArrayList<>();

        Session session = null;
        try {
            session = this.slingRepository.loginService(ROOT_READ_USER, null);
            if (session == null) {
                throw new Exception("Unable to login session to obtain path");
            }
            RowIterator it = getRowIterator(session, sqs);
            while (it != null && it.hasNext()) {
                Row row = it.nextRow();
                Node pageNode = row.getNode(selector);
                nodePaths.add(pageNode.getPath());
            }
        } catch (RepositoryException e) {
            throw new Exception("Encountered exception with query", e);
        } finally {
            logout(session);
        }
        return nodePaths;
    }

	/**
	 * Return a @SlingQueryStatement's NodeIterator from an existing session.
	 * 
	 * @param session session
	 * @param sqs slingQueryStatement
	 * @return node iterator
	 */
	NodeIterator getNodeIterator(Session session, SlingQueryStatement sqs) {
		NodeIterator nodeIter = null;
		
		if (sqs == null || StringUtils.isBlank(sqs.getQueryStatement())) {
			log.trace(">>> sqs for getNodeIterator was null or empty");
			return null;
		}		
		try {
			QueryManager queryManager = session.getWorkspace().getQueryManager();
			if (queryManager != null) {
				String queryStatement = sqs.compile();
				if (StringUtils.isNotBlank(queryStatement)) {
					Query query = queryManager.createQuery(queryStatement, sqs.getQueryLanguage());
					// log.trace(">>> createQuery() returns a query ...");					
					// log.trace(">>> statement: " + query.getStatement());
					// log.trace(">>> language: " + query.getLanguage());
					QueryResult results = null;
					if (query != null) {
						results = query.execute();
						if (results != null) {
							nodeIter = results.getNodes();
						}
					}					
				} else {
					log.trace("queryStatement was null from sqs.compile().");
				}
			} else {
				log.trace("QueryManager was null from session workspace.");
			}
		} catch (InvalidQueryException iqe) {
			log.debug("encountered InvalidQueryException with getNodeIterator(): " + ExceptionUtils.getFullStackTrace(iqe));
		} catch (ItemNotFoundException infe) {
			log.debug("encountered ItemNotFoundException with getNodeIterator(): " + ExceptionUtils.getFullStackTrace(infe));
		} catch (RepositoryException re) {
			log.debug("encountered RepositoryException with getNodeIterator(): " + ExceptionUtils.getFullStackTrace(re));
		} catch (NullPointerException npe) {
			log.debug("encountered NullPointerException with getNodeIterator(): " + ExceptionUtils.getFullStackTrace(npe));
		} catch (Exception e) {
			log.debug("encountered Exception with getNodeIterator(): " + ExceptionUtils.getFullStackTrace(e));
		}
		return nodeIter;
	}

    /**
     * Return a @SlingQueryStatement's RowIterator from an existing session.
     *
     * @param session session
     * @param sqs slingQueryStatement
     * @return row iterator
     */
    RowIterator getRowIterator(Session session, SlingQueryStatement sqs) {
        RowIterator rowIterator = null;

        if (sqs == null || StringUtils.isBlank(sqs.getQueryStatement())) {
            log.trace(">>> sqs for getRowIterator was null or empty");
            return null;
        }
        try {
            QueryManager queryManager = session.getWorkspace().getQueryManager();
            if (queryManager != null) {
                String queryStatement = sqs.compile();
                if (StringUtils.isNotBlank(queryStatement)) {
                    Query query = queryManager.createQuery(queryStatement, sqs.getQueryLanguage());
                    // log.trace(">>> createQuery() returns a query ...");
                    // log.trace(">>> statement: " + query.getStatement());
                    // log.trace(">>> language: " + query.getLanguage());
                    QueryResult results = null;
                    if (query != null) {
                        results = query.execute();
                        if (results != null) {
                            rowIterator = results.getRows();
                        }
                    }
                } else {
                    log.trace("queryStatement was null from sqs.compile().");
                }
            } else {
                log.trace("QueryManager was null from session workspace.");
            }
        } catch (InvalidQueryException iqe) {
            log.debug("encountered InvalidQueryException with getNodeIterator(): " + ExceptionUtils.getFullStackTrace(iqe));
        } catch (ItemNotFoundException infe) {
            log.debug("encountered ItemNotFoundException with getNodeIterator(): " + ExceptionUtils.getFullStackTrace(infe));
        } catch (RepositoryException re) {
            log.debug("encountered RepositoryException with getNodeIterator(): " + ExceptionUtils.getFullStackTrace(re));
        } catch (NullPointerException npe) {
            log.debug("encountered NullPointerException with getNodeIterator(): " + ExceptionUtils.getFullStackTrace(npe));
        } catch (Exception e) {
            log.debug("encountered Exception with getNodeIterator(): " + ExceptionUtils.getFullStackTrace(e));
        }
        return rowIterator;
    }

	@Override
	public <T> List<T> queryByTag(String rootPath, String tagName, Class<T> type) {
		ResourceResolver resourceResolver = null;
		List<T> entityList = new ArrayList<>();
		try {
			resourceResolver = resourceResolverFactory.getServiceResourceResolver(params);
			TagManager tagManager = resourceResolver.adaptTo(TagManager.class);
			String[] channelArray = new String[1];
			channelArray[0] = tagName;
			RangeIterator<Resource> childPageIterator = tagManager.find(rootPath, channelArray);
			while (childPageIterator != null && childPageIterator.hasNext()) {
				Resource resource = childPageIterator.next();
				T entity = resource.adaptTo(type);
				if (entity != null) {
					entityList.add(entity);
				}
			}
		} catch (LoginException e) {
			log.debug("Failed to acquire resourceResolver. " + e.getMessage());
		} finally {
			close(resourceResolver);
		}
		return entityList;
	}

	/**
	 * Adapt the current ResourceResolver to a Session object.
	 * 
	 * @param resourceResolver
	 * @return
	 */
	Session getSession(ResourceResolver resourceResolver) {
		if (resourceResolver.equals(null)) {
			log.trace("resourceResolver was null from getSession()!");
		}
		return resourceResolver.adaptTo(Session.class);
	}

	public boolean pathHasProperty(String path, String propertyName) {
		ResourceResolver resourceResolver = null;
		boolean hasProperty = false;
		if (StringUtils.isBlank(path) || StringUtils.isBlank(propertyName)) {
			return false;
		}
		try {
			resourceResolver = resourceResolverFactory.getServiceResourceResolver(params);
			Resource resource = resourceResolver.getResource(path);
			if (resource != null) {
				String value = resource.getValueMap().get(propertyName, String.class);
				if (StringUtils.isNotBlank(value)) {
					hasProperty = true;
				}
			}
		} catch (LoginException e) {
			log.debug(FAILED_RESOLVER_MESSAGE + e.getMessage());
		} finally {
			close(resourceResolver);
		}
		return hasProperty;
	}

	/**
	 * Get the String value of a property at a given path.
	 */
	public String getResourceProperty(String path, String propertyName) {
		ResourceResolver resourceResolver = null;
		if (StringUtils.isBlank(path) || StringUtils.isBlank(propertyName)) {
			return null;
		}
		try {
			resourceResolver = resourceResolverFactory.getServiceResourceResolver(params);
			Resource resource = resourceResolver.getResource(path);
			if (resource != null) {
				return resource.getValueMap().get(propertyName, String.class);
			}
		} catch (LoginException e) {
			log.debug(FAILED_RESOLVER_MESSAGE + e.getMessage());
		} finally {
			close(resourceResolver);
		}
		return null;
	}

	public String[] getResourceMultiProperty(String path, String propertyName) {
		ResourceResolver resourceResolver = null;
		if (StringUtils.isBlank(path) || StringUtils.isBlank(propertyName)) {
			return null;
		}
		try {
			resourceResolver = resourceResolverFactory.getServiceResourceResolver(params);
			Resource resource = resourceResolver.getResource(path);
			if (resource != null) {
				ValueMap valueMap = resource.getValueMap();
				if (valueMap != null && valueMap.containsKey(propertyName)) {
					Object value = valueMap.get(propertyName);
					if (value instanceof String[]) {
						return (String[]) value;
					} else if (value instanceof String) {
						return new String[] { (String) value };
					} else if (value instanceof String) {
						return new String[] { (String) value };
					}
				}
			}
		} catch (LoginException e) {
			log.debug(FAILED_RESOLVER_MESSAGE + e.getMessage());
		} finally {
			close(resourceResolver);
		}
		return null;
	}

	/**
	 * Fetches the specific property from all the matching nodes which has any of
	 * the given componentResourceTypes under this page.
	 * 
	 * Example: fetch articlePath from all the marquee video components or video
	 * component on this page
	 * /jcr:root/content/ccui/us/en//element(*,nt:unstructured)[@sling:resourceType='ccui/components/text'
	 * or @sling:resourceType='ccui/components/accordion']
	 *
	 * @param componentResourceTypes list of sling resourceTypes of the component
	 *                               node. Example:
	 *                               ccui/components/cp16/marquee-video
	 * 
	 * @param propertyName           name of the property. Example: articlePath
	 * 
	 * @return list of property values
	 */
	public List<String> getPropertyFromComponents(String path, List<String> componentResourceTypes,
			String propertyName) {
		List<String> properties = new ArrayList<>();
		if (StringUtils.isBlank(path) || StringUtils.isBlank(propertyName) || componentResourceTypes.equals(null) || componentResourceTypes.isEmpty()) {
			return properties;
		}
		String query = getMultiResourceXPathQuery(path, componentResourceTypes);
		SlingQueryStatement slingQueryStatement = new SlingQueryStatement(query, XPATH);
		ResourceResolver resourceResolver = null;
		Session session = null;
		log.trace("query= " + query);
		if (slingQueryStatement != null) {
			try {
				resourceResolver = resourceResolverFactory.getServiceResourceResolver(params);
				session = getSession(resourceResolver);
				// Use internalQuery() so we could iterate through
				// the results without the session getting shut down
				List<Node> nodes = this.internalQuery(resourceResolver, session, slingQueryStatement, Node.class);
				if (nodes != null && nodes.size() > 0) {
					for (Node node : nodes) {
						if (node.hasProperty(propertyName) && node.getProperty(propertyName).isMultiple()) {
							Value[] values = node.getProperty(propertyName).getValues();
							for (Value value : values) {
								properties.add(value.getString());
							}
						} else if (node.hasProperty(propertyName) && !node.getProperty(propertyName).isMultiple()) {
							properties.add(node.getProperty(propertyName).getString());
						}
					}
				} else {
					log.trace("No nodes found for: " + slingQueryStatement.getQueryStatement());
				}
			} catch (RepositoryException | LoginException err) {
				log.debug("Error in entityBuilder.getPropertyFromComponents(): " + ExceptionUtils.getFullStackTrace(err));
			} finally {
				logout(session);
				close(resourceResolver);
			}
		}			
		return properties;
	}

	private String getMultiResourceXPathQuery(String path, List<String> componentResourceTypes) {
		if (componentResourceTypes != null && !componentResourceTypes.isEmpty() && componentResourceTypes.size() > 0) {
			StringBuilder orCondition = new StringBuilder("@sling:resourceType='");
			for (int i = 0; i < componentResourceTypes.size(); i++) {
				orCondition.append(componentResourceTypes.get(i)).append("'");
				if (i != componentResourceTypes.size() - 1)
					orCondition.append(" or @sling:resourceType='");
			}
			return "/jcr:root" + path + "//element(*,nt:unstructured)[" + orCondition.toString() + "]";			
		}
		return "/jcr:root" + path + "//element(*,nt:unstructured)";		
	}
	
	private void close(ResourceResolver resResolver) {
		// log.trace("close(resourceResolver);");
		if (resResolver != null && resResolver.isLive()) {
			resResolver.close();
		}
	}
	
	private void logout(Session sess) {
		// log.trace("logout(session);");
		if (sess != null && sess.isLive()) {
			sess.logout();
		}
	}
}
