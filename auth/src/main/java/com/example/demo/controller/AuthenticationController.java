package com.example.demo.controller;

import com.example.demo.dao.entity.User;
import com.example.demo.dao.entity.UserDTO;
import com.example.demo.dao.repository.UserRepository;
import com.example.demo.request.AuthenticationRequest;
import com.example.demo.response.AuthenticationResponse;
import com.example.demo.request.RegistrationRequest;
import com.example.demo.security.JwtService;
import com.example.demo.service.AuthenticationService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("auth")
public class AuthenticationController {
    private final AuthenticationService service ;
    private final JwtService jwtService;
    @Autowired
    private UserRepository userRepository ;

    public AuthenticationController(AuthenticationService service, JwtService jwtService, UserRepository userRepository) {
        this.service = service;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }


    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<?> register(
            @RequestBody @Valid RegistrationRequest request
    ) throws MessagingException {
        service.register(request);
        return ResponseEntity.accepted().build();
    }
    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody AuthenticationRequest request
    ) {
        return ResponseEntity.ok(service.authenticate(request));
    }
    @GetMapping("/activate-account")
    public void confirm(
            @RequestParam String token
    ) throws MessagingException {
        service.activateAccount(token);

    }
    @GetMapping("/roles")
    public String getUserRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return "Authorities: " + auth.getAuthorities();
    }
    @PostMapping("/forgot-password")
    public ResponseEntity<String> sendResetCode(@RequestParam String email) {
        try {
            service.sendPasswordResetCode(email);
            return ResponseEntity.ok("Un code de réinitialisation a été envoyé à votre email.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (MessagingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Une erreur est survenue lors de l'envoi de l'e-mail.");
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(
            @RequestParam String code,
            @RequestParam String newPassword) {
        try {
            boolean success = service.resetPassword(code, newPassword);
            if (success) {
                return ResponseEntity.ok("Mot de passe mis à jour avec succès.");
            } else {
                return ResponseEntity.badRequest().body("Échec de la réinitialisation du mot de passe.");
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    @GetMapping("/user/{id}")
    public ResponseEntity<UserDTO> getById(@PathVariable Integer id) {
        User user = service.getById(id);
        return ResponseEntity.ok(service.convertToDTO(user));
    }
    @GetMapping("/validate-token")
    public ResponseEntity<Boolean> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.ok(false);
            }

            String jwt = authHeader.substring(7);
            String userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null) {
                UserDetails userDetails = userRepository.findByEmail(userEmail)
                        .orElse(null);

                if (userDetails != null) {
                    return ResponseEntity.ok(jwtService.isTokenValid(jwt, userDetails));
                }
            }
            return ResponseEntity.ok(false);
        } catch (Exception e) {
            return ResponseEntity.ok(false);
        }
    }
}
