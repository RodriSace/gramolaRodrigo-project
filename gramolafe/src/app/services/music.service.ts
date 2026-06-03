import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/**
 * SERVICIO FRONTEND DE CONTROL MUSICAL (MusicService)
 * 
 * ¿Qué es y para qué sirve?
 * Este servicio inyectable encapsula todas las peticiones HTTP relativas a la interacción
 * musical del cliente o del barman: búsquedas, cola de canciones, procesamiento del pago de Stripe
 * por canción y activación de suscripciones premium.
 */
@Injectable({
  providedIn: 'root'
})
export class MusicService {
  
  // Endpoint base de música en el backend de Spring Boot
  private apiUrl = 'http://127.0.0.1:8080/api/music';

  constructor(private http: HttpClient) { }

  // ========================================================
  // 💳 FLUJO 3: PAGO DE LA SUSCRIPCIÓN — RUTA B (Servicio)
  // Llama al backend a /users/activate-subscription para poner 'subscriptionActive = true' en MySQL
  // ========================================================
  /**
   * Activar manualmente la suscripción.
   * Llama al endpoint de activación de suscripciones en caliente (ideal para simulaciones
   * locales o integraciones sin pasarela externa completa).
   */
  activateSubscription(token: string): Observable<any> {
    return this.http.post(`http://127.0.0.1:8080/users/activate-subscription`, { token });
  }

  // ========================================================
  // 💰 FLUJO 7: UN CLIENTE PAGA PARA COLAR SU CANCIÓN — PASO 1 (Servicio)
  // Envía una petición GET al backend para buscar canciones en la API de Spotify
  // ========================================================
  /**
   * Buscar canciones en Spotify a través de nuestro backend.
   * Envía el texto de búsqueda y el token de acceso temporal de Spotify del local.
   * 
   * @param query Texto a buscar (ej: "Creep Radiohead")
   * @param token Access Token de Spotify del local.
   */
  search(query: string, token: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/search?query=${query}&token=${token}`);
  }

  // ========================================================
  // 💰 FLUJO 7: UN CLIENTE PAGA PARA COLAR SU CANCIÓN — PASO 2 (Servicio)
  // Envía un POST al backend solicitando iniciar el proceso de Checkout en Stripe (pago único)
  // ========================================================
  /**
   * Pagar por priorizar una canción en la cola.
   * Envía un POST al backend con la información del tema seleccionado y el ID del local.
   * El backend responde iniciando la pasarela de Stripe (retorna una URL de checkout).
   * 
   * @param trackData Objeto con el trackId, título, artista, duración, carátula e ID del bar.
   */
  paySong(trackData: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/pay`, trackData);
  }

  // ========================================================
  // 💰 FLUJO 7: UN CLIENTE PAGA PARA COLAR SU CANCIÓN — PASO 3 (Servicio)
  // Hace un POST a /api/music/queue/add para añadir la canción a la tabla 'playback_queue' en MySQL
  // ========================================================
  /**
   * Añadir una canción a la cola activa.
   * Usado principalmente para añadir música ambiental sin coste (false en isPaid) 
   * directamente al final de la lista del local.
   */
  addToQueue(payload: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/queue/add`, payload);
  }
}

