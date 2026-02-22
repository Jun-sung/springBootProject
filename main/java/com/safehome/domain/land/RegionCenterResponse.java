package com.safehome.domain.land;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegionCenterResponse {
    private double lat;
    private double lng;
    private int zoom;
}