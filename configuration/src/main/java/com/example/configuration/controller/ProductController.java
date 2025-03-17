package com.example.configuration.controller;

import com.example.configuration.dao.entity.Product;
import com.example.configuration.dao.entity.RestAPIConfiguration;
import com.example.configuration.dao.entity.Tiers;
import com.example.configuration.dto.TiersRequest;
import com.example.configuration.service.ProductMappingService;
import com.example.configuration.service.TiersConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/config")

@RestController
public class ProductController {
    @Autowired
    private ProductMappingService service ;
    @Autowired
    private TiersConfigurationService tiersConfigurationService;

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
                    .body(null); // Tu peux créer une structure d'erreur plus propre si besoin
        }
    }

    @GetMapping("/import/all")
    public ResponseEntity<List<Product>> importAllProducts() {
        List<Product> products = service.importAllProductsFromAllTiers();
        return ResponseEntity.ok(products);
    }
    @PostMapping("/api/tiers")
    public ResponseEntity<Tiers> createTiersWithConfig(@RequestBody TiersRequest request) {
        try {
            Tiers createdTiers = tiersConfigurationService.createTiersWithConfig(request);
            return ResponseEntity.ok(createdTiers);
        } catch (Exception e) {
            // Gestion d'erreur basique, à améliorer selon le cas
            return ResponseEntity.badRequest().build();
        }
    }
    @PutMapping("/api/tiers/{tiersName}/config/{configName}")
    public ResponseEntity<Tiers> updateTiersWithConfig(
            @PathVariable String tiersName,
            @PathVariable String configName,
            @RequestBody TiersRequest request) {
        try {
            Tiers updatedTiers = tiersConfigurationService.updateTiersWithConfig(tiersName, configName, request);
            return ResponseEntity.ok(updatedTiers);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }
    @GetMapping("tiers")
    public ResponseEntity<List<Tiers>> getAllTiers() {
        List<Tiers> tiersList = tiersConfigurationService.getAllTiers();
        return ResponseEntity.ok(tiersList);
    }
    @GetMapping("tiers/{tiersId}/configs")
    public ResponseEntity<List<RestAPIConfiguration>> getConfigsByTiersId(@PathVariable Long tiersId) {
        List<RestAPIConfiguration> configs = tiersConfigurationService.getConfigurationsByTiersId(tiersId);
        return ResponseEntity.ok(configs);
    }

    // OU par nom de tiers
    @GetMapping("/configs")
    public ResponseEntity<List<RestAPIConfiguration>> getConfigsByTiersNom(@RequestParam String nomTiers) {
        List<RestAPIConfiguration> configs = tiersConfigurationService.getConfigurationsByTiersNom(nomTiers);
        return ResponseEntity.ok(configs);
    }
    @DeleteMapping("/tiers/{tiersId}/configs/{configId}")
    public ResponseEntity<String> deleteConfiguration(
            @PathVariable Long tiersId,
            @PathVariable Long configId
    ) {
        try {
            tiersConfigurationService.deleteConfigurationByTiers(tiersId, configId);
            return ResponseEntity.ok("Configuration supprimée avec succès !");
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }
    }
}


