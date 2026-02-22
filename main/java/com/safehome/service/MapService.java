package com.safehome.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.safehome.domain.land.LandpriceUpsertRow;
import com.safehome.domain.land.RegionCenterResponse;
import com.safehome.domain.land.TransactionRaw;
import com.safehome.domain.land.TransactionUpsertRow;
import com.safehome.mapper.MapMapper;
import com.safehome.util.MlitApiProps;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MapService {

    private final MapMapper mapMapper;

    // TileCacheService와 같은 방식: props + RestClient 주입
    private final MlitApiProps props;
    private final RestClient mlitRestClient;
    private final ObjectMapper objectMapper;	
    private static final String EMPTY_FC = "{\"type\":\"FeatureCollection\",\"features\":[]}";

    // ── JVM 인메모리 타일 캐시 ──────────────────────────────────────────────
    // 키: "FL:14:14625:6015"  값: GeoJSON 문자열
    // 재시작 전까지 유효, DB 왕복 없이 ~0ms 즉시 반환
    // ConcurrentHashMap: 멀티스레드 안전 (여러 요청이 동시에 같은 타일 요청해도 OK)
    private final java.util.concurrent.ConcurrentHashMap<String, String> tileMemCache =
            new java.util.concurrent.ConcurrentHashMap<>(1024);
    
 // =========================
 // 1) sync: type별로 호출 (컨트롤러와 맞춤)
 // =========================
 @Transactional
 public void syncTileDataToDb(String type, int z, int x, int y, String regionCodeHintOrNull) {
     System.out.println("SYNC TILE CALLED type=" + type + " z=" + z + " x=" + x + " y=" + y);

     // 기간 파라미터 (프로젝트 정책대로)
     String from = "20244";
     String to   = "20261";

     if (type == null) return;

     switch (type) {
         case "XPT001": {
             List<TransactionUpsertRow> txRows = fetchXpt001GeoJsonAndMapToRows(z, x, y, from, to);
             System.out.println("[XPT001] rows=" + (txRows==null ? "null" : txRows.size()));
             if (txRows == null || txRows.isEmpty()) return;
             batchUpsertTransactions(txRows, 1000);
             return;
         }
         case "XPT002": {
             List<LandpriceUpsertRow> lpRows = fetchXpt002GeoJsonAndMapToRows(z, x, y, from, to);
             System.out.println("[XPT001] rows=" + (lpRows==null ? "null" : lpRows.size()));
             if (lpRows == null || lpRows.isEmpty()) return;
             batchUpsertLandprices(lpRows, 1000);
             return;
         }
         // 필요하면 SHELTER / HAZARD tile sync도 여기 추가
         default:
             return;
     }
 }

 private void batchUpsertTransactions(List<TransactionUpsertRow> rows, int batchSize) {
	    if (rows == null || rows.isEmpty()) return;
	    for (int i = 0; i < rows.size(); i += batchSize) {
	        List<TransactionUpsertRow> chunk = rows.subList(i, Math.min(i + batchSize, rows.size()));
	        upsertWithRetry(chunk);
	    }
	}

	private void upsertWithRetry(List<TransactionUpsertRow> chunk) {
	    int maxRetry = 3;
	    for (int attempt = 1; attempt <= maxRetry; attempt++) {
	        try {
	            mapMapper.upsertTransactions(chunk);
	            return;
	        } catch (Exception e) {
	            String msg = e.getMessage() != null ? e.getMessage() : "";
	            boolean isDeadlock = msg.contains("Deadlock") || msg.contains("deadlock")
	                              || msg.contains("1213");
	            if (isDeadlock && attempt < maxRetry) {
	                try { Thread.sleep(200L * attempt); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return; }
	                System.out.println("[upsert] deadlock retry " + attempt);
	            } else {
	                System.out.println("[upsert] failed after " + attempt + " attempts: " + msg);
	                return; // 실패해도 다른 청크/타일 계속 진행
	            }
	        }
	    }
	}

	private void batchUpsertLandprices(List<LandpriceUpsertRow> rows, int batchSize) {
	    if (rows == null || rows.isEmpty()) return;   // ✅ 여기
	    for (int i = 0; i < rows.size(); i += batchSize) {
	        List<LandpriceUpsertRow> chunk = rows.subList(i, Math.min(i + batchSize, rows.size()));
	        mapMapper.upsertLandprices(chunk);
	    }
	}

    // =========================
    // 2) XPT001: tile -> geojson(Map) -> rows
    // =========================
 @SuppressWarnings({"rawtypes","unchecked"})
 public List<TransactionUpsertRow> fetchXpt001GeoJsonAndMapToRows(int z, int x, int y, String from, String to) {

     String base = props.getBaseUrl().replaceAll("/+$", "");
     String url = base + "/XPT001"
             + "?response_format=geojson"
             + "&z=" + z + "&x=" + x + "&y=" + y
             + "&from=" + from + "&to=" + to;

     try {
         Map body = mlitRestClient.get()
                 .uri(url)
                 .header("Ocp-Apim-Subscription-Key", props.getApiKey()) // ✅ 추가
                 .header(HttpHeaders.ACCEPT, "application/json")
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

             String externalId = (f.get("id") != null) ? String.valueOf(f.get("id")) : null;

             Map propsMap = (Map) f.get("properties");
             Map geom     = (Map) f.get("geometry");
             if (geom == null || !"Point".equals(String.valueOf(geom.get("type")))) continue;

             List coords = (List) geom.get("coordinates");
             if (coords == null || coords.size() < 2) continue;

             double lng = toDouble(coords.get(0));
             double lat = toDouble(coords.get(1));
             if (Double.isNaN(lat) || Double.isNaN(lng)) continue;

             String regionCode   = buildRegionCode(propsMap);
             Integer tradeYear   = parseTradeYear(propsMap);
             Integer price       = parsePriceYen(propsMap.get("u_transaction_price_total_ja"));
             Integer buildingYear= parseInt(propsMap.get("u_construction_year_ja"));
             BigDecimal area = parseDecimal(propsMap.get("u_area_ja"));

             String buildingType = mapBuildingType(propsMap);
             String structure    = str(propsMap.get("building_structure_name_ja"));
             String address      = buildAddress(propsMap);
             String floorPlan    = str(propsMap.get("floor_plan_name_ja"));

             if (externalId == null || externalId.isBlank()) {
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

             // ✅ 최소 조건(너 정책대로)
             if (row.getPrice() != null) rows.add(row);
         }

         return rows;

     } catch (HttpClientErrorException e) {
         // ✅ 400(줌 불법) 같은 케이스: sync 전체 실패시키지 말고 빈 리스트로
         System.out.println("XPT001 client error: " + e.getStatusCode() + " " + e.getResponseBodyAsString());
         return List.of();
     } catch (ResourceAccessException | HttpServerErrorException e) {
         System.out.println("XPT001 server/network error: " + e.getMessage());
         return List.of();
     }
 }

    // =========================
    // 3) XPT002: tile -> geojson(Map) -> rows
    // =========================
 @SuppressWarnings({"rawtypes","unchecked"})
 public List<LandpriceUpsertRow> fetchXpt002GeoJsonAndMapToRows(int z, int x, int y, String from, String to) {

     String base = props.getBaseUrl().replaceAll("/+$", "");
     String url = base + "/XPT002"
             + "?response_format=geojson"
             + "&z=" + z + "&x=" + x + "&y=" + y
             + "&from=" + from + "&to=" + to;

     try {
         Map body = mlitRestClient.get()
                 .uri(url)
                 .header("Ocp-Apim-Subscription-Key", props.getApiKey()) // ✅ 추가
                 .header(HttpHeaders.ACCEPT, "application/json")
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

             Map propsMap = (Map) f.get("properties");
             Map geom     = (Map) f.get("geometry");
             if (geom == null || !"Point".equals(String.valueOf(geom.get("type")))) continue;

             List coords = (List) geom.get("coordinates");
             if (coords == null || coords.size() < 2) continue;

             double lng = toDouble(coords.get(0));
             double lat = toDouble(coords.get(1));
             if (Double.isNaN(lat) || Double.isNaN(lng)) continue;

             LandpriceUpsertRow row = new LandpriceUpsertRow();
             row.setRegionCode(buildRegionCode(propsMap));

             Integer year = parseInt(propsMap.get("year"));
             if (year == null) year = parseInt(propsMap.get("survey_year"));
             row.setSurveyYear(year);

             row.setPricePerM2(parsePriceYen(propsMap.get("u_current_years_price_ja")));
             row.setAddress(buildAddress(propsMap));
             row.setLat(lat);
             row.setLng(lng);

             if (row.getRegionCode() != null && row.getSurveyYear() != null && row.getPricePerM2() != null) {
                 rows.add(row);
             }
         }

         return rows;

     } catch (HttpClientErrorException e) {
         // ✅ 여기서 네가 본 400(줌 불법)이 발생함. 빈 리스트로 돌려서 sync 계속 진행하게 함
         System.out.println("XPT002 client error: " + e.getStatusCode() + " " + e.getResponseBodyAsString());
         return List.of();
     } catch (ResourceAccessException | HttpServerErrorException e) {
         System.out.println("XPT002 server/network error: " + e.getMessage());
         return List.of();
     }
 }

    // =========================
    // 공용 헬퍼들 (네가 요청한 것들)
    // =========================
    @SuppressWarnings({"rawtypes"})
    private String buildRegionCode(Map propsMap) {
        if (propsMap == null) return null;

        // 이미 region_code로 내려오는 경우
        String rc = str(propsMap.get("region_code"));
        if (rc == null) rc = str(propsMap.get("regionCode"));
        if (rc != null) return rc;

        // Reinfolib에서 흔히 오는 code 조합
        String pref = digitsOnly(propsMap.get("prefecture_code"));
        if (pref == null) pref = digitsOnly(propsMap.get("pref_code"));

        String city = digitsOnly(propsMap.get("city_code"));
        if (city == null) city = digitsOnly(propsMap.get("city_cd"));

        if (pref == null && city == null) return null;

        // 보통 pref 2자리, city 3자리
        if (pref != null && pref.length() == 1) pref = "0" + pref;
        if (city != null && city.length() == 1) city = "00" + city;
        if (city != null && city.length() == 2) city = "0" + city;

        if (pref == null) return city;
        if (city == null) return pref;
        return pref + city;
    }

    @SuppressWarnings({"rawtypes"})
    private Integer parseTradeYear(Map propsMap) {
        if (propsMap == null) return null;

        // 후보 키들
        Object v = propsMap.get("trade_year");
        if (v == null) v = propsMap.get("point_in_time_name_ja");  // e.g. "2024年4半期"
        if (v == null) v = propsMap.get("year");

        if (v == null) return null;

        String s = String.valueOf(v).trim();
        if (s.isBlank()) return null;

        // 문자열 어디에 있든 "연속 4자리"를 우선 연도로 잡음 (2024年4半期, 2024년 4반기, 2024Q4 등)
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(19\\d{2}|20\\d{2})").matcher(s);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); } catch (Exception ignore) {}
        }

        // fallback: 기존 숫자 추출
        return parseInt(v);
    }

    // "1,234万円" / "12340000" / 12340000 -> 12340000
    private Integer parsePriceYen(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();

        String s = String.valueOf(v).trim();
        if (s.isBlank()) return null;

        // 숫자만 남김
        s = s.replaceAll("[^0-9\\-]", "");
        if (s.isBlank()) return null;

        try { return Integer.parseInt(s); }
        catch (Exception e) { return null; }
    }

    @SuppressWarnings({"rawtypes"})
    private String mapBuildingType(Map propsMap) {
        // DB enum('LAND','HOUSE','MANSION','APARTMENT','OTHER')에 맞춰 문자열 리턴
        if (propsMap == null) return "OTHER";

        String use = str(propsMap.get("building_use_name_ja"));
        String land = str(propsMap.get("land_type_name_ja"));
        String plan = str(propsMap.get("floor_plan_name_ja"));

        String text = (use == null ? "" : use) + " " + (land == null ? "" : land) + " " + (plan == null ? "" : plan);

        if (text.contains("宅地") || text.contains("土地") || text.contains("畑") || text.contains("田")) return "LAND";
        if (text.contains("一戸建") || text.contains("戸建") || text.contains("住宅")) return "HOUSE";
        if (text.contains("マンション")) return "MANSION";
        if (text.contains("共同住宅") || text.contains("アパート")) return "APARTMENT";

        return "OTHER";
    }

    @SuppressWarnings({"rawtypes"})
    private String buildAddress(Map propsMap) {
        if (propsMap == null) return null;

        // 가능한 주소 구성 요소들을 순서대로 붙임
        String pref = str(propsMap.get("prefecture_name_ja"));
        String city = str(propsMap.get("city_name_ja"));
        String dist = str(propsMap.get("district_name_ja"));

        // XPT002는 이쪽 키가 있을 수도
        String residence = str(propsMap.get("residence_display_name_ja"));
        String place = str(propsMap.get("place_name_ja"));
        String locNo = str(propsMap.get("location_number_ja"));

        StringBuilder sb = new StringBuilder();
        appendIf(sb, pref);
        appendIf(sb, city);
        appendIf(sb, dist);
        appendIf(sb, residence);
        appendIf(sb, place);
        appendIf(sb, locNo);

        String out = sb.toString().trim();
        return out.isBlank() ? null : out;
    }

    private void appendIf(StringBuilder sb, String part) {
        if (part == null || part.isBlank()) return;
        sb.append(part);
    }

    private String str(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isBlank() ? null : s;
    }

    private double toDouble(Object v) {
        if (v == null) return Double.NaN;
        if (v instanceof Number) return ((Number) v).doubleValue();
        try { return Double.parseDouble(String.valueOf(v)); }
        catch (Exception e) { return Double.NaN; }
    }

    private Integer parseInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();

        String s = String.valueOf(v).trim();
        if (s.isBlank()) return null;

        // "35㎡" 같은 문자열도 숫자만 추출
        s = s.replaceAll("[^0-9\\-]", "");
        if (s.isBlank()) return null;

        try { return Integer.parseInt(s); }
        catch (Exception e) { return null; }
    }

    private String digitsOnly(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim().replaceAll("[^0-9]", "");
        return s.isBlank() ? null : s;
    }
    
    
    
    //===============
    public ObjectNode identifyNearestTransaction(double lat, double lng, int z, Integer radiusMeters) {

        int radius =
            (radiusMeters != null && radiusMeters > 0) ? radiusMeters :
            (z >= 15) ? 100 :
            (z == 14) ? 200 :
            (z == 13) ? 400 :
            (z == 12) ? 800 : 1200;

        double latDelta = radius / 111_320.0;
        double lngDelta = radius / (111_320.0 * 0.81);
        TransactionRaw t = mapMapper.findNearestTransaction(lat, lng, radius, latDelta, lngDelta);
        System.out.println("identifyNearestTransaction 진입"+t);

        ObjectNode out = objectMapper.createObjectNode(); // ✅ 주입받은 objectMapper 사용
        if (t == null) {
            out.put("found", false);
            return out;
        }

        out.put("found", true);
        out.put("id", t.getId());
        if (t.getPrice() != null) out.put("price", t.getPrice());
        if (t.getArea() != null) out.put("area", t.getArea().toString());
        if (t.getAddress() != null) out.put("address", t.getAddress());
        if (t.getBuildingType() != null) out.put("buildingType", t.getBuildingType().name());
        if (t.getTradeYear() != null) out.put("tradeYear", t.getTradeYear());
        if (t.getBuildingYear() != null) out.put("buildingYear", t.getBuildingYear());
        if (t.getStructure() != null)    out.put("structure", t.getStructure());
        if (t.getFloorPlanNameJa() != null) out.put("floorPlanNameJa", t.getFloorPlanNameJa());

        return out;
    }
    

    public RegionCenterResponse getRegionCenter(String regionCode) {
        // fallback: 일본 전체 중심(대충)
        final double fallbackLat = 36.2048;
        final double fallbackLng = 138.2529;
        final int fallbackZoom = 5;

        if (regionCode == null || regionCode.isBlank()) {
            return new RegionCenterResponse(fallbackLat, fallbackLng, fallbackZoom);
        }

        String code = regionCode.toLowerCase();

        // ✅ index/nav에서 바로 열기용: 대표 도시/권역 + 적절한 zoom
        switch (code) {

            // ─────────────────────
            // 홋카이도 & 주요 도시
            // ─────────────────────
            case "hokkaido": // 홋카이도 전체(살짝 넓게)
                return new RegionCenterResponse(43.5, 142.0, 7);
            case "sapporo":
                return new RegionCenterResponse(43.0618, 141.3545, 11);
            case "otaru":
                return new RegionCenterResponse(43.1907, 140.9947, 11);
            case "asahikawa":
                return new RegionCenterResponse(43.7706, 142.3650, 11);
            case "hakodate":
                return new RegionCenterResponse(41.7687, 140.7288, 11);

            // ─────────────────────
            // 혼슈 & 주요 도시
            // ─────────────────────
            case "honshu": // 혼슈 전체(도쿄 중심 쪽)
                return new RegionCenterResponse(35.6812, 139.7671, 11);
            case "tokyo":
                return new RegionCenterResponse(35.6812, 139.7671, 11);
            case "yokohama":
                return new RegionCenterResponse(35.4658, 139.6221, 11);
            case "sendai":
                return new RegionCenterResponse(38.2688, 140.8721, 11);
            case "niigata":
                return new RegionCenterResponse(37.9161, 139.0364, 11);
            case "hiroshima":
                return new RegionCenterResponse(34.3853, 132.4553, 11);

            // ─────────────────────
            // 나고야 / 중부
            // ─────────────────────
            case "nagoya-area": // 중부 전체
                return new RegionCenterResponse(35.3, 137.0, 7);
            case "nagoya":
                return new RegionCenterResponse(35.1815, 136.9066, 11);
            case "toyota":
                return new RegionCenterResponse(35.0844, 137.1560, 11);

            // ─────────────────────
            // 간사이(오사카, 교토, 고베)
            // ─────────────────────
            case "kansai": // 간사이 넓은 권역
                return new RegionCenterResponse(34.8, 135.5, 7);
            case "osaka":
                return new RegionCenterResponse(34.6937, 135.5023, 11);
            case "kyoto":
                return new RegionCenterResponse(35.0116, 135.7681, 11);
            case "kobe":
                return new RegionCenterResponse(34.6901, 135.1955, 11);

            // ─────────────────────
            // 시코쿠
            // ─────────────────────
            case "shikoku": // 시코쿠 전체
                return new RegionCenterResponse(33.8, 133.6, 7);
            case "matsuyama":
                return new RegionCenterResponse(33.8392, 132.7657, 11);
            case "takamatsu":
                return new RegionCenterResponse(34.3428, 134.0466, 11);
            case "tokushima":
                return new RegionCenterResponse(34.0703, 134.5548, 11);
            case "kochi":
                return new RegionCenterResponse(33.5597, 133.5311, 11);

            // ─────────────────────
            // 규슈
            // ─────────────────────
            case "kyushu": // 규슈 전체
                return new RegionCenterResponse(33.0, 131.0, 7);
            case "fukuoka":
                return new RegionCenterResponse(33.5902, 130.4017, 11);
            case "nagasaki":
                return new RegionCenterResponse(32.7503, 129.8777, 11);
            case "kumamoto":
                return new RegionCenterResponse(32.8031, 130.7079, 11);
            case "kagoshima":
                return new RegionCenterResponse(31.5966, 130.5571, 11);

            // ─────────────────────
            // 오키나와 & 도서
            // ─────────────────────
            case "okinawa": // 오키나와 전체
                return new RegionCenterResponse(26.5, 128.0, 7);
            case "naha":
                return new RegionCenterResponse(26.2124, 127.6809, 11);
            case "ishigaki":
                return new RegionCenterResponse(24.3448, 124.1572, 11);
            case "miyako":
                return new RegionCenterResponse(24.8053, 125.2811, 11);

            // ─────────────────────
            // 기타 기존 코드 호환
            // (예전에 쓰던 aichi 등)
            // ─────────────────────
            case "aichi":
                return new RegionCenterResponse(35.1815, 136.9066, 11);

            default:
                break;
        }

        // ─────────────────────
        // DB의 region_bounds 기반 동적 계산
        // ─────────────────────
        MapMapper.RegionBounds b = mapMapper.selectRegionBounds(regionCode);
        if (b == null || b.cnt == null || b.cnt == 0
                || b.minLat == null || b.maxLat == null || b.minLng == null || b.maxLng == null) {
            return new RegionCenterResponse(fallbackLat, fallbackLng, fallbackZoom);
        }

        double centerLat = (b.minLat + b.maxLat) / 2.0;
        double centerLng = (b.minLng + b.maxLng) / 2.0;

        // bbox 크기에 따라 대략 zoom 추정(정교한 수학 대신 실무용 휴리스틱)
        double latSpan = Math.abs(b.maxLat - b.minLat);
        double lngSpan = Math.abs(b.maxLng - b.minLng);
        double span = Math.max(latSpan, lngSpan);

        int zoom;
        if (span > 8) zoom = 6;
        else if (span > 4) zoom = 7;
        else if (span > 2) zoom = 8;
        else if (span > 1) zoom = 9;
        else if (span > 0.5) zoom = 10;
        else if (span > 0.25) zoom = 11;
        else if (span > 0.12) zoom = 12;
        else if (span > 0.06) zoom = 13;
        else zoom = 14;

        // ✅ nav 클릭이나 region 단위로 열릴 때 최소 11 이상으로 보이게
        return new RegionCenterResponse(centerLat, centerLng, Math.max(13, zoom));
    }
    
    
    
    
    
    /**
     * ✅ DB 저장 없이 shelters(避難所) GeoJSON을 외부 API에서 받아 그대로 반환
     *
     * regionCode를 "z/x/y" 형태로 받으면 해당 타일 호출.
     * 그 외에는 기본 타일로 반환(데모).
     */
    public String fetchSheltersGeoJson(int z, int x, int y) {
        String base = props.getBaseUrl().replaceAll("/+$", "");
        String url = base + "/XGT001?response_format=geojson&z=" + z + "&x=" + x + "&y=" + y;

        try {
            ResponseEntity<String> ext = mlitRestClient.get()
                .uri(url)
                .header("Ocp-Apim-Subscription-Key", props.getApiKey())
                .header(HttpHeaders.ACCEPT, "application/json")
                .retrieve()
                .toEntity(String.class);

            String body = ext.getBody();
            if (body == null || body.isBlank()) return EMPTY_FC;

            String t = body.trim();
            if (!t.startsWith("{") || !t.contains("\"features\"")) return EMPTY_FC;

            return body;

        } catch (ResourceAccessException | HttpClientErrorException | HttpServerErrorException e) {
            return EMPTY_FC;
        }
    }

    // ---------------------------
    // 기존 기능(필요 시 유지)
    // ---------------------------

    
    /**
     * Hazard GeoJSON:
     * 1) DB 조회
     * 2) 없으면(캐시 미스) Reinfolib API 호출 -> DB upsert
     * 3) DB 데이터를 merge해서 반환
     *
     * ✅ 버튼 클릭 시: DB가 있으면 DB, 없으면 API fetch 후 저장/표시 (요구사항)
     */
    public String getHazardGeoJson(String regionCode, List<String> hazardTypes) {
        // 1) DB 먼저
        String fromDb = mergeHazardParts(mapMapper.findHazardGeoJsonList(regionCode, hazardTypes));
        if (hasAnyFeature(fromDb)) return fromDb;

        // 2) 캐시 미스면 API fetch -> upsert
        syncHazardsFromApiToDb(regionCode, hazardTypes);

        // 3) 다시 DB 읽어서 반환
        return mergeHazardParts(mapMapper.findHazardGeoJsonList(regionCode, hazardTypes));
    }

    private String mergeHazardParts(List<String> parts) {
        if (parts == null || parts.isEmpty()) return EMPTY_FC;

        try {
            ArrayNode merged = objectMapper.createArrayNode();

            for (String s : parts) {
                if (s == null || s.isBlank()) continue;
                JsonNode root = objectMapper.readTree(s);
                JsonNode feats = root.get("features");
                if (feats != null && feats.isArray()) merged.addAll((ArrayNode) feats);
            }

            ObjectNode out = objectMapper.createObjectNode();
            out.put("type", "FeatureCollection");
            out.set("features", merged);
            return objectMapper.writeValueAsString(out);

        } catch (Exception e) {
            return EMPTY_FC;
        }
    }

    private boolean hasAnyFeature(String geojson) {
        if (geojson == null || geojson.isBlank()) return false;
        try {
            JsonNode root = objectMapper.readTree(geojson);
            JsonNode feats = root.get("features");
            return feats != null && feats.isArray() && feats.size() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * hazardTypes: ["FLOOD","TSUNAMI","LANDSLIDE"] (DB enum 문자열)
     * - DB UNIQUE KEY (region_code, hazard_type) 기준으로 upsert
     */
    private void syncHazardsFromApiToDb(String regionCode, List<String> hazardTypes) {
        if (regionCode == null || regionCode.isBlank()) return;
        if (hazardTypes == null || hazardTypes.isEmpty()) return;

        for (String hazardType : hazardTypes) {
            if (hazardType == null || hazardType.isBlank()) continue;

            String apiName = switch (hazardType) {
                case "FLOOD" -> "XKT026";
                case "TSUNAMI" -> "XKT028";
                case "LANDSLIDE" -> "XKT029";
                default -> null;
            };
            if (apiName == null) continue;

            String geojson = fetchHazardGeoJsonFromReinfolib(apiName, regionCode);
            if (!hasAnyFeature(geojson)) continue;

            // ✅ region_code + hazard_type 1건으로 저장
            mapMapper.upsertHazardZone(regionCode, hazardType, geojson);
        }
    }

    /**
     * Reinfolib hazard API 호출 (regionCode 단위)
     *
     * ⚠️ regionCode 파라미터명은 Reinfolib 엔드포인트 규격에 따라 다를 수 있음.
     * - 기본값: region_code
     * - 만약 동작 안 하면: regionCode / region_code / region_code_like 등으로 조정
     */
    private String fetchHazardGeoJsonFromReinfolib(String apiName, String regionCode) {
        String base = props.getBaseUrl().replaceAll("/+$", "");
        String url = base + "/" + apiName
                + "?response_format=geojson&region_code=" + regionCode;

        try {
            ResponseEntity<String> ext = mlitRestClient.get()
                    .uri(url)
                    .header("Ocp-Apim-Subscription-Key", props.getApiKey())
                    .header(HttpHeaders.ACCEPT, "application/json")
                    .retrieve()
                    .toEntity(String.class);

            String body = ext.getBody();
            if (body == null || body.isBlank()) return EMPTY_FC;

            String t = body.trim();
            if (!t.startsWith("{") || !t.contains("\"features\"")) return EMPTY_FC;

            return body;

        } catch (Exception e) {
            return EMPTY_FC;
        }
    }
    public List<TransactionRaw> getTransactions(
            String regionCode,
            Integer yearFrom, Integer yearTo,
            Integer minPrice, Integer maxPrice,
            BigDecimal minArea, BigDecimal maxArea,
            String buildingType
    ) {
        return mapMapper.findTransactions(
                regionCode,
                yearFrom, yearTo,
                minPrice, maxPrice,
                minArea, maxArea,
                buildingType
        );
    }

    /**
     * ✅ (현재 사용) 지도 화면 bbox 기반 조회
     * MapController가 bbox(west,south,east,north)로 호출함.
     */
    public List<TransactionRaw> getTransactions(
            BigDecimal west, BigDecimal south, BigDecimal east, BigDecimal north,
            Integer yearFrom, Integer yearTo,
            Integer minPrice, Integer maxPrice,
            BigDecimal minArea, BigDecimal maxArea,
            List<String> buildingTypes,Integer buildingYearFrom
    ) {
        if (west == null || south == null || east == null || north == null) return Collections.emptyList();

        return mapMapper.findTransactionsByBbox(
                south.doubleValue(), north.doubleValue(),
                west.doubleValue(), east.doubleValue(),
                yearFrom, yearTo,
                minPrice, maxPrice,
                minArea, maxArea,
                buildingTypes,buildingYearFrom
        );
    }

    /**
     * ✅ regionCode 상관없이, 현재 지도 화면(BBOX) 기준으로만 매물 조회
     * bbox 형식: "minLng,minLat,maxLng,maxLat" (Leaflet: bounds.toBBoxString())
     */
    public List<TransactionRaw> getTransactionsByBbox(
            String bbox,
            Integer yearFrom, Integer yearTo,
            Integer minPrice, Integer maxPrice,
            BigDecimal minArea, BigDecimal maxArea,
            List<String> buildingTypes, Integer buildingYearFrom
    ) {
        if (bbox == null || bbox.isBlank()) return Collections.emptyList();

        String[] p = bbox.split(",");
        if (p.length != 4) return Collections.emptyList();

        double minLng = Double.parseDouble(p[0]);
        double minLat = Double.parseDouble(p[1]);
        double maxLng = Double.parseDouble(p[2]);
        double maxLat = Double.parseDouble(p[3]);

        return mapMapper.findTransactionsByBbox(
                minLat, maxLat,
                minLng, maxLng,
                yearFrom, yearTo,
                minPrice, maxPrice,
                minArea, maxArea,
                buildingTypes,buildingYearFrom
        );
    }

    public String fetchHazardTileGeoJson(String type, int z, int x, int y) {
        String base = props.getBaseUrl().replaceAll("/+$", "");

        // type(lowercase) -> DB enum(uppercase) + API path 매핑
        String apiName = switch (type.toLowerCase()) {
            case "flood"     -> "XKT026";
            case "tsunami"   -> "XKT028";
            case "landslide" -> "XKT029";
            default -> null;
        };
        if (apiName == null) return EMPTY_FC;

        String dbEnum = type.toUpperCase(); // FLOOD / TSUNAMI / LANDSLIDE
        // tile 기반 캐시 키: "FLOOD:14:14625:6015"
        // tile 캐시키: "F:14:14625:6015" 형태로 단축 (VARCHAR 길이 제한 대응)
        String typePrefix = switch (dbEnum) {
            case "FLOOD"     -> "FL";
            case "TSUNAMI"   -> "TS";
            case "LANDSLIDE" -> "LS";
            default          -> dbEnum.substring(0, Math.min(2, dbEnum.length()));
        };
        String tileKey = typePrefix + ":" + z + ":" + x + ":" + y; // max ~18 chars → VARCHAR(32) 충분

        // 0) JVM 메모리 캐시 (가장 빠름: ~0ms, DB/API 왕복 없음)
        String hit = tileMemCache.get(tileKey);
        if (hit != null) return hit;

        // 1) DB 캐시 확인
        List<String> cached = mapMapper.findHazardGeoJsonList(tileKey, List.of(dbEnum));
        if (cached != null && !cached.isEmpty()) {
            String fromDb = mergeHazardParts(cached);
            if (hasAnyFeature(fromDb)) {
                tileMemCache.put(tileKey, fromDb); // 다음 요청은 메모리에서 즉시 처리
                return fromDb;
            }
            // 빈 타일도 메모리에 올려 DB 재조회 방지
            tileMemCache.put(tileKey, EMPTY_FC);
            return EMPTY_FC;
        }

        // 2) API 호출
        String url = base + "/" + apiName + "?response_format=geojson&z=" + z + "&x=" + x + "&y=" + y;

        try {
            ResponseEntity<String> ext = mlitRestClient.get()
                .uri(url)
                .header("Ocp-Apim-Subscription-Key", props.getApiKey())
                .header(HttpHeaders.ACCEPT, "application/json")
                .retrieve()
                .toEntity(String.class);

            String body = ext.getBody();
            if (body == null || body.isBlank()) return EMPTY_FC;

            String trimmed = body.trim();
            if (!trimmed.startsWith("{") || !trimmed.contains("\"features\"")) return EMPTY_FC;

            // 3) DB 저장 + 메모리 캐시 등록
            if (hasAnyFeature(body)) {
                try {
                    mapMapper.upsertHazardZone(tileKey, dbEnum, body);
                } catch (Exception ex) {
                    System.out.println("[hazard] DB upsert warn: " + ex.getMessage());
                }
                tileMemCache.put(tileKey, body); // ✅ 이후 요청은 메모리에서 즉시 반환
            } else {
                tileMemCache.put(tileKey, EMPTY_FC); // 빈 타일도 캐시 → API 재호출 방지
            }

            return body;
        } catch (HttpClientErrorException e) {
            System.out.println("[hazard] API error " + type + " z=" + z + ": " + e.getStatusCode());
            return EMPTY_FC;
        } catch (Exception e) {
            return EMPTY_FC;
        }
    }
//    public List<Long> getFavoriteTransactionIds(Long userId) {
//        return mapMapper.findFavoriteTransactionIds(userId);
//    }
//
//    @Transactional
//    public boolean addFavorite(Long userId, Long transactionId) {
//        if (mapMapper.existsFavorite(userId, transactionId) == 1) return true;
//        return mapMapper.insertFavorite(userId, transactionId) == 1;
//    }
//
//    @Transactional
//    public boolean removeFavorite(Long userId, Long transactionId) {
//        return mapMapper.deleteFavorite(userId, transactionId) == 1;
//    }


    // ---------------------------
    // Parsing helpers (BigDecimal)
    // ---------------------------

    /**
     * JsonNode에서 소수 포함 숫자를 BigDecimal로 안전하게 파싱.
     * - "35.12", "35", 숫자 타입(Json number) 모두 처리
     * - "35㎡" 같은 문자열도 최대한 숫자만 추출
     */
    private BigDecimal pickDecimal(JsonNode node, String... keys) {
        if (node == null || node.isMissingNode()) return null;

        for (String k : keys) {
            JsonNode v = node.get(k);
            if (v == null || v.isNull()) continue;

            if (v.isNumber()) return v.decimalValue();

            String s = v.asText();
            if (s == null) continue;
            s = s.trim();
            if (s.isBlank()) continue;

            s = s.replaceAll("[^0-9.\\-]", "");
            if (s.isBlank() || s.equals(".") || s.equals("-") || s.equals("-."))
                continue;

            try {
                return new BigDecimal(s);
            } catch (NumberFormatException ignore) {
            }
        }
        return null;
    }

    /**
     * Map 기반 파싱을 하는 경우(예: props.get("u_area_ja"))에 쓰는 안전 변환기.
     * - String/Number/null 모두 처리
     */
    
    private BigDecimal parseDecimal(Object v) {
        if (v == null) return null;

        if (v instanceof Number) {
            return new BigDecimal(v.toString());
        }

        String s = String.valueOf(v).trim();
        if (s.isBlank()) return null;

        s = s.replaceAll("[^0-9.\\-]", "");
        if (s.isBlank()) return null;

        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    
    
}