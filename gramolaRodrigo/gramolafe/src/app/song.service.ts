import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject } from 'rxjs';
import { API_URL } from './api.config';

@Injectable({
  providedIn: 'root'
})
export class SongService {

  constructor(private http: HttpClient) { }

  // Subject para notificar cambios en la cola (ej. canción añadida)
  private queueChangedSubject = new Subject<void>();
  queueChanged$ = this.queueChangedSubject.asObservable();

  /**
   * Llama al backend para buscar canciones en la API de Deezer.
   * @param query El término de búsqueda.
   */
  search(query: string): Observable<any> {
    return this.http.get(`${API_URL}/songs/search?query=${query}`);
  }

  getQueue(): Observable<any> {
    return this.http.get(`${API_URL}/queue`);
  }

  // 👇 MÉTODO NUEVO 👇
  playNext(): Observable<any> {
    return this.http.post(`${API_URL}/queue/next`, {});
  }

  // Notifica a los suscriptores que la cola ha cambiado
  notifyQueueChanged(): void {
    this.queueChangedSubject.next();
  }
}