package com.suayan.core.extractor;

import java.util.List;
import org.json.JSONObject;

public interface JsonTransformer {
    public List<JSONObject> transform(List<JSONObject> jsonList);
}
