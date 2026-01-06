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
  // Quitamos styleUrl si no tienes un CSS específico, o déjalo si ya existe
})
export class SongSearchComponent implements OnInit {
  // Variables de búsqueda
  query: string = ''; // Antes searchTerm
  results: any[] = [];
  isLoading = false;

  // Variables para el modal de pago
  selectedSongForPayment: any = null;
  priceInCents: number = 99; // Precio por defecto (0.99€)

  constructor(private http: HttpClient, private router: Router) {}

  ngOnInit(): void {
    // Obtenemos el precio real desde el backend al cargar
    this.http.get<{amountInCents:number}>(`${API_URL}/plans/song-price`).subscribe({
      next: (d) => this.priceInCents = d.amountInCents,
      error: () => this.priceInCents = 99 // Fallback
    });
  }

  // Método de búsqueda (vinculado al botón "Buscar")
  search() {
    if (!this.query.trim()) return;
    
    this.isLoading = true;
    this.results = []; // Limpiar resultados anteriores
    
    // Llamada al endpoint de búsqueda (ajusta la ruta si es diferente en tu backend)
    this.http.get<any[]>(`${API_URL}/songs/search?q=${this.query}`).subscribe({
      next: (data) => {
        this.results = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error buscando canciones:', err);
        this.isLoading = false;
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

  // 3. Confirmar Pago: El usuario pulsa "Pagar" en el modal
  processPaymentSuccess() {
    const song = this.selectedSongForPayment;
    
    // Llamada al backend para añadir a la cola
    this.http.post(`${API_URL}/queue/add`, song).subscribe({
      next: () => {
        alert(`¡Pago aceptado! "${song.title}" añadida a la cola.`);
        this.selectedSongForPayment = null; // Cerrar modal
        // Opcional: Redirigir a la vista de cola
        // this.router.navigate(['/queue']);
      },
      error: (err) => {
        console.error('Error añadiendo a la cola:', err);
        const msg = err.error?.message || 'Error al procesar la solicitud.';
        alert(msg);
      }
    });
  }
}