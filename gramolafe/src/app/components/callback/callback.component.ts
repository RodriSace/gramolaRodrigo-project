import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { SpotiService } from '../../services/spoti.service';

/**
 * COMPONENTE DE RETORNO O CALLBACK (CallBackComponent)
 * 
 * ¿Qué es y para qué sirve?
 * Actúa como la pantalla intermedia invisible encargada de recibir al barman
 * cuando regresa triunfal desde el portal de Spotify.
 */
@Component({
  selector: 'app-callback',
  standalone: true,
  template: '<div style="color: white; padding: 20px; font-family: sans-serif; text-align: center;">Viculando tu local con Spotify, un momento por favor...</div>'
})
export class CallBackComponent implements OnInit {
  
  // Inyectamos ActivatedRoute para recuperar los datos de la URL, Router para redirigir 
  // y SpotiService para comunicarnos con el servidor.
  constructor(
    private route: ActivatedRoute,
    private router: Router,
    public spoti: SpotiService
  ) {}

  // ========================================================
  // 🔐 FLUJO 4: LOGIN Y CONEXIÓN CON SPOTIFY (OAuth2) — PASO 3
  // ngOnInit() lee queryParams (code, state) de la URL y limpia la URL con history.replaceState
  // ========================================================
  /**
   * Método disparado de inmediato cuando la página intermedia se dibuja.
   */
  ngOnInit(): void {
    const qp = this.route.snapshot.queryParamMap;
    const code  = qp.get('code');   // Código temporal provisto por Spotify
    const state = qp.get('state');  // Estado aleatorio de seguridad
    const error = qp.get('error');  // Bandera de error (si el usuario rechazó el permiso)

    // Escenario 1: El barman pulsó "Cancelar" o rechazó los permisos en Spotify
    if (error) {
      console.warn("El usuario rechazó los permisos en Spotify");
      this.router.navigateByUrl('/login'); // Lo regresamos al login
      return;
    }
    
    // Escenario 2: La URL no contiene los parámetros esperados de OAuth2
    if (!code || !state) {
      console.error("Falta el código de autorización o el estado de seguridad");
      this.router.navigateByUrl('/login');
      return;
    }

    // ========================================================
    // 🔐 FLUJO 4: LOGIN Y CONEXIÓN CON SPOTIFY (OAuth2) — PASO 4 (Llamada Canje)
    // Llama al backend a GET /spoti/getAuthorizationToken para canjear el código por tokens
    // ========================================================
    // Enviamos el código asíncronamente al backend de Spring Boot a través de nuestro servicio
    this.spoti.getAuthorizationToken(code).subscribe({
      next: (data) => {
        // Almacenamos el Token de Acceso para realizar llamadas HTTP directas a Spotify desde Angular
        this.spoti.spotiToken = data.access_token;
        
        // Marcamos en la sesión local del barman que Spotify ya está completamente vinculado
        const userData = localStorage.getItem('user');
        if (userData) {
          const user = JSON.parse(userData);
          user.spotifyConnected = true; // Variable booleana en frontend
          localStorage.setItem('user', JSON.stringify(user));
        }
        
        // Redirección exitosa a la pantalla principal del reproductor
        this.router.navigateByUrl('/music');
      },
      error: (err) => {
        console.error('Error al solicitar el access token de Spotify:', err);
        this.router.navigateByUrl('/login'); // Si falla la comunicación, volvemos a la zona segura
      }
    });
  }
}
