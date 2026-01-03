import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from './api.config'; // Usamos la constante de la URL

@Injectable({
  providedIn: 'root'
})
export class UserService {

  constructor(private http: HttpClient) { }

  /**
   * Llama al endpoint del backend para registrar un nuevo bar.
   * @param userData Un objeto con los datos del usuario.
   */
  register(userData: any): Observable<any> {
    return this.http.post(`${API_URL}/bars/register`, userData);
  }

  /**
   * Llama al endpoint del backend para hacer login.
   * @param credentials Un objeto con email y contraseña.
   */
  login(credentials: any): Observable<any> {
    return this.http.post(`${API_URL}/bars/login`, credentials);
  }
}