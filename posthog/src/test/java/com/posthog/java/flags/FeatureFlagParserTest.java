package com.posthog.java.flags;

import org.json.JSONObject;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class FeatureFlagParserTest {

    @Test
    public void testParse() {
        // Arrange
        String jsonString = "{\n"
            + "  \"flags\": [\n"
            + "    {\n"
            + "      \"id\": 1000,\n"
            + "      \"team_id\": 20000,\n"
            + "      \"name\": \"\",\n"
            + "      \"key\": \"java-feature-flag\",\n"
            + "      \"filters\": {\n"
            + "        \"groups\": [\n"
            + "          {\n"
            + "            \"variant\": \"variant-1\",\n"
            + "            \"properties\": [\n"
            + "              {\n"
            + "                \"key\": \"distinct_id\",\n"
            + "                \"type\": \"person\",\n"
            + "                \"value\": \"is_set\",\n"
            + "                \"operator\": \"is_set\"\n"
            + "              }\n"
            + "            ],\n"
            + "            \"rollout_percentage\": 53\n"
            + "          },\n"
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
            + "                  \"\\\"id-1\\\"\"\n"
            + "                ],\n"
            + "                \"operator\": \"exact\"\n"
            + "              }\n"
            + "            ],\n"
            + "            \"rollout_percentage\": 30\n"
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

        // Act
        FeatureFlags flags = FeatureFlagParser.parse(new JSONObject(jsonString));

        // Assert
        assertNotNull(flags);
        assertEquals(1, flags.getFlags().size());
        FeatureFlag flag = flags.getFlags().get(0);
        assertEquals(1000, flag.getId());
        assertEquals(20000, flag.getTeamId());
        assertEquals("java-feature-flag", flag.getKey());
        assertTrue(flag.isActive());
        assertFalse(flag.isSimpleFlag());
        assertFalse(flag.isEnsureExperienceContinuity());
        assertFalse(flag.isSimpleFlag());
        assertFalse(flag.isDeleted());

        assertTrue(flag.getFilter().isPresent());
        final FeatureFlagFilter filter = flag.getFilter().get();
        assertEquals(0, filter.getAggregationGroupTypeIndex());
        assertEquals(4, filter.getGroups().size());
        assertEquals(2, filter.getPayloads().size());

        final List<FeatureFlagCondition> groups = filter.getGroups();
        assertEquals(4, groups.size());
        assertTrue(groups.get(0).getVariant().isPresent());
        assertEquals("variant-1", groups.get(0).getVariant().get());
        assertEquals(1, groups.get(0).getProperties().size());
        assertEquals(53, groups.get(0).getRolloutPercentage());
        assertFalse(groups.get(0).getProperties().get(0).getValue().isEmpty());
        assertTrue(groups.get(0).getProperties().get(0).getType().isPresent());
        assertEquals("person", groups.get(0).getProperties().get(0).getType().get());

        assertTrue(groups.get(1).getVariant().isPresent());
        assertEquals("variant-2", groups.get(1).getVariant().get());
        assertEquals(1, groups.get(1).getProperties().size());
        assertEquals(39, groups.get(1).getRolloutPercentage());
        assertFalse(groups.get(1).getProperties().get(0).getValue().isEmpty());
        assertTrue(groups.get(1).getProperties().get(0).getType().isPresent());
        assertEquals("cohort", groups.get(1).getProperties().get(0).getType().get());

        assertTrue(filter.getMultivariate().isPresent());
        final FeatureFlagVariants multivariate = filter.getMultivariate().get();
        assertEquals(2, multivariate.getVariants().size());

        final FeatureFlagVariant variant1 = multivariate.getVariants().get(0);
        assertEquals("variant-1", variant1.getKey());
        assertEquals("", variant1.getName());
        assertEquals(100, variant1.getRolloutPercentage());

        final FeatureFlagVariant variant2 = multivariate.getVariants().get(1);
        assertEquals("variant-2", variant2.getKey());
        assertEquals("with description", variant2.getName());
        assertEquals(0, variant2.getRolloutPercentage());

        final FeatureFlagCondition group = filter.getGroups().get(0);
        assertTrue(group.getVariant().isPresent());
        assertEquals("variant-1", group.getVariant().get());
        assertEquals(1, group.getProperties().size());
        assertEquals("distinct_id", group.getProperties().get(0).getKey());

        final FeatureFlagProperty property = group.getProperties().get(0);
        assertTrue(property.getOperator().isPresent());
        assertEquals(FeatureFlagPropertyOperator.IS_SET, property.getOperator().get());
        assertFalse(property.getValue().isEmpty());
        assertEquals(Collections.singletonList("is_set"), property.getValue());
        assertTrue(property.getType().isPresent());
        assertEquals("person", property.getType().get());
        assertEquals("distinct_id", property.getKey());

        assertEquals(2, filter.getPayloads().size());
        assertTrue(filter.getPayloads().containsKey("variant-1"));
        assertEquals("{\"something\": 1}", filter.getPayloads().get("variant-1"));

        assertTrue(filter.getPayloads().containsKey("variant-2"));
        assertEquals("1", filter.getPayloads().get("variant-2"));
    }

}
