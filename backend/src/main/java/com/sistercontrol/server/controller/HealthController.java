package com.sistercontrol.server.controller;

import com.sistercontrol.server.dto.ApiDtos.OkResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public OkResponse health() {
        return OkResponse.success();
    }
}
