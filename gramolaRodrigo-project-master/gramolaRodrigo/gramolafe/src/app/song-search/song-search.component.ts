import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { API_URL } from '../api.config';
import { PaymentFormComponent } from '../payment-form/payment-form.component';
import { Router } from '@angular/router';

@Component({
  selector: 'app-song-search',
  standalone: true,
  imports: [CommonModule, FormsModule, PaymentFormComponent],
  templateUrl: './song-search.component.html',
  styleUrl: './song-search.component.css'
})
export class SongSearchComponent implements OnInit {
  // Variables de búsqueda
  query: string = ''; // Antes searchTerm
  results: any[] = [];
  isLoading = false;
  searchTimeout: any;

  // Variables para el modal de pago
  selectedSongForPayment: any = null;
  priceInCents: number = 99; // Precio por defecto (0.99€)

  // Variables para la notificación
  showNotification: boolean = false;
  notificationMessage: string = '';

  constructor(private http: HttpClient, private router: Router) {}

  ngOnInit(): void {
    // Obtenemos el precio real desde el backend al cargar
    this.http.get<{amountInCents:number}>(`${API_URL}/plans/song-price`).subscribe({
      next: (d) => this.priceInCents = d.amountInCents,
      error: () => this.priceInCents = 99 // Fallback
    });
  }

  onQueryChange() {
    if (this.searchTimeout) {
      clearTimeout(this.searchTimeout);
    }
    this.searchTimeout = setTimeout(() => {
      this.performSearch();
    }, 300); // Debounce 300ms
  }

  // Método de búsqueda (vinculado al botón "Buscar")
  performSearch() {
    const trimmedQuery = this.query.trim();
    if (!trimmedQuery) {
      this.results = [];
      return;
    }

    // Client-side validation: require at least 3 characters
    if (trimmedQuery.length < 3) {
      this.results = [];
      return;
    }

    this.isLoading = true;
    this.results = []; // Limpiar resultados anteriores

    // Llamada al endpoint de búsqueda (ajusta la ruta si es diferente en tu backend)
    this.http.get(`${API_URL}/songs/search?q=${trimmedQuery}`, { responseType: 'text' }).subscribe({
      next: (data: string) => {
        try {
          const parsed = JSON.parse(data);
          if (parsed.error) {
            console.error('Error from backend:', parsed.error);
            alert(parsed.error);
            this.results = [];
          } else {
            this.results = (parsed.data || []).map((song: any) => ({
              ...song,
              artist: song.artist.name,
              albumCover: song.album.cover
            }));
          }
        } catch (e) {
          console.error('Error parsing response:', e);
          this.results = [];
        }
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error buscando canciones:', err);
        this.isLoading = false;
        this.results = [];
        alert('Error al buscar canciones. Inténtalo de nuevo.');
      }
    });
  }

  // 1. Abrir Modal: El usuario hace clic en el precio de una canción
  initiatePayment(song: any) {
    this.selectedSongForPayment = song;
  }

  // 2. Cancelar Modal: El usuario pulsa "Cancelar" en el modal
  cancelPayment() {
    this.selectedSongForPayment = null;
  }

  // 3. Pago exitoso: Stripe ha procesado el pago y añadido a la cola
  onPaymentSuccess() {
    const song = this.selectedSongForPayment;
    this.notificationMessage = `Pagado con éxito. "${song.title}" añadida a la cola.`;
    this.showNotification = true;
    this.selectedSongForPayment = null; // Cerrar modal

    // Ocultar notificación después de 4 segundos
    setTimeout(() => {
      this.showNotification = false;
    }, 4000);

    // Opcional: Redirigir a la vista de cola
    // this.router.navigate(['/queue']);
  }
}
