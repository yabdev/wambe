package com.wambe.api.config;

import com.wambe.api.integration.scanner.GoogleScannerIdentityTokenProvider;
import com.wambe.api.integration.scanner.LocalScannerIdentityTokenProvider;
import com.wambe.api.integration.scanner.ScannerIdentityTokenProvider;
import java.io.IOException;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.Environment;

@Configuration
public class ScannerIdentityTokenConfiguration {

    @Bean
    @DependsOn("deployedSecurityValidator")
    ScannerIdentityTokenProvider scannerIdentityTokenProvider(
            Environment environment,
            @Value("${wambe.scanner.audience:}") String audience) throws IOException {
        if (isExplicitDevOnly(environment)) {
            return new LocalScannerIdentityTokenProvider();
        }
        return new GoogleScannerIdentityTokenProvider(audience);
    }

    static boolean isExplicitDevOnly(Environment environment) {
        String[] profiles = environment.getActiveProfiles();
        return profiles.length > 0
                && Arrays.stream(profiles)
                        .allMatch(profile -> "local".equals(profile) || "test".equals(profile));
    }
}
