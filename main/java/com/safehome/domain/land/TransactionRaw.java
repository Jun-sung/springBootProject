package com.safehome.domain.land;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class TransactionRaw {

    private Long id;
    private String regionCode;
    private Integer tradeYear;
    private Integer price;
    private BigDecimal area;
    private Integer buildingYear;
    private BuildingType buildingType;
    private String structure;
    private String address;
    private BigDecimal lat;
    private BigDecimal lng;
    private LocalDateTime createdAt;
    private String floorPlanNameJa;

    public enum BuildingType {
        LAND, HOUSE, MANSION, APARTMENT, OTHER
    }
}