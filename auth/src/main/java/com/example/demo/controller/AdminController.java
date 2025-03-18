package com.example.demo.controller;

import com.example.demo.dao.entity.User;
import com.example.demo.request.RegistrationRequest;
import com.example.demo.request.UpdateEmployeeRequest;
import com.example.demo.service.AuthenticationService;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200")

public class AdminController {
    private final AuthenticationService authenticationService;

    @PostMapping("/register-employee")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ResponseEntity<?> registerEmployee(
            @RequestBody @Valid RegistrationRequest request
    ) throws MessagingException{
            authenticationService.registerEmployee(request);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/employees")
    public ResponseEntity<List<User>> getAllEmployees() {
        List<User> employees = authenticationService.getAllEmployees();
        return ResponseEntity.ok(employees);
    }

    @PutMapping("/update-employee/{id}")
    public ResponseEntity<?> updateEmployee(
            @PathVariable Integer id,
            @RequestBody @Valid UpdateEmployeeRequest request
    ) {
        authenticationService.updateEmployee(id, request);
        return ResponseEntity.ok().body("Employee updated successfully");
    }
    @PutMapping("/disable-employee/{id}")
    public ResponseEntity<?> disableEmployee(@PathVariable Integer id) {
        authenticationService.disableEmployee(id);
        return ResponseEntity.ok("Employee access has been disabled.");
    }
    @PutMapping("/enable-employee/{id}")
    public ResponseEntity<?> enableEmployee(@PathVariable Integer id) {
        authenticationService.enableEmployee(id);
        return ResponseEntity.ok("Employee access has been enabled.");
    }




}
