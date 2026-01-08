import { Component, OnInit, OnDestroy, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SongService } from '../song.service';
import { API_URL } from '../api.config';
import { Subscription } from 'rxjs';

interface QueuedSong {
  id?: number;
  songId: string;
  title: string;
  artist: string;
  albumCover: string;
  previewUrl: string;
  duration?: number;
  position?: number;
  timestamp?: number;
}

@Component({
  selector: 'app-queue',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './queue.component.html',
  styleUrls: ['./queue.component.css']
})
export class QueueComponent implements OnInit, OnDestroy, AfterViewInit {
  @ViewChild('audioPlayer') audioPlayer!: ElementRef<HTMLAudioElement>;

  nowPlaying: QueuedSong | null = null;
  upNext: QueuedSong[] = []; 
  
  private fetchInterval: any;
  private queueChangedSub?: Subscription;
  
  remainingSeconds: number | null = null;
  audioStatus: string | null = 'Sincronizando...';
  
  // CERROJO DE SEGURIDAD: Evita que el polling reinicie la canción durante un salto
  private isTransitioning = false;

  constructor(private songService: SongService) { }

  ngOnInit(): void {
    // 1. Polling cada 5 segundos para refrescar la lista visual
    this.fetchInterval = setInterval(() => this.fetchQueue(), 5000);
    
    // 2. Escuchar cambios manuales (ej: nuevas canciones pagadas)
    this.queueChangedSub = this.songService.queueChanged$.subscribe(() => {
      this.fetchQueue();
    });
    
    // 3. Desbloqueo inicial por interacción del usuario
    document.addEventListener('click', () => {
      if (this.audioPlayer && this.audioPlayer.nativeElement.paused && this.nowPlaying) {
        this.audioPlayer.nativeElement.play().catch(() => {});
      }
    }, { once: true });
  }

  ngAfterViewInit(): void {
    if (this.audioPlayer) {
      const audio = this.audioPlayer.nativeElement;
      
      // Evento: La canción terminó de sonar
      audio.addEventListener('ended', () => {
        this.handleTrackEnd();
      });

      // Evento: Error de carga (URL de Deezer caducada o fallo de red)
      audio.addEventListener('error', () => {
        console.error('Error detectado en la reproducción, saltando...');
        setTimeout(() => this.handleTrackEnd(), 1000);
      });
      
      // Actualización del segundero
      audio.addEventListener('timeupdate', () => {
        if(audio.duration) {
           this.remainingSeconds = Math.ceil(audio.duration - audio.currentTime);
        }
      });
    }
    this.fetchQueue();
  }

  ngOnDestroy(): void {
    if (this.fetchInterval) clearInterval(this.fetchInterval);
    if (this.queueChangedSub) this.queueChangedSub.unsubscribe();
  }

  /**
   * Obtiene la cola del servidor y actualiza la UI
   */
  fetchQueue() {
  // Si estamos cambiando de canción (isTransitioning), ignoramos lo que diga el servidor
  if (this.isTransitioning) return;

  this.songService.getQueue().subscribe({
    next: (data: QueuedSong[]) => {
      if (data && data.length > 0) {
        const incoming = data[0];

        // Solo cambiamos el audio si el ID es realmente distinto
        if (!this.nowPlaying || this.nowPlaying.songId !== incoming.songId) {
          this.nowPlaying = { ...incoming, timestamp: Date.now() };
          this.upNext = data.slice(1).map(song => ({ ...song, timestamp: Date.now() }));
          this.resolveAndPlay(this.nowPlaying);
        } else {
          // Si es la misma canción, solo actualizamos la lista visual de "Próximas"
          this.upNext = data.slice(1).map(song => ({ ...song, timestamp: Date.now() }));
        }
      }
    }
  });
}

  /**
   * Carga el archivo de audio en el reproductor
   */
  resolveAndPlay(song: QueuedSong) {
    if (!this.audioPlayer) return;

    if (song.previewUrl && song.previewUrl.startsWith('http')) {
        this.setAudioSrcAndPlay(song.previewUrl);
    } else {
        // Si no hay URL, intentamos recuperarla del endpoint de Deezer
        fetch(`${API_URL}/api/deezer/url/${song.songId}`)
            .then(res => res.json())
            .then(data => {
                if (data.url) this.setAudioSrcAndPlay(data.url);
                else throw new Error("URL no disponible");
            })
            .catch(() => this.handleTrackEnd());
    }
  }

  setAudioSrcAndPlay(url: string) {
    const audio = this.audioPlayer.nativeElement;
    
    // Evitar recargas innecesarias si es la misma URL
    if (audio.src === url && !audio.paused) return;

    audio.src = url;
    audio.load();
    audio.play()
      .then(() => this.audioStatus = 'En antena')
      .catch(() => {
        this.audioStatus = 'Audio en espera (Requiere Click)';
        // Fallback: Silenciar para intentar auto-play (política Chrome)
        audio.muted = true;
        audio.play().catch(() => {});
      });
  }

  /**
   * Lógica de transición al finalizar la pista
   */
  private handleTrackEnd() {
    if (this.isTransitioning) return;
    this.isTransitioning = true;
    
    this.audioStatus = 'Cargando siguiente...';

    this.songService.playNext().subscribe({
      next: (nextSong: QueuedSong) => {
        if (nextSong && nextSong.songId) {
            this.nowPlaying = nextSong;
            this.upNext = []; 
            this.resolveAndPlay(nextSong);
            
            // Liberamos el cerrojo tras 2 segundos para dar tiempo al backend a estabilizarse
            setTimeout(() => {
                this.isTransitioning = false;
                this.fetchQueue();
            }, 2000);
        } else {
            this.nowPlaying = null;
            this.audioStatus = 'Fin de la lista';
            this.isTransitioning = false;
        }
      },
      error: (err) => {
        console.error("Fallo en la transición de canción:", err);
        this.isTransitioning = false;
      }
    });
  }

  formatTime(seconds: number | null): string {
    if (seconds === null || isNaN(seconds)) return '0:00';
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  }

  getTimestamp(): number {
    return Date.now();
  }
}