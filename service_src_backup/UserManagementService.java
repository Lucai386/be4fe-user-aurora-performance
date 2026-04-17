package com.bff_user_aurora_performance.service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bff_user_aurora_performance.dto.admin.CreateUserRequest;
import com.bff_user_aurora_performance.dto.admin.CreateUserResponse;
import com.bff_user_aurora_performance.dto.admin.DeleteUserResponse;
import com.bff_user_aurora_performance.dto.admin.ListUsersResponse;
import com.bff_user_aurora_performance.dto.admin.UserDto;
import com.bff_user_aurora_performance.enums.ErrorCode;
import com.bff_user_aurora_performance.model.User;
import com.bff_user_aurora_performance.repository.UserRepository;

import static com.bff_user_aurora_performance.enums.AppConstants.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserManagementService {

    private final UserRepository userRepository;
    private final KeycloakAdminService keycloakAdminService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Elenca gli utenti per il codice ISTAT dell'admin corrente
     */
    public ListUsersResponse listUsers(Authentication authentication) {
        User admin = getAdminUser(authentication);
        if (admin == null) {
            return ListUsersResponse.error(ErrorCode.NOT_AUTHORIZED, MSG_USER_NOT_AUTHORIZED);
        }

        String codiceIstat = admin.getCodiceIstat();
        if (codiceIstat == null || codiceIstat.isBlank()) {
            return ListUsersResponse.error(ErrorCode.NO_ENTE, MSG_NO_ENTE);
        }

        List<User> users = userRepository.findByCodiceIstat(codiceIstat.trim());
        
        List<UserDto> userDtos = users.stream()
                .filter(u -> !u.getId().equals(admin.getId())) // Escludi l'admin stesso
                .map(this::toDto)
                .toList();

        log.info("Listed {} users for codiceIstat: {}", userDtos.size(), codiceIstat);
        return ListUsersResponse.success(userDtos);
    }

    /**
     * Crea un nuovo utente per l'ente dell'admin corrente
     */
    @Transactional
    public CreateUserResponse createUser(Authentication authentication, CreateUserRequest request) {
        User admin = getAdminUser(authentication);
        if (admin == null) {
            return CreateUserResponse.error(ErrorCode.NOT_AUTHORIZED, MSG_USER_NOT_AUTHORIZED);
        }

        String codiceIstat = admin.getCodiceIstat();
        if (codiceIstat == null || codiceIstat.isBlank()) {
            return CreateUserResponse.error(ErrorCode.NO_ENTE, MSG_NO_ENTE);
        }

        // Verifica che il codice fiscale non esista già
        if (userRepository.findByCodiceFiscale(request.getCodiceFiscale()).isPresent()) {
            return CreateUserResponse.error(ErrorCode.CF_EXISTS, MSG_CF_EXISTS);
        }

        // Crea l'utente su Keycloak
        String keycloakId = keycloakAdminService.createUser(
                request.getEmail(),
                request.getNome(),
                request.getCognome(),
                request.getPassword()
        );

        if (keycloakId == null) {
            return CreateUserResponse.error(ErrorCode.KEYCLOAK_ERROR, MSG_KEYCLOAK_ERROR);
        }

        // Crea l'utente nel database locale
        User newUser = User.builder()
                .keycloakId(keycloakId)
                .nome(request.getNome())
                .cognome(request.getCognome())
                .email(request.getEmail())
                .codiceFiscale(request.getCodiceFiscale().toUpperCase())
                .codiceIstat(codiceIstat.trim())
                .ruolo(request.getRuolo())
                .build();

        User savedUser = userRepository.save(newUser);
        log.info("Created user {} ({}) for ente {}", savedUser.getId(), request.getEmail(), codiceIstat);

        return CreateUserResponse.success(toDto(savedUser));
    }

    /**
     * Elimina un utente dall'ente dell'admin corrente
     */
    @Transactional
    public DeleteUserResponse deleteUser(Authentication authentication, Integer userId) {
        User admin = getAdminUser(authentication);
        if (admin == null) {
            return DeleteUserResponse.error(ErrorCode.NOT_AUTHORIZED, MSG_USER_NOT_AUTHORIZED);
        }

        String codiceIstat = admin.getCodiceIstat();
        if (codiceIstat == null || codiceIstat.isBlank()) {
            return DeleteUserResponse.error(ErrorCode.NO_ENTE, MSG_NO_ENTE);
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return DeleteUserResponse.error(ErrorCode.USER_NOT_FOUND, MSG_USER_NOT_FOUND);
        }

        User userToDelete = userOpt.get();

        // Verifica che l'utente appartenga allo stesso ente
        if (!codiceIstat.trim().equals(userToDelete.getCodiceIstat())) {
            return DeleteUserResponse.error(ErrorCode.NOT_AUTHORIZED, MSG_CANNOT_DELETE_OTHER_ENTE);
        }

        // Non permettere di eliminare altri admin
        if ("AD".equals(userToDelete.getRuolo())) {
            return DeleteUserResponse.error(ErrorCode.CANNOT_DELETE_ADMIN, MSG_CANNOT_DELETE_ADMIN);
        }

        // Elimina da Keycloak
        boolean keycloakDeleted = keycloakAdminService.deleteUser(userToDelete.getKeycloakId());
        if (!keycloakDeleted) {
            log.warn("Could not delete user {} from Keycloak, proceeding with DB deletion", userToDelete.getKeycloakId());
        }

        // Elimina dal database
        userRepository.delete(userToDelete);
        log.info("Deleted user {} from ente {}", userId, codiceIstat);

        return DeleteUserResponse.success();
    }

    private User getAdminUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Jwt jwt = (Jwt) authentication.getPrincipal();
        String keycloakId = jwt.getSubject();

        Optional<User> userOpt = userRepository.findByKeycloakId(keycloakId);
        if (userOpt.isEmpty()) {
            return null;
        }

        User user = userOpt.get();
        if (!"AD".equals(user.getRuolo())) {
            return null;
        }

        return user;
    }

    private UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .nome(user.getNome())
                .cognome(user.getCognome())
                .email(user.getEmail())
                .codiceFiscale(user.getCodiceFiscale())
                .codiceIstat(user.getCodiceIstat())
                .ruolo(user.getRuolo())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().format(DATE_FORMATTER) : null)
                .build();
    }
}
