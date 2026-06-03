import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

/**
 * SERVICIO DE GESTIÓN DE USUARIOS (UserService)
 * 
 * ¿Qué es y para qué sirve?
 * Es una clase de servicio de Angular anotada con @Injectable, lo que significa
 * que puede ser inyectada en cualquier componente (como Login o Register) mediante
 * el mecanismo de Inyección de Dependencias de Angular.
 * 
 * Su función exclusiva es actuar como puente de comunicación HTTP entre el frontend de Angular
 * y los endpoints REST del backend correspondientes a la gestión de usuarios (/users) en Spring Boot.
 */
@Injectable({
  providedIn: 'root' // Indica que el servicio está disponible de manera global en toda la aplicación
})
export class UserService {
  
  // URL base del controlador de usuarios del backend (Spring Boot en puerto 8080)
  private apiUrl = 'http://127.0.0.1:8080/users';

  // Inyectamos el cliente HttpClient oficial de Angular para poder disparar peticiones HTTP (GET, POST...)
  constructor(private http: HttpClient) { }

  // ========================================================
  // 📝 FLUJO 1: REGISTRO DE UN BAR — PASO 2 (Servicio)
  // Envía los datos del formulario mediante un POST al endpoint /users/register de Spring Boot
  // ========================================================
  /**
   * Petición de Registro de Local.
   * Envía un POST HTTP al backend con los datos del formulario (email, password, nombre del bar, etc.).
   * Retorna un Observable con la respuesta del servidor.
   */
  register(userData: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/register`, userData);
  }

  // ========================================================
  // 📧 FLUJO 2: CONFIRMACIÓN DE CORREO — PASO 1 (Servicio)
  // Realiza la confirmación enviando el token mediante un GET a /users/confirmToken
  // ========================================================
  /**
   * Petición de Confirmación de Correo Electrónico.
   * Dispara un GET al backend enviando el token recibido por email en la URL para verificar la cuenta.
   */
  confirm(token: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/confirmToken/any?token=${token}`);
  }

  // ========================================================
  // 🔐 FLUJO 4: LOGIN Y CONEXIÓN CON SPOTIFY — PASO 1 (Servicio)
  // Envía el email y contraseña en formato JSON al endpoint /users/login del backend
  // ========================================================
  /**
   * Petición de Inicio de Sesión (Login).
   * Envía las credenciales (correo y contraseña) al backend en formato JSON.
   * Importante: El backend responde con un String simple (correo del usuario logueado en texto plano), 
   * por lo que configuramos '{ responseType: "text" }' para evitar que Angular intente parsearlo como JSON.
   */
  login(email: string, password: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/login`, { email, password }, { responseType: 'text' });
  }
}

