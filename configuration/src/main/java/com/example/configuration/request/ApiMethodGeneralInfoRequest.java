package com.example.configuration.request;

import lombok.Data;

@Data
public class ApiMethodGeneralInfoRequest {
    private String httpMethod;
    private String endpoint;
    private String methodHeaders;
    private boolean paginated;
    private String paginationParamName;
    private String pageSizeParamName;
    private String totalPagesFieldInResponse;
    private String contentFieldInResponse;
    private String type;
}
