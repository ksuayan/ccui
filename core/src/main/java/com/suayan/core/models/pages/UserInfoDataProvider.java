package com.suayan.core.models.pages;

import java.security.Principal;

import org.apache.sling.api.SlingHttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.cq.sightly.WCMUsePojo;

public class UserInfoDataProvider extends WCMUsePojo {
  private SlingHttpServletRequest request;
  
  private final Logger logger = LoggerFactory.getLogger(UserInfoDataProvider.class);
  
  @Override
  public void activate() throws Exception {
	  logger.trace("UserInfoDataProvider activated.");
  }
    
  public String getUserId() {
	  request = getRequest();
	  Principal currentUserPrincipal = request.getUserPrincipal();
	  return currentUserPrincipal.getName();	  	  
  }
}