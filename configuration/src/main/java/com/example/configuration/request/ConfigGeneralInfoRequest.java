package com.example.configuration.request;

import lombok.Data;

@Data
public class ConfigGeneralInfoRequest {
    private String configName;
    private String url;
    private String headers;
}
