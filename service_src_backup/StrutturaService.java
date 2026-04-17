package com.bff_user_aurora_performance.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bff_user_aurora_performance.dto.org.AddStaffRequest;
import com.bff_user_aurora_performance.dto.org.CreateStrutturaRequest;
import com.bff_user_aurora_performance.dto.org.DeleteStrutturaResponse;
import com.bff_user_aurora_performance.dto.org.ListStruttureResponse;
import com.bff_user_aurora_performance.dto.org.StrutturaDto;
import com.bff_user_aurora_performance.dto.org.StrutturaResponse;
import com.bff_user_aurora_performance.dto.org.StrutturaUtentiResponse;
import com.bff_user_aurora_performance.dto.org.UpdateStrutturaRequest;
import com.bff_user_aurora_performance.enums.ErrorCode;
import com.bff_user_aurora_performance.model.Struttura;
import com.bff_user_aurora_performance.model.StrutturaStaff;
import com.bff_user_aurora_performance.model.User;
import com.bff_user_aurora_performance.repository.StrutturaRepository;
import com.bff_user_aurora_performance.repository.StrutturaStaffRepository;
import com.bff_user_aurora_performance.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class StrutturaService {

    private final StrutturaRepository strutturaRepository;
    private final StrutturaStaffRepository strutturaStaffRepository;
    private final UserRepository userRepository;

    /**
     * Lista tutte le strutture per il codice ISTAT dell'admin corrente
     */
    public ListStruttureResponse listStrutture(Authentication authentication) {
        User admin = getAdminUser(authentication);
        if (admin == null) {
            return ListStruttureResponse.error(ErrorCode.NOT_AUTHORIZED);
        }

        String codiceIstat = admin.getCodiceIstat();
        if (codiceIstat == null || codiceIstat.isBlank()) {
            return ListStruttureResponse.error(ErrorCode.NO_ENTE);
        }

        List<Struttura> strutture = strutturaRepository.findByCodiceIstatWithStaff(codiceIstat.trim());
        
        // Mappa per lookup rapido dei parent
        Map<Integer, Struttura> byId = strutture.stream()
                .collect(Collectors.toMap(Struttura::getId, s -> s));

        List<StrutturaDto> dtos = strutture.stream()
                .map(s -> toDto(s, byId))
                .toList();

        log.info("Listed {} strutture for codiceIstat: {}", dtos.size(), codiceIstat);
        return ListStruttureResponse.success(dtos);
    }

    /**
     * Crea una nuova struttura
     */
    @Transactional
    public StrutturaResponse createStruttura(Authentication authentication, CreateStrutturaRequest request) {
        User admin = getAdminUser(authentication);
        if (admin == null) {
            return StrutturaResponse.error(ErrorCode.NOT_AUTHORIZED);
        }

        String codiceIstat = admin.getCodiceIstat();
        if (codiceIstat == null || codiceIstat.isBlank()) {
            return StrutturaResponse.error(ErrorCode.NO_ENTE);
        }

        // Verifica che il parent (se specificato) appartenga allo stesso ente
        if (request.getIdParent() != null) {
            Optional<Struttura> parent = strutturaRepository.findById(request.getIdParent());
            if (parent.isEmpty()) {
                return StrutturaResponse.error(ErrorCode.PARENT_NOT_FOUND);
            }
            if (!codiceIstat.trim().equals(parent.get().getCodiceIstatComune())) {
                return StrutturaResponse.error(ErrorCode.PARENT_DIFFERENT_ENTE);
            }
        }

        // Verifica che il responsabile (se specificato) appartenga allo stesso ente
        if (request.getIdResponsabile() != null) {
            Optional<User> responsabile = userRepository.findById(request.getIdResponsabile());
            if (responsabile.isEmpty()) {
                return StrutturaResponse.error(ErrorCode.RESPONSABILE_NOT_FOUND);
            }
            if (!codiceIstat.trim().equals(responsabile.get().getCodiceIstat())) {
                return StrutturaResponse.error(ErrorCode.RESPONSABILE_DIFFERENT_ENTE);
            }
        }

        Struttura struttura = Struttura.builder()
                .nome(request.getNome())
                .codiceIstatComune(codiceIstat.trim())
                .tipo(request.getTipo())
                .idParent(request.getIdParent())
                .idResponsabile(request.getIdResponsabile())
                .ruoloLabel(request.getRuoloLabel())
                .colore(request.getColore())
                .ordine(request.getOrdine() != null ? request.getOrdine() : 0)
                .build();

        Struttura saved = strutturaRepository.save(struttura);
        log.info("Created struttura {} for ente {}", saved.getId(), codiceIstat);

        // Ricarica con le relazioni
        List<Struttura> all = strutturaRepository.findByCodiceIstatWithStaff(codiceIstat.trim());
        Map<Integer, Struttura> byId = all.stream()
                .collect(Collectors.toMap(Struttura::getId, s -> s));
        Struttura reloaded = byId.get(saved.getId());

        return StrutturaResponse.success(toDto(reloaded != null ? reloaded : saved, byId));
    }

    /**
     * Aggiorna una struttura esistente
     */
    @Transactional
    public StrutturaResponse updateStruttura(Authentication authentication, Integer id, UpdateStrutturaRequest request) {
        User admin = getAdminUser(authentication);
        if (admin == null) {
            return StrutturaResponse.error(ErrorCode.NOT_AUTHORIZED);
        }

        String codiceIstat = admin.getCodiceIstat();
        if (codiceIstat == null || codiceIstat.isBlank()) {
            return StrutturaResponse.error(ErrorCode.NO_ENTE);
        }

        Optional<Struttura> existing = strutturaRepository.findById(id);
        if (existing.isEmpty()) {
            return StrutturaResponse.error(ErrorCode.STRUTTURA_NOT_FOUND);
        }

        Struttura struttura = existing.get();

        // Verifica che la struttura appartenga allo stesso ente
        if (!codiceIstat.trim().equals(struttura.getCodiceIstatComune())) {
            return StrutturaResponse.error(ErrorCode.NOT_AUTHORIZED);
        }

        // Aggiorna i campi se forniti
        if (request.getNome() != null) {
            struttura.setNome(request.getNome());
        }
        if (request.getTipo() != null) {
            struttura.setTipo(request.getTipo());
        }

        // Gestione parent: rimuovi o aggiorna
        if (Boolean.TRUE.equals(request.getRemoveParent())) {
            struttura.setIdParent(null);
        } else if (request.getIdParent() != null) {
            // Verifica parent
            if (request.getIdParent().equals(id)) {
                return StrutturaResponse.error(ErrorCode.INVALID_PARENT);
            }
            Optional<Struttura> parent = strutturaRepository.findById(request.getIdParent());
            if (parent.isEmpty()) {
                return StrutturaResponse.error(ErrorCode.PARENT_NOT_FOUND);
            }
            if (!codiceIstat.trim().equals(parent.get().getCodiceIstatComune())) {
                return StrutturaResponse.error(ErrorCode.PARENT_DIFFERENT_ENTE);
            }
            struttura.setIdParent(request.getIdParent());
        }

        // Gestione responsabile: rimuovi o aggiorna
        if (Boolean.TRUE.equals(request.getRemoveResponsabile())) {
            struttura.setIdResponsabile(null);
        } else if (request.getIdResponsabile() != null) {
            Optional<User> responsabile = userRepository.findById(request.getIdResponsabile());
            if (responsabile.isEmpty()) {
                return StrutturaResponse.error(ErrorCode.RESPONSABILE_NOT_FOUND);
            }
            if (!codiceIstat.trim().equals(responsabile.get().getCodiceIstat())) {
                return StrutturaResponse.error(ErrorCode.RESPONSABILE_DIFFERENT_ENTE);
            }
            struttura.setIdResponsabile(request.getIdResponsabile());
        }
        if (request.getRuoloLabel() != null) {
            struttura.setRuoloLabel(request.getRuoloLabel());
        }
        if (request.getColore() != null) {
            struttura.setColore(request.getColore());
        }
        if (request.getOrdine() != null) {
            struttura.setOrdine(request.getOrdine());
        }

        Struttura saved = strutturaRepository.save(struttura);
        log.info("Updated struttura {} for ente {}", saved.getId(), codiceIstat);

        // Ricarica con le relazioni
        List<Struttura> all = strutturaRepository.findByCodiceIstatWithStaff(codiceIstat.trim());
        Map<Integer, Struttura> byId = all.stream()
                .collect(Collectors.toMap(Struttura::getId, s -> s));

        return StrutturaResponse.success(toDto(byId.getOrDefault(saved.getId(), saved), byId));
    }

    /**
     * Elimina una struttura
     */
    @Transactional
    public DeleteStrutturaResponse deleteStruttura(Authentication authentication, Integer id) {
        User admin = getAdminUser(authentication);
        if (admin == null) {
            return DeleteStrutturaResponse.error(ErrorCode.NOT_AUTHORIZED);
        }

        String codiceIstat = admin.getCodiceIstat();
        if (codiceIstat == null || codiceIstat.isBlank()) {
            return DeleteStrutturaResponse.error(ErrorCode.NO_ENTE);
        }

        Optional<Struttura> existing = strutturaRepository.findById(id);
        if (existing.isEmpty()) {
            return DeleteStrutturaResponse.error(ErrorCode.STRUTTURA_NOT_FOUND);
        }

        Struttura struttura = existing.get();

        // Verifica che la struttura appartenga allo stesso ente
        if (!codiceIstat.trim().equals(struttura.getCodiceIstatComune())) {
            return DeleteStrutturaResponse.error(ErrorCode.NOT_AUTHORIZED);
        }

        // Verifica che non ci siano figli
        List<Struttura> children = strutturaRepository.findByIdParent(id);
        if (!children.isEmpty()) {
            return DeleteStrutturaResponse.error(ErrorCode.HAS_CHILDREN);
        }

        strutturaRepository.delete(struttura);
        log.info("Deleted struttura {} from ente {}", id, codiceIstat);

        return DeleteStrutturaResponse.success();
    }

    /**
     * Aggiunge un membro allo staff di una struttura
     */
    @Transactional
    public StrutturaResponse addStaff(Authentication authentication, Integer strutturaId, AddStaffRequest request) {
        User admin = getAdminUser(authentication);
        if (admin == null) {
            return StrutturaResponse.error(ErrorCode.NOT_AUTHORIZED);
        }

        String codiceIstat = admin.getCodiceIstat();
        if (codiceIstat == null || codiceIstat.isBlank()) {
            return StrutturaResponse.error(ErrorCode.NO_ENTE);
        }

        Optional<Struttura> existing = strutturaRepository.findById(strutturaId);
        if (existing.isEmpty()) {
            return StrutturaResponse.error(ErrorCode.STRUTTURA_NOT_FOUND);
        }

        Struttura struttura = existing.get();

        // Verifica che la struttura appartenga allo stesso ente
        if (!codiceIstat.trim().equals(struttura.getCodiceIstatComune())) {
            return StrutturaResponse.error(ErrorCode.NOT_AUTHORIZED);
        }

        // Verifica che l'utente esista e appartenga allo stesso ente
        Optional<User> user = userRepository.findById(request.getIdUser());
        if (user.isEmpty()) {
            return StrutturaResponse.error(ErrorCode.USER_NOT_FOUND);
        }
        if (!codiceIstat.trim().equals(user.get().getCodiceIstat())) {
            return StrutturaResponse.error(ErrorCode.RESPONSABILE_DIFFERENT_ENTE);
        }

        // Crea l'assegnazione
        StrutturaStaff staff = StrutturaStaff.builder()
                .idStruttura(strutturaId)
                .idUser(request.getIdUser())
                .ruoloStruttura(request.getRuoloStruttura())
                .ordine(request.getOrdine() != null ? request.getOrdine() : 0)
                .build();

        strutturaStaffRepository.save(staff);
        log.info("Added user {} to staff of struttura {}", request.getIdUser(), strutturaId);

        // Ricarica con le relazioni
        List<Struttura> all = strutturaRepository.findByCodiceIstatWithStaff(codiceIstat.trim());
        Map<Integer, Struttura> byId = all.stream()
                .collect(Collectors.toMap(Struttura::getId, s -> s));

        return StrutturaResponse.success(toDto(byId.getOrDefault(strutturaId, struttura), byId));
    }

    /**
     * Rimuove un membro dallo staff di una struttura
     */
    @Transactional
    public StrutturaResponse removeStaff(Authentication authentication, Integer strutturaId, Integer userId) {
        User admin = getAdminUser(authentication);
        if (admin == null) {
            return StrutturaResponse.error(ErrorCode.NOT_AUTHORIZED);
        }

        String codiceIstat = admin.getCodiceIstat();
        if (codiceIstat == null || codiceIstat.isBlank()) {
            return StrutturaResponse.error(ErrorCode.NO_ENTE);
        }

        Optional<Struttura> existing = strutturaRepository.findById(strutturaId);
        if (existing.isEmpty()) {
            return StrutturaResponse.error(ErrorCode.STRUTTURA_NOT_FOUND);
        }

        Struttura struttura = existing.get();

        // Verifica che la struttura appartenga allo stesso ente
        if (!codiceIstat.trim().equals(struttura.getCodiceIstatComune())) {
            return StrutturaResponse.error(ErrorCode.NOT_AUTHORIZED);
        }

        strutturaStaffRepository.deleteByIdStrutturaAndIdUser(strutturaId, userId);
        log.info("Removed user {} from staff of struttura {}", userId, strutturaId);

        // Ricarica con le relazioni
        List<Struttura> all = strutturaRepository.findByCodiceIstatWithStaff(codiceIstat.trim());
        Map<Integer, Struttura> byId = all.stream()
                .collect(Collectors.toMap(Struttura::getId, s -> s));

        return StrutturaResponse.success(toDto(byId.getOrDefault(strutturaId, struttura), byId));
    }

    private StrutturaDto toDto(Struttura struttura, Map<Integer, Struttura> byId) {
        // Nome del parent
        String parentNome = null;
        if (struttura.getIdParent() != null) {
            Struttura parent = byId.get(struttura.getIdParent());
            if (parent != null) {
                parentNome = parent.getNome();
            }
        }

        // Nome del responsabile
        String responsabileNome = null;
        if (struttura.getResponsabile() != null) {
            responsabileNome = struttura.getResponsabile().getNomeCompleto();
        }

        // Staff
        List<StrutturaDto.StaffMemberDto> staffDtos = new ArrayList<>();
        if (struttura.getStaff() != null) {
            for (StrutturaStaff ss : struttura.getStaff()) {
                if (ss.getUser() != null) {
                    staffDtos.add(StrutturaDto.StaffMemberDto.builder()
                            .id(ss.getId())
                            .userId(ss.getIdUser())
                            .nome(ss.getUser().getNome())
                            .cognome(ss.getUser().getCognome())
                            .nomeCompleto(ss.getUser().getNomeCompleto())
                            .ruoloStruttura(ss.getRuoloStruttura())
                            .ordine(ss.getOrdine())
                            .build());
                }
            }
        }

        return StrutturaDto.builder()
                .id(struttura.getId())
                .nome(struttura.getNome())
                .tipo(struttura.getTipo())
                .idParent(struttura.getIdParent())
                .parentNome(parentNome)
                .idResponsabile(struttura.getIdResponsabile())
                .responsabileNome(responsabileNome)
                .ruoloLabel(struttura.getRuoloLabel())
                .colore(struttura.getColore())
                .ordine(struttura.getOrdine())
                .staff(staffDtos.isEmpty() ? null : staffDtos)
                .build();
    }

    private static final List<String> AUTHORIZED_ROLES = List.of("AD", "SC", "RA", "DR");

    private User getAdminUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String keycloakId = jwt.getSubject();

        Optional<User> userOpt = userRepository.findByKeycloakId(keycloakId);
        if (userOpt.isEmpty()) {
            return null;
        }

        User user = userOpt.get();
        // Verifica che abbia un ruolo autorizzato
        if (!AUTHORIZED_ROLES.contains(user.getRuolo())) {
            return null;
        }

        return user;
    }

    /**
     * Restituisce gli utenti (responsabile + staff) di una struttura
     */
    public StrutturaUtentiResponse getUtentiStruttura(Authentication authentication, Integer strutturaId) {
        User admin = getAdminUser(authentication);
        if (admin == null) {
            return StrutturaUtentiResponse.error(ErrorCode.NOT_AUTHORIZED);
        }

        String codiceIstat = admin.getCodiceIstat();
        if (codiceIstat == null || codiceIstat.isBlank()) {
            return StrutturaUtentiResponse.error(ErrorCode.NO_ENTE);
        }

        Optional<Struttura> strutturaOpt = strutturaRepository.findById(strutturaId);
        if (strutturaOpt.isEmpty()) {
            return StrutturaUtentiResponse.error(ErrorCode.STRUTTURA_NOT_FOUND);
        }

        Struttura struttura = strutturaOpt.get();
        if (!codiceIstat.trim().equals(struttura.getCodiceIstatComune())) {
            return StrutturaUtentiResponse.error(ErrorCode.NOT_AUTHORIZED);
        }

        List<StrutturaUtentiResponse.UtenteStrutturaDto> utenti = new ArrayList<>();

        // Aggiungi il responsabile se presente
        if (struttura.getIdResponsabile() != null) {
            Optional<User> responsabileOpt = userRepository.findById(struttura.getIdResponsabile());
            responsabileOpt.ifPresent(r -> utenti.add(
                StrutturaUtentiResponse.UtenteStrutturaDto.builder()
                    .id(r.getId())
                    .nome(r.getNome())
                    .cognome(r.getCognome())
                    .nomeCompleto(r.getNome() + " " + r.getCognome())
                    .email(r.getEmail())
                    .ruolo("responsabile")
                    .ruoloStruttura(struttura.getRuoloLabel())
                    .build()
            ));
        }

        // Aggiungi lo staff
        List<StrutturaStaff> staffList = strutturaStaffRepository.findByIdStrutturaWithUser(strutturaId);
        for (StrutturaStaff ss : staffList) {
            User u = ss.getUser();
            if (u != null) {
                utenti.add(
                    StrutturaUtentiResponse.UtenteStrutturaDto.builder()
                        .id(u.getId())
                        .nome(u.getNome())
                        .cognome(u.getCognome())
                        .nomeCompleto(u.getNome() + " " + u.getCognome())
                        .email(u.getEmail())
                        .ruolo("staff")
                        .ruoloStruttura(ss.getRuoloStruttura())
                        .build()
                );
            }
        }

        log.info("Found {} utenti for struttura {}", utenti.size(), strutturaId);
        return StrutturaUtentiResponse.success(utenti);
    }
}
