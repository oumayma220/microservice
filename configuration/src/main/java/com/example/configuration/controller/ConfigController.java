package com.example.configuration.controller;
import com.example.configuration.dao.entity.APIMethod;
import com.example.configuration.dao.entity.FieldMapping;
import com.example.configuration.dao.entity.RestAPIConfiguration;
import com.example.configuration.dao.entity.Tiers;
import com.example.configuration.dto.FieldMappingDTO;
import com.example.configuration.request.ApiMethodGeneralInfoRequest;
import com.example.configuration.request.ConfigGeneralInfoRequest;
import com.example.configuration.request.TiersGeneralInfoRequest;
import com.example.configuration.request.TiersRequest;
import com.example.configuration.service.ProductMappingService;
import com.example.configuration.service.TiersConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.NoSuchElementException;

@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/config/admin")
@RestController
public class ConfigController {
    @Autowired
    private ProductMappingService service ;
    @Autowired
    private TiersConfigurationService tiersConfigurationService;
    @PostMapping("/api/tiers")
    public ResponseEntity<String> createTiersWithConfig(@RequestBody TiersRequest request) {
        try {
            Tiers createdTiers = tiersConfigurationService.createTiersWithConfig(request);
            return ResponseEntity.ok("Tiers créé avec succès !");
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    @GetMapping("tiers/configs/{tiersId}")
    public ResponseEntity<List<RestAPIConfiguration>> getConfigsByTiersId(@PathVariable Long tiersId) {
        List<RestAPIConfiguration> configs = tiersConfigurationService.getConfigurationsByTiersId(tiersId);
        return ResponseEntity.ok(configs);
    }
    @PutMapping("/{id}/general-info")
    public ResponseEntity<String> updateTiersGeneralInfo(
            @PathVariable Long id,
            @RequestBody TiersGeneralInfoRequest request
    ) {
        try {
            Tiers updatedTiers = tiersConfigurationService.updateTiersGeneralInfo(id, request);
            String responseMessage = "Tiers mis à jour avec succès";
            return ResponseEntity.ok(responseMessage);
        } catch (IllegalArgumentException e) {
            String errorMessage = e.getMessage();
            return ResponseEntity.badRequest().body(errorMessage);
        } catch (RuntimeException e) {
            String errorMessage = "Tiers non trouvé";
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur interne du serveur");
        }
    }
    @PutMapping("/{id}/update-config")
    public ResponseEntity<String> updateConfigGeneralInfo(
            @PathVariable Long id,
            @RequestBody ConfigGeneralInfoRequest request
    ) {
        try {
            RestAPIConfiguration updatedConfig = tiersConfigurationService.updateConfigGeneralInfo(id, request);
            String responseMessage = "Config mis à jour avec succès";
            return ResponseEntity.ok(responseMessage);
        } catch (IllegalArgumentException e) {
            String errorMessage = e.getMessage();
            return ResponseEntity.badRequest().body(errorMessage);
        } catch (RuntimeException e) {
            String errorMessage = "Config non trouvé";
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur interne du serveur");
        }
    }
    @PutMapping("/{id}/update-method")
    public ResponseEntity<String> updateapimethod(
            @PathVariable Long id,
            @RequestBody ApiMethodGeneralInfoRequest request
    ) {
        try {
            APIMethod updatedMethod = tiersConfigurationService.updateApiMethod(id, request);
            String responseMessage = "méthode mis à jour avec succès";
            return ResponseEntity.ok(responseMessage);
        } catch (IllegalArgumentException e) {
            String errorMessage = e.getMessage();
            return ResponseEntity.badRequest().body(errorMessage);
        } catch (RuntimeException e) {
            String errorMessage = "méthode non trouvé";
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur interne du serveur");
        }
    }
    @PutMapping("/{id}/update-mapping")
    public ResponseEntity<String> updateMapping(
            @PathVariable Long id,
            @RequestBody List<FieldMappingDTO> request
    ) {
        try {
            List<FieldMapping> updatedMappings = tiersConfigurationService.updateFieldMappings(id, request);
            String responseMessage = "Mappings mis à jour avec succès";
            return ResponseEntity.ok(responseMessage);
        } catch (IllegalArgumentException e) {
            String errorMessage = e.getMessage();
            return ResponseEntity.badRequest().body(errorMessage);
        } catch (RuntimeException e) {
            String errorMessage = "Méthode API non trouvée";
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorMessage);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur interne du serveur");
        }
    }
    @PostMapping("/{tiersId}/configs")
    public ResponseEntity<?> addConfigToTiers(@PathVariable Long tiersId, @RequestBody TiersRequest request) {
        try {
            RestAPIConfiguration newConfig = tiersConfigurationService.addConfigToTiersById(tiersId, request);
        return ResponseEntity.ok("config  créé avec succès ");
    }catch (Exception e) {
        return ResponseEntity.badRequest().build();
    }
    }
    @PostMapping("/addApiMethodAndFieldMappings/{configId}")
    public ResponseEntity<?> addApiMethodAndFieldMappingsToConfig(
            @PathVariable Long configId,
            @RequestBody TiersRequest request) {
        try {
            RestAPIConfiguration updatedConfig = tiersConfigurationService.addApiMethodAndFieldMappingsToConfig(configId, request);
            return ResponseEntity.ok(" apimethod créé avec succès ");
        }catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    @PostMapping("/add/{apiMethodId}")
    public ResponseEntity<String> addFieldMappingsByApiMethodId(
            @PathVariable Long apiMethodId,
            @RequestBody List<FieldMappingDTO> fieldMappingsRequest) {

        try {
            tiersConfigurationService.addFieldMappingsByApiMethodId(apiMethodId, fieldMappingsRequest);
            return ResponseEntity.ok("Field mappings added successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(400).body("Error: " + e.getMessage());
        }
    }
    @DeleteMapping("/{tiersId}/delete")
    public ResponseEntity<String> deleteTiers(@PathVariable Long tiersId) {
        try {
            tiersConfigurationService.deleteTiers(tiersId);
            return ResponseEntity.ok("Tiers et ses configurations ont été supprimés avec succès !");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors de la suppression : " + e.getMessage());
        }
    }
    @DeleteMapping("/delete/config/{configId}")
    public ResponseEntity<String> deleteconfig(@PathVariable Long configId) {
        try {
            tiersConfigurationService.deleteConfig(configId);
            return ResponseEntity.ok("configurations a été supprimés avec succès !");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors de la suppression : " + e.getMessage());
        }
    }
    @DeleteMapping("/delete/method/{MethodId}")
    public ResponseEntity<String> method(@PathVariable Long MethodId) {
        try {
            tiersConfigurationService.deleteApiMethod(MethodId);
            return ResponseEntity.ok("ApiMethod a été supprimés avec succès !");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors de la suppression : " + e.getMessage());
        }
    }
    @DeleteMapping("/delete/field/{MethodId}")
    public ResponseEntity<String> deletefieldmapping (@PathVariable Long MethodId) {
        try {
            tiersConfigurationService.deleteAllFieldMappingsByApiMethodId(MethodId);
            return ResponseEntity.ok("fieldmapping a été supprimés avec succès !");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors de la suppression : " + e.getMessage());
        }
    }
    @GetMapping("config/{id}")
    public ResponseEntity<?> getRestAPIConfigById(@PathVariable Long id) {
        try {
            RestAPIConfiguration config = tiersConfigurationService.getConfigById(id);
            return ResponseEntity.ok(config);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Configuration non trouvée pour cet ID et tenant.");
        }
    }



    }








