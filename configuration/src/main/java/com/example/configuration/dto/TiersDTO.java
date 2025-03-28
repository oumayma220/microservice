package com.example.configuration.dto;

import com.example.configuration.dao.entity.APIConfiguration;
import com.example.configuration.dao.entity.RestAPIConfiguration;
import com.example.configuration.dao.entity.Tiers;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class TiersDTO {
    private Long id;
    private String nom;
    private String email;
    private String numero;
    private List<String> configNames;
    private Integer tenantid;
    public TiersDTO(Tiers tiers) {
        this.id = tiers.getId();
        this.nom = tiers.getNom();
        this.email=tiers.getEmail();
        this.numero= tiers.getNumero();
        this.tenantid = tiers.getTenantid();
        this.configNames = tiers.getApiConfigurations().stream()
                .filter(config -> config != null && config.getConfigName() != null)
                .map(APIConfiguration::getConfigName)
                .collect(Collectors.toList());
    }

}
