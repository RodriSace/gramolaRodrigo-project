import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/**
 * Servicio Angular para gestionar la integración con la API de Spotify
 * y controlar el reproductor Web Playback SDK.
 */
@Injectable({
  providedIn: 'root'
})
export class SpotiService {
  // URLs de configuración para llamadas al backend y Spotify API
  apiUrl = 'http://127.0.0.1:8080/spoti';
  authorizeUrl = 'https://accounts.spotify.com/authorize';
  redirectUrl = 'http://127.0.0.1:4200/callback';
  
  // Referencias globales del reproductor (Web Playback SDK)
  sdkPlayer: any = null;
  sdkDeviceId: string | null = null;
  sdkReady = false;
  
  // Getters y setters para persistir credenciales de sesión
  get spotiToken(): string | null {
    return sessionStorage.getItem('spotiToken');
  }

  set spotiToken(value: string | null) {
    if (value) sessionStorage.setItem('spotiToken', value);
  }

  get clientId(): string | null {
    return sessionStorage.getItem('clientId');
  }

  set clientId(value: string | null) {
    if (value) sessionStorage.setItem('clientId', value);
  }

  get email(): string | null {
    return sessionStorage.getItem('userEmail');
  }

  set email(value: string | null) {
    if (value) sessionStorage.setItem('userEmail', value);
  }

  constructor(private http: HttpClient) { }

  // ========================================================
  // 🔐 FLUJO 4: LOGIN Y CONEXIÓN CON SPOTIFY (OAuth2) — PASO 2
  // sessionStorage.setItem("clientId", ...) y redirectToSpotify() (construye URL de autorización)
  // ========================================================
  /**
   * Redirige al barman al portal de autorización OAuth2 de Spotify.
   */
  redirectToSpotify() {
    const scopes = [
      'user-read-private',
      'user-read-email',
      'user-read-playback-state',
      'user-modify-playback-state',
      'user-read-currently-playing',
      'streaming',
      'playlist-read-private',
      'playlist-read-collaborative',
      'user-library-read',
      'user-library-modify',
      'user-read-recently-played',
      'user-top-read',
      'app-remote-control'
    ];
    
    // Cadena aleatoria de seguridad (CSRF)
    const state = Math.random().toString(36).substring(7);
    sessionStorage.setItem("oauth_state", state);

    // Parámetros de petición de autorización
    let params = `response_type=code&client_id=${this.clientId}&scope=${encodeURIComponent(scopes.join(" "))}&redirect_uri=${this.redirectUrl}&state=${state}&show_dialog=true`;
    
    window.location.href = `${this.authorizeUrl}?${params}`;
  }

  // ========================================================
  // 🔐 FLUJO 4: LOGIN Y CONEXIÓN CON SPOTIFY (OAuth2) — PASO 4 (Servicio Canje)
  // getAuthorizationToken() realiza la llamada HTTP GET al backend para el canje seguro de tokens
  // ========================================================
  /**
   * CANJEAR CÓDIGO TEMPORAL POR TOKENS DEFINITIVOS
   * 
   * Llama a nuestro backend en Spring Boot para realizar el intercambio seguro del código OAuth2.
   */
  getAuthorizationToken(code: string): Observable<any> {
    let url = `${this.apiUrl}/getAuthorizationToken?code=${code}&clientId=${this.clientId}&email=${this.email}`;
    return this.http.get(url);
  }

  // ========================================================
  // 🔐 FLUJO 4: LOGIN Y CONEXIÓN CON SPOTIFY (OAuth2) — REFRESO DE TOKENS (Servicio)
  // refreshToken() realiza la llamada HTTP GET al backend para solicitar un nuevo token fresco
  // ========================================================
  /**
   * REFRESCAR EL ACCESS TOKEN CADUCADO
   * 
   * Envía una petición al backend para solicitar un token temporal fresco usando el refresh_token guardado en MySQL.
   */
  refreshToken(email: string): Observable<any> {
    let url = `${this.apiUrl}/refreshToken?email=${email}`;
    return this.http.get(url);
  }

  // ========================================================
  // 🎵 FLUJO 5: EL BARMAN PONE MÚSICA DE FONDO — PASO 1 (Tarea B y C)
  // Métodos de lectura de dispositivos y playlists desde Spotify API
  // ========================================================

  /**
   * OBTENER DISPOSITIVOS SPOTIFY ACTIVOS
   * 
   * Hace un GET HTTP directo a la API de Spotify (inyectando la cabecera Bearer)
   * para conocer los altavoces o reproductores disponibles.
   */
  getDevices(): Observable<any> {
    const headers = { 'Authorization': `Bearer ${this.spotiToken}` };
    return this.http.get('https://api.spotify.com/v1/me/player/devices', { headers });
  }

  /**
   * CARGAR PLAYLISTS DEL BARMAN
   * 
   * Recupera las listas de reproducción creadas por el dueño en su Spotify personal.
   */
  getPlaylists(): Observable<any> {
    const headers = { 'Authorization': `Bearer ${this.spotiToken}` };
    return this.http.get('https://api.spotify.com/v1/me/playlists', { headers });
  }

  // ========================================================
  // 🎵 FLUJO 5: EL BARMAN PONE MÚSICA DE FONDO — PASO 2
  // Métodos de consulta de las canciones dentro de una lista de Spotify
  // ========================================================

  /**
   * OBTENER CANCIONES DENTRO DE UNA PLAYLIST
   * 
   * Recupera las canciones de una lista concreta elegida en el reproductor del local.
   */
  getPlaylistTracks(playlistId: string): Observable<any> {
    const headers = { 'Authorization': `Bearer ${this.spotiToken}` };
    return this.http.get(`https://api.spotify.com/v1/playlists/${playlistId}/tracks`, { headers });
  }

  // ========================================================
  // 🎵 FLUJO 5: EL BARMAN PONE MÚSICA DE FONDO — PASO 4 (Y 🔄 FLUJO 6 — PASO 4)
  // Envía la orden HTTP PUT a Spotify para reproducir una canción concreta en el dispositivo
  // ========================================================

  /**
   * REPRODUCIR UNA CANCIÓN INDIVIDUAL
   * 
   * Pide a Spotify reproducir una pista concreta en el reproductor del navegador (deviceId)
   */
  playTrack(trackId: string, deviceId: string): Observable<any> {
    const headers = { 'Authorization': `Bearer ${this.spotiToken}` };
    const body = { uris: [`spotify:track:${trackId}`] };
    const url = `https://api.spotify.com/v1/me/player/play?device_id=${deviceId}`;
    return this.http.put(url, body, { headers });
  }

  // ========================================================
  // OTROS MÉTODOS Y UTILIDADES AUXILIARES DE SPOTIFY
  // ========================================================

  /**
   * REPRODUCIR UNA PLAYLIST COMPLETA DESDE UN ÍNDICE
   * 
   * Ordena a Spotify activar el hilo musical a partir de una canción en concreto.
   */
  playPlaylist(playlistUri: string, deviceId: string, offsetIndex: number = 0): Observable<any> {
    const headers = { 'Authorization': `Bearer ${this.spotiToken}` };
    const body: any = { 
        context_uri: playlistUri,
        offset: { position: offsetIndex }
    };
    return this.http.put(`https://api.spotify.com/v1/me/player/play?device_id=${deviceId}`, body, { headers });
  }

  /**
   * AÑADIR A LA COLA DE REPRODUCCIÓN FÍSICA DE SPOTIFY
   * 
   * Inyecta una canción en la cola interna que Spotify gestiona para reproducirla a continuación.
   */
  addToQueue(trackUri: string, deviceId: string): Observable<any> {
    const headers = { 'Authorization': `Bearer ${this.spotiToken}` };
    return this.http.post(`https://api.spotify.com/v1/me/player/queue?uri=${encodeURIComponent(trackUri)}&device_id=${deviceId}`, null, { headers });
  }
}
