package com.example.configuration.dto;

import lombok.Data;

import java.util.List;

@Data
public class TiersRequest {
    private String nom;
    private String configName;
    private String url;
    private String headers;
    private String httpMethod;
    private String endpoint;
    private String methodHeaders;
    private boolean paginated;
    private String paginationParamName;
    private String pageSizeParamName;
   // private Integer pageSize;
    private String totalPagesFieldInResponse;
    private String contentFieldInResponse;

    private List<FieldMappingDTO> fieldMappings;
}
