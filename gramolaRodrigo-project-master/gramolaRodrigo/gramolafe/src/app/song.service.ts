import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, Subject, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { API_URL } from './api.config';

@Injectable({
  providedIn: 'root'
})
export class SongService {
  // Proxy para saltar el bloqueo CORS de Deezer
  private corsProxy = 'https://corsproxy.io/?';

  constructor(private http: HttpClient) { }

  private queueChangedSubject = new Subject<void>();
  queueChanged$ = this.queueChangedSubject.asObservable();

  /**
   * Obtiene 10 canciones aleatorias de Deezer usando un Proxy para evitar CORS
   */
  getRandomSongs(): Observable<any[]> {
    const genres = ['rock', 'pop', 'dance', 'disco', 'hits'];
    const randomQuery = genres[Math.floor(Math.random() * genres.length)];
    const targetUrl = `https://api.deezer.com/search?q=${randomQuery}`;
    
    // Concatenamos el proxy con la URL de Deezer
    return this.http.get<any>(this.corsProxy + encodeURIComponent(targetUrl)).pipe(
      map(res => {
        if (!res || !res.data) return [];
        return res.data.slice(0, 10).map((track: any) => ({
          songId: track.id.toString(),
          title: track.title,
          artist: track.artist.name,
          albumCover: track.album.cover_medium,
          previewUrl: track.preview,
          timestamp: Date.now()
        }));
      }),
      catchError(err => {
        console.error('Error en getRandomSongs:', err);
        return of([]);
      })
    );
  }

  search(query: string): Observable<any> {
    return this.http.get(`${API_URL}/songs/search?query=${query}`);
  }

  getQueue(): Observable<any> {
    return this.http.get(`${API_URL}/queue`);
  }

  playNext(): Observable<any> {
    return this.http.post(`${API_URL}/queue/next`, {});
  }

  notifyQueueChanged(): void {
    this.queueChangedSubject.next();
  }
}