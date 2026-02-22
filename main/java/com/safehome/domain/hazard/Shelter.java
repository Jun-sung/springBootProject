package com.safehome.domain.hazard;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Shelter {
    private Long id;
    private String regionCode;
    private String nameJa;
    private String addressJa;
    private Double lat;
    private Double lng;

    // 어떤 재해에 대응하는지(홍수/토사/쓰나미 등) 플래그들

    private LocalDateTime createdAt;
}