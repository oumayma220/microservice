package com.example.demo.controller;

import com.example.demo.dao.entity.User;
import com.example.demo.dao.entity.UserDTO;
import com.example.demo.service.AuthenticationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("authenticated")
public class authController {
    private final AuthenticationService service ;

    public authController(AuthenticationService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public ResponseEntity<UserDTO> getPrincipal() {
        User user = service.getPrincipal();
        return ResponseEntity.ok(service.convertToDTO(user));
    }
}
