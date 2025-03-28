package com.example.configuration.service;

import com.example.configuration.dao.entity.*;
import com.example.configuration.dao.repository.APIMethodRepository;
import com.example.configuration.dao.repository.FieldMappingRepository;
import com.example.configuration.dao.repository.RestAPIConfigRepository;
import com.example.configuration.dao.repository.TiersRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProductMappingService {
    private static final Logger logger = LoggerFactory.getLogger(ProductMappingService.class);

    @Autowired
    private RestAPIConfigRepository restAPIConfigRepository;

    @Autowired
    private APIMethodRepository apiMethodRepository;

    @Autowired
    private FieldMappingRepository fieldMappingRepository;
    @Autowired
    private TiersRepository tiersRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Product> importProducts(String configName, String endpoint) {
        RestAPIConfiguration config = restAPIConfigRepository.findByConfigName(configName)
                .orElseThrow(() -> new RuntimeException("API Configuration not found: " + configName));
        List<APIMethod> apiMethods = config.getApiMethods();
        if (apiMethods == null || apiMethods.isEmpty()) {
            logger.warn("No API Methods found for config: {}", configName);
            throw new RuntimeException("No API Methods found for config: " + configName);
        }
        APIMethod apiMethod = apiMethods.stream()
                .filter(method -> endpoint.equals(method.getEndpoint()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No API Method found for endpoint: " + endpoint));

        if (apiMethod.getEndpoint() == null || apiMethod.getEndpoint().isEmpty()) {
            logger.warn("Endpoint not defined in API method for config: {}", configName);
            throw new RuntimeException("Endpoint not defined in API method for config: " + configName);
        }

        List<FieldMapping> fieldMappings = apiMethod.getFieldMappings();

        if (fieldMappings == null || fieldMappings.isEmpty()) {
            logger.warn("No FieldMappings defined for API method: {}", apiMethod.getId());
            throw new RuntimeException("No FieldMappings defined for this API method.");
        }

        String mappingType = apiMethod.getType();

        if (mappingType == null || mappingType.isEmpty()) {
            logger.warn("Mapping type is not defined for API method: {}", apiMethod.getId());
            throw new RuntimeException("Mapping type is not defined for this API method.");
        }

        logger.info("Mapping type for API Method [{}]: {}", apiMethod.getId(), mappingType);

        if ("jsonPath".equalsIgnoreCase(mappingType)) {
            if (apiMethod.isPaginated()) {
                return fetchPaginatedProductsJsonPath(config, apiMethod);
            } else {
                return fetchSimpleProductsJsonPath(config, apiMethod);
            }
        } else if ("reflection".equalsIgnoreCase(mappingType)) {
            if (apiMethod.isPaginated()) {
                return fetchPaginatedProductsReflection(config, apiMethod);
            } else {
                return fetchSimpleProductsReflection(config, apiMethod);
            }
        } else {
            throw new RuntimeException("Unknown mapping type: " + mappingType);
        }
    }

    private List<Product> fetchPaginatedProductsJsonPath(RestAPIConfiguration config, APIMethod apiMethod) {
        List<Product> allProducts = new ArrayList<>();
        int page = 0;
        int size = apiMethod.getPageSize();
        int totalPages = 1;

        logger.info("Démarrage de la récupération paginée depuis {}", config.getUrl());

        while (page < totalPages) {
            String paginatedUrl = String.format("%s%s?%s=%d&%s=%d",
                    config.getUrl(),
                    apiMethod.getEndpoint(),
                    apiMethod.getPaginationParamName(), page,
                    apiMethod.getPageSizeParamName(), size
            );

            logger.debug("Appel API paginé : {}", paginatedUrl);

            Object response = restTemplate.getForObject(paginatedUrl, Object.class);

            if (response == null) {
                logger.error("Réponse API vide ou nulle pour la page {}", page);
                break;
            }

            String responseJson;
            try {
                responseJson = objectMapper.writeValueAsString(response);
            } catch (JsonProcessingException e) {
                logger.error("Erreur lors de la conversion de la réponse en JSON : {}", e.getMessage());
                throw new RuntimeException("Erreur lors de la conversion de la réponse en JSON", e);
            }

            logger.debug("Réponse brute JSON : {}", responseJson);

            List<Map<String, Object>> pageResults;

            if (apiMethod.getContentFieldInResponse() != null && !apiMethod.getContentFieldInResponse().isEmpty()) {
                try {
                    pageResults = JsonPath.read(responseJson, apiMethod.getContentFieldInResponse());
                } catch (Exception e) {
                    logger.error("Erreur lors de la lecture du contentFieldInResponse '{}' : {}", apiMethod.getContentFieldInResponse(), e.getMessage());
                    break;
                }
            } else {
                logger.warn("ContentFieldInResponse non défini, impossible d'extraire les données !");
                break;
            }

            if (pageResults != null && !pageResults.isEmpty()) {
                logger.info("Nombre de produits récupérés pour la page {} : {}", page, pageResults.size());
                for (Map<String, Object> productData : pageResults) {
                    Product product = mapProductFromResponse(productData, apiMethod);
                    allProducts.add(product);
                }
            }

            if (apiMethod.getTotalPagesFieldInResponse() != null && !apiMethod.getTotalPagesFieldInResponse().isEmpty()) {
                try {
                    totalPages = JsonPath.read(responseJson, apiMethod.getTotalPagesFieldInResponse());
                    logger.debug("Total pages détecté : {}", totalPages);
                } catch (Exception e) {
                    logger.error("Erreur lors de la lecture de totalPagesFieldInResponse '{}'", apiMethod.getTotalPagesFieldInResponse());
                    totalPages = page + 1;
                }
            } else {
                totalPages = (pageResults == null || pageResults.size() < size) ? page + 1 : totalPages + 1;
            }

            page++;
        }

        logger.info("Total des produits importés avec pagination : {}", allProducts.size());

        return allProducts;
    }

    private List<Product> fetchSimpleProductsJsonPath(RestAPIConfiguration config, APIMethod apiMethod) {
        String url = config.getUrl() + apiMethod.getEndpoint();
        logger.debug("Appel API simple : {}", url);

        Object apiResponse = restTemplate.getForObject(url, Object.class);
        if (apiResponse == null) {
            logger.error("Réponse API vide ou nulle.");
            throw new RuntimeException("Réponse API vide ou nulle.");
        }

        // Conversion de la réponse en JSON
        String responseJson;
        try {
            responseJson = objectMapper.writeValueAsString(apiResponse);
        } catch (JsonProcessingException e) {
            logger.error("Erreur lors de la conversion de la réponse en JSON : {}", e.getMessage());
            throw new RuntimeException("Erreur lors de la conversion de la réponse en JSON", e);
        }

        logger.debug("Réponse brute JSON : {}", responseJson);
        List<Map<String, Object>> productsData = new ArrayList<>();

        if (apiMethod.getContentFieldInResponse() != null && !apiMethod.getContentFieldInResponse().isEmpty()) {
            try {
                productsData = JsonPath.read(responseJson, apiMethod.getContentFieldInResponse());
                logger.debug("Données extraites avec JsonPath : {}", productsData.size());
            } catch (Exception e) {
                logger.error("Erreur lors de la lecture avec JsonPath '{}' : {}",
                        apiMethod.getContentFieldInResponse(), e.getMessage());
                throw new RuntimeException("Erreur lors de l'extraction des données avec JsonPath", e);
            }
        } else {
            if (apiResponse instanceof List) {
                productsData = (List<Map<String, Object>>) apiResponse;
            } else {
                logger.error("Réponse API inattendue. Une liste était attendue.");
                throw new RuntimeException("Réponse API inattendue. Format non pris en charge.");
            }
        }

        logger.info("Nombre de produits récupérés : {}", productsData.size());

        List<Product> products = new ArrayList<>();
        for (Map<String, Object> productData : productsData) {
            Product product = mapProductFromResponse(productData, apiMethod);
            products.add(product);
        }

        logger.info("Total des produits importés sans pagination : {}", products.size());
        return products;
    }
    private List<Product> fetchPaginatedProductsReflection(RestAPIConfiguration config, APIMethod apiMethod) {

        List<Product> allProducts = new ArrayList<>();
        int page = 0;
        int size = apiMethod.getPageSize();
        int totalPages = 1;

        logger.info("Démarrage de la récupération paginée depuis {}", config.getUrl());

        while (page < totalPages) {
            String paginatedUrl = String.format("%s%s?%s=%d&%s=%d",
                    config.getUrl(),
                    apiMethod.getEndpoint(),
                    apiMethod.getPaginationParamName(), page,
                    apiMethod.getPageSizeParamName(), size
            );

            logger.debug("Appel API paginé : {}", paginatedUrl);

            Map<String, Object> response = restTemplate.getForObject(paginatedUrl, Map.class);

            if (response == null) {
                logger.error("Réponse API vide ou nulle pour la page {}", page);
                break;
            }

            List<Map<String, Object>> pageResults = null;

            // CAS 1 : contentField est défini
            if (apiMethod.getContentFieldInResponse() != null && !apiMethod.getContentFieldInResponse().isEmpty()) {
                if (!response.containsKey(apiMethod.getContentFieldInResponse())) {
                    logger.error("Contenu introuvable dans la réponse API pour la page {}", page);
                    break;
                }

                pageResults = (List<Map<String, Object>>) response.get(apiMethod.getContentFieldInResponse());
            }
            else {
                try {
                    pageResults = (List<Map<String, Object>>) (Object) response;
                } catch (ClassCastException e) {
                    logger.error("La réponse API n'est pas une liste valide pour la page {}", page);
                    break;
                }
            }

            if (pageResults != null) {
                logger.info("Nombre de produits récupérés pour la page {} : {}", page, pageResults.size());
                for (Map<String, Object> productData : pageResults) {
                    Product product = mapProductFromResponse(productData, apiMethod);
                    allProducts.add(product);
                }
            }

            if (apiMethod.getTotalPagesFieldInResponse() != null) {
                totalPages = ((Number) response.get(apiMethod.getTotalPagesFieldInResponse())).intValue();
            } else {
                totalPages = (pageResults == null || pageResults.size() < size) ? page + 1 : totalPages + 1;
            }

            page++;
        }

        logger.info("Total des produits importés avec pagination : {}", allProducts.size());

        return allProducts;
    }


    private List<Product> fetchSimpleProductsReflection(RestAPIConfiguration config, APIMethod apiMethod) {

        String url = config.getUrl() + apiMethod.getEndpoint();

        logger.debug("Appel API simple : {}", url);

        // On récupère la réponse générique (ça peut être une Map ou une List)
        Object apiResponse = restTemplate.getForObject(url, Object.class);

        if (apiResponse == null) {
            logger.error("Réponse API vide ou nulle.");
            throw new RuntimeException("Réponse API vide ou nulle.");
        }

        List<Map<String, Object>> productsData = new ArrayList<>();

        if (apiMethod.getContentFieldInResponse() != null && !apiMethod.getContentFieldInResponse().isEmpty()) {
            if (!(apiResponse instanceof Map)) {
                logger.error("Réponse API inattendue. Un Map était attendu pour accéder à '{}'", apiMethod.getContentFieldInResponse());
                throw new RuntimeException("Réponse API inattendue.");
            }

            Map<String, Object> responseMap = (Map<String, Object>) apiResponse;

            Object content = responseMap.get(apiMethod.getContentFieldInResponse());

            if (content instanceof List) {
                productsData = (List<Map<String, Object>>) content;
            } else {
                logger.error("Le champ '{}' ne contient pas une liste.", apiMethod.getContentFieldInResponse());
                throw new RuntimeException("Le champ '" + apiMethod.getContentFieldInResponse() + "' ne contient pas une liste.");
            }
        } else {
            if (apiResponse instanceof List) {
                productsData = (List<Map<String, Object>>) apiResponse;
            } else {
                logger.error("Réponse API inattendue. Une liste était attendue.");
                throw new RuntimeException("Réponse API inattendue.");
            }
        }

        logger.info("Nombre de produits récupérés : {}", productsData.size());

        List<Product> products = new ArrayList<>();

        for (Map<String, Object> productData : productsData) {
            Product product = mapProductFromResponse(productData, apiMethod);
            products.add(product);
        }

        logger.info("Total des produits importés sans pagination : {}", products.size());

        return products;
    }

    private Product mapProductFromResponse(Map<String, Object> productData, APIMethod apiMethod) {
        Product product = new Product();

        List<FieldMapping> fieldMappings = fieldMappingRepository.findByApiMethod(apiMethod);

        if (fieldMappings == null || fieldMappings.isEmpty()) {
            logger.warn("Aucun mapping trouvé pour l'API Method : {}", apiMethod.getEndpoint());
            return product; // Ou tu peux throw si c'est bloquant
        }

        String mappingType = apiMethod.getType();

        if (mappingType == null || mappingType.isEmpty()) {
            logger.warn("Le type de mapping n'est pas défini pour l'API Method : {}", apiMethod.getId());
            throw new RuntimeException("Le type de mapping n'est pas défini pour cette API Method.");
        }

        try {
            switch (mappingType.toLowerCase()) {
                case "jsonpath":
                    JsonPathMappingStrategy jsonPathStrategy = new JsonPathMappingStrategy();
                    for (FieldMapping fieldMapping : fieldMappings) {
                        jsonPathStrategy.mapFields(product, productData, fieldMapping);
                    }
                    break;

                case "reflection":
                    ReflectionMappingStrategy reflectionStrategy = new ReflectionMappingStrategy();
                    for (FieldMapping fieldMapping : fieldMappings) {
                        reflectionStrategy.mapFields(product, productData, fieldMapping);
                    }
                    break;

                default:
                    logger.error("Type de mapping inconnu : {}", mappingType);
                    throw new RuntimeException("Type de mapping inconnu : " + mappingType);
            }
        } catch (Exception e) {
            logger.error("Erreur lors du mapping du produit pour l'API Method [{}]: {}", apiMethod.getId(), e.getMessage(), e);
            throw new RuntimeException("Erreur lors du mapping du produit", e);
        }

        return product;
    }

    public List<Product> importAllProductsFromAllTiers() {
        logger.info("Début de l'importation de tous les produits de tous les tiers");

        List<Product> allProducts = new ArrayList<>();

        List<RestAPIConfiguration> configurations = restAPIConfigRepository.findAll();

        if (configurations == null || configurations.isEmpty()) {
            logger.warn("Aucune configuration API trouvée.");
            return allProducts;
        }

        for (RestAPIConfiguration config : configurations) {
            String configName = config.getConfigName();
            logger.info("Traitement de la configuration : {}", configName);

            List<APIMethod> apiMethods = config.getApiMethods();

            if (apiMethods == null || apiMethods.isEmpty()) {
                logger.warn("Aucune méthode API trouvée pour la configuration : {}", configName);
                continue;
            }

            for (APIMethod apiMethod : apiMethods) {
                String endpoint = apiMethod.getEndpoint();
                logger.info("Traitement de l'endpoint : {} pour la configuration : {}", endpoint, configName);

                try {
                    List<Product> productsFromEndpoint = importProducts(configName, endpoint);
                    logger.info("Nombre de produits récupérés depuis {} : {}", endpoint, productsFromEndpoint.size());
                    allProducts.addAll(productsFromEndpoint);
                } catch (Exception e) {
                    logger.error("Erreur lors de l'importation des produits pour config '{}' et endpoint '{}': {}",
                            configName, endpoint, e.getMessage(), e);
                }
            }
        }

        logger.info("Importation terminée. Nombre total de produits récupérés : {}", allProducts.size());

        return allProducts;
    }

    public List<Product> importProductsForTier(Long tierId) {
        logger.info("Début de l'importation des produits pour le tier : {}", tierId);

        Tiers tier = tiersRepository.findById(tierId)
                .orElseThrow(() -> new RuntimeException("Tier not found with ID: " + tierId));

        List<Product> allProducts = new ArrayList<>();

        List<RestAPIConfiguration> configurations = restAPIConfigRepository.findByTiers_Id(tierId);

        if (configurations == null || configurations.isEmpty()) {
            logger.warn("Aucune configuration API trouvée pour le tier : {}", tierId);
            return allProducts;
        }

        for (RestAPIConfiguration config : configurations) {
            String configName = config.getConfigName();
            logger.info("Traitement de la configuration : {}", configName);

            List<APIMethod> getApiMethods = config.getApiMethods().stream()
                    .filter(method -> "GET".equalsIgnoreCase(method.getHttpMethod()))
                    .collect(Collectors.toList());

            if (getApiMethods.isEmpty()) {
                logger.warn("Aucune méthode GET trouvée pour la configuration : {}", configName);
                continue;
            }

            for (APIMethod apiMethod : getApiMethods) {
                String endpoint = apiMethod.getEndpoint();
                logger.info("Traitement de l'endpoint GET : {} pour la configuration : {}", endpoint, configName);

                try {
                    List<Product> productsFromEndpoint = importProducts(configName, endpoint);
                    logger.info("Nombre de produits récupérés depuis {} : {}", endpoint, productsFromEndpoint.size());
                    allProducts.addAll(productsFromEndpoint);
                } catch (Exception e) {
                    logger.error("Erreur lors de l'importation des produits pour config '{}' et endpoint '{}': {}",
                            configName, endpoint, e.getMessage(), e);
                }
            }
        }

        logger.info("Importation terminée pour le tier {}. Nombre total de produits récupérés : {}",
                tierId, allProducts.size());

        return allProducts;
    }







}
