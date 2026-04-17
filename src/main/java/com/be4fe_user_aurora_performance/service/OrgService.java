package com.be4fe_user_aurora_performance.service;

import com.be4fe_user_aurora_performance.client.CoreApiClient;
import com.be4fe_user_aurora_performance.dto.org.OrgResponse;
import com.be4fe_user_aurora_performance.dto.org.OrgRowDto;
import com.be4fe_user_aurora_performance.enums.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.be4fe_user_aurora_performance.enums.AppConstants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrgService {

    private final CoreApiClient coreApiClient;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public OrgResponse getOrg(Authentication authentication) {
        Map currentUser = getCurrentUserMap(authentication);
        if (currentUser == null) {
            return OrgResponse.error(ErrorCode.NOT_AUTHORIZED, MSG_USER_NOT_AUTHORIZED);
        }

        String codiceIstat = strVal(currentUser, "codiceIstat");
        if (codiceIstat == null || codiceIstat.isBlank()) {
            return OrgResponse.error(ErrorCode.NO_ENTE, MSG_NO_ENTE);
        }

        List<Map> strutture = coreApiClient.getStrutture(codiceIstat.trim());
        if (strutture.isEmpty()) {
            return OrgResponse.success(List.of(), List.of());
        }

        return buildOrgResponse(strutture, strutture);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public OrgResponse getMyOrg(Authentication authentication) {
        Map currentUser = getCurrentUserMap(authentication);
        if (currentUser == null) {
            return OrgResponse.error(ErrorCode.NOT_AUTHORIZED, MSG_USER_NOT_AUTHORIZED);
        }

        String codiceIstat = strVal(currentUser, "codiceIstat");
        if (codiceIstat == null || codiceIstat.isBlank()) {
            return OrgResponse.error(ErrorCode.NO_ENTE, MSG_NO_ENTE);
        }

        Integer userId = currentUser.get("id") != null
                ? Integer.parseInt(currentUser.get("id").toString()) : null;
        if (userId == null) {
            return OrgResponse.success(List.of(), List.of());
        }

        List<Map> allStrutture = coreApiClient.getStrutture(codiceIstat.trim());
        if (allStrutture.isEmpty()) {
            return OrgResponse.success(List.of(), List.of());
        }

        // Trova la struttura radice dell'utente
        Integer rootId = findUserStrutturaId(userId, allStrutture);
        if (rootId == null) {
            return OrgResponse.success(List.of(), List.of());
        }

        // Raccoglie il sotto-albero
        Map<Object, Map> byId = new HashMap<>();
        Map<Object, List<Map>> childrenMap = new HashMap<>();
        for (Map s : allStrutture) {
            byId.put(s.get("id"), s);
            Object parentId = s.get("idParent");
            if (parentId != null) {
                childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(s);
            }
        }

        List<Map> subtree = new ArrayList<>();
        collectSubtree(rootId, byId, childrenMap, subtree);

        if (subtree.isEmpty()) {
            return OrgResponse.success(List.of(), List.of());
        }

        return buildOrgResponse(subtree, allStrutture);
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
    private Integer findUserStrutturaId(Integer userId, List<Map> allStrutture) {
        String userIdStr = userId.toString();
        // Prima cerca come responsabile
        for (Map s : allStrutture) {
            if (userIdStr.equals(strVal(s, "idResponsabile"))) return intVal(s, "id");
        }
        // Poi cerca come staff
        for (Map s : allStrutture) {
            Object staffObj = s.get("staff");
            if (!(staffObj instanceof List)) continue;
            for (Object ss : (List<?>) staffObj) {
                if (ss instanceof Map ssMap && userIdStr.equals(strVal(ssMap, "idUser"))) {
                    return intVal(s, "id");
                }
            }
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void collectSubtree(Integer nodeId, Map<Object, Map> byId,
                                Map<Object, List<Map>> childrenMap, List<Map> result) {
        Map node = byId.get(nodeId);
        if (node == null) node = byId.get((long) nodeId);
        if (node == null) return;
        result.add(node);
        List<Map> children = childrenMap.get(nodeId);
        if (children == null) children = childrenMap.get((long) nodeId);
        if (children != null) {
            for (Map child : children) {
                Integer childId = intVal(child, "id");
                if (childId != null) collectSubtree(childId, byId, childrenMap, result);
            }
        }
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

    @SuppressWarnings("unchecked")
    private Map getCurrentUserMap(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return null;
        Jwt jwt = (Jwt) authentication.getPrincipal();
        return coreApiClient.getUserByKeycloakId(jwt.getSubject()).orElse(null);
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
