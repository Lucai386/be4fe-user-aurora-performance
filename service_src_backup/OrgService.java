package com.bff_user_aurora_performance.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import com.bff_user_aurora_performance.dto.org.OrgResponse;
import com.bff_user_aurora_performance.dto.org.OrgRowDto;
import com.bff_user_aurora_performance.enums.ErrorCode;
import com.bff_user_aurora_performance.model.Struttura;
import com.bff_user_aurora_performance.model.StrutturaStaff;
import com.bff_user_aurora_performance.model.User;
import com.bff_user_aurora_performance.repository.StrutturaRepository;
import com.bff_user_aurora_performance.repository.StrutturaStaffRepository;
import com.bff_user_aurora_performance.repository.UserRepository;

import static com.bff_user_aurora_performance.enums.AppConstants.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrgService {

    private final StrutturaRepository strutturaRepository;
    private final StrutturaStaffRepository strutturaStaffRepository;
    private final UserRepository userRepository;

    /**
     * Recupera l'organigramma per il codice ISTAT dell'utente corrente.
     * Le strutture radice (senza parent) sono al livello 0.
     */
    public OrgResponse getOrg(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return OrgResponse.error(ErrorCode.NOT_AUTHORIZED, MSG_USER_NOT_AUTHORIZED);
        }

        String codiceIstat = currentUser.getCodiceIstat();
        if (codiceIstat == null || codiceIstat.isBlank()) {
            return OrgResponse.error(ErrorCode.NO_ENTE, MSG_NO_ENTE);
        }

        List<Struttura> strutture = strutturaRepository.findByCodiceIstatWithStaff(codiceIstat.trim());

        if (strutture.isEmpty()) {
            log.info("No structures found for codiceIstat: {}", codiceIstat);
            return OrgResponse.success(List.of(), List.of());
        }

        List<OrgRowDto> rows = new ArrayList<>();
        Map<Integer, List<String>> levelMap = new HashMap<>();

        for (Struttura s : strutture) {
            OrgRowDto row = toOrgRowDto(s);
            rows.add(row);

            int depth = calculateDepth(s, strutture);
            levelMap.computeIfAbsent(depth, k -> new ArrayList<>()).add(row.getId());
        }

        // Costruisci i livelli ordinati
        List<List<String>> levels = new ArrayList<>();
        int maxDepth = levelMap.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        for (int i = 0; i <= maxDepth; i++) {
            levels.add(levelMap.getOrDefault(i, List.of()));
        }

        log.info("Loaded org chart with {} structures for codiceIstat: {}", rows.size(), codiceIstat);
        return OrgResponse.success(rows, levels);
    }

    /**
     * Recupera solo il sotto-albero dell'utente corrente:
     * - Se è responsabile di una struttura → da quella in giù
     * - Se è nello staff di una struttura → da quella in giù
     * - Altrimenti → lista vuota
     */
    public OrgResponse getMyOrg(Authentication authentication) {
        User currentUser = getCurrentUser(authentication);
        if (currentUser == null) {
            return OrgResponse.error(ErrorCode.NOT_AUTHORIZED, MSG_USER_NOT_AUTHORIZED);
        }

        String codiceIstat = currentUser.getCodiceIstat();
        if (codiceIstat == null || codiceIstat.isBlank()) {
            return OrgResponse.error(ErrorCode.NO_ENTE, MSG_NO_ENTE);
        }

        // Trova la struttura dell'utente (come responsabile o come staff)
        Integer userStrutturaId = findUserStrutturaId(currentUser);
        if (userStrutturaId == null) {
            log.info("User {} has no associated struttura", currentUser.getId());
            return OrgResponse.success(List.of(), List.of());
        }

        // Carica tutte le strutture dell'ente
        List<Struttura> allStrutture = strutturaRepository.findByCodiceIstatWithStaff(codiceIstat.trim());
        if (allStrutture.isEmpty()) {
            return OrgResponse.success(List.of(), List.of());
        }

        // Filtra: prendi solo il sotto-albero dalla struttura dell'utente in giù
        Map<Integer, Struttura> byId = new HashMap<>();
        Map<Integer, List<Struttura>> childrenMap = new HashMap<>();
        for (Struttura s : allStrutture) {
            byId.put(s.getId(), s);
            if (s.getIdParent() != null) {
                childrenMap.computeIfAbsent(s.getIdParent(), k -> new ArrayList<>()).add(s);
            }
        }

        // Raccogli ricorsivamente il sotto-albero
        List<Struttura> subtree = new ArrayList<>();
        collectSubtree(userStrutturaId, byId, childrenMap, subtree);

        if (subtree.isEmpty()) {
            return OrgResponse.success(List.of(), List.of());
        }

        // Converti in DTO
        List<OrgRowDto> rows = new ArrayList<>();
        Map<Integer, List<String>> levelMap = new HashMap<>();

        // La radice del sotto-albero parte da depth 0
        int rootDepth = calculateDepth(subtree.get(0), allStrutture);

        for (Struttura s : subtree) {
            OrgRowDto row = toOrgRowDto(s);
            rows.add(row);

            int depth = calculateDepth(s, allStrutture) - rootDepth;
            levelMap.computeIfAbsent(depth, k -> new ArrayList<>()).add(row.getId());
        }

        List<List<String>> levels = new ArrayList<>();
        int maxDepth = levelMap.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        for (int i = 0; i <= maxDepth; i++) {
            levels.add(levelMap.getOrDefault(i, List.of()));
        }

        log.info("Loaded user subtree with {} structures for user {}", rows.size(), currentUser.getId());
        return OrgResponse.success(rows, levels);
    }

    /**
     * Trova l'ID della struttura dell'utente:
     * 1. Come responsabile (struttura più alta in gerarchia)
     * 2. Come membro dello staff
     */
    private Integer findUserStrutturaId(User user) {
        // Prima cerca come responsabile
        List<Struttura> asResponsabile = strutturaRepository.findAllByIdResponsabile(user.getId());
        if (!asResponsabile.isEmpty()) {
            return asResponsabile.get(0).getId(); // Prima = più alta (ORDER BY idParent ASC NULLS FIRST)
        }

        // Poi cerca come staff
        List<StrutturaStaff> asStaff = strutturaStaffRepository.findByIdUser(user.getId());
        if (!asStaff.isEmpty()) {
            return asStaff.get(0).getIdStruttura();
        }

        return null;
    }

    /**
     * Raccoglie ricorsivamente una struttura e tutti i suoi discendenti
     */
    private void collectSubtree(Integer nodeId, Map<Integer, Struttura> byId,
                                Map<Integer, List<Struttura>> childrenMap, List<Struttura> result) {
        Struttura node = byId.get(nodeId);
        if (node == null) return;
        result.add(node);

        List<Struttura> children = childrenMap.get(nodeId);
        if (children != null) {
            for (Struttura child : children) {
                collectSubtree(child.getId(), byId, childrenMap, result);
            }
        }
    }

    private OrgRowDto toOrgRowDto(Struttura struttura) {
        String id = "struct-" + struttura.getId();
        
        // Manager: null se è radice, altrimenti riferimento al parent
        String managerId = struttura.getIdParent() != null 
                ? "struct-" + struttura.getIdParent() 
                : null;

        // Nome del responsabile
        String head = null;
        if (struttura.getResponsabile() != null) {
            head = struttura.getResponsabile().getNomeCompleto();
        }

        // Staff: lista di nomi
        List<String> staffNames = new ArrayList<>();
        if (struttura.getStaff() != null) {
            for (StrutturaStaff ss : struttura.getStaff()) {
                if (ss.getUser() != null) {
                    String staffName = ss.getUser().getNomeCompleto();
                    if (staffName != null) {
                        staffNames.add(staffName);
                    }
                }
            }
        }

        return OrgRowDto.builder()
                .id(id)
                .label(struttura.getNome())
                .manager(managerId)
                .role(struttura.getRuoloLabel())
                .head(head)
                .staff(staffNames.isEmpty() ? null : staffNames)
                .color(struttura.getColore())
                .build();
    }

    /**
     * Calcola la profondità di una struttura nell'albero
     */
    private int calculateDepth(Struttura struttura, List<Struttura> allStrutture) {
        if (struttura.getIdParent() == null) {
            return 0;
        }

        Map<Integer, Struttura> byId = new HashMap<>();
        for (Struttura s : allStrutture) {
            byId.put(s.getId(), s);
        }

        int depth = 0;
        Integer currentParentId = struttura.getIdParent();
        while (currentParentId != null && depth < 100) { // Limite anti-loop
            depth++;
            Struttura parent = byId.get(currentParentId);
            if (parent == null) break;
            currentParentId = parent.getIdParent();
        }

        return depth;
    }

    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String keycloakId = jwt.getSubject();

        return userRepository.findByKeycloakId(keycloakId).orElse(null);
    }
}
