package com.safehome.mapper;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.safehome.domain.hazard.HazardZone;
import com.safehome.domain.hazard.Shelter;
import com.safehome.domain.land.TransactionRaw;

@Mapper
public interface MapMapper {
	
    class RegionBounds {
        public Double minLat;
        public Double maxLat;
        public Double minLng;
        public Double maxLng;
        public Long cnt;
    }
    
    // 해저드 업서트
    int upsertTransactions(@org.apache.ibatis.annotations.Param("rows") java.util.List<com.safehome.domain.land.TransactionUpsertRow> rows);

    int upsertLandprices(@org.apache.ibatis.annotations.Param("rows") java.util.List<com.safehome.domain.land.LandpriceUpsertRow> rows);

    // hazard upsert
    int upsertHazardZone(@org.apache.ibatis.annotations.Param("regionCode") String regionCode,
                         @org.apache.ibatis.annotations.Param("hazardType") String hazardType,
                         @org.apache.ibatis.annotations.Param("geojson") String geojson);
    
    
    TransactionRaw findNearestTransaction(
    	    @Param("lat") double lat,
    	    @Param("lng") double lng,
    	    @Param("radiusMeters") int radiusMeters,
    	    @Param("latDelta") double latDelta,
    	    @Param("lngDelta") double lngDelta
    	);
    
    
    RegionBounds selectRegionBounds(@Param("regionCode") String regionCode);
    
   
	
	
	List<String> findHazardGeoJsonList(@Param("regionCode") String regionCode,
            @Param("hazardTypes") List<String> hazardTypes);
	
	
	List<TransactionRaw> findTransactions(
			  @Param("regionCode") String regionCode,
			  @Param("yearFrom") Integer yearFrom,
			  @Param("yearTo") Integer yearTo,
			  @Param("minPrice") Integer minPrice,
			  @Param("maxPrice") Integer maxPrice,
			  @Param("minArea") BigDecimal minArea,
			  @Param("maxArea") BigDecimal maxArea,
			  @Param("buildingType") String buildingType
			);

	/**
	 * ✅ 현재 지도 화면(BBOX) 기준 거래 포인트 조회
	 * - regionCode에 의존하지 않음
	 * - 필터가 null이면 조건 없이 전체
	 */
	List<TransactionRaw> findTransactionsByBbox(
			@Param("minLat") double minLat,
			@Param("maxLat") double maxLat,
			@Param("minLng") double minLng,
			@Param("maxLng") double maxLng,
			@Param("yearFrom") Integer yearFrom,
			@Param("yearTo") Integer yearTo,
			@Param("minPrice") Integer minPrice,
			@Param("maxPrice") Integer maxPrice,
			@Param("minArea") BigDecimal minArea,
			@Param("maxArea") BigDecimal maxArea,
			@Param("buildingTypes") List<String> buildingTypes,
			@Param("buildingYearFrom") Integer buildingYearFrom

			
	);

	// ===== Hazards / Shelters (bbox based) =====
	List<HazardZone> selectHazardsInBbox(
			@Param("minLat") BigDecimal minLat,
			@Param("maxLat") BigDecimal maxLat,
			@Param("minLng") BigDecimal minLng,
			@Param("maxLng") BigDecimal maxLng,
			@Param("hazardType") String hazardType
	);

	List<Shelter> selectSheltersInBbox(
			@Param("minLat") BigDecimal minLat,
			@Param("maxLat") BigDecimal maxLat,
			@Param("minLng") BigDecimal minLng,
			@Param("maxLng") BigDecimal maxLng
	);


	
    // favorites
    List<Long> findFavoriteTransactionIds(@Param("userId") Long userId);

    int existsFavorite(@Param("userId") Long userId,
                       @Param("transactionId") Long transactionId);

    int insertFavorite(@Param("userId") Long userId,
                       @Param("transactionId") Long transactionId);

    int deleteFavorite(@Param("userId") Long userId,
                       @Param("transactionId") Long transactionId);
}