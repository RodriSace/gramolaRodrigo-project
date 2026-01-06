import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, of } from 'rxjs';
import { tap, map, catchError } from 'rxjs/operators';
import { API_URL } from './api.config';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private currentUserSubject = new BehaviorSubject<any>(this.getUserFromStorage());
  public currentUser$ = this.currentUserSubject.asObservable();

  constructor(private http: HttpClient) { }

  private getUserFromStorage(): any {
    try {
      const raw = sessionStorage.getItem('currentUser');
      return raw ? JSON.parse(raw) : null;
    } catch { return null; }
  }

  public get currentUserValue(): any {
    return this.currentUserSubject.value;
  }

  register(userData: any): Observable<any> {
    return this.http.post(`${API_URL}/bars/register`, userData);
  }

  login(credentials: any): Observable<any> {
    return this.http.post(`${API_URL}/bars/login`, credentials).pipe(
      tap((user: any) => {
        sessionStorage.setItem('currentUser', JSON.stringify(user));
        this.currentUserSubject.next(user);
      })
    );
  }

  // NUEVO: Comprueba si el bar tiene plan activo
  checkSubscriptionStatus(): Observable<boolean> {
    const user = this.currentUserValue;
    if (!user || !user.id) return of(false);

    return this.http.get<{active: boolean}>(`${API_URL}/bars/subscription-status?barId=${user.id}`).pipe(
      map(response => response.active),
      catchError(() => of(false))
    );
  }

  logout() {
    sessionStorage.removeItem('currentUser');
    this.currentUserSubject.next(null);
  }
}