package com.suayan.core.utils;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

/**
 * Given a JSON string, return the comma delimited list using 
 * properties as column.
 * 
 * @author Kyo Suayan
 */
public class CsvUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final CsvMapper csvMapper = new CsvMapper();

    public static String jsonArrayToCsv(String jsonArrayString) throws IOException {
        if (jsonArrayString == null || jsonArrayString.isEmpty()) {
            throw new IOException("Please pass a valid JSON array string");
        }

        // Convert JSON array string to JsonNode
        JsonNode jsonNode = objectMapper.readTree(jsonArrayString);
        if (!jsonNode.isArray()) {
            throw new IOException("Input is not a valid JSON array");
        }

        // Create CSV schema based on the first element of the array
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        String csvOutput = csvMapper.writer(schema).writeValueAsString(jsonNode);

        return csvOutput;
    }
}
