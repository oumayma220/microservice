package com.example.configuration.controller;

import com.example.configuration.dao.entity.Product;
import com.example.configuration.dao.entity.RestAPIConfiguration;
import com.example.configuration.dao.entity.Tiers;
import com.example.configuration.dto.TiersDTO;
import com.example.configuration.request.TiersRequest;
import com.example.configuration.service.ProductMappingService;
import com.example.configuration.service.TiersConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/config/admin")

@RestController
public class ConfigController {
    @Autowired
    private ProductMappingService service ;
    @Autowired
    private TiersConfigurationService tiersConfigurationService;
    @PostMapping("/api/tiers")
    public ResponseEntity<TiersDTO> createTiersWithConfig(@RequestBody TiersRequest request) {
        try {
            Tiers createdTiers = tiersConfigurationService.createTiersWithConfig(request);
            TiersDTO response = new TiersDTO(createdTiers);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
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


