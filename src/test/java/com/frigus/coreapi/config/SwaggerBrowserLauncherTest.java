package com.frigus.coreapi.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class SwaggerBrowserLauncherTest {

    private final String originalOs = System.getProperty("os.name");
    private final String originalHeadless = System.getProperty("java.awt.headless");

    @AfterEach
    void tearDown() {
        if (originalOs != null) {
            System.setProperty("os.name", originalOs);
        }
        if (originalHeadless != null) {
            System.setProperty("java.awt.headless", originalHeadless);
        } else {
            System.clearProperty("java.awt.headless");
        }
    }

    @Test
    void shouldNotLaunchWhenOpenOnStartupIsFalse() {
        SwaggerBrowserLauncher launcher = new SwaggerBrowserLauncher();
        ReflectionTestUtils.setField(launcher, "openOnStartup", false);
        ReflectionTestUtils.setField(launcher, "serverPort", "8080");

        assertDoesNotThrow(launcher::openSwagger);
    }

    @Test
    void shouldNotLaunchWhenOnLinuxOrNonWindows() {
        System.setProperty("os.name", "Linux");
        SwaggerBrowserLauncher launcher = new SwaggerBrowserLauncher();
        ReflectionTestUtils.setField(launcher, "openOnStartup", true);
        ReflectionTestUtils.setField(launcher, "serverPort", "8080");

        assertDoesNotThrow(launcher::openSwagger);
    }

    @Test
    void shouldNotLaunchWhenHeadless() {
        System.setProperty("os.name", "Windows 11");
        System.setProperty("java.awt.headless", "true");
        SwaggerBrowserLauncher launcher = new SwaggerBrowserLauncher();
        ReflectionTestUtils.setField(launcher, "openOnStartup", true);
        ReflectionTestUtils.setField(launcher, "serverPort", "8080");

        assertDoesNotThrow(launcher::openSwagger);
    }
}
