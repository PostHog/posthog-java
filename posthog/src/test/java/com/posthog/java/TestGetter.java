package com.posthog.java;

import org.json.JSONObject;

import java.util.Map;

public class TestGetter implements Getter {

    private String jsonString;

    public String getJsonString() {
        if (jsonString == null) {
            return "{\n"
                    + "  \"flags\": [\n"
                    + "    {\n"
                    + "      \"id\": 1000,\n"
                    + "      \"team_id\": 20000,\n"
                    + "      \"name\": \"\",\n"
                    + "      \"key\": \"java-feature-flag\",\n"
                    + "      \"filters\": {\n"
                    + "        \"groups\": [\n"
                    + "          {\n"
                    + "            \"variant\": \"variant-2\",\n"
                    + "            \"properties\": [\n"
                    + "              {\n"
                    + "                \"key\": \"id\",\n"
                    + "                \"type\": \"cohort\",\n"
                    + "                \"value\": 17231\n"
                    + "              }\n"
                    + "            ],\n"
                    + "            \"rollout_percentage\": 39\n"
                    + "          },\n"
                    + "          {\n"
                    + "            \"variant\": null,\n"
                    + "            \"properties\": [\n"
                    + "              {\n"
                    + "                \"key\": \"distinct_id\",\n"
                    + "                \"type\": \"person\",\n"
                    + "                \"value\": [\n"
                    + "                  \"id-1\"\n"
                    + "                ],\n"
                    + "                \"operator\": \"exact\"\n"
                    + "              }\n"
                    + "            ],\n"
                    + "            \"rollout_percentage\": 100\n"
                    + "          },\n"
                    + "          {\n"
                    + "            \"variant\": \"variant-2\",\n"
                    + "            \"properties\": [\n"
                    + "              {\n"
                    + "                \"key\": \"distinct_id\",\n"
                    + "                \"type\": \"person\",\n"
                    + "                \"value\": \"a-value\",\n"
                    + "                \"operator\": \"icontains\"\n"
                    + "              }\n"
                    + "            ],\n"
                    + "            \"rollout_percentage\": 41\n"
                    + "          }\n"
                    + "        ],\n"
                    + "        \"payloads\": {\n"
                    + "          \"variant-1\": \"{\\\"something\\\": 1}\",\n"
                    + "          \"variant-2\": \"1\"\n"
                    + "        },\n"
                    + "        \"multivariate\": {\n"
                    + "          \"variants\": [\n"
                    + "            {\n"
                    + "              \"key\": \"variant-1\",\n"
                    + "              \"name\": \"\",\n"
                    + "              \"rollout_percentage\": 100\n"
                    + "            },\n"
                    + "            {\n"
                    + "              \"key\": \"variant-2\",\n"
                    + "              \"name\": \"with description\",\n"
                    + "              \"rollout_percentage\": 0\n"
                    + "            }\n"
                    + "          ]\n"
                    + "        }\n"
                    + "      },\n"
                    + "      \"deleted\": false,\n"
                    + "      \"active\": true,\n"
                    + "      \"ensure_experience_continuity\": false\n"
                    + "    }\n"
                    + "  ],\n"
                    + "  \"group_type_mapping\": {},\n"
                    + "  \"cohorts\": {}\n"
                    + "}";
        }
        return jsonString;
    }

    public void setJsonString(String jsonString) {
        this.jsonString = jsonString;
    }

    @Override
    public JSONObject get(String route, Map<String, String> headers) {
        return new JSONObject(this.getJsonString());
    }

}
