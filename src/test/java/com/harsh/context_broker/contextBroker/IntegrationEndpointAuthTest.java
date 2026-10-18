//package com.harsh.context_broker.contextBroker;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.web.client.TestRestTemplate;
//import org.springframework.http.ResponseEntity;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
//
//@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
//public class IntegrationEndpointAuthTest {
//
//    @Autowired
//    private TestRestTemplate restTemplate;
//
//    @Test
//    void unauthenticatedRequestsAreRejected() {
//        ResponseEntity<String> resp = restTemplate.getForEntity("/integrations", String.class);
//        assertThat(resp.getStatusCode().value()).isEqualTo(401);
//    }
//}