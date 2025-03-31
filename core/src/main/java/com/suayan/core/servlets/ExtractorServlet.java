package com.suayan.core.servlets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.suayan.core.services.ScannerService;
import com.suayan.core.utils.CsvUtils;
/**
 * An API endpoint for running JCR Queries defined under /apps/ccui/extractor.
 * 
 * Trigger by:
 * /bin/api/extract.json?conf=name_of_json_config
 * To generate CSV instead:
 * /bin/api/extract.csv?conf=name_of_json_config
 * 
 * @author Kyo Suayan
 *
 */        
@Component(
    service = Servlet.class,
    property = {
        "service.description=Extractor Servlet",
        "service.vendor=CCUI",
        "sling.servlet.extensions=json,csv",
        "sling.servlet.paths=/bin/api/extract"
    },
    scope = ServiceScope.SINGLETON
)    
public class ExtractorServlet extends SlingAllMethodsServlet {
    private static final long serialVersionUID = 78634124L;
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Reference
    private ScannerService scanner;

	@Override
	protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServletException, IOException {

		String conf = request.getParameter("conf");
		String extension = request.getRequestPathInfo().getExtension();
		Writer w = response.getWriter();
		if (StringUtils.isEmpty(conf)) {
			response.setContentType("application/json");
			w.write("{\"error\":\"Please provide a conf parameter.\"}");
		} else {
			List<JSONObject> results = null;
			results = scanner.queryByJsonConfig(conf, request.getParameterMap());
			JSONArray jsonList = new JSONArray(results);
			if (results == null || results.size() == 0) {
				response.setContentType("application/json");
				w.write("{\"error\":\"No results found.\"}");
			} else if (extension.toLowerCase().equals("csv")) {
				response.setContentType("text/csv");
				w.write(CsvUtils.jsonArrayToCsv(jsonList.toString()));
			} else {
				response.setContentType("application/json");
				w.write(jsonList.toString());
			}
		}
		w.close();
	}
    
	@Override
	protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServletException, IOException {

		String extension = request.getRequestPathInfo().getExtension();
		Writer w = response.getWriter();
		JSONObject jsonPostData = null;
		try {
			jsonPostData = new JSONObject(getPostData(request));
			List<JSONObject> results = null;
			results = scanner.queryByJsonObject(jsonPostData, request.getParameterMap());
			JSONArray jsonList = new JSONArray(results);
			if (results == null || results.size() == 0) {
				response.setContentType("application/json");
				w.write("{\"error\":\"No results found.\"}");
			} else if (extension.toLowerCase().equals("csv")) {
				response.setContentType("text/csv");
				w.write(CsvUtils.jsonArrayToCsv(jsonList.toString()));
			} else {
				response.setContentType("application/json");
				w.write(jsonList.toString());
			}
		} catch (JSONException e) {
			throw new IOException("Error parsing JSON request string from POST call.");
		}
		w.close();
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
    
}
