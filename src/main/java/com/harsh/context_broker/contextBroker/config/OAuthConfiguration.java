package com.harsh.context_broker.contextBroker.config;

import com.harsh.context_broker.contextBroker.config.jiraoauth.JiraOAuthProperties;
import com.harsh.context_broker.contextBroker.config.slackoauth.SlackOAuthProperties;
import com.harsh.context_broker.contextBroker.frontend.AppProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({SlackOAuthProperties.class, AppProperties.class, JiraOAuthProperties.class})
public class OAuthConfiguration {
}
