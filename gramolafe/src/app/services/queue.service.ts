import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, timer } from 'rxjs';
import { switchMap } from 'rxjs/operators';

/**
 * SERVICIO FRONTEND DE LA COLA DE REPRODUCCIÓN (QueueService)
 * 
 * ¿Qué es y para qué sirve?
 * Este servicio interactúa con el backend de Spring Boot para obtener la cola de reproducción
 * en tiempo real de un bar determinado.
 * 
 * Concepto Clave para Defensa: POLLING EN VIVO (Muestreo Periódico)
 * Para cumplir el requisito de actualizar la cola de música del bar en la pantalla del cliente
 * sin complicar el sistema con WebSockets bidireccionales, implementamos "Polling".
 * 
 * ¿Cómo funciona 'getQueuePolling'?
 * 1. 'timer(0, 5000)': Genera un temporizador que se dispara de inmediato (retraso inicial 0)
 *    y luego se ejecuta periódicamente cada 5000 milisegundos (5 segundos).
 * 2. 'switchMap(...)': Es un operador reactivo de RxJS sumamente potente. 
 *    Cada vez que el temporizador emite una señal (cada 5 segundos), 'switchMap' cancela la petición HTTP
 *    anterior (si estuviese colgada) y lanza una petición GET HTTP nueva para traer la cola fresca del backend.
 * 3. De esta forma, si otro cliente paga por una canción, en menos de 5 segundos todos los móviles
 *    de la sala verán la cola actualizada automáticamente de forma sincronizada y fluida.
 */
@Injectable({
  providedIn: 'root'
})
export class QueueService {
  
  // Endpoint del backend para consultar la cola
  private apiUrl = 'http://127.0.0.1:8080/api/music/queue';

  constructor(private http: HttpClient) { }

  // ========================================================
  // 🎵 FLUJO 5: EL BARMAN PONE MÚSICA DE FONDO — PASO 3 (Servicio)
  // getQueue() realiza la petición GET HTTP a /api/music/queue para recuperar la cola de MySQL
  // ========================================================
  /**
   * Obtiene la cola de canciones de un bar haciendo una única petición GET HTTP al backend.
   * 
   * @param barId ID único de base de datos del bar que queremos consultar.
   */
  getQueue(barId: number): Observable<any> {
    return this.http.get(`${this.apiUrl}?barId=${barId}`);
  }

  // ========================================================
  // 🎵 FLUJO 5 / 🔄 FLUJO 6: CUANDO UNA CANCIÓN TERMINA — PASO 1 (Servicio Polling)
  // getQueuePolling() realiza un muestreo periódico (polling) cada 5s usando timer y switchMap
  // ========================================================
  /**
   * Crea un flujo observable continuo que refresca la cola de reproducción de forma periódica
   * cada 5 segundos mediante la técnica de Polling.
   * 
   * @param barId ID del local.
   */
  getQueuePolling(barId: number): Observable<any[]> {
    return timer(0, 5000).pipe(
      switchMap(() => this.getQueue(barId))
    );
  }
}

