package com.example.configuration.dao.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
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
public class APIMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String httpMethod;

    private String endpoint;

    private String headers;

    @ManyToOne
    @JsonBackReference
    private RestAPIConfiguration restAPIConfig;
    @OneToMany(mappedBy = "apiMethod", cascade = CascadeType.ALL)
    @JsonManagedReference
    private  List<FieldMapping> fieldMappings;
    private boolean paginated;

    private String paginationParamName; // "page"

    private String pageSizeParamName; // "size"

    private Integer pageSize;

    private String totalPagesFieldInResponse; //  (ex: "totalPages")

    private String contentFieldInResponse; // si tu dois chercher la liste dans un champ "content", "data", etc.

}
