package com.example.configuration.auth;
import com.example.configuration.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
@FeignClient(name = "authentification", url = "${auth-service.url}")
public interface AuthServiceClient {
    @GetMapping("/auth/validate-token")
    boolean validateToken(@RequestHeader("Authorization") String token);
    @GetMapping("/authenticated/me")
    UserDTO getUserFromToken(@RequestHeader("Authorization") String token);
}
