package com.safehome.domain;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {

    private Long id;
    private String email;
    private String password;
    private Role role;
    private LocalDateTime createdAt;

    public enum Role {
        USER, ADMIN
    }
    
    
}