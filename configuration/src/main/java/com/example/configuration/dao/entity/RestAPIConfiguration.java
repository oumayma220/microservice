package com.example.configuration.dao.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RestAPIConfiguration extends APIConfiguration {

    private String url;

    private String headers;
    @OneToMany(mappedBy = "restAPIConfig", cascade = CascadeType.ALL,orphanRemoval = true)
    @JsonManagedReference
    private List<APIMethod> apiMethods;



}