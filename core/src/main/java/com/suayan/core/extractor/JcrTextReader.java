package com.suayan.core.extractor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple JCR text file reader.
 * 
 * @author Kyo Suayan
 *
 */
@Component(immediate = true)
public class JcrTextReader implements JcrReader {

    @Reference
    private SlingRepository repository;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private Session session = null;
    private Node rootNode = null;
    private String filepath = null;

    /**
     * OSGi Activate.
     */
    @Activate
    protected void activate() {
        try {
            this.session = this.repository.loginService("ccui-root-read-user", null);
            rootNode = this.session.getRootNode();
        } catch (Exception e) {
            log.error("error >>> " + e.getMessage());
        }
    }

    /**
     * OSGi Deactivate.
     */
    @Deactivate
    protected void deactivate() {
        if (this.session != null) {
            this.session.logout();
        }
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public String readToString() {
        StringBuilder sb = new StringBuilder();
        try {
            Reader fileReader = getReaderFromJcrFile();
            if (fileReader != null) {
                BufferedReader in = new BufferedReader(fileReader);
                String line;
                int row = 0;
                while ((line = in.readLine()) != null) {
                    sb.append(line);
                    row++;
                }
                fileReader.close();
                log.info(">> lines read: " + row);
            } else {
                log.error(">> fileReader was null.");
            }
        } catch (IOException e) {
            log.error(">> Error: readToString " + e.getMessage());
            e.printStackTrace();
        }
        return sb.toString();
    }

    public Reader getReaderFromJcrFile() {
        Reader reader = null;
        try {
            Node fileContent = rootNode.getNode(filepath).getNode("jcr:content");
            Binary bin = null;
            // try to locate where jcr:data is stored based on JCR primaryType.
            String primaryType = fileContent.getProperty("jcr:primaryType").getValue().getString();
            String damAssetData = "renditions/original/jcr:content/jcr:data";
            String fileData = "jcr:data";
            log.info("Reading file {} of type {}", filepath, primaryType);
            if (primaryType.equals("dam:AssetContent") && fileContent.hasProperty(damAssetData)) {
                bin = fileContent.getProperty(damAssetData).getBinary();
            } else if (primaryType.equals("nt:resource") && fileContent.hasProperty(fileData)) {
                bin = fileContent.getProperty(fileData).getBinary();
            }
            if (bin == null) {
                log.error("Unable to read file from JCR: {}", filepath);
                return null;
            }
            InputStream content = bin.getStream();
            reader = new InputStreamReader(content);
            bin.dispose();
        } catch (RepositoryException e) {
            log.error("RepositoryException in getReaderFromJcrFile " + e.getMessage());
        }
        return reader;
    }
}
