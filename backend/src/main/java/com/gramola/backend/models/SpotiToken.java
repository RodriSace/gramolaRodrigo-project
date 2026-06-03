package com.gramola.backend.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Modelo auxiliar para mapear la respuesta JSON de autenticación de Spotify.
 */
@Data
public class SpotiToken {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private int expiresIn;
    
    @JsonProperty("refresh_token")
    private String refreshToken;
}
