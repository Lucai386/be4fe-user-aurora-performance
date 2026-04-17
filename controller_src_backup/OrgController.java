package com.bff_user_aurora_performance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bff_user_aurora_performance.dto.org.OrgResponse;
import com.bff_user_aurora_performance.service.OrgService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class OrgController {

    private final OrgService orgService;

    @PostMapping("/readOrg")
    public ResponseEntity<OrgResponse> readOrg(Authentication authentication) {
        log.info("Reading org chart for authenticated user");
        OrgResponse response = orgService.getOrg(authentication);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/readMyOrg")
    public ResponseEntity<OrgResponse> readMyOrg(Authentication authentication) {
        log.info("Reading user subtree org chart for authenticated user");
        OrgResponse response = orgService.getMyOrg(authentication);
        return ResponseEntity.ok(response);
    }
}
