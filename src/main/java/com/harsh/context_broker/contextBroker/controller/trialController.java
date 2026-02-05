package com.harsh.context_broker.contextBroker.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class trialController {
    @PostMapping("/postMsg")
    public String incomingMsg(@RequestBody Map<String, Object> payload){
        System.out.println("message recieved");
        System.out.println("payload" + payload);
        return "recieved payload"+ payload;
    }
}
