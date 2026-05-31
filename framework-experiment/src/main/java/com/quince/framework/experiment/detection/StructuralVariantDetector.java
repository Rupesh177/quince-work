package com.quince.framework.experiment.detection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.quince.framework.core.DetectionContext;
import com.quince.framework.core.VariantDetector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.NoSuchElementException;

import java.io.InputStream;
import java.util.*;

/**
 * Scores variants based on presence/absence of known DOM elements.
 * Reads variant structure definition from variant-structure.yml
 */
public class StructuralVariantDetector implements VariantDetector {
    private static final Logger logger = LogManager.getLogger(StructuralVariantDetector.class);
    private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
    private Map<String, Map<String, VariantStructure>> structures;

    public StructuralVariantDetector() {
        loadStructures();
    }

    /**
     * Loads variant structure definitions from YAML resource file.
     */
    @SuppressWarnings("unchecked")
    private void loadStructures() {
        try {
            InputStream is = StructuralVariantDetector.class
                    .getClassLoader()
                    .getResourceAsStream("variant-structure.yml");

            if (is == null) {
                logger.warn("variant-structure.yml not found; structural detection disabled");
                structures = new HashMap<>();
                return;
            }

            Map<String, Object> data = objectMapper.readValue(is, Map.class);
            this.structures = new HashMap<>();

            for (String flagKey : data.keySet()) {
                Map<String, VariantStructure> variants = new HashMap<>();
                Object flagData = data.get(flagKey);
                if (flagData instanceof Map) {
                    for (Object variantKey : ((Map<?, ?>) flagData).keySet()) {
                        Object variantData = ((Map<?, ?>) flagData).get(variantKey);
                        if (variantData instanceof Map) {
                            Map<String, Object> variantMap = (Map<String, Object>) variantData;
                            @SuppressWarnings("unchecked")
                            List<String> present = (List<String>) variantMap.get("present");
                            @SuppressWarnings("unchecked")
                            List<String> absent = (List<String>) variantMap.get("absent");
                            variants.put(variantKey.toString(),
                                    new VariantStructure(present != null ? present : List.of(),
                                            absent != null ? absent : List.of()));
                        }
                    }
                }
                structures.put(flagKey, variants);
            }
            logger.info("Loaded structural definitions for {} experiments", structures.size());
        } catch (Exception e) {
            logger.error("Error loading variant-structure.yml", e);
            structures = new HashMap<>();
        }
    }

    @Override
    public Optional<String> detect(String flagKey, DetectionContext context) {
        if (context.webDriver() == null) {
            logger.debug("No WebDriver in context, skipping structural detection");
            return Optional.empty();
        }

        Map<String, VariantStructure> variants = structures.get(flagKey);
        if (variants == null || variants.isEmpty()) {
            logger.debug("No structural definition found for {}", flagKey);
            return Optional.empty();
        }

        WebDriver driver = context.webDriver();
        Map<String, Double> scores = new HashMap<>();

        for (String variantKey : variants.keySet()) {
            double score = scoreVariant(driver, variants.get(variantKey));
            scores.put(variantKey, score);
        }

        // Find variant with highest score
        String bestVariant = scores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        if (bestVariant != null) {
            double confidence = scores.get(bestVariant);
            if (confidence >= 0.5) {
                logger.warn("Detected variant via structural heuristic: {} (confidence: {})",
                        bestVariant, String.format("%.2f", confidence));
                return Optional.of(bestVariant);
            } else {
                logger.warn("Structural detection confidence too low ({}); using control",
                        String.format("%.2f", confidence));
            }
        }

        return Optional.empty();
    }

    /**
     * Scores a variant based on how many of its expected elements are present.
     */
    private double scoreVariant(WebDriver driver, VariantStructure structure) {
        int matches = 0;
        int totalChecks = structure.present().size() + structure.absent().size();

        for (String selector : structure.present()) {
            if (elementExists(driver, selector)) {
                matches++;
            }
        }

        for (String selector : structure.absent()) {
            if (!elementExists(driver, selector)) {
                matches++;
            }
        }

        return totalChecks > 0 ? (double) matches / totalChecks : 0.0;
    }

    /**
     * Checks if an element exists in the DOM.
     */
    private boolean elementExists(WebDriver driver, String selector) {
        try {
            // Assume selector is CSS
            driver.findElement(By.cssSelector(selector));
            return true;
        } catch (NoSuchElementException e) {
            return false;
        } catch (Exception e) {
            logger.debug("Error checking element existence: {}", selector, e);
            return false;
        }
    }

    @Override
    public String getName() {
        return "StructuralVariantDetector";
    }

    /**
     * Record for variant structure definition.
     */
    public record VariantStructure(List<String> present, List<String> absent) {
    }
}