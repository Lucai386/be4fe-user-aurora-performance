package com.bff_user_aurora_performance.service;

import java.util.Collections;
import java.util.List;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class KeycloakAdminService {

    @Value("${keycloak.auth-server-url}")
    private String authServerUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.admin.username:admin}")
    private String adminUsername;

    @Value("${keycloak.admin.password:admin}")
    private String adminPassword;

    private Keycloak keycloak;

    @PostConstruct
    public void init() {
        this.keycloak = KeycloakBuilder.builder()
                .serverUrl(authServerUrl)
                .realm("master")
                .grantType(OAuth2Constants.PASSWORD)
                .clientId("admin-cli")
                .username(adminUsername)
                .password(adminPassword)
                .build();
        log.info("Keycloak admin client initialized for server: {}", authServerUrl);
    }

    /**
     * Crea un utente su Keycloak
     * @return keycloakId dell'utente creato, o null in caso di errore
     */
    public String createUser(String email, String firstName, String lastName, String password) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UsersResource usersResource = realmResource.users();

            // Verifica se l'utente esiste già
            List<UserRepresentation> existingUsers = usersResource.searchByEmail(email, true);
            if (!existingUsers.isEmpty()) {
                log.warn("User with email {} already exists in Keycloak", email);
                return null;
            }

            // Crea la rappresentazione utente
            UserRepresentation user = new UserRepresentation();
            user.setEnabled(true);
            user.setUsername(email);
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setEmailVerified(true);

            // Crea l'utente
            Response response = usersResource.create(user);
            
            if (response.getStatus() == 201) {
                // Estrai l'ID dall'header Location
                String locationPath = response.getLocation().getPath();
                String keycloakId = locationPath.substring(locationPath.lastIndexOf('/') + 1);
                
                // Imposta la password
                CredentialRepresentation credential = new CredentialRepresentation();
                credential.setType(CredentialRepresentation.PASSWORD);
                credential.setValue(password);
                credential.setTemporary(false);
                
                usersResource.get(keycloakId).resetPassword(credential);
                
                // Assegna il ruolo default
                assignRole(keycloakId, "user");
                
                log.info("User {} created successfully in Keycloak with id: {}", email, keycloakId);
                return keycloakId;
            } else {
                log.error("Failed to create user in Keycloak. Status: {}", response.getStatus());
                return null;
            }
        } catch (Exception e) {
            log.error("Error creating user in Keycloak", e);
            return null;
        }
    }

    /**
     * Elimina un utente da Keycloak
     */
    public boolean deleteUser(String keycloakId) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            UsersResource usersResource = realmResource.users();
            
            Response response = usersResource.delete(keycloakId);
            
            if (response.getStatus() == 204) {
                log.info("User {} deleted successfully from Keycloak", keycloakId);
                return true;
            } else {
                log.error("Failed to delete user from Keycloak. Status: {}", response.getStatus());
                return false;
            }
        } catch (Exception e) {
            log.error("Error deleting user from Keycloak", e);
            return false;
        }
    }

    /**
     * Assegna un ruolo realm a un utente
     */
    private void assignRole(String keycloakId, String roleName) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            var roleRepresentation = realmResource.roles().get(roleName).toRepresentation();
            realmResource.users().get(keycloakId).roles().realmLevel()
                    .add(Collections.singletonList(roleRepresentation));
            log.debug("Role {} assigned to user {}", roleName, keycloakId);
        } catch (Exception e) {
            log.warn("Could not assign role {} to user {}: {}", roleName, keycloakId, e.getMessage());
        }
    }
}
