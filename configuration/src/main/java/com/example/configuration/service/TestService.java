package com.example.configuration.service;
import com.example.configuration.dao.entity.Product;
import com.example.configuration.dto.FieldMappingDTO;
import com.example.configuration.request.TiersRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
@Service
public class TestService {
    private static final Logger logger = LoggerFactory.getLogger(ProductMappingService.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    public List<Product> importProducts(TiersRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("TiersRequest cannot be null");
        }

        if (StringUtils.isEmpty(request.getUrl())) {
            throw new IllegalArgumentException("API URL is required");
        }

        String endpoint = request.getEndpoint();
        List<FieldMappingDTO> fieldMappings = request.getFieldMappings();

        if (fieldMappings == null || fieldMappings.isEmpty()) {
            throw new RuntimeException("FieldMappings cannot be null or empty");
        }

        String mappingType = request.getType();
        if (mappingType == null || mappingType.isEmpty()) {
            throw new RuntimeException("Mapping type is not defined for this API method.");
        }

        System.out.println("Mapping type: " + mappingType);

        if ("jsonPath".equalsIgnoreCase(mappingType)) {
            if (request.isPaginated()) {
                return fetchPaginatedProductsJsonPath(request);
            } else {
                return fetchSimpleProductsJsonPath(request);
            }
        } else if ("reflection".equalsIgnoreCase(mappingType)) {
            if (request.isPaginated()) {
                return fetchPaginatedProductsReflection(request);
            } else {
                return fetchSimpleProductsReflection(request);
            }
        } else {
            throw new RuntimeException("Unknown mapping type: " + mappingType);
        }
    }
    private List<Product> fetchPaginatedProductsJsonPath(TiersRequest request) {
        List<Product> allProducts = new ArrayList<>();
        int page = 0;
        int size = 10;
        int totalPages = 1;

        logger.info("Démarrage de la récupération paginée depuis {}", request.getUrl());

        while (page < totalPages) {
            String paginatedUrl = String.format("%s%s?%s=%d&%s=%d",
                    request.getUrl(),
                    request.getEndpoint(),
                    request.getPaginationParamName(), page,
                    request.getPageSizeParamName(), size
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

            if (request.getContentFieldInResponse() != null && !request.getContentFieldInResponse().isEmpty()) {
                try {
                    pageResults = JsonPath.read(responseJson, request.getContentFieldInResponse());
                } catch (Exception e) {
                    logger.error("Erreur lors de la lecture du contentFieldInResponse '{}' : {}", request.getContentFieldInResponse(), e.getMessage());
                    break;
                }
            } else {
                logger.warn("ContentFieldInResponse non défini, impossible d'extraire les données !");
                break;
            }

            if (pageResults != null && !pageResults.isEmpty()) {
                logger.info("Nombre de produits récupérés pour la page {} : {}", page, pageResults.size());
                for (Map<String, Object> productData : pageResults) {
                    Product product = mapProductFromResponse(productData, request);
                    allProducts.add(product);
                }
            }

            if (request.getTotalPagesFieldInResponse() != null && !request.getTotalPagesFieldInResponse().isEmpty()) {
                try {
                    totalPages = JsonPath.read(responseJson, request.getTotalPagesFieldInResponse());
                    logger.debug("Total pages détecté : {}", totalPages);
                } catch (Exception e) {
                    logger.error("Erreur lors de la lecture de totalPagesFieldInResponse '{}'", request.getTotalPagesFieldInResponse());
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

    private List<Product> fetchSimpleProductsJsonPath(TiersRequest request) {
        String url = request.getUrl() + request.getEndpoint();
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

        if (request.getContentFieldInResponse() != null && !request.getContentFieldInResponse().isEmpty()) {
            try {
                productsData = JsonPath.read(responseJson, request.getContentFieldInResponse());
                logger.debug("Données extraites avec JsonPath : {}", productsData.size());
            } catch (Exception e) {
                logger.error("Erreur lors de la lecture avec JsonPath '{}' : {}",
                        request.getContentFieldInResponse(), e.getMessage());
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
            Product product = mapProductFromResponse(productData, request);
            products.add(product);
        }

        logger.info("Total des produits importés sans pagination : {}", products.size());
        return products;
    }

    private List<Product> fetchPaginatedProductsReflection(TiersRequest request) {

        List<Product> allProducts = new ArrayList<>();
        int page = 0;
        int size = 10;
        int totalPages = 1;

        logger.info("Démarrage de la récupération paginée depuis {}", request.getUrl());

        while (page < totalPages) {
            String paginatedUrl = String.format("%s%s?%s=%d&%s=%d",
                    request.getUrl(),
                    request.getEndpoint(),
                    request.getPaginationParamName(), page,
                    request.getPageSizeParamName(), size
            );

            logger.debug("Appel API paginé : {}", paginatedUrl);

            Map<String, Object> response = restTemplate.getForObject(paginatedUrl, Map.class);

            if (response == null) {
                logger.error("Réponse API vide ou nulle pour la page {}", page);
                break;
            }

            List<Map<String, Object>> pageResults = null;

            // CAS 1 : contentField est défini
            if (request.getContentFieldInResponse() != null && !request.getContentFieldInResponse().isEmpty()) {
                if (!response.containsKey(request.getContentFieldInResponse())) {
                    logger.error("Contenu introuvable dans la réponse API pour la page {}", page);
                    break;
                }

                pageResults = (List<Map<String, Object>>) response.get(request.getContentFieldInResponse());
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
                    Product product = mapProductFromResponse(productData, request);
                    allProducts.add(product);
                }
            }

            if (request.getTotalPagesFieldInResponse() != null) {
                totalPages = ((Number) response.get(request.getTotalPagesFieldInResponse())).intValue();
            } else {
                totalPages = (pageResults == null || pageResults.size() < size) ? page + 1 : totalPages + 1;
            }

            page++;
        }

        logger.info("Total des produits importés avec pagination : {}", allProducts.size());

        return allProducts;
    }

    private List<Product> fetchSimpleProductsReflection(TiersRequest request) {

        String url = request.getUrl() + request.getEndpoint();

        logger.debug("Appel API simple : {}", url);

        Object apiResponse = restTemplate.getForObject(url, Object.class);

        if (apiResponse == null) {
            logger.error("Réponse API vide ou nulle.");
            throw new RuntimeException("Réponse API vide ou nulle.");
        }

        List<Map<String, Object>> productsData = new ArrayList<>();

        if (request.getContentFieldInResponse() != null && !request.getContentFieldInResponse().isEmpty()) {
            if (!(apiResponse instanceof Map)) {
                logger.error("Réponse API inattendue. Un Map était attendu pour accéder à '{}'", request.getContentFieldInResponse());
                throw new RuntimeException("Réponse API inattendue.");
            }

            Map<String, Object> responseMap = (Map<String, Object>) apiResponse;

            Object content = responseMap.get(request.getContentFieldInResponse());

            if (content instanceof List) {
                productsData = (List<Map<String, Object>>) content;
            } else {
                logger.error("Le champ '{}' ne contient pas une liste.", request.getContentFieldInResponse());
                throw new RuntimeException("Le champ '" + request.getContentFieldInResponse() + "' ne contient pas une liste.");
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
            Product product = mapProductFromResponse(productData, request);
            products.add(product);
        }

        logger.info("Total des produits importés sans pagination : {}", products.size());

        return products;
    }
    private Product mapProductFromResponse(Map<String, Object> productData, TiersRequest request) {
        Product product = new Product();

        List<FieldMappingDTO> fieldMappings = request.getFieldMappings();


        String mappingType = request.getType();

        if (mappingType == null || mappingType.isEmpty()) {
            throw new RuntimeException("Le type de mapping n'est pas défini pour cette API Method.");
        }

        try {
            switch (mappingType.toLowerCase()) {
                case "jsonpath":
                    JsonPathMappingStrategy jsonPathStrategy = new JsonPathMappingStrategy();
                    for (FieldMappingDTO fieldMapping : fieldMappings) {
                        jsonPathStrategy.mapFieldstest(product, productData, fieldMapping);
                    }
                    break;

                case "reflection":
                    ReflectionMappingStrategy reflectionStrategy = new ReflectionMappingStrategy();
                    for (FieldMappingDTO fieldMapping : fieldMappings) {
                        reflectionStrategy.mapFieldstest(product, productData, fieldMapping);
                    }
                    break;

                default:
                    logger.error("Type de mapping inconnu : {}", mappingType);
                    throw new RuntimeException("Type de mapping inconnu : " + mappingType);
            }
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors du mapping du produit", e);
        }

        return product;
    }




}
