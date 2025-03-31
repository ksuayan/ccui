package com.suayan.core.extractor;

import java.io.Reader;

/**
 * Read a file in the JCR and read the value as a String.
 * 
 * @author Kyo Suayan
 *
 */
public interface JcrReader {
    public void setFilepath(String filepath);
    public String readToString();
    public Reader getReaderFromJcrFile();
}