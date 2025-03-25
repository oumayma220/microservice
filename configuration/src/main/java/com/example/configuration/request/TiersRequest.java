package com.example.configuration.request;

import com.example.configuration.dto.FieldMappingDTO;
import lombok.Data;

import java.util.List;

@Data
public class TiersRequest {
    private String nom;
    private String email;
    private String numero;
    private String configName;
    private String url;
    private String headers;
    private String httpMethod;
    private String endpoint;
    private String methodHeaders;
    private boolean paginated;
    private String paginationParamName;
    private String pageSizeParamName;
    private String totalPagesFieldInResponse;
    private String contentFieldInResponse;
    private String type;

    private List<FieldMappingDTO> fieldMappings;
}
