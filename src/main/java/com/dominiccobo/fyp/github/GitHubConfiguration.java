package com.dominiccobo.fyp.github;

import org.kohsuke.github.GitHub;
import org.kohsuke.github.GitHubBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class GitHubConfiguration {

    @Bean
    public GitHub gitHub(@Value("${oAuthToken}") String oAuthToken) throws IOException {
        return new GitHubBuilder().withOAuthToken(oAuthToken).build();
    }
}
