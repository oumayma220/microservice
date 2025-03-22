package com.example.configuration.service;

import com.example.configuration.dao.entity.*;
import com.example.configuration.dao.repository.APIMethodRepository;
import com.example.configuration.dao.repository.FieldMappingRepository;
import com.example.configuration.dao.repository.RestAPIConfigRepository;
import com.example.configuration.dao.repository.TiersRepository;
import com.example.configuration.dto.TiersDTO;
import com.example.configuration.dto.UserDTO;
import com.example.configuration.request.TiersGeneralInfoRequest;
import com.example.configuration.request.TiersRequest;
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
    public Tiers updateTiersWithConfig(String tiersName, String configName, TiersRequest request) {
        Tiers tiers = tiersRepository.findByNom(tiersName)
                .orElseThrow(() -> new RuntimeException("Tiers not found with name: " + tiersName));
        tiers.setEmail(request.getEmail());
        tiers.setNumero(request.getNumero());

        RestAPIConfiguration apiConfig = restAPIConfigRepository.findByTiersAndConfigName(tiers, configName)
                .orElseThrow(() -> new RuntimeException("API Configuration not found for: " + configName));
        apiConfig.setUrl(request.getUrl());
        apiConfig.setHeaders(request.getHeaders());
        restAPIConfigRepository.save(apiConfig);

        // 4. Recherche de la méthode API liée à cette configuration
        APIMethod apiMethod = apiMethodRepository.findByRestAPIConfig(apiConfig)
                .orElseThrow(() -> new RuntimeException("API Method not found for config: " + configName));

        // 5. Mise à jour des détails de l'API method
        apiMethod.setHttpMethod(request.getHttpMethod());
        apiMethod.setEndpoint(request.getEndpoint());
        apiMethod.setHeaders(request.getMethodHeaders());
        apiMethod.setPaginated(request.isPaginated());
        apiMethod.setContentFieldInResponse(request.getContentFieldInResponse());
        if (request.isPaginated()) {
            apiMethod.setPaginationParamName(request.getPaginationParamName());
            apiMethod.setPageSizeParamName(request.getPageSizeParamName());
            apiMethod.setPageSize(10); // Tu peux rendre ça dynamique si besoin
            apiMethod.setTotalPagesFieldInResponse(request.getTotalPagesFieldInResponse());
        } else {
            apiMethod.setPaginationParamName(null);
            apiMethod.setPageSizeParamName(null);
            apiMethod.setPageSize(null);
            apiMethod.setTotalPagesFieldInResponse(null);
        }
        apiMethod.setType(request.getType());
        apiMethodRepository.save(apiMethod);
        List<FieldMapping> existingMappings = fieldMappingRepository.findByApiMethod(apiMethod);

        if (!existingMappings.isEmpty()) {
            fieldMappingRepository.deleteAll(existingMappings);
        }

        Integer currentTenantId = getCurrentTenantId();

        request.getFieldMappings().forEach(mapping -> {
            FieldMapping fieldMapping = new FieldMapping();
            fieldMapping.setApiMethod(apiMethod);
            fieldMapping.setSource(mapping.getSource());
            fieldMapping.setTarget(mapping.getTarget());
            fieldMapping.setTenantid(currentTenantId);

            fieldMappingRepository.save(fieldMapping);
        });

        return tiers;
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

    public List<Tiers> getAllTiers() {

        Integer currentTenantId = getCurrentTenantId();
        return tiersRepository.findByTenantid(currentTenantId);
    }
    public List<RestAPIConfiguration> getConfigurationsByTiersId(Long tiersId) {
        Integer currentTenantId = getCurrentTenantId();
        return restAPIConfigRepository.findByTiers_IdAndTenantid(tiersId, currentTenantId);
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
    public TiersDTO getTiersById(Long tiersId) {
        Integer currentTenantId = getCurrentTenantId();

        Tiers tiers = tiersRepository.findByIdAndTenantid(tiersId, currentTenantId)
                .orElseThrow(() -> new RuntimeException("Tiers non trouvé avec l'ID: " + tiersId + " pour ce locataire"));

        // Initialisation du DTO
        TiersDTO dto = new TiersDTO(tiers);

        dto.setNom(tiers.getNom());
        dto.setEmail(tiers.getEmail());
        dto.setNumero(tiers.getNumero());

        return dto;
    }


}



