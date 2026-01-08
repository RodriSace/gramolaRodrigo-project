package com.example.gramolaRodrigo.controllers.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegisterRequest {
    private String name;
    private String email;
    private String pwd1;
    private String pwd2;
    private String clientId;      // ID opcional para la API de Deezer
    private String clientSecret;  // Secret opcional para la API de Deezer
}