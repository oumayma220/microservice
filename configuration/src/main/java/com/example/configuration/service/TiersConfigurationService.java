package com.example.configuration.service;

import com.example.configuration.dao.entity.*;
import com.example.configuration.dao.repository.APIMethodRepository;
import com.example.configuration.dao.repository.FieldMappingRepository;
import com.example.configuration.dao.repository.RestAPIConfigRepository;
import com.example.configuration.dao.repository.TiersRepository;
import com.example.configuration.dto.FieldMappingDTO;
import com.example.configuration.dto.TiersRequest;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TiersConfigurationService {
    @Autowired
    private TiersRepository tiersRepository;

    @Autowired
    private RestAPIConfigRepository restAPIConfigRepository;

    @Autowired
    private APIMethodRepository apiMethodRepository;

    @Autowired
    private FieldMappingRepository fieldMappingRepository;

    @Transactional
    public Tiers createTiersWithConfig(TiersRequest request) {
        Tiers tiers = getOrCreateTiers(request.getNom());

        RestAPIConfiguration apiConfig = getOrCreateRestAPIConfig(
                tiers,
                request.getConfigName(),
                request.getUrl(),
                request.getHeaders()
        );

        APIMethod apiMethod = getOrCreateAPIMethod(
                apiConfig,
                request.getHttpMethod(),
                request.getEndpoint(),
                request.getMethodHeaders(),
                request.isPaginated(),
                request.getPaginationParamName(),
                request.getPageSizeParamName(),
                request.getTotalPagesFieldInResponse(),
                request.getContentFieldInResponse()
        );

        request.getFieldMappings().forEach(mapping -> {
            addFieldMapping(apiMethod, mapping.getSource(), mapping.getTarget(), mapping.getType());
        });

        return tiers;
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

    private APIMethod getOrCreateAPIMethod(
            RestAPIConfiguration restAPIConfig,
            String httpMethod,
            String endpoint,
            String headers,
            boolean paginated,
            String paginationParamName,
            String pageSizeParamName,
            String totalPagesFieldInResponse,
            String contentFieldInResponse
    ) {
        return apiMethodRepository.findByHttpMethodAndEndpoint(httpMethod, endpoint).orElseGet(() -> {
            APIMethod apiMethod = new APIMethod();
            apiMethod.setHttpMethod(httpMethod);
            apiMethod.setEndpoint(endpoint);
            apiMethod.setHeaders(headers);
            apiMethod.setRestAPIConfig(restAPIConfig);
            apiMethod.setPaginated(paginated);
            apiMethod.setContentFieldInResponse(contentFieldInResponse);
            if (paginated) {
                apiMethod.setPaginationParamName(paginationParamName);
                apiMethod.setPageSizeParamName(pageSizeParamName);
                apiMethod.setPageSize(10);
                apiMethod.setTotalPagesFieldInResponse(totalPagesFieldInResponse);
            }
            return apiMethodRepository.save(apiMethod);
        });
    }

    private void addFieldMapping(APIMethod apiMethod, String source, String target, String type) {
        if (fieldMappingRepository.findByApiMethodAndSourceAndTarget(apiMethod, source, target).isEmpty()) {
            FieldMapping fieldMapping = new FieldMapping();
            fieldMapping.setApiMethod(apiMethod);
            fieldMapping.setSource(source);
            fieldMapping.setTarget(target);
            fieldMapping.setType(type);
            fieldMappingRepository.save(fieldMapping);
        }
    }
    @Transactional
    public Tiers updateTiersWithConfig(String tiersName, String configName, TiersRequest request) {

        Tiers tiers = tiersRepository.findByNom(tiersName)
                .orElseThrow(() -> new RuntimeException("Tiers not found with name: " + tiersName));

        RestAPIConfiguration apiConfig = restAPIConfigRepository.findByTiersAndConfigName(tiers, configName)
                .orElseThrow(() -> new RuntimeException("API Configuration not found for: " + configName));

        apiConfig.setUrl(request.getUrl());
        apiConfig.setHeaders(request.getHeaders());
        restAPIConfigRepository.save(apiConfig);

        APIMethod apiMethod = apiMethodRepository.findByRestAPIConfig(apiConfig)
                .orElseThrow(() -> new RuntimeException("API Method not found for config: " + configName));

        apiMethod.setHttpMethod(request.getHttpMethod());
        apiMethod.setEndpoint(request.getEndpoint());
        apiMethod.setHeaders(request.getMethodHeaders());
        apiMethod.setPaginated(request.isPaginated());
        apiMethod.setContentFieldInResponse(request.getContentFieldInResponse());
        if (request.isPaginated()) {
            apiMethod.setPaginationParamName(request.getPaginationParamName());
            apiMethod.setPageSizeParamName(request.getPageSizeParamName());
            apiMethod.setPageSize(10);
            apiMethod.setTotalPagesFieldInResponse(request.getTotalPagesFieldInResponse());
        }

        apiMethodRepository.save(apiMethod);

        // Supprime les anciens mappings et ajoute les nouveaux
        List<FieldMapping> existingMappings = fieldMappingRepository.findByApiMethod(apiMethod);
        fieldMappingRepository.deleteAll(existingMappings);

        request.getFieldMappings().forEach(mapping -> {
            FieldMapping fieldMapping = new FieldMapping();
            fieldMapping.setApiMethod(apiMethod);
            fieldMapping.setSource(mapping.getSource());
            fieldMapping.setTarget(mapping.getTarget());
            fieldMapping.setType(mapping.getType());
            fieldMappingRepository.save(fieldMapping);
        });

        return tiers;
    }

    public List<Tiers> getAllTiers() {
        return tiersRepository.findAll();
    }
    public List<RestAPIConfiguration> getConfigurationsByTiersId(Long tiersId) {
        return restAPIConfigRepository.findByTiers_Id(tiersId);
    }

    public List<RestAPIConfiguration> getConfigurationsByTiersNom(String nomTiers) {
        return restAPIConfigRepository.findByTiers_Nom(nomTiers);
    }
    @Transactional
    public void deleteConfigurationById(Long configId) {
        RestAPIConfiguration config = restAPIConfigRepository.findById(configId)
                .orElseThrow(() -> new RuntimeException("Configuration not found"));

        Optional<APIMethod> apiMethodOpt = apiMethodRepository.findByRestAPIConfig(config);
        apiMethodOpt.ifPresent(apiMethodRepository::delete);
        restAPIConfigRepository.delete(config);
    }


    @Transactional
    public void deleteConfigurationByTiers(Long tiersId, Long configId) {
        RestAPIConfiguration config = restAPIConfigRepository.findByIdAndTiers_Id(configId, tiersId)
                .orElseThrow(() -> new RuntimeException("Configuration not found for the specified Tiers"));

        Optional<APIMethod> apiMethodOpt = apiMethodRepository.findByRestAPIConfig(config);
        apiMethodOpt.ifPresent(apiMethodRepository::delete);

        restAPIConfigRepository.delete(config);
    }



}



