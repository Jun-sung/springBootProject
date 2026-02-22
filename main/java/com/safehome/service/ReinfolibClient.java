package com.safehome.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.safehome.domain.land.LandpriceUpsertRow;
import com.safehome.domain.land.TransactionUpsertRow;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ReinfolibClient {

  // ✅ MlitRestClientConfig에서 만든 Bean 주입
  private final RestClient mlitRestClient;

  // -----------------------------
  // XPT001 geojson fetch -> rows
  // -----------------------------
  public List<TransactionUpsertRow> fetchXpt001GeoJsonAndMapToRows(
      int z, int x, int y, String from, String to
  ) {
    Map body = mlitRestClient.get()
        .uri(uriBuilder -> uriBuilder
            .scheme("https")
            .host("www.reinfolib.mlit.go.jp")
            .path("/ex-api/external/XPT001")
            .queryParam("response_format", "geojson")
            .queryParam("z", z)
            .queryParam("x", x)
            .queryParam("y", y)
            .queryParam("from", from)
            .queryParam("to", to)
            .build()
        )
        // ✅ geojson(JSON)이므로 요청별로 Accept override
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .body(Map.class);

    if (body == null) return List.of();

    Object featuresObj = body.get("features");
    if (!(featuresObj instanceof List)) return List.of();

    List<?> features = (List<?>) featuresObj;
    List<TransactionUpsertRow> rows = new ArrayList<>();

    for (Object fObj : features) {
      if (!(fObj instanceof Map)) continue;
      Map f = (Map) fObj;

      String externalId = null;
      if (f.get("id") != null) externalId = String.valueOf(f.get("id"));

      Map props = (Map) f.get("properties");
      Map geom  = (Map) f.get("geometry");

      if (geom == null || !"Point".equals(String.valueOf(geom.get("type")))) continue;
      List coords = (List) geom.get("coordinates");
      if (coords == null || coords.size() < 2) continue;

      double lng = toDouble(coords.get(0));
      double lat = toDouble(coords.get(1));

      String regionCode    = buildRegionCode(props);
      Integer tradeYear    = parseTradeYear(props);
      Integer price        = parsePriceYen(props.get("u_transaction_price_total_ja"));
      BigDecimal area	   = parseDecimal(props.get("u_area_ja"));
      Integer buildingYear = parseInt(props.get("u_construction_year_ja"));
      String buildingType  = mapBuildingType(props);
      String structure     = str(props.get("building_structure_name_ja"));
      String address       = buildAddress(props);
      String floorPlan     = str(props.get("floor_plan_name_ja"));

      if (externalId == null) {
        externalId = "xpt001:" + z + ":" + x + ":" + y + ":" + lat + ":" + lng + ":" + price + ":" + tradeYear;
      }

      TransactionUpsertRow row = new TransactionUpsertRow();

      row.setExternalId(externalId);
      row.setRegionCode(regionCode);
      row.setTradeYear(tradeYear);
      row.setPrice(price);
      row.setArea(area);
      row.setBuildingYear(buildingYear);
      row.setBuildingType(buildingType);
      row.setStructure(structure);
      row.setAddress(address);
      row.setLat(lat);
      row.setLng(lng);
      row.setFloorPlanNameJa(floorPlan);
      if (row.getPrice() != null) rows.add(row);
    }

    return rows;
  }
  //decimal 파싱
  private BigDecimal parseDecimal(Object val) {
	    if (val == null) return null;

	    if (val instanceof Number) {
	        return new BigDecimal(val.toString());
	    }

	    String s = String.valueOf(val).trim();
	    if (s.isBlank()) return null;

	    // "35㎡" 같은 경우 대비
	    s = s.replaceAll("[^0-9.\\-]", "");

	    if (s.isBlank()) return null;

	    try {
	        return new BigDecimal(s);
	    } catch (NumberFormatException e) {
	        return null;
	    }
	}

  // -----------------------------
  // XPT002 geojson fetch -> rows
  // -----------------------------
  public List<LandpriceUpsertRow> fetchXpt002GeoJsonAndMapToRows(
      int z, int x, int y, String year, Integer priceClassification
  ) {
    Map body = mlitRestClient.get()
        .uri(uriBuilder -> {
          uriBuilder
              .scheme("https")
              .host("www.reinfolib.mlit.go.jp")
              .path("/ex-api/external/XPT002")
              .queryParam("response_format", "geojson")
              .queryParam("z", z)
              .queryParam("x", x)
              .queryParam("y", y)
              .queryParam("year", year);

          if (priceClassification != null) {
            uriBuilder.queryParam("priceClassification", priceClassification);
          }
          return uriBuilder.build();
        })
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .body(Map.class);

    if (body == null) return List.of();

    Object featuresObj = body.get("features");
    if (!(featuresObj instanceof List)) return List.of();

    List<?> features = (List<?>) featuresObj;
    List<LandpriceUpsertRow> rows = new ArrayList<>();

    for (Object fObj : features) {
      if (!(fObj instanceof Map)) continue;
      Map f = (Map) fObj;

      Map props = (Map) f.get("properties");
      Map geom  = (Map) f.get("geometry");
      if (geom == null || !"Point".equals(String.valueOf(geom.get("type")))) continue;

      List coords = (List) geom.get("coordinates");
      if (coords == null || coords.size() < 2) continue;

      double lng = toDouble(coords.get(0));
      double lat = toDouble(coords.get(1));

      String regionCode = buildRegionCode(props);
      Integer surveyYear = parseInt(props.get("target_year_name_ja"));
      if (surveyYear == null) surveyYear = parseInt(year);

      Integer pricePerM2 = parseInt(props.get("u_current_years_price_ja"));
      String address = buildLandpriceAddress(props);

      if (pricePerM2 == null) continue;

      LandpriceUpsertRow row = new LandpriceUpsertRow();
      row.setRegionCode(regionCode);
      row.setSurveyYear(surveyYear);
      row.setPricePerM2(pricePerM2);
      row.setAddress(address);
      row.setLat(lat);
      row.setLng(lng);

      rows.add(row);
    }

    return rows;
  }

  // -----------------------------
  // utils (그대로 유지)
  // -----------------------------
  private static double toDouble(Object o) { return Double.parseDouble(String.valueOf(o)); }
  private static String str(Object o) { return (o == null) ? null : String.valueOf(o).trim(); }

  private static Integer parseInt(Object o) {
    if (o == null) return null;
    String s = String.valueOf(o).replaceAll("[^0-9]", "");
    if (s.isBlank()) return null;
    return Integer.valueOf(s);
  }

  private static Integer parsePriceYen(Object o) {
    if (o == null) return null;
    String s = String.valueOf(o).replaceAll("[^0-9]", "");
    if (s.isBlank()) return null;
    return Integer.valueOf(s);
  }

  private static String buildRegionCode(Map props) {
    String pref = str(props.get("prefecture_code"));
    String city = str(props.get("city_code"));
    if (pref == null || city == null) return "UNKNOWN";
    return pref + city;
  }

  private static Integer parseTradeYear(Map props) {
    Object v = props.get("point_in_time_name_ja");
    return parseInt(v);
  }

  private static String buildAddress(Map props) {
    String pref = str(props.get("prefecture_name_ja"));
    String city = str(props.get("city_name_ja"));
    String dist = str(props.get("district_name_ja"));
    return String.join(" ", Arrays.asList(pref, city, dist)
        .stream().filter(s -> s != null && !s.isBlank()).toList());
  }

  private static String mapBuildingType(Map props) {
    String landType = str(props.get("land_type_name_ja"));
    String bUse = str(props.get("building_use_name_ja"));

    if (landType != null && landType.contains("土地")) return "LAND";
    if (bUse != null && (bUse.contains("共同住宅") || bUse.contains("マンション"))) return "MANSION";
    if (bUse != null && bUse.contains("アパート")) return "APARTMENT";
    if (bUse != null && (bUse.contains("住宅") || bUse.contains("戸建"))) return "HOUSE";
    return "HOUSE";
  }

  private static String buildLandpriceAddress(Map props) {
    String pref = str(props.get("prefecture_name_ja"));
    String city = str(props.get("city_name_ja"));
    String resi = str(props.get("residence_display_name_ja"));
    String locNo = str(props.get("location_number_ja"));
    String place = str(props.get("place_name_ja"));

    return String.join(" ", Arrays.asList(pref, city, place, resi, locNo)
        .stream().filter(s -> s != null && !s.isBlank()).toList());
  }
}