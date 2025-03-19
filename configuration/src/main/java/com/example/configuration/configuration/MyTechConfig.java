package com.example.configuration.configuration;

import com.example.configuration.dao.entity.APIMethod;
import com.example.configuration.dao.entity.FieldMapping;
import com.example.configuration.dao.entity.RestAPIConfiguration;
import com.example.configuration.dao.entity.Tiers;
import com.example.configuration.dao.repository.APIMethodRepository;
import com.example.configuration.dao.repository.FieldMappingRepository;
import com.example.configuration.dao.repository.RestAPIConfigRepository;
import com.example.configuration.dao.repository.TiersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyTechConfig {

    @Autowired
    private TiersRepository tiersRepository;

    @Autowired
    private RestAPIConfigRepository restAPIConfigRepository;

    @Autowired
    private APIMethodRepository apiMethodRepository;

    @Autowired
    private FieldMappingRepository fieldMappingRepository;

    @Bean
    public Tiers getMyTech() {
        Tiers myTechTiers = getOrCreateTiers("MyTech");

        RestAPIConfiguration restAPIConfiguration = getOrCreateRestAPIConfig(myTechTiers, "MyTech API", "http://51.77.116.35:8080", "Content-Type: application/json");

        APIMethod apiMethod = getOrCreateAPIMethod(restAPIConfiguration, "GET", "/my-tech/products", "Content-Type: application/json");

        createFieldMappings(apiMethod);

        return myTechTiers;
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
        return apiMethodRepository.findByHttpMethodAndEndpoint(httpMethod, endpoint).orElseGet(() -> {
            APIMethod apiMethod = new APIMethod();
            apiMethod.setHttpMethod(httpMethod);
            apiMethod.setEndpoint(endpoint);
            apiMethod.setHeaders(headers);
            apiMethod.setRestAPIConfig(restAPIConfig);
            apiMethod.setPaginated(false);
            return apiMethodRepository.save(apiMethod);
        });
    }

    private void createFieldMappings(APIMethod apiMethod) {
        addFieldMapping(apiMethod, "$.product_name", "name");
        addFieldMapping(apiMethod, "$.retail_price", "price");
        addFieldMapping(apiMethod, "$.short_summary", "description");
        addFieldMapping(apiMethod, "$.product_url", "url");
        addFieldMapping(apiMethod, "$.referenceNo", "reference");



    }

    private void addFieldMapping(APIMethod apiMethod, String source, String target) {
        if (fieldMappingRepository.findByApiMethodAndSourceAndTarget(apiMethod, source, target).isEmpty()) {
            FieldMapping fieldMapping = new FieldMapping();
            fieldMapping.setType("jsonPath");
            fieldMapping.setSource(source);
            fieldMapping.setTarget(target);
            fieldMapping.setApiMethod(apiMethod);
            fieldMappingRepository.save(fieldMapping);
        }
    }
}