package com.example.configuration.controller;

import com.example.configuration.dao.entity.Product;
import com.example.configuration.dao.entity.Tiers;
import com.example.configuration.dao.repository.TiersRepository;
import com.example.configuration.dto.TiersDTO;
import com.example.configuration.request.TiersRequest;
import com.example.configuration.request.testrequest;
import com.example.configuration.service.ProductMappingService;
import com.example.configuration.service.TestService;
import com.example.configuration.service.TiersConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/config")

@RestController
public class ProductController {
    @Autowired
    private ProductMappingService service ;
    @Autowired
    private TiersConfigurationService tiersConfigurationService;
    @Autowired
    private TiersRepository tiersRepository;
    @Autowired
    private TestService testService;

    @GetMapping("/import-products")
    public ResponseEntity<List<Product>> importProducts(
            @RequestParam String configName,
            @RequestParam String endpoint
    ) {
        try {
            List<Product> products = service.importProducts(configName, endpoint);
            return ResponseEntity.ok(products);
        } catch (RuntimeException ex) {
            return ResponseEntity
                    .badRequest()
                    .body(null);
        }
    }

    @GetMapping("/import/all")
    public ResponseEntity<List<Product>> importAllProducts() {
        List<Product> products = service.importAllProductsFromAllTiers();
        return ResponseEntity.ok(products);
    }
    @GetMapping("tiers")
    public ResponseEntity<List<Tiers>> getAllTiers() {
        List<Tiers> tiersList = tiersConfigurationService.getAllTiers();
        return ResponseEntity.ok(tiersList);
    }
    @GetMapping("tiers/{id}")
    public ResponseEntity<TiersDTO> getTiersById(@PathVariable("id") Long id) {
        TiersDTO tiersDTO = tiersConfigurationService.getTiersById(id);
        return ResponseEntity.ok(tiersDTO);
    }


   // @GetMapping("/configs")
   // public ResponseEntity<List<RestAPIConfiguration>> getConfigsByTiersNom(@RequestParam String nomTiers) {
    //    List<RestAPIConfiguration> configs = tiersConfigurationService.getConfigurationsByTiersNom(nomTiers);
    //    return ResponseEntity.ok(configs);
  //  }
    @GetMapping("/current")
    public ResponseEntity<?> getCurrentTenantId() {
        try {
            Integer tenantId = tiersConfigurationService.getCurrentTenantId();
            return ResponseEntity.ok(Map.of("tenantId", tenantId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }
    @GetMapping("/products/{tierId}")
    public ResponseEntity<List<Product>> getProductsForTier(@PathVariable Long tierId) {
        if (!tiersRepository.existsById(tierId)) {
            return ResponseEntity.notFound().build();
        }
        try {
            List<Product> products = service.importProductsForTier(tierId);
            if (products.isEmpty()) {
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.ok(products);
        } catch (Exception e) {
            // Log the error
            return ResponseEntity.internalServerError().build();
        }
    }
    @PostMapping("/import")
    public ResponseEntity<List<Product>> importProducts(@RequestBody TiersRequest request) {
        List<Product> products = testService.importProducts(request);
        return ResponseEntity.ok(products);
    }


}
