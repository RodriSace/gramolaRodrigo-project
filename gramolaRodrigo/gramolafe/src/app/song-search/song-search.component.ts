import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SongService } from '../song.service';
import { PaymentsService } from '../payments.service';
import { HttpClient } from '@angular/common/http';
import { API_URL } from '../api.config';
import { PaymentFormComponent } from '../payment-form/payment-form.component';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';

declare var Stripe: any;

@Component({
  selector: 'app-song-search',
  standalone: true,
  imports: [CommonModule, FormsModule, PaymentFormComponent],
  templateUrl: './song-search.component.html',
  styleUrl: './song-search.component.css'
})
export class SongSearchComponent implements OnInit {
  searchTerm: string = '';
  results: any;
  clientSecret: string | null = null;
  stripe: any;
  message: string | null = null;
  payMessage: string | null = null;
  priceInCents: number | null = null;
  

  isSubscribed = false;

  constructor(private songService: SongService, private paymentsService: PaymentsService, private http: HttpClient, private auth: AuthService, private router: Router) { }

  ngOnInit(): void {
    this.stripe = Stripe('pk_test_51SGRYtLYhxLLPwjrdBNI8h9ON82IcZ3vgt4lRa1rx9U9vMEyC9cLjeLvKB3IUHzzfUvoR55YvXHGNKhlrCIO5A7P00cvJSZ47X'); // RECUERDA PONER TU CLAVE
    this.checkStatus();
    this.http.get<{amountInCents:number}>(`${API_URL}/plans/song-price`).subscribe({
      next: d => this.priceInCents = d.amountInCents,
      error: () => this.priceInCents = null
    });
    const bar = this.auth.currentUserValue;
    if (bar?.id) {
      this.http.get<any>(`${API_URL}/bars/subscription-status`, { params: { barId: bar.id } }).subscribe(s => {
        this.isSubscribed = !!s.active;
      });
    }
  }

  buscarCanciones() {
    if (!this.searchTerm.trim()) return;
    this.clientSecret = null;
    this.message = null;
    this.results = null;

    this.songService.search(this.searchTerm).subscribe({
      next: (data) => {
        this.results = data;
      },
      error: (err) => {
        console.error('Error al buscar canciones', err);
      }
    });
  }

  async checkStatus() {
    const clientSecret = new URLSearchParams(window.location.search).get('payment_intent_client_secret');
    if (!clientSecret) { return; }

    const { paymentIntent } = await this.stripe.retrievePaymentIntent(clientSecret);
    switch (paymentIntent.status) {
      case 'succeeded':
        this.message = '¡Pago completado! Añadiendo canción a la cola...';

        // RECUPERAMOS la canción que guardamos antes de la redirección
        const selectedSong = JSON.parse(sessionStorage.getItem('selectedSong')!);

        if (selectedSong) {
          this.paymentsService.confirmPayment(paymentIntent.id, selectedSong).subscribe({
            next: (response) => {
              this.message = '¡Canción añadida a la cola!';
              sessionStorage.removeItem('selectedSong'); // Limpiamos el almacenamiento
              // Notificamos a otros componentes (ej. QueueComponent) que la cola ha cambiado
              this.songService.notifyQueueChanged();
              // Reanudar audio automáticamente tras completar pago (por si el flujo de pago consumió el gesto de usuario)
              try { window.dispatchEvent(new Event('app:resume-audio')); } catch {}
            },
            error: (err) => {
              console.error('Error al confirmar el pago en el backend', err);
              // Si el backend devuelve un mensaje en err.error.message, muéstralo
              const backendMsg = err?.error?.message || err?.message || JSON.stringify(err);
              this.message = `Error al añadir la canción a la cola: ${backendMsg}`;
            }
          });
        }
        break;
      default:
        this.message = 'El pago no se ha completado.';
        break;
    }
  }

  selectSong(song: any) {
    // Permitir pago por canción aunque no haya suscripción activa
    this.clientSecret = null;
    this.message = null;
    // GUARDAMOS la canción en el sessionStorage antes de iniciar el pago
    sessionStorage.setItem('selectedSong', JSON.stringify(song));

    this.paymentsService.createPaymentIntent(song.id).subscribe({
      next: (response) => {
        this.clientSecret = response.clientSecret;
      },
      error: (err) => {
        console.error('Error al iniciar el pago', err);
        this.message = 'Hubo un error al iniciar el pago.';
      }
    });
  }

  // Nuevo flujo sin recargar: manejamos éxito desde el hijo
  onPaymentSuccess(paymentIntentId: string) {
    const selectedSong = JSON.parse(sessionStorage.getItem('selectedSong')!);
    if (!selectedSong) {
      this.payMessage = 'No se encontró la canción seleccionada para confirmar el pago.';
      return;
    }
    this.paymentsService.confirmPayment(paymentIntentId, selectedSong).subscribe({
      next: () => {
        this.payMessage = '¡Canción añadida a la cola!';
        sessionStorage.removeItem('selectedSong');
        this.songService.notifyQueueChanged();
        // Opcional: limpiar el cliente de pago para próximos pagos
        this.clientSecret = null;
        // Reanudar audio automáticamente tras completar pago en el flujo embebido
        try { window.dispatchEvent(new Event('app:resume-audio')); } catch {}
      },
      error: (err) => {
        if (err?.status === 402) {
          this.payMessage = 'Necesitas una suscripción activa para añadir canciones.';
        } else {
          const backendMsg = err?.error?.message || err?.message || JSON.stringify(err);
          this.payMessage = `Error al añadir la canción a la cola: ${backendMsg}`;
        }
      }
    });
  }
}