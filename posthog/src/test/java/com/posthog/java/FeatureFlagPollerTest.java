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

    private FeatureFlagPoller sut;

    @Before
    public void setUp() {
        TestGetter testGetter = new TestGetter();
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
}
