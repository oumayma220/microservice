package com.example.configuration.service;

import com.example.configuration.dao.entity.*;
import com.example.configuration.dao.repository.*;
import com.example.configuration.dto.FieldMappingDTO;
import com.example.configuration.dto.TiersDTO;
import com.example.configuration.dto.UserDTO;
import com.example.configuration.request.ApiMethodGeneralInfoRequest;
import com.example.configuration.request.ConfigGeneralInfoRequest;
import com.example.configuration.request.TiersGeneralInfoRequest;
import com.example.configuration.request.TiersRequest;
import com.jayway.jsonpath.JsonPath;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
    @Autowired
    private APIConfigurationRepository ApiConfigurationRepository;
    @Autowired
    private FieldMappingRepository FieldmappingRepository;

    @Transactional
    public Tiers createTiersWithConfig(TiersRequest request) {
        Integer currentTenantId  = getCurrentTenantId();

        Tiers tiers = getOrCreateTiers(request.getNom(),request.getEmail(),request.getNumero(),currentTenantId );

        RestAPIConfiguration apiConfig = getOrCreateRestAPIConfig(
                tiers,
                request.getConfigName(),
                request.getUrl(),
                request.getHeaders(),
                currentTenantId
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
                request.getContentFieldInResponse(),
                request.getType(),
                currentTenantId
        );

        request.getFieldMappings().forEach(mapping -> {
            addFieldMapping(apiMethod, mapping.getSource(), mapping.getTarget(), currentTenantId);
        });

        return tiers;
    }

    private Tiers getOrCreateTiers(String name,  String email, String numero ,Integer tenantid) {
        return tiersRepository.findByNom(name).orElseGet(() -> {
            Tiers tiers = new Tiers();
            tiers.setNom(name);
            tiers.setEmail(email);
            tiers.setNumero(numero);
            tiers.setTenantid(tenantid);


            return tiersRepository.save(tiers);
        });
    }
    private RestAPIConfiguration getOrCreateRestAPIConfig(Tiers tiers, String configName, String url, String headers ,Integer tenantid) {
        return restAPIConfigRepository.findByConfigName(configName).orElseGet(() -> {
            RestAPIConfiguration config = new RestAPIConfiguration();
            config.setConfigName(configName);
            config.setTiers(tiers);
            config.setUrl(url);
            config.setHeaders(headers);
            config.setTenantid(tenantid);
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
            String contentFieldInResponse,
            String type,
            Integer tenantid
    ) {
        return apiMethodRepository.findByHttpMethodAndEndpoint(httpMethod, endpoint).orElseGet(() -> {
            APIMethod apiMethod = new APIMethod();
            apiMethod.setHttpMethod(httpMethod);
            apiMethod.setEndpoint(endpoint);
            apiMethod.setHeaders(headers);
            apiMethod.setRestAPIConfig(restAPIConfig);
            apiMethod.setPaginated(paginated);
            apiMethod.setContentFieldInResponse(contentFieldInResponse);
            apiMethod.setType(type);
            if (paginated) {
                apiMethod.setPaginationParamName(paginationParamName);
                apiMethod.setPageSizeParamName(pageSizeParamName);
                apiMethod.setPageSize(10);
                apiMethod.setTotalPagesFieldInResponse(totalPagesFieldInResponse);
            }
            apiMethod.setTenantid(tenantid);
            return apiMethodRepository.save(apiMethod);
        });
    }

    private void addFieldMapping(APIMethod apiMethod, String source, String target,  Integer tenantid) {
        if (fieldMappingRepository.findByApiMethodAndSourceAndTarget(apiMethod, source, target).isEmpty()) {
            FieldMapping fieldMapping = new FieldMapping();
            fieldMapping.setApiMethod(apiMethod);
            fieldMapping.setSource(source);
            fieldMapping.setTarget(target);
            fieldMapping.setTenantid(tenantid);
            fieldMappingRepository.save(fieldMapping);
        }
    }

    @Transactional
    public Tiers updateTiersGeneralInfo(Long id, TiersGeneralInfoRequest request) {
        Tiers tiers = tiersRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tiers not found with id: " + id));
        if (tiersRepository.existsByEmail(request.getEmail()) && !tiers.getEmail().equals(request.getEmail())) {
            throw new IllegalArgumentException("Email déjà utilisé");
        }
        if (tiersRepository.existsByNumero(request.getNumero()) && !tiers.getNumero().equals(request.getNumero())) {
            throw new IllegalArgumentException("Numéro déjà utilisé");
        }
        tiers.setEmail(request.getEmail());
        tiers.setNumero(request.getNumero());
        tiers.setNom(request.getNom());
        return tiersRepository.save(tiers);
    }
    public RestAPIConfiguration updateConfigGeneralInfo(Long id, ConfigGeneralInfoRequest request){
        RestAPIConfiguration config = restAPIConfigRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("config not found with id: " + id));
        config.setConfigName(request.getConfigName());
        config.setUrl(request.getUrl());
        config.setHeaders(request.getHeaders());
        return restAPIConfigRepository.save(config);
    }
    public APIMethod updateApiMethod(Long id, ApiMethodGeneralInfoRequest request){
        APIMethod method = apiMethodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("method not found with id: " + id));
        method.setHttpMethod(request.getHttpMethod());
        method.setEndpoint(request.getEndpoint());
        method.setHeaders(request.getMethodHeaders());
        method.setPaginated(request.isPaginated());
        method.setPaginationParamName(request.getPaginationParamName());
        method.setPageSizeParamName(request.getPageSizeParamName());
        method.setTotalPagesFieldInResponse(request.getTotalPagesFieldInResponse());
        method.setContentFieldInResponse(request.getContentFieldInResponse());
        method.setType(request.getType());
        return apiMethodRepository.save(method);
    }
    public List<Tiers> getAllTiers() {
        Integer currentTenantId = getCurrentTenantId();
        return tiersRepository.findByTenantid(currentTenantId);
    }
    public List<RestAPIConfiguration> getConfigurationsByTiersId(Long tiersId) {
        Integer currentTenantId = getCurrentTenantId();
        return restAPIConfigRepository.findByTiers_IdAndTenantid(tiersId, currentTenantId);
    }

    public Integer getCurrentTenantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDTO) {
            UserDTO userDTO = (UserDTO) authentication.getPrincipal();
            return userDTO.getTenantid();
        }
        throw new RuntimeException("User not authenticated or tenant information missing");
    }
    @Transactional
    public void deleteTiers(Long tiersId) {
        Tiers tiers = tiersRepository.findById(tiersId)
                .orElseThrow(() -> new RuntimeException("Tiers non trouvé avec l'ID: " + tiersId));
        System.out.println("APIConfigurations à supprimer : " + tiers.getApiConfigurations());
        tiersRepository.delete(tiers);
    }
    @Transactional
    public void deleteConfig(Long configId) {
        APIConfiguration config = ApiConfigurationRepository.findById(configId)
                .orElseThrow(() -> new RuntimeException("Configuration non trouvée avec l'ID: " + configId));

        System.out.println("Suppression de la configuration avec l'ID: " + configId);
        ApiConfigurationRepository.delete(config);
    }
    @Transactional
    public void deleteApiMethod(Long MethodId) {
        APIMethod method = apiMethodRepository.findById(MethodId)
                .orElseThrow(() -> new RuntimeException("apimethod non trouvée avec l'ID: " + MethodId));
        System.out.println("Suppression de la apimethod avec l'ID: " + MethodId);
        apiMethodRepository.delete(method);
    }
    @Transactional
    public void deleteAllFieldMappingsByApiMethodId(Long MethodId) {
        APIMethod apiMethod = apiMethodRepository.findById(MethodId)
                .orElseThrow(() -> new RuntimeException("API Method not found with ID: " + MethodId));
        List<FieldMapping> fieldMappings = fieldMappingRepository.findByApiMethod(apiMethod);
        if (!fieldMappings.isEmpty()) {
            fieldMappingRepository.deleteAll(fieldMappings);
        }
    }

    public TiersDTO getTiersById(Long tiersId) {
        Integer currentTenantId = getCurrentTenantId();

        Tiers tiers = tiersRepository.findByIdAndTenantid(tiersId, currentTenantId)
                .orElseThrow(() -> new RuntimeException("Tiers non trouvé avec l'ID: " + tiersId + " pour ce locataire"));
        TiersDTO dto = new TiersDTO(tiers);

        dto.setNom(tiers.getNom());
        dto.setEmail(tiers.getEmail());
        dto.setNumero(tiers.getNumero());

        return dto;
    }
    @Transactional
    public RestAPIConfiguration addConfigToTiersById(Long tiersId, TiersRequest request) {

        Integer currentTenantId = getCurrentTenantId();

        Tiers tiers = tiersRepository.findByIdAndTenantid(tiersId, currentTenantId)
                .orElseThrow(() -> new RuntimeException("Tiers non trouvé avec l'ID: " + tiersId));

        if (restAPIConfigRepository.existsByTiersAndConfigName(tiers, request.getConfigName())) {
            throw new IllegalArgumentException("Une configuration avec ce nom existe déjà pour ce tiers.");
        }
        RestAPIConfiguration apiConfig = new RestAPIConfiguration();
        apiConfig.setConfigName(request.getConfigName());
        apiConfig.setUrl(request.getUrl());
        apiConfig.setHeaders(request.getHeaders());
        apiConfig.setTenantid(currentTenantId);
        apiConfig.setTiers(tiers);
        restAPIConfigRepository.save(apiConfig);
        APIMethod apiMethod = new APIMethod();
        apiMethod.setRestAPIConfig(apiConfig);
        apiMethod.setHttpMethod(request.getHttpMethod());
        apiMethod.setEndpoint(request.getEndpoint());
        apiMethod.setHeaders(request.getMethodHeaders());
        apiMethod.setPaginated(request.isPaginated());
        apiMethod.setContentFieldInResponse(request.getContentFieldInResponse());
        apiMethod.setType(request.getType());
        apiMethod.setTenantid(currentTenantId);

        if (request.isPaginated()) {
            apiMethod.setPaginationParamName(request.getPaginationParamName());
            apiMethod.setPageSizeParamName(request.getPageSizeParamName());
            apiMethod.setPageSize(10);
            apiMethod.setTotalPagesFieldInResponse(request.getTotalPagesFieldInResponse());
        }

        apiMethodRepository.save(apiMethod);

        request.getFieldMappings().forEach(mapping -> {
            FieldMapping fieldMapping = new FieldMapping();
            fieldMapping.setApiMethod(apiMethod);
            fieldMapping.setSource(mapping.getSource());
            fieldMapping.setTarget(mapping.getTarget());
            fieldMapping.setTenantid(currentTenantId);

            fieldMappingRepository.save(fieldMapping);
        });

        return apiConfig;
    }
    @Transactional
    public RestAPIConfiguration addApiMethodAndFieldMappingsToConfig(Long configId, TiersRequest request) {

        Integer currentTenantId = getCurrentTenantId();
        RestAPIConfiguration apiConfig = restAPIConfigRepository.findById(configId)
                .orElseThrow(() -> new RuntimeException("RestAPIConfiguration not found with id: " + configId));
        if (!apiConfig.getTenantid().equals(currentTenantId)) {
            throw new RuntimeException("Unauthorized access to this configuration");
        }
        APIMethod apiMethod = new APIMethod();
        apiMethod.setRestAPIConfig(apiConfig);
        apiMethod.setHttpMethod(request.getHttpMethod());
        apiMethod.setEndpoint(request.getEndpoint());
        apiMethod.setHeaders(request.getMethodHeaders());
        apiMethod.setPaginated(request.isPaginated());
        apiMethod.setContentFieldInResponse(request.getContentFieldInResponse());
        apiMethod.setType(request.getType());
        apiMethod.setTenantid(currentTenantId);
        if (request.isPaginated()) {
            apiMethod.setPaginationParamName(request.getPaginationParamName());
            apiMethod.setPageSizeParamName(request.getPageSizeParamName());
            apiMethod.setPageSize(10); // Valeur par défaut de pageSize
            apiMethod.setTotalPagesFieldInResponse(request.getTotalPagesFieldInResponse());
        }
        apiMethodRepository.save(apiMethod);
        System.out.println("APIMethod saved: " + apiMethod); // Log de confirmation
        request.getFieldMappings().forEach(mapping -> {
            FieldMapping fieldMapping = new FieldMapping();
            fieldMapping.setApiMethod(apiMethod);
            fieldMapping.setSource(mapping.getSource());
            fieldMapping.setTarget(mapping.getTarget());
            fieldMapping.setTenantid(currentTenantId);

            try {
                fieldMappingRepository.save(fieldMapping);
                System.out.println("FieldMapping saved: " + fieldMapping); // Log de confirmation
            } catch (Exception e) {
                System.err.println("Error saving field mapping: " + e.getMessage());
                throw new RuntimeException("Error saving field mapping", e);
            }
        });
        return apiConfig;
    }
    @Transactional
    public void addFieldMappingsByApiMethodId(Long apiMethodId, List<FieldMappingDTO> fieldMappingsRequest) {
        Integer currentTenantId = getCurrentTenantId();

        APIMethod apiMethod = apiMethodRepository.findById(apiMethodId)
                .orElseThrow(() -> new RuntimeException("API Method not found with ID: " + apiMethodId));
        if (!apiMethod.getTenantid().equals(currentTenantId)) {
            throw new RuntimeException("Unauthorized access to this API method");
        }
        fieldMappingsRequest.forEach(mappingRequest -> {
            Optional<FieldMapping> existingMapping = fieldMappingRepository.findByApiMethodAndSourceAndTarget(
                    apiMethod, mappingRequest.getSource(), mappingRequest.getTarget());
            if (existingMapping.isEmpty()) {
                FieldMapping fieldMapping = new FieldMapping();
                fieldMapping.setApiMethod(apiMethod);
                fieldMapping.setSource(mappingRequest.getSource());
                fieldMapping.setTarget(mappingRequest.getTarget());
                fieldMapping.setTenantid(currentTenantId);
                fieldMappingRepository.save(fieldMapping);
            }
        });
    }







}



