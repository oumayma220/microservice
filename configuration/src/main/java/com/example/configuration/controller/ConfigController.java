package com.example.configuration.controller;

import com.example.configuration.dao.entity.Tiers;
import com.example.configuration.request.TiersGeneralInfoRequest;
import com.example.configuration.request.TiersRequest;
import com.example.configuration.service.ProductMappingService;
import com.example.configuration.service.TiersConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
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
    public ResponseEntity<String> createTiersWithConfig(@RequestBody TiersRequest request) {
        try {
            Tiers createdTiers = tiersConfigurationService.createTiersWithConfig(request);
           // TiersDTO response = new TiersDTO(createdTiers);
           // return ResponseEntity.ok(response);
            return ResponseEntity.ok("Tiers créé avec succès !");

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    @PutMapping("/{id}/general-info")
    public ResponseEntity<String> updateTiersGeneralInfo(
            @PathVariable Long id,
            @RequestBody TiersGeneralInfoRequest request
    ) {
        try {
            Tiers updatedTiers = tiersConfigurationService.updateTiersGeneralInfo(id, request);

            // Message de succès
            String responseMessage = "Tiers mis à jour avec succès";
            return ResponseEntity.ok(responseMessage);  // Retourne le message en texte
        } catch (IllegalArgumentException e) {
            // Message d'erreur pour email ou numéro déjà utilisés
            String errorMessage = e.getMessage();
            return ResponseEntity.badRequest().body(errorMessage);  // Retourne l'erreur en texte
        } catch (RuntimeException e) {
            // Tiers non trouvé
            String errorMessage = "Tiers non trouvé";
            return ResponseEntity.notFound().build();  // Pas de corps, mais un code 404
        } catch (Exception e) {
            // Erreur interne du serveur
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur interne du serveur");
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
    @DeleteMapping("/{tiersId}/delete")
    public ResponseEntity<String> deleteTiers(@PathVariable Long tiersId) {
        try {
            tiersConfigurationService.deleteTiers(tiersId);
            return ResponseEntity.ok("Tiers et ses configurations ont été supprimés avec succès !");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erreur lors de la suppression : " + e.getMessage());
        }
    }



}


