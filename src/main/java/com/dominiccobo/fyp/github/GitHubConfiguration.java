package com.dominiccobo.fyp.github;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class GitHubConfiguration {

    private static final String GITHUB_COM_BASE_URI = "https://api.github.com";

    @Bean()
    @Scope("prototype")
    RestTemplate restTemplate(@Value("${oAuthToken}") String oAuthToken) {

        DefaultUriBuilderFactory uriBuilderFactory = new DefaultUriBuilderFactory(GITHUB_COM_BASE_URI);

        RestTemplate template = new RestTemplateBuilder()
                .uriTemplateHandler(uriBuilderFactory)
                .build();
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add(new LoggingInterceptor());
        interceptors.add(new HeaderInterceptor("Authorization", "token " + oAuthToken));
        template.setInterceptors(interceptors);
        return template;
    }

    public static class LoggingInterceptor implements ClientHttpRequestInterceptor {

        private static final Logger LOG = LoggerFactory.getLogger(LoggingInterceptor.class);

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            LOG.debug("Sending {} request to {}", request.getMethod(), request.getURI());
            return execution.execute(request, body);
        }
    }

    public static class HeaderInterceptor implements ClientHttpRequestInterceptor {

        private final String key;
        private final String value;

        public HeaderInterceptor(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            request.getHeaders().set(key, value);
            return execution.execute(request, body);
        }
    }
}
