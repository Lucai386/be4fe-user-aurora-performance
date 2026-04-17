package com.be4fe_user_aurora_performance.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.be4fe_user_aurora_performance.dto.org.OrgResponse;
import com.be4fe_user_aurora_performance.principal.UserPrincipal;
import com.be4fe_user_aurora_performance.service.OrgService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class OrgController {

    private final OrgService orgService;
    private final UserPrincipal userPrincipal;

    @PostMapping("/readOrg")
    public ResponseEntity<OrgResponse> readOrg() {
        log.info("Reading org chart for authenticated user");
        return ResponseEntity.ok(orgService.getOrg(userPrincipal));
    }

    @PostMapping("/readMyOrg")
    public ResponseEntity<OrgResponse> readMyOrg() {
        log.info("Reading user subtree org chart for authenticated user");
        return ResponseEntity.ok(orgService.getMyOrg(userPrincipal));
    }
}
