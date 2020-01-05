package org.aostw;

import org.apache.commons.text.StringSubstitutor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

public class Helpers {
    public static List<String> getAllMatches(Matcher matcher) {
        List<String> allMatches = new ArrayList<String>();

        for (int j = 1; j <= matcher.groupCount(); j++) {
            allMatches.add(matcher.group(j));
        }

        return allMatches;
    }


    private static Map<String, Object> toMap(List list) {
        Map<String, Object> map = new HashMap<>();

        // put every value list to Map
        for (Integer i = 0; i < list.size(); i++) {
            map.put(i.toString(), list.get(i));
        }

        return map;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, String> flatMap(String parentKey, Map<String, Object> nestedMap) {
        Map<String, String> flatMap = new HashMap<String, String>();
        String prefixKey = parentKey != null ? parentKey + "." : "";

        for (Map.Entry<String, Object> entry : nestedMap.entrySet()) {
            Object value = entry.getValue();

            if (value instanceof String) {
                flatMap.put(prefixKey + entry.getKey(), (String) value);
            }
            if (value instanceof Map) {
                flatMap.putAll(flatMap(prefixKey + entry.getKey(), (Map<String, Object>) value));
            }
            if (value instanceof List) {
                flatMap.putAll(flatMap(prefixKey + entry.getKey(), Helpers.toMap((List) value)));
            }
        }
        return flatMap;
    }


    public static String getClientStyle() throws IOException {
        return new String(readAllBytes("web/client.css"));
    }

    public static byte[] readAllBytes(String path) throws IOException {
        return Files.readAllBytes(Paths.get(path));
    }

    public static String getSoapEnvelope(String path, String requestName, Map<String, Object> valuesMap) {
        try {
            String templateString = new String(Helpers.readAllBytes(path + requestName + ".xml"));
            StringSubstitutor sub = new StringSubstitutor(Helpers.flatMap(null, valuesMap));

            return sub.replace(templateString);
        } catch (IOException e) {
            return "";
        }
    }
}
