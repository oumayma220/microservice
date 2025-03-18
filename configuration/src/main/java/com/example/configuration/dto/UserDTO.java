package com.example.configuration.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;
@Getter
@Setter
public class UserDTO {
    private Integer id;
    private String firstname;
    private String lastname;
    private String email;
    private LocalDateTime createdDate;
    private Set<String> roles;
    private Integer tenantid;

}
