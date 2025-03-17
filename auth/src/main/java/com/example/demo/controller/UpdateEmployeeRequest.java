package com.example.demo.controller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateEmployeeRequest {
    @NotBlank
    private String firstname;

    @NotBlank
    private String lastname;

    @Email
    private String email;
}
