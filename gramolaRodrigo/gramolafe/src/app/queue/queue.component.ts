import { Component, OnInit, OnDestroy, AfterViewInit, ViewChild, ElementRef } from '@angular/core'; // Añadir imports
import { CommonModule } from '@angular/common';
import { SongService } from '../song.service';
import { API_URL } from '../api.config';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-queue',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './queue.component.html',
  styleUrls: ['./queue.component.css']
})
export class QueueComponent implements OnInit, OnDestroy, AfterViewInit {
  // Obtenemos una referencia a nuestro elemento <audio> del HTML
  @ViewChild('audioPlayer') audioPlayer!: ElementRef<HTMLAudioElement>;

  nowPlaying: any = null;
  upNext: any[] = [];
  private songEndTimer: any;
  private fetchInterval: any;
  private queueChangedSub?: Subscription;
  // Si el navegador bloquea el autoplay, pediremos una interacción del usuario
  showEnableAudio = false;
  // Evitar dobles transiciones al finalizar pista (onended + timer)
  private endHandled = false;
  // Ya no usamos listeners globales; sólo el botón del overlay
  // Estado interno: fallback timer
  remainingSeconds: number | null = null;
  audioStatus: string | null = null;
  private countdownInterval: any;
  // Exponer API_URL al template
  API_URL = API_URL;
  isPaused = false;
  // True once a call to audio.play() has succeeded (muted or not) in this session — allows subsequent tracks to autoplay
  private autoplayAllowed = false;
  // True if the user has interacted and we unmuted the audio
  private userUnmuted = false;

  constructor(private songService: SongService) { }

  // Nuevo método: consulta el estado global al iniciar
  private fetchPlaybackState() {
    // removed global playback state; always attempt to play when there's a song
  }

  togglePlayback() {
    // intentionally no-op: global toggle removed
  }

  private applyGlobalPause() {
    // removed
  }

  ngOnInit(): void {
    // Esperamos a que la vista esté lista para manipular el elemento audio en ngAfterViewInit
    this.fetchInterval = setInterval(() => this.fetchQueue(), 10000);
    // Suscribir a cambios de cola para refrescar inmediatamente
    this.queueChangedSub = this.songService.queueChanged$.subscribe(() => {
      this.fetchQueue();
    });
    // removed global state and persistence; always play current song if available
  }

  ngAfterViewInit(): void {
    // Cuando el audio se carga en la vista, nos aseguramos de escuchar 'ended' para encadenar
    if (this.audioPlayer) {
      const audio: HTMLAudioElement = this.audioPlayer.nativeElement;
      audio.addEventListener('ended', () => {
        // Cuando el preview termina (aprox 30s), pedimos al backend la siguiente canción
        this.handleTrackEnd();
      });
    }
    // Carga inicial de la cola
    this.fetchQueue();
    // no persistence hooks

    // Escuchar evento global para reanudar audio (útil después de flujos de pago que consumen el gesto de usuario)
    window.addEventListener('app:resume-audio', this.onGlobalResume);
  }

  ngOnDestroy(): void {
    if (this.songEndTimer) clearTimeout(this.songEndTimer);
    if (this.fetchInterval) clearInterval(this.fetchInterval);
    if (this.countdownInterval) clearInterval(this.countdownInterval);
    if (this.queueChangedSub) this.queueChangedSub.unsubscribe();
    // nada adicional
    window.removeEventListener('app:resume-audio', this.onGlobalResume);
  }

  // Handler para reanudar/desmutear audio cuando otra parte de la UI lo solicita
  private onGlobalResume = () => {
    try {
      if (!this.audioPlayer || !this.nowPlaying) return;
      const audio: HTMLAudioElement = this.audioPlayer.nativeElement;
      this.userUnmuted = true;
      audio.muted = false;
      audio.volume = 1.0;
      // intentar reproducir inmediatamente
      audio.play().catch(() => {});
      this.audioStatus = 'Reproduciendo...';
    } catch (e) {
      // ignore
    }
  };

  fetchQueue() {
    this.songService.getQueue().subscribe({
      next: (data) => {
        const currentSongKey = this.nowPlaying ? this.nowPlaying.songId : null;

        if (data.length > 0) {
          const nextNowPlaying = data[0];
          const nextKey = nextNowPlaying?.songId ?? null;
          // Actualizamos lista siguiente siempre
          this.upNext = data.slice(1);
          // Si está sonando la misma canción, NO reiniciamos reproducción
          if (currentSongKey === nextKey) {
            // Mantener la pista actual y sólo refrescar "up next"
            return;
          }
          // Cambió la canción: asignamos y reproducimos
          this.nowPlaying = nextNowPlaying;
          // Cambió la canción: asignamos y reproducimos (siempre intentamos reproducir)
          this.playCurrentSong();
        } else {
          this.nowPlaying = null;
          this.upNext = [];
          if (this.audioPlayer) this.audioPlayer.nativeElement.pause(); // Si la cola se vacía, paramos la música
        }
      },
      error: (err) => console.error('Error al obtener la cola', err)
    });
  }

  playCurrentSong() {
    // Nos aseguramos de que el reproductor y la URL existen
    if (this.audioPlayer && this.nowPlaying) {
      const audio: HTMLAudioElement = this.audioPlayer.nativeElement;
  // No pausar explícitamente aquí: cambiar el src y llamar a play() suele ser suficiente.
      // Reset estado
      this.audioStatus = 'Cargando preview...';
      this.remainingSeconds = null;
      if (this.countdownInterval) clearInterval(this.countdownInterval);
      if (this.songEndTimer) clearTimeout(this.songEndTimer);
      // Preferimos usar el proxy del backend para evitar problemas CORS
      if (this.nowPlaying.songId) {
        audio.src = `${API_URL}/api/deezer/preview/${this.nowPlaying.songId}`;
      } else if (this.nowPlaying.previewUrl) {
        audio.src = this.nowPlaying.previewUrl;
      } else {
        console.warn('No hay preview disponible para la canción', this.nowPlaying);
        this.audioStatus = 'No hay preview disponible para esta canción';
        return;
      }
      this.endHandled = false;
      // Asegurar configuración del elemento
  audio.crossOrigin = 'anonymous';
  // Intentar autoplay muted: muchos navegadores permiten autoplay si está silenciado.
  audio.autoplay = true;
  // Si el usuario ya interactuó y desmutó, mantener sonido; si no, arrancar silenciado
  audio.muted = !this.userUnmuted;
  audio.volume = 1.0;

  // Listeners (reemplazan anteriores)
      // El preview de Deezer dura ~30s; capamos la duración efectiva a 30s
      const duration = Math.min(this.nowPlaying.duration || 30, 30);
      audio.onloadedmetadata = () => {
        this.audioStatus = 'Listo para reproducir';
        // Inicializamos contador al valor total (30s por defecto)
        this.remainingSeconds = duration;
        // always attempt to start from the beginning of the preview; removed restore-from-db logic
      };
      audio.oncanplay = () => {
        this.audioStatus = 'Reproduciendo...';
      };
      audio.onplay = () => {
        this.audioStatus = 'Reproduciendo...';
      };
      audio.onpause = () => {
        if (!audio.ended) {
          this.audioStatus = 'Pausado';
        }
      };
      audio.onerror = (ev: any) => {
        console.error('Error del elemento audio', ev);
        // Si falla el proxy, probamos con la URL directa de Deezer
        const usingProxy = !!audio.src && audio.src.includes('/api/deezer/preview/');
        if (usingProxy && this.nowPlaying?.previewUrl) {
          // Call diagnostic endpoint to see why proxy failed
          fetch(`${API_URL}/api/deezer/check/${this.nowPlaying.songId}`).then(r=>r.json()).then(d=>console.log('Deezer check', d)).catch(e=>console.warn('Deezer check failed', e));
          console.warn('Fallo en proxy, probando previewUrl directo de Deezer');
          audio.src = this.nowPlaying.previewUrl;
          audio.load();
          audio.play()
            .then(() => {
              this.audioStatus = 'Reproduciendo...';
              const effectiveDuration = Math.min(this.nowPlaying?.duration || 30, 30);
              this.startSongTimer(effectiveDuration);
            })
            .catch((e2) => {
              console.error('También falló la URL directa del preview', e2);
              this.audioStatus = 'Error al reproducir audio';
              this.requestUserInteractionToPlay();
            });
        } else {
          this.audioStatus = 'Error al reproducir audio';
        }
      };
      audio.onstalled = () => {
        this.audioStatus = 'Red lenta o recurso no disponible (stalled)';
      };
      audio.ontimeupdate = () => {
        const left = Math.ceil(duration - (audio.currentTime || 0));
        this.remainingSeconds = left > 0 ? left : 0;
      };
      audio.onended = () => {
        this.audioStatus = 'Finalizado';
        this.handleTrackEnd();
      };

      audio.load();
      // Intentamos reproducir. Si en esta sesión ya se permitió autoplay, esto debería resolverse.
      audio.play().then(() => {
        this.autoplayAllowed = true;
        const left = Math.ceil(Math.min(duration, 30) - (audio.currentTime || 0));
        console.log(`Reproduciendo: ${this.nowPlaying.title} (quedan ${left}s)`);
        if (left > 0) this.startSongTimer(left);
      }).catch((err) => {
        // Si no se permite play incluso en muted, intentar forzar muted autoplay
        console.warn('play() rechazado, intentando muted autoplay', err);
        try {
          audio.muted = true;
          audio.play().then(() => {
            this.autoplayAllowed = true;
            const left = Math.ceil(Math.min(duration, 30) - (audio.currentTime || 0));
            if (left > 0) this.startSongTimer(left);
          }).catch((err2) => {
            console.warn('Muted autoplay también falló', err2);
            this.audioStatus = 'Pulsa en la página para iniciar el audio';
          });
        } catch (e) {
          this.audioStatus = 'Pulsa en la página para iniciar el audio';
        }
      });

      // Registrar handler de interacción por una sola vez para desmutear y asegurar reproducción con sonido
      const resumeOnInteraction = () => {
        try {
          if (!audio) return;
          audio.muted = false;
          this.userUnmuted = true;
          audio.volume = 1.0;
          audio.play().catch(() => {});
          this.audioStatus = 'Reproduciendo...';
        } catch (e) {
          // ignore
        }
      };
      document.addEventListener('click', resumeOnInteraction, { once: true });
      document.addEventListener('touchstart', resumeOnInteraction, { once: true });
    }
  }

  startSongTimer(durationInSeconds: number) {
    if (this.songEndTimer) clearTimeout(this.songEndTimer);

    const durationInMs = durationInSeconds * 1000;
    console.log(`La canción ${this.nowPlaying.title} terminará en ${durationInSeconds} segundos.`);

    this.songEndTimer = setTimeout(() => {
      console.log(`La canción ${this.nowPlaying.title} ha terminado.`);
      this.handleTrackEnd();
    }, durationInMs);
  }

  private handleTrackEnd() {
    if (this.endHandled) return;
    this.endHandled = true;
    if (this.songEndTimer) clearTimeout(this.songEndTimer);
    if (this.countdownInterval) clearInterval(this.countdownInterval);
    this.playNextSong();
  }

  playNextSong() {
    this.songService.playNext().subscribe({
      next: () => {
        this.fetchQueue();
      },
      error: (err) => console.error('Error al pasar a la siguiente canción', err)
    });
  }

  private requestUserInteractionToPlay() {
    // Solicitaste quitar el aviso, así que no mostramos overlay.
    this.showEnableAudio = false;
  }

  private removeUserInteractionHandlers() {}

  // Método público para el botón del overlay: intenta reproducir y oculta el overlay
  enableAudio() {
    if (this.audioPlayer) {
      const audio = this.audioPlayer.nativeElement;
      // Técnica: primero reproducir silenciado (permitido), luego activar volumen
      const previousMuted = audio.muted;
      const previousVolume = audio.volume;
      audio.muted = true;
      audio.volume = 0.0;
      audio.play()
        .then(() => {
          // Ya hay una reproducción iniciada; ahora activamos sonido
          setTimeout(() => {
            audio.muted = false;
            audio.volume = 1.0;
            // Si por alguna razón queda pausado, forzar play de nuevo
            if (audio.paused) {
              audio.play().catch(() => {});
            }
            this.audioStatus = 'Reproduciendo...';
            this.showEnableAudio = false;
            // Iniciar timer si aún no está, calculando el tiempo restante
            if (!this.songEndTimer) {
              const effectiveDuration = Math.min((this.nowPlaying?.duration) || 30, 30);
              const left = Math.ceil(effectiveDuration - (audio.currentTime || 0));
              if (left > 0) this.startSongTimer(left);
            }
          }, 100);
        })
        .catch(err => {
          console.error('No se pudo iniciar el audio tras interacción (muted shim)', err);
          // Si el motivo es falta de fuente (NotSupportedError), intentamos fallback directo
          const usingProxy = !!audio.src && audio.src.includes('/api/deezer/preview/');
          if (usingProxy && this.nowPlaying?.previewUrl) {
            console.warn('Intentando fallback a previewUrl directo tras interacción');
            audio.src = this.nowPlaying.previewUrl;
            audio.load();
            audio.play().then(() => {
              audio.muted = false;
              audio.volume = 1.0;
              this.audioStatus = 'Reproduciendo...';
              this.showEnableAudio = false;
              if (!this.songEndTimer) {
                const effectiveDuration = Math.min((this.nowPlaying?.duration) || 30, 30);
                const left = Math.ceil(effectiveDuration - (audio.currentTime || 0));
                if (left > 0) this.startSongTimer(left);
              }
            }).catch((e2) => {
              console.error('Fallback directo también falló', e2);
              // Restaurar estados y mantener overlay
              audio.muted = previousMuted;
              audio.volume = previousVolume;
              this.audioStatus = 'No se pudo iniciar el audio automáticamente.';
              this.showEnableAudio = false;
            });
          } else {
            // Restaurar estados y mantener overlay
            audio.muted = previousMuted;
            audio.volume = previousVolume;
            this.audioStatus = 'No se pudo iniciar el audio automáticamente.';
            this.showEnableAudio = false;
          }
        });
    }
  }

  formatTime(totalSeconds: number | null): string {
    if (totalSeconds === null || totalSeconds < 0) return '';
    const m = Math.floor(totalSeconds / 60);
    const s = totalSeconds % 60;
    const mm = m.toString().padStart(2, '0');
    const ss = s.toString().padStart(2, '0');
    return `${mm}:${ss}`;
  }
}