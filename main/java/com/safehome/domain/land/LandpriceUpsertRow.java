package com.safehome.domain.land;

import lombok.Data;

@Data
public class LandpriceUpsertRow {
  private String regionCode;
  private Integer surveyYear;
  private Integer pricePerM2;
  private String address;
  private Double lat;
  private Double lng;
}