package com.posthog.java;

import com.posthog.java.TestGetter;
import com.posthog.java.flags.FeatureFlag;
import com.posthog.java.flags.FeatureFlagConfig;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

public class FeatureFlagPollerTest {

    private TestGetter testGetter;
    private FeatureFlagPoller sut;

    @Before
    public void setUp() {
        testGetter = new TestGetter();
        sut = new FeatureFlagPoller.Builder("", "", testGetter)
                .build();

        sut.poll();
    }

    @Test
    public void shouldRetrieveAllFlags() {
        final List<FeatureFlag> flags = sut.getFeatureFlags();
        assertEquals(1, flags.size());
        assertEquals("java-feature-flag", flags.get(0).getKey());
        assertEquals(1000, flags.get(0).getId());
        assertEquals(20000, flags.get(0).getTeamId());
    }

    @Test
    public void shouldReturnTrueWhenFeatureFlagIsEnabledForUser() {
        FeatureFlagConfig config = new FeatureFlagConfig.Builder("java-feature-flag", "id-1")
                .build();

        final boolean enabled = sut.isFeatureFlagEnabled(config);
        assertTrue(enabled);
    }

    @Test
    public void shouldReturnFalseWhenFeatureFlagIsDisabledForUser() {
        FeatureFlagConfig config = new FeatureFlagConfig.Builder("java-feature-flag", "some-id")
                .build();

        final boolean enabled = sut.isFeatureFlagEnabled(config);
        assertFalse(enabled);
    }

    @Test
    public void shouldReturnFeatureFlagVariant() {
        FeatureFlagConfig config = new FeatureFlagConfig.Builder("java-feature-flag", "id-1")
                .build();

        final Optional<String> variant = sut.getFeatureFlagVariant(config);
        assertTrue(variant.isPresent());
    }

    @Test
    public void shouldBeAbleToReturnTheFullFeatureFlag() {
        FeatureFlagConfig config = new FeatureFlagConfig.Builder("java-feature-flag", "id-1")
                .build();

        final Optional<FeatureFlag> flag = sut.getFeatureFlag(config);
        assertTrue(flag.isPresent());
        assertEquals("java-feature-flag", flag.get().getKey());
        assertEquals(1000, flag.get().getId());
        assertEquals(20000, flag.get().getTeamId());
    }

    @Test
    public void reloadFeatureFlags() {
        final List<FeatureFlag> flags = sut.getFeatureFlags();
        assertEquals(1, flags.size());
        assertEquals("java-feature-flag", flags.get(0).getKey());
        assertEquals(1000, flags.get(0).getId());
        assertEquals(20000, flags.get(0).getTeamId());


        testGetter.setJsonString(
                "{\n"
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
                        + "    },\n"
                        + "    {\n"
                        + "      \"id\": 1001,\n"
                        + "      \"team_id\": 20000,\n"
                        + "      \"name\": \"\",\n"
                        + "      \"key\": \"java-feature-flag-2\",\n"
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
                        + "}"
        );

        sut.forceReload();

        final List<FeatureFlag> flags2 = sut.getFeatureFlags();
        assertEquals(2, flags2.size());
        assertEquals("java-feature-flag", flags2.get(0).getKey());
        assertEquals(1000, flags2.get(0).getId());
        assertEquals(20000, flags2.get(0).getTeamId());
        assertEquals("java-feature-flag-2", flags2.get(1).getKey());
        assertEquals(1001, flags2.get(1).getId());
        assertEquals(20000, flags2.get(1).getTeamId());
    }

}
