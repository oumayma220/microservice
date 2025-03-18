package com.example.demo.service;
import com.example.demo.request.AuthenticationRequest;
import com.example.demo.response.AuthenticationResponse;
import com.example.demo.request.RegistrationRequest;
import com.example.demo.request.UpdateEmployeeRequest;
import com.example.demo.dao.entity.*;
import com.example.demo.dao.repository.TokenRepository;
import com.example.demo.dao.repository.UserRepository;
import com.example.demo.security.JwtService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JavaMailSender emailSender;
    private final TemplateEngine templateEngine;

public void register(RegistrationRequest request) throws MessagingException {
    var clientRole = Role.CLIENT;
    var user = User.builder()
            .firstname(request.getFirstname())
            .lastname(request.getLastname())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .accountLocked(false)
            .enabled(false)
            .roles(List.of(clientRole))
            .build();
    userRepository.save(user);
    sendValidationEmail(user);
}
    private void sendValidationEmail(User user) throws MessagingException {
        var newToken = generateAndSaveActivationToken(user);
        emailService.SendEmail(
                user.getEmail(),
                user.fullName(),
                EmailTemplateName.ACTIVATE_ACCOUNT,
                newToken,
                "Account activation"
        );
    }
    private String generateAndSaveActivationToken(User user) {

        String generatedToken =generateActivationCode(6);
        var token = Token.builder()
                .token(generatedToken)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .user(user)
                .build();
        tokenRepository.save(token);
        return generatedToken;
    }
    private String generateActivationCode(int length) {
        String characters = "0123456789";
        StringBuilder codeBuilder = new StringBuilder();
        SecureRandom secureRandom = new SecureRandom();
        for(int i =0; i<length;i++){
            int randomIndex = secureRandom.nextInt(characters.length());
            codeBuilder.append(characters.charAt(randomIndex));
        }
        return codeBuilder.toString();
    }
    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        var auth = authenticationManager.authenticate(
               new UsernamePasswordAuthenticationToken(
                 request.getEmail(),
                  request.getPassword()
               )
        );
        var claims = new HashMap<String, Object>();
        var user = ((User)auth.getPrincipal());
        claims.put("fullName",user.fullName());
        var jwtToken = jwtService.generateToken(claims, user);
        return AuthenticationResponse.builder()
                .token(jwtToken).build();
    }
    public void activateAccount(String token) throws MessagingException {
        Token savedToken = tokenRepository.findByToken(token)
                .orElseThrow(()->new RuntimeException("Invalid token"));
        if (LocalDateTime.now().isAfter(savedToken.getExpiresAt())){
            sendValidationEmail(savedToken.getUser());
            throw new RuntimeException("Activation token has expired. A new token has been sent");
        }
        var  user = userRepository.findById(savedToken.getUser().getId())
                .orElseThrow(()-> new UsernameNotFoundException("User not found"));
        user.setEnabled(true);
        userRepository.save(user);
        savedToken.setValidatedAt(LocalDateTime.now());
        tokenRepository.save(savedToken);

    }
    public void registerEmployee(RegistrationRequest request) throws MessagingException {
        Integer tenantId = getAdminTenantId();

        var employeeRole = Role.EMPLOYEE;
        var user = User.builder()
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .accountLocked(false)
                .enabled(true)
                .roles(List.of(employeeRole))
                .tenantid(tenantId)
                .build();

        userRepository.save(user);
    }

     public List<User> getAllEmployees() {
         Integer currentTenantId = getPrincipal().getTenantid();
         List<User> employees = userRepository.findByRolesContaining(Role.EMPLOYEE);
         return employees.stream()
                 .filter(user -> user.getTenantid().equals(currentTenantId)) // Filtrer selon tenantId
                 .filter(User::isEnabled)
                 .collect(Collectors.toList());
     }

    public void updateEmployee(Integer id, UpdateEmployeeRequest request) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setFirstname(request.getFirstname());
        user.setLastname(request.getLastname());
        user.setEmail(request.getEmail());

        userRepository.save(user);
    }
    public void disableEmployee(Integer id) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setEnabled(false);
        userRepository.save(user);
    }
    public void enableEmployee(Integer id) {
        var user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setEnabled(true);
        userRepository.save(user);
    }

           public void sendPasswordResetCode(String email) throws MessagingException {
               Optional<User> optionalUser = userRepository.findByEmail(email);
               if (optionalUser.isPresent()) {
                   User user = optionalUser.get();

                   String code = String.format("%06d", new Random().nextInt(999999));

                   LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(15);

                   user.setResetCode(code);
                   user.setResetCodeExpiry(expiryTime);
                   userRepository.save(user);

                   emailService.sendPasswordResetEmail(user.getEmail(), code, "Réinitialisation de mot de passe");
               } else {
                   throw new IllegalArgumentException("Adresse e-mail non trouvée : " + email);
               }
           }
    public boolean resetPassword(String code, String newPassword) {
        // Find the user by the reset code
        Optional<User> optionalUser = userRepository.findByResetCode(code);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();

            if (user.getResetCode() != null
                    && user.getResetCode().equals(code)
                    && user.getResetCodeExpiry().isAfter(LocalDateTime.now())) {
                user.setPassword(passwordEncoder.encode(newPassword));
                user.setResetCode(null);
                user.setResetCodeExpiry(null);
                userRepository.save(user);
                return true;
            } else {
                throw new IllegalArgumentException("Code invalide ou expiré");
            }
        }
        return false;
    }
    public User getById(Integer id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    public User getPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        throw new RuntimeException("User not authenticated");
    }
    public UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setFirstname(user.getFirstname());
        dto.setLastname(user.getLastname());
        dto.setEmail(user.getEmail());
        dto.setCreatedDate(user.getCreatedDate());
        dto.setRoles(user.getRoles().stream()
                .map(role -> role.toString())
                .collect(Collectors.toSet()));
        dto.setTenantid(user.getTenantid());
        return dto;
    }
    public static Integer generateTenant() {
        return (int) (Math.random() * 9000) + 1000;
    }
    public Integer getAdminTenantId() {
        User admin = getPrincipal();
        if (admin != null && admin.getRoles().contains(Role.ADMIN)) {
            return admin.getTenantid();
        }
        throw new RuntimeException("Administrator not found or not authenticated");
    }




}
















