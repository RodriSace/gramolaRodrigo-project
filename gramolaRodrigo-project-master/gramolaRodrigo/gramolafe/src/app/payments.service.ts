import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_URL } from './api.config';
import { AuthService } from './auth.service';

@Injectable({
  providedIn: 'root'
})
export class PaymentsService {

  constructor(private http: HttpClient, private auth: AuthService) { }
  // Modificamos este método para que acepte el objeto canción completo
  confirmPayment(paymentIntentId: string, song: any): Observable<any> {
    const bar = this.auth.currentUserValue;
    if (!bar?.id) {
      throw new Error('No hay usuario en sesión. Inicia sesión para pagar.');
    }
    const data = { 
      paymentIntentId: paymentIntentId,
      songId: song.id,
      title: song.title,
      artist: song.artist.name,
      albumCover: song.album.cover_small
      ,
      previewUrl: song.preview, // <-- AÑADE ESTA LÍNEA (la API de Deezer usa 'preview')
      duration: song.duration, // <-- AÑADE ESTA LÍNEA
      barId: bar.id
    };
    return this.http.post(`${API_URL}/payments/confirm`, data);
  }
  createPaymentIntent(songId: string): Observable<any> {
    const data = { songId: songId };
    return this.http.post(`${API_URL}/payments/create-payment-intent`, data);
  }
}