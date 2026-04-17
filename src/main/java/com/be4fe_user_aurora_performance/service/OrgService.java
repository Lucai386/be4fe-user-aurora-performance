package com.be4fe_user_aurora_performance.service;

import com.be4fe_user_aurora_performance.client.CoreApiClient;
import com.be4fe_user_aurora_performance.dto.org.OrgResponse;
import com.be4fe_user_aurora_performance.dto.org.OrgRowDto;
import com.be4fe_user_aurora_performance.enums.ErrorCode;
import com.be4fe_user_aurora_performance.principal.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.be4fe_user_aurora_performance.enums.AppConstants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrgService {

    private final CoreApiClient coreApiClient;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public OrgResponse getOrg(UserPrincipal userPrincipal) {
        if (!userPrincipal.isResolved()) {
            return OrgResponse.error(ErrorCode.NOT_AUTHORIZED, MSG_USER_NOT_AUTHORIZED);
        }

        String codiceIstat = userPrincipal.getCodiceIstat();
        if (codiceIstat == null || codiceIstat.isBlank()) {
            return OrgResponse.error(ErrorCode.NO_ENTE, MSG_NO_ENTE);
        }

        List<Map> strutture = coreApiClient.getStrutture();
        if (strutture.isEmpty()) {
            return OrgResponse.success(List.of(), List.of());
        }

        return buildOrgResponse(strutture, strutture);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    @SuppressWarnings({"unchecked", "rawtypes"})
    private OrgResponse buildOrgResponse(List<Map> strutture, List<Map> allStrutture) {
        List<OrgRowDto> rows = new ArrayList<>();
        Map<Integer, List<String>> levelMap = new HashMap<>();

        int minDepth = strutture.stream()
                .mapToInt(s -> calculateDepth(s, allStrutture))
                .min().orElse(0);

        for (Map s : strutture) {
            OrgRowDto row = toOrgRowDto(s);
            rows.add(row);
            int depth = calculateDepth(s, allStrutture) - minDepth;
            levelMap.computeIfAbsent(depth, k -> new ArrayList<>()).add(row.getId());
        }

        List<List<String>> levels = new ArrayList<>();
        int maxDepth = levelMap.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        for (int i = 0; i <= maxDepth; i++) {
            levels.add(levelMap.getOrDefault(i, List.of()));
        }
        return OrgResponse.success(rows, levels);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private OrgRowDto toOrgRowDto(Map s) {
        String id = "struct-" + s.get("id");
        String managerId = s.get("idParent") != null ? "struct-" + s.get("idParent") : null;

        String head = null;
        Object resp = s.get("responsabile");
        if (resp instanceof Map rm) {
            String nome = strVal(rm, "nome");
            String cognome = strVal(rm, "cognome");
            head = (nome != null && cognome != null) ? nome + " " + cognome
                    : (nome != null ? nome : cognome);
        }

        List<String> staffNames = new ArrayList<>();
        Object staffObj = s.get("staff");
        if (staffObj instanceof List staffList) {
            for (Object ss : staffList) {
                if (ss instanceof Map ssMap) {
                    Object userObj = ssMap.get("user");
                    if (userObj instanceof Map userMap) {
                        String nome = strVal(userMap, "nome");
                        String cognome = strVal(userMap, "cognome");
                        String fullName = (nome != null && cognome != null) ? nome + " " + cognome
                                : (nome != null ? nome : cognome);
                        if (fullName != null) staffNames.add(fullName);
                    }
                }
            }
        }

        return OrgRowDto.builder()
                .id(id)
                .label(strVal(s, "nome"))
                .manager(managerId)
                .role(strVal(s, "ruoloLabel"))
                .head(head)
                .staff(staffNames.isEmpty() ? null : staffNames)
                .color(strVal(s, "colore"))
                .build();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private int calculateDepth(Map struttura, List<Map> allStrutture) {
        Map<Object, Map> byId = new HashMap<>();
        for (Map s : allStrutture) byId.put(s.get("id"), s);

        int depth = 0;
        Object currentParentId = struttura.get("idParent");
        while (currentParentId != null && depth < 100) {
            depth++;
            Map parent = byId.get(currentParentId);
            if (parent == null) break;
            currentParentId = parent.get("idParent");
        }
        return depth;
    }

    private String strVal(Map<?, ?> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }

    private Integer intVal(Map<?, ?> map, String key) {
        Object v = map.get(key);
        if (v == null) return null;
        try {
            return Integer.parseInt(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
