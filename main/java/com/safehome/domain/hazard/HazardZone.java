package com.safehome.domain.hazard;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class HazardZone {
    private Long id;
    private String regionCode;          // 시군구 등 너의 region_code 체계
    private HazardType hazardType;
    private String geojson;             // (선택) 저장할 경우
    private LocalDateTime createdAt;

    public enum HazardType {
        FLOOD, LANDSLIDE, TSUNAMI // EARTHQUAKE는 제외
        // 필요하면 STORM_SURGE(高潮)도 추가 가능 (API 35)
    }
}