package com.example.configuration.configuration;

import com.example.configuration.dao.entity.APIMethod;
import com.example.configuration.dao.entity.FieldMapping;
import com.example.configuration.dao.entity.RestAPIConfiguration;
import com.example.configuration.dao.entity.Tiers;
import com.example.configuration.dao.repository.APIMethodRepository;
import com.example.configuration.dao.repository.FieldMappingRepository;
import com.example.configuration.dao.repository.RestAPIConfigRepository;
import com.example.configuration.dao.repository.TiersRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FakeProductConfig {
    @Autowired
    private TiersRepository tiersRepository;

    @Autowired
    private RestAPIConfigRepository restAPIConfigRepository;

    @Autowired
    private APIMethodRepository apiMethodRepository;

    @Autowired
    private FieldMappingRepository fieldMappingRepository;

    @Bean
    @Transactional
    public Tiers getFakeProduct() {
        Tiers tunisiaNetTiers = getOrCreateTiers("FakeProduct");

        RestAPIConfiguration restAPIConfiguration = getOrCreateRestAPIConfig(
                tunisiaNetTiers,
                "FakeProduct API",
                "http://51.77.116.35:8080",
                "Content-Type: application/json"
        );

        APIMethod apiMethod = getOrCreateAPIMethod(restAPIConfiguration, "GET", "/fake-product/product", "Content-Type: application/json");

        createFieldMappings(apiMethod);

        return tunisiaNetTiers;
    }

    private Tiers getOrCreateTiers(String name) {
        return tiersRepository.findByNom(name).orElseGet(() -> {
            Tiers tiers = new Tiers();
            tiers.setNom(name);
            return tiersRepository.save(tiers);
        });
    }

    private RestAPIConfiguration getOrCreateRestAPIConfig(Tiers tiers, String configName, String url, String headers) {
        return restAPIConfigRepository.findByConfigName(configName).orElseGet(() -> {
            RestAPIConfiguration config = new RestAPIConfiguration();
            config.setConfigName(configName);
            config.setTiers(tiers);
            config.setUrl(url);
            config.setHeaders(headers);
            return restAPIConfigRepository.save(config);
        });
    }

    private APIMethod getOrCreateAPIMethod(RestAPIConfiguration restAPIConfig, String httpMethod, String endpoint, String headers) {
        // Vérifier si le APIMethod existe déjà
        return apiMethodRepository.findByHttpMethodAndEndpoint(httpMethod, endpoint).orElseGet(() -> {
            APIMethod apiMethod = new APIMethod();
            apiMethod.setHttpMethod(httpMethod);
            apiMethod.setEndpoint(endpoint);
            apiMethod.setHeaders(headers);
            apiMethod.setRestAPIConfig(restAPIConfig);
            apiMethod.setPaginated(true);
            apiMethod.setPaginationParamName("page");
            apiMethod.setPageSizeParamName("size");
            apiMethod.setPageSize(10);
            apiMethod.setTotalPagesFieldInResponse("$.totalPages");
            apiMethod.setContentFieldInResponse("$.content[*]");
            apiMethod.setType("jsonPath");
            return apiMethodRepository.save(apiMethod);
        });
    }

    private void createFieldMappings(APIMethod apiMethod) {
        addFieldMapping(apiMethod, "$.title", "name");
        addFieldMapping(apiMethod, "$.description", "description");
        addFieldMapping(apiMethod, "$.price", "price");
        addFieldMapping(apiMethod, "$.image", "url");
        addFieldMapping(apiMethod, "$.ref", "reference");

    }

    private void addFieldMapping(APIMethod apiMethod, String source, String target) {
        if (fieldMappingRepository.findByApiMethodAndSourceAndTarget(apiMethod, source, target).isEmpty()) {
            FieldMapping fieldMapping = new FieldMapping();
            fieldMapping.setSource(source);
            fieldMapping.setTarget(target);
            fieldMapping.setApiMethod(apiMethod);
            fieldMappingRepository.save(fieldMapping);
        }
    }

}
