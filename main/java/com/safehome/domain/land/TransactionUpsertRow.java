package com.safehome.domain.land;

import java.math.BigDecimal;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class TransactionUpsertRow {
  private String externalId;
  private String regionCode;
  private Integer tradeYear;
  private Integer price;
  private BigDecimal area;
  private Integer buildingYear;
  private String buildingType;
  private String structure;
  private String address;
  private Double lat;
  private Double lng;
  private String floorPlanNameJa;
}