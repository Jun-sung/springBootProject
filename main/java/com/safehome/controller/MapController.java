package com.safehome.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.safehome.domain.land.TransactionRaw;
import com.safehome.service.MapService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;

    @GetMapping(value = "/hazard", produces = MediaType.APPLICATION_JSON_VALUE)
    public String hazard(@RequestParam(name="regionCode") String regionCode,
                         @RequestParam(name="hazardTypes", required=false) List<String> hazardTypes) {

        if (regionCode == null || regionCode.isBlank()) return "{}";
        if (hazardTypes == null || hazardTypes.isEmpty()) {
            hazardTypes = List.of("FLOOD", "TSUNAMI", "LANDSLIDE");
        } else {
            hazardTypes = hazardTypes.stream()
                    .map(s -> s.trim().toUpperCase())
                    .toList();
        }

        return mapService.getHazardGeoJson(regionCode, hazardTypes);
    }

    @GetMapping("/sync-tile")
    public ResponseEntity<?> syncTile(
        @RequestParam("type") String type,
        @RequestParam("z") int z,
        @RequestParam("x") int x,
        @RequestParam("y") int y,
        @RequestParam(name="regionCode", required=false) String regionCode
    ) {
        System.out.println("[sync-tile] type=" + type + ", z=" + z + ", x=" + x + ", y=" + y);
        try {
            if (!isValidZoomForType(type, z)) {
                return ResponseEntity.ok().build();
            }
            mapService.syncTileDataToDb(type, z, x, y, regionCode);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(e.getClass().getName() + ": " + e.getMessage());
        }
    }

    private boolean isValidZoomForType(String type, int z) {
        if (type == null) return false;
        return switch (type) {
            case "XPT001" -> (z >= 11 && z <= 15);
            case "XPT002" -> (z >= 13 && z <= 15);
            default -> false;
        };
    }

    @GetMapping("/transactions")
    public List<TransactionRaw> transactions(
        @RequestParam("bbox") String bbox,
        @RequestParam(name="yearFrom", required=false) Integer yearFrom,
        @RequestParam(name="yearTo", required=false) Integer yearTo,
        @RequestParam(name="minPrice", required=false) Integer minPrice,
        @RequestParam(name="maxPrice", required=false) Integer maxPrice,
        @RequestParam(name="minArea", required=false) BigDecimal minArea,
        @RequestParam(name="maxArea", required=false) BigDecimal maxArea,
        @RequestParam(name="buildingTypes", required=false) List<String> buildingTypes,
        @RequestParam(name="buildingType", required=false, defaultValue="ALL") String buildingType,
        @RequestParam(name="buildingYearFrom", required=false) Integer buildingYearFrom

    ) {
        String[] p = bbox.split(",");
        BigDecimal west  = new BigDecimal(p[0]);
        BigDecimal south = new BigDecimal(p[1]);
        BigDecimal east  = new BigDecimal(p[2]);
        BigDecimal north = new BigDecimal(p[3]);

        List<String> types = buildingTypes;
        if ((types == null || types.isEmpty()) && buildingType != null && !"ALL".equalsIgnoreCase(buildingType)) {
            types = List.of(buildingType);
        }

        return mapService.getTransactions(
            west, south, east, north,
            yearFrom, yearTo,
            minPrice, maxPrice,
            minArea, maxArea,
            types,buildingYearFrom
        );
    }

    @GetMapping(value="/hazard-tile", produces=MediaType.APPLICATION_JSON_VALUE)
    public String hazardTile(@RequestParam("type") String type,
                             @RequestParam("z") int z,
                             @RequestParam("x") int x,
                             @RequestParam("y") int y) {
        return mapService.fetchHazardTileGeoJson(type, z, x, y);
    }

    @GetMapping(value = "/region-center", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> regionCenter(@RequestParam("regionCode") String regionCode) {
        return ResponseEntity.ok(mapService.getRegionCenter(regionCode));
    }

    @GetMapping(value="/transactions/near", produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> near(
        @RequestParam("lat") double lat,
        @RequestParam("lng") double lng,
        @RequestParam(name="z", required=false, defaultValue="15") int z,
        @RequestParam(name="radius", required=false) Integer radius
    ) {
        return ResponseEntity.ok(mapService.identifyNearestTransaction(lat, lng, z, radius));
    }

    @GetMapping(value="/transactions/identify", produces=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> identifyAlias(
        @RequestParam("lat") double lat,
        @RequestParam("lng") double lng,
        @RequestParam(name="zoom", required=false, defaultValue="15") int zoom
    ) {
        return ResponseEntity.ok(mapService.identifyNearestTransaction(lat, lng, zoom, null));
    }

    @GetMapping(value="/shelters", produces=MediaType.APPLICATION_JSON_VALUE)
    public String shelters(@RequestParam("z") int z, @RequestParam("x") int x, @RequestParam("y") int y) {
        return mapService.fetchSheltersGeoJson(z, x, y);
    }
}