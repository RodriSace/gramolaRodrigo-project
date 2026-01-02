import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  // BehaviorSubject guarda el último valor emitido y lo da a los nuevos suscriptores.
  // Lo inicializamos con el valor que haya en sessionStorage (o null si no hay nada).
  private currentUserSubject = new BehaviorSubject<any>((() => {
    try {
      const raw = sessionStorage.getItem('currentUser');
      return raw ? JSON.parse(raw) : null;
    } catch {
      return null;
    }
  })());
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor() { }

  // Método para obtener el valor actual del usuario
  public get currentUserValue(): any {
    return this.currentUserSubject.value;
  }

  // Se llama desde el LoginComponent cuando el login es exitoso
  login(user: any) {
    sessionStorage.setItem('currentUser', JSON.stringify(user));
    this.currentUserSubject.next(user); // Notifica a todos los suscriptores
  }

  // Se llamará desde un botón de "Cerrar Sesión"
  logout() {
    sessionStorage.removeItem('currentUser');
    this.currentUserSubject.next(null); // Notifica que ya no hay usuario
  }
}
