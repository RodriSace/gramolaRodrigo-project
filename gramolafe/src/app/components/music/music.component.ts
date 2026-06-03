import { Component, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MusicService } from '../../services/music.service';
import { QueueService } from '../../services/queue.service';
import { SpotiService } from '../../services/spoti.service';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http';

// Declaración global de la variable global de Spotify cargada de forma asíncrona desde su script web oficial
declare var Spotify: any;

/**
 * Componente principal de reproducción, control y cola de reproducción de música.
 */
@Component({
  selector: 'app-music',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './music.component.html',
  styleUrls: ['./music.component.css']
})
export class MusicComponent implements OnInit, OnDestroy {
  searchQuery: string = '';
  searchResults: any[] = [];
  queue: any[] = [];         // Cola COMPLETA del backend (pagadas + playlist)
  barId: number = 0;
  spotifyConnected: boolean = false;
  songPrice: number = 1.00;
  isPaused: boolean = true;

  private sdkPlayer: any = null;
  private sdkDeviceId: string | null = null;
  private sdkReady = false;
  private queueDataReady = false;

  // DISPOSITIVOS
  devices: any[] = [];
  currentDevice: any;
  deviceError: string = '';

  private searchSubject = new Subject<string>();
  private queueSub?: Subscription;

  // REPRODUCTOR
  playlists: any[] = [];
  activePlaylistUri: string | null = null;
  private currentPlayingId: string | null = null;
  currentTrackPlaying: any = null;
  currentProgress: number = 0;
  currentDuration: number = 0;
  private progressInterval: any;

  // COLA VISUAL: Separamos pagadas y de fondo para el HTML
  localPaidQueue: any[] = [];
  nextTracks: any[] = [];

  constructor(
    private musicService: MusicService,
    private queueService: QueueService,
    private spoti: SpotiService,
    private router: Router,
    private http: HttpClient,
    private cdr: ChangeDetectorRef
  ) {
    this.searchSubject.pipe(
      debounceTime(500),
      distinctUntilChanged()
    ).subscribe(query => {
      this.executeSearch(query);
    });
  }

  // ========================================================
  // 🎵 FLUJO 5: EL BARMAN PONE MÚSICA DE FONDO — PASO 1
  // ngOnInit() y Métodos de Preparación Automática (Tokens, Dispositivos, Playlists y SDK)
  // ========================================================

  // Inicialización del componente: Carga datos de sesión, configura Spotify y arranca el polling de la cola
  ngOnInit() {
    const userData = localStorage.getItem('user'); // Recupera sesión del localstorage
    if (userData) {
      const user = JSON.parse(userData);
      this.barId = user.id;
      this.spotifyConnected = user.spotifyConnected;
      this.songPrice = (user.songPriceCents || 100) / 100; // Céntimos a Euros
      if (this.spotifyConnected) {
        if (user.currentPlaylistUri) {
          this.activePlaylistUri = user.currentPlaylistUri;
        }

        // Renueva el token de Spotify y carga la configuración
        this.refreshSpotifyToken(() => {
          this.getDevices();
          this.getPlaylists();
          this.initSpotifySDK();
        });
      }
    } else {
      this.router.navigate(['/login']);
      return;
    }

    // Polling reactivo para sincronizar la cola de reproducción
    this.queueSub = this.queueService.getQueuePolling(this.barId).subscribe(data => {
      const oldQueueLength = this.queue.length;
      const oldFirstId = this.queue.length > 0 ? this.queue[0].spotifyTrackId : null;
      this.queue = data;

      const activeId = this.currentPlayingId || (data.length > 0 ? data[0].spotifyTrackId : null);

      // Divide la cola en canciones pagadas y canciones de fondo para su renderizado
      this.localPaidQueue = data
        .filter((q: any) => q.paid === true)
        .map((q: any) => this.queueItemToVisual(q))
        .filter((q: any) => q.id !== activeId);

      this.nextTracks = data
        .filter((q: any) => !q.paid)
        .map((q: any) => this.queueItemToVisual(q))
        .filter((q: any) => q.id !== activeId);

      const newFirstId = data.length > 0 ? data[0].spotifyTrackId : null;

      // Control de arranque y transición automática de reproducción
      if (oldQueueLength === 0 && data.length > 0 && this.sdkDeviceId) {
        this.playQueuePosition0();
      }
      else if (oldFirstId && newFirstId && oldFirstId !== newFirstId && this.sdkDeviceId) {
        this.playQueuePosition0();
      }

      if (!this.queueDataReady) {
        this.queueDataReady = true;
        this.tryResumePlayback();
      }

      this.cdr.detectChanges();
    });

    // Escucha mensajes de la ventana emergente de Stripe para notificar el pago exitoso
    window.addEventListener('message', this.handlePaymentMessage);
  }

  private isRefreshingToken = false;

  refreshSpotifyToken(callback?: () => void) {
    if (this.isRefreshingToken) {
      if (callback) {
        setTimeout(() => callback(), 1000);
      }
      return;
    }
    this.isRefreshingToken = true;

    const userData = localStorage.getItem('user');
    if (!userData) {
      this.router.navigate(['/login']);
      this.isRefreshingToken = false;
      return;
    }

    const user = JSON.parse(userData);
    console.log("Refrescando token de Spotify para el bar:", user.email);

    this.spoti.refreshToken(user.email).subscribe({
      next: (data: any) => {
        console.log("Token de Spotify refrescado con éxito!");
        this.spoti.spotiToken = data.access_token;
        sessionStorage.setItem('spotiToken', data.access_token);
        localStorage.setItem('spotiToken', data.access_token);

        user.spotifyAccessToken = data.access_token;
        user.spotifyConnected = true;
        localStorage.setItem('user', JSON.stringify(user));
        this.spotifyConnected = true;

        this.isRefreshingToken = false;

        if (callback) {
          callback();
        } else {
          this.getDevices();
          this.getPlaylists();
          this.initSpotifySDK();
        }
      },
      error: (err) => {
        console.error("Error al refrescar token de Spotify automáticamente:", err);
        this.isRefreshingToken = false;
        this.spoti.redirectToSpotify();
      }
    });
  }

  // ========================================================
  // DISPOSITIVOS Y PLAYLISTS
  // ========================================================
  getDevices() {
    this.deviceError = '';
    this.spoti.getDevices().subscribe({
      next: (result) => {
        this.devices = result.devices;
        this.currentDevice = this.devices.find((d: any) => d.is_active);
      },
      error: (err) => {
        this.deviceError = "Error al cargar dispositivos: " + err.message;
        if (err.status === 401) {
          this.refreshSpotifyToken(() => this.getDevices());
        }
      }
    });
  }

  getPlaylists() {
    this.spoti.getPlaylists().subscribe({
      next: (res) => {
        this.playlists = res.items || [];
      },
      error: (err) => {
        console.error("Error al cargar playlists:", err);
        if (err.status === 401) {
          this.refreshSpotifyToken(() => this.getPlaylists());
        }
      }
    });
  }

  // ========================================================
  // SPOTIFY SDK
  // ========================================================
  initSpotifySDK() {
    const token = this.spoti.spotiToken || sessionStorage.getItem('spotiToken') || localStorage.getItem('spotiToken');
    if (!token) {
      console.error("No se ha encontrado el token de Spotify. Intentando refrescar...");
      this.refreshSpotifyToken();
      return;
    }

    // Si ya existe un reproductor global en el servicio singleton, lo reutilizamos
    if (this.spoti.sdkPlayer) {
      console.log("Reutilizando reproductor global de Spotify existente.");
      this.sdkPlayer = this.spoti.sdkPlayer;
      this.sdkDeviceId = this.spoti.sdkDeviceId;
      this.sdkReady = this.spoti.sdkReady;

      // Limpiar listeners viejos para evitar llamadas a componentes destruidos
      this.sdkPlayer.removeListener('initialization_error');
      this.sdkPlayer.removeListener('authentication_error');
      this.sdkPlayer.removeListener('account_error');
      this.sdkPlayer.removeListener('playback_error');
      this.sdkPlayer.removeListener('player_state_changed');
      this.sdkPlayer.removeListener('ready');
      this.sdkPlayer.removeListener('not_ready');

      // Registrar nuevos listeners vinculados a esta instancia
      this.registerPlayerListeners();

      this.queueDataReady = true;
      this.tryResumePlayback();
      return;
    }

    const setupPlayer = () => {
      this.sdkPlayer = new Spotify.Player({
        name: 'Gramola Player',
        getOAuthToken: (cb: any) => {
          const activeToken = this.spoti.spotiToken || sessionStorage.getItem('spotiToken') || localStorage.getItem('spotiToken');
          cb(activeToken);
        },
        volume: 0.8
      });

      // Guardar en el servicio global
      this.spoti.sdkPlayer = this.sdkPlayer;

      // Activar audio con primer clic
      document.body.addEventListener('click', () => {
        if (this.sdkPlayer) {
          this.sdkPlayer.activateElement().catch((e: any) => { });
        }
      }, { once: true });

      this.registerPlayerListeners();

      this.sdkPlayer.connect();
    };

    if ((window as any).Spotify) {
      setupPlayer();
    } else {
      (window as any).onSpotifyWebPlaybackSDKReady = setupPlayer;
      if (!document.getElementById('spotify-sdk-script')) {
        const script = document.createElement('script');
        script.id = 'spotify-sdk-script';
        script.src = 'https://sdk.scdn.co/spotify-player.js';
        script.async = true;
        document.body.appendChild(script);
      }
    }
  }

  // ========================================================
  // 🔄 FLUJO 6: CUANDO UNA CANCIÓN TERMINA — PASO 1 (Detección en Listeners)
  // registerPlayerListeners() y Detección de Fin de Canción (Métodos A, B y C)
  // ========================================================
  private registerPlayerListeners() {
    if (!this.sdkPlayer) return;

    // Errores
    this.sdkPlayer.addListener('initialization_error', ({ message }: any) => { console.error(message); });
    this.sdkPlayer.addListener('authentication_error', ({ message }: any) => {
      console.error("Token de Spotify expirado en el SDK:", message);
      this.refreshSpotifyToken();
    });
    this.sdkPlayer.addListener('account_error', ({ message }: any) => { console.error(message); });
    this.sdkPlayer.addListener('playback_error', ({ message }: any) => { console.error(message); });

    // Estado de reproducción
    let sessionActive = false;

    this.sdkPlayer.addListener('player_state_changed', (state: any) => {
      if (!state || !state.track_window || !state.track_window.current_track) {
        this.currentTrackPlaying = null;
        this.isPaused = true;
        return;
      }

      if (state.paused && !sessionActive) return;
      sessionActive = true;

      const currentTrack = state.track_window.current_track;
      this.currentTrackPlaying = currentTrack;
      this.currentProgress = state.position;
      this.currentDuration = state.duration;
      this.isPaused = state.paused;

      const previousPlayingId = this.currentPlayingId;
      this.currentPlayingId = currentTrack.id;

      // DETECCIÓN DE FIN DE CANCIÓN:
      // Método A: Spotify cambió de track automáticamente
      if (this.queue.length > 0 &&
        previousPlayingId &&
        previousPlayingId === this.queue[0].spotifyTrackId &&
        currentTrack.id !== previousPlayingId) {
        console.log("[FIN] Canción terminada (cambio de track). Anterior:", previousPlayingId, "→ Nueva:", currentTrack.id);
        this.finishCurrentSong();
      }
      // Método B: La canción llegó al final y se pausó
      else if (state.paused &&
        state.duration > 0 &&
        state.position >= state.duration - 1500 &&
        this.queue.length > 0 &&
        currentTrack.id === this.queue[0].spotifyTrackId) {
        console.log("[FIN] Canción terminada (fin de pista). Track:", currentTrack.name, "Pos:", state.position, "/", state.duration);
        this.finishCurrentSong();
      }

      // Barra de progreso + MÉTODO C de detección de fin
      clearInterval(this.progressInterval);
      if (!state.paused) {
        this.progressInterval = setInterval(() => {
          this.currentProgress += 1000;
          if (this.currentProgress > this.currentDuration) {
            this.currentProgress = this.currentDuration;
          }
          this.cdr.detectChanges();

          // MÉTODO C: El timer detecta que la canción llegó al final
          if (this.currentDuration > 0 &&
            this.currentProgress >= this.currentDuration - 2000 &&
            this.queue.length > 0 &&
            this.currentPlayingId === this.queue[0].spotifyTrackId) {
            console.log("[FIN-C] Timer detectó fin de canción. Progress:", this.currentProgress, "/", this.currentDuration);
            clearInterval(this.progressInterval);
            this.finishCurrentSong();
          }
        }, 1000);
      }

      this.cdr.detectChanges();
    });

    // Reproductor listo
    this.sdkPlayer.addListener('ready', ({ device_id }: any) => {
      console.log('Gramola Player listo con ID:', device_id);
      this.sdkDeviceId = device_id;
      this.spoti.sdkDeviceId = device_id;
      this.getDevices();

      // Enviamos orden a Spotify de transferir el reproductor activo a nuestro dispositivo
      const activeToken = this.spoti.spotiToken || sessionStorage.getItem('spotiToken') || localStorage.getItem('spotiToken');
      this.http.put(`https://api.spotify.com/v1/me/player`, { device_ids: [device_id], play: false }, {
        headers: { 'Authorization': `Bearer ${activeToken}` }
      }).subscribe({
        next: () => {
          console.log("Sesión transferida a la Gramola con éxito!");
          this.sdkReady = true;
          this.spoti.sdkReady = true;
          this.tryResumePlayback();
        },
        error: (err) => {
          console.error("Error al transferir sesión:", err);
        }
      });
    });

    this.sdkPlayer.addListener('not_ready', ({ device_id }: any) => {
      this.sdkDeviceId = null;
      this.spoti.sdkDeviceId = null;
    });
  }

  // ========================================================
  // 🎵 FLUJO 5: EL BARMAN PONE MÚSICA DE FONDO — PASOS 2 y 3
  // selectPlaylist() — Selección de Playlist y Volcado de Canciones en Backend
  // ========================================================

  // ========================================================
  // SELECCIONAR PLAYLIST → Cargar TODAS las canciones en la cola del backend
  // ========================================================
  selectPlaylist(playlist: any) {
    this.activePlaylistUri = playlist.uri;

    // Guardar la URI de la playlist activa en el servidor
    this.http.post('http://127.0.0.1:8080/users/update-playback', {
      userId: this.barId,
      uri: playlist.uri,
      index: 0
    }).subscribe({
      next: () => {
        const userData = localStorage.getItem('user');
        if (userData) {
          const user = JSON.parse(userData);
          user.currentPlaylistUri = playlist.uri;
          localStorage.setItem('user', JSON.stringify(user));
        }
      }
    });

    // Obtener canciones de la playlist desde Spotify
    let playlistUrl = `https://api.spotify.com/v1/playlists/${playlist.id}`;
    this.http.get(playlistUrl, { headers: { 'Authorization': `Bearer ${this.spoti.spotiToken}` } }).subscribe({
      next: (res: any) => {
        console.log("[PLAYLIST] Respuesta Spotify:", res);

        let fetchedItems: any[] = [];
        if (res.items && res.items.items) {
          fetchedItems = res.items.items;
        } else if (res.tracks && res.tracks.items) {
          fetchedItems = res.tracks.items;
        } else if (Array.isArray(res.items)) {
          fetchedItems = res.items;
        } else if (Array.isArray(res.tracks)) {
          fetchedItems = res.tracks;
        }

        console.log("[PLAYLIST] Items de Spotify:", fetchedItems.length);
        if (fetchedItems.length > 0) {
          console.log("[PLAYLIST] Primer item:", fetchedItems[0]);
        }

        // Mapear usando la misma lógica que ANTES funcionaba
        const tracks = fetchedItems
          .filter((item: any) => item !== null)
          .map((item: any) => {
            let t = item.item ? item.item : (item.track ? item.track : item);
            if (t) {
              if (!t.id && t.uri) t.id = t.uri; // Fallback: usar URI como ID
              if (!t.album) t.album = { images: [] };
              if (!t.album.images) t.album.images = [];
              if (t.album.images.length === 0 && t.album.url) {
                t.album.images = [{ url: t.album.url }];
              }
            }
            return t;
          })
          .filter((t: any) => t && (t.uri || t.id)); // Permisivo: basta con uri O id

        console.log("[PLAYLIST] Tracks mapeados:", tracks.length);

        // Mostrar en la UI central
        this.searchResults = [...tracks];

        // ENVIAR TODAS LAS CANCIONES AL BACKEND (cola persistente)
        const tracksForBackend = tracks.map((t: any) => ({
          title: t.name || 'Sin título',
          artist: t.artists?.[0]?.name || 'Desconocido',
          trackId: t.id,
          albumArtUrl: t.album?.images?.[0]?.url || '',
          durationMs: t.duration_ms || 0
        }));

        console.log("[PLAYLIST] Tracks para backend:", tracksForBackend.length);

        this.http.post('http://127.0.0.1:8080/api/music/queue/load-playlist', {
          barId: this.barId,
          tracks: tracksForBackend
        }).subscribe({
          next: (res: any) => {
            console.log("Playlist cargada en la cola del backend:", res);
            // INMEDIATAMENTE leer la cola y reproducir (sin esperar al polling)
            this.queueService.getQueue(this.barId).subscribe((queue: any[]) => {
              console.log("Cola recibida del backend:", queue.length, "canciones");
              this.queue = queue;
              // Actualizar visuales
              this.localPaidQueue = queue
                .filter((q: any) => q.paid === true)
                .map((q: any) => this.queueItemToVisual(q));
              this.nextTracks = queue
                .filter((q: any) => !q.paid)
                .map((q: any) => this.queueItemToVisual(q));
              this.cdr.detectChanges();
              // Reproducir la posición 0
              if (queue.length > 0 && this.sdkDeviceId) {
                this.currentPlayingId = null; // Forzar que suene
                this.playQueuePosition0();
              }
            });
          },
          error: (err) => console.error("Error al cargar playlist en backend:", err)
        });

        this.searchQuery = '';
        if (!playlist.tracks) playlist.tracks = {};
        playlist.tracks.total = this.searchResults.length;
      },
      error: (err) => {
        this.searchResults = [];
      }
    });
  }

  // ========================================================
  // 🎵 FLUJO 5: EL BARMAN PONE MÚSICA DE FONDO — PASO 4 (Y 🔄 FLUJO 6 — PASO 4)
  // playQueuePosition0() y tryResumePlayback() — Coordinador de Reproducción de la Canción 0 de la Cola
  // ========================================================

  // ========================================================
  // COORDINADOR: Arranca la reproducción cuando SDK + Cola están listos
  // ========================================================
  private tryResumePlayback() {
    if (!this.sdkReady || !this.queueDataReady || !this.sdkDeviceId) {
      console.log("[RESUME] Aún no listo. SDK:", this.sdkReady, "Cola:", this.queueDataReady, "Device:", !!this.sdkDeviceId);
      return;
    }

    // Si hay canciones en la cola del backend → dejarlas listas para reproducir (evitando el bloqueo automático de Autoplay del navegador)
    if (this.queue.length > 0) {
      console.log("[RESUME] Cola tiene", this.queue.length, "canciones. Listas en el reproductor esperando interacción del usuario.");
      // No llamamos a playQueuePosition0() automáticamente para evitar el bloqueo de Autoplay del navegador.
      // Quedará en estado pausado ("Listo para reproducir") con displayedTrack = queue[0].
      // En cuanto el usuario pulse el botón Play premium (togglePlay), se activará la reproducción de forma interactiva legítima.
      this.isPaused = true;
    } else {
      console.log("[RESUME] Cola vacía. Esperando a que se seleccione una playlist.");
    }
  }

  // REPRODUCE LA CANCIÓN EN POSICIÓN 0 DE LA COLA DEL BACKEND
  private playQueuePosition0() {
    if (!this.sdkDeviceId || this.queue.length === 0) {
      console.warn("[PLAY] No se puede reproducir. DeviceId:", this.sdkDeviceId, "Queue:", this.queue.length);
      return;
    }

    const song = this.queue[0];
    // No reproducir si ya es la que está sonando
    if (this.currentPlayingId === song.spotifyTrackId) {
      console.log("[PLAY] Ya está sonando:", song.title);
      return;
    }

    console.log("[PLAY] Lanzando playTrack para:", song.title, "| TrackId:", song.spotifyTrackId, "| DeviceId:", this.sdkDeviceId);
    this.spoti.playTrack(song.spotifyTrackId, this.sdkDeviceId).subscribe({
      next: () => {
        console.log("[PLAY] ✅ Reproduciendo:", song.title);
        const headers = { 'Authorization': `Bearer ${this.spoti.spotiToken}` };
        this.http.put(`https://api.spotify.com/v1/me/player/repeat?state=off&device_id=${this.sdkDeviceId}`, null, { headers }).subscribe({ error: () => { } });
        this.http.put(`https://api.spotify.com/v1/me/player/shuffle?state=false&device_id=${this.sdkDeviceId}`, null, { headers }).subscribe({ error: () => { } });
      },
      error: (err) => console.error("[PLAY] ❌ Error reproduciendo:", song.title, err)
    });
  }

  // ========================================================
  // 🔄 FLUJO 6: CUANDO UNA CANCIÓN TERMINA — PASOS 2 Y 4
  // finishCurrentSong() — Eliminar de la Cola y Notificar al Backend para Avanzar
  // ========================================================

  // ========================================================
  // FIN DE CANCIÓN → Notificar al backend
  // ========================================================
  private isFinishing = false; // Evitar doble-ejecución

  private finishCurrentSong() {
    if (this.isFinishing) return; // Debounce
    this.isFinishing = true;

    console.log("[FINISH] Notificando al backend que terminó la canción.");

    // Eliminar localmente para refresco visual instantáneo
    if (this.queue.length > 0) {
      const finished = this.queue.shift();
      console.log("[FINISH] Removida localmente:", finished?.title, "| Quedan:", this.queue.length);
      this.cdr.detectChanges();
    }

    this.http.post('http://127.0.0.1:8080/api/music/queue/finish', { barId: this.barId }).subscribe({
      next: () => {
        console.log("[FINISH] Cola avanzada en el servidor.");
        this.isFinishing = false;

        // Refrescar la cola desde el backend para estar 100% sincronizado
        this.queueService.getQueue(this.barId).subscribe((freshQueue: any[]) => {
          this.queue = freshQueue;
          // Actualizar visuales
          this.localPaidQueue = freshQueue
            .filter((q: any) => q.paid === true)
            .map((q: any) => this.queueItemToVisual(q));
          this.nextTracks = freshQueue
            .filter((q: any) => !q.paid)
            .map((q: any) => this.queueItemToVisual(q));
          this.cdr.detectChanges();

          console.log("[FINISH] Cola refrescada:", freshQueue.length, "canciones.");

          // Reproducir la nueva posición 0 si hay más canciones
          if (freshQueue.length > 0 && this.sdkDeviceId) {
            this.currentPlayingId = null; // Forzar que playQueuePosition0 no se bloquee
            setTimeout(() => this.playQueuePosition0(), 300);
          } else {
            console.log("[FINISH] Cola vacía. La Gramola se detiene.");
            this.currentTrackPlaying = null;
            this.currentPlayingId = null;
            this.cdr.detectChanges();
          }
        });
      },
      error: (err) => {
        console.error("[FINISH] Error:", err);
        this.isFinishing = false;
      }
    });
  }

  // ========================================================
  // 💰 FLUJO 7: UN CLIENTE PAGA PARA COLAR SU CANCIÓN — PASO 1
  // onSearchInput() y executeSearch() — Búsqueda Reactiva con Debounce y RxJS
  // ========================================================

  // ========================================================
  // BÚSQUEDA
  // ========================================================
  onSearchInput() {
    this.searchSubject.next(this.searchQuery);
  }

  executeSearch(query: string) {
    if (!query || query.trim().length < 2) {
      this.searchResults = [];
      return;
    }

    const token = this.spoti.spotiToken;
    if (!token) return;

    this.musicService.search(query, token).subscribe({
      next: (res: any) => {
        if (res && res.tracks && res.tracks.items) {
          this.searchResults = res.tracks.items;
          this.cdr.detectChanges();
        }
      },
      error: (err) => console.error("Error en búsqueda:", err)
    });
  }

  // ========================================================
  // 💰 FLUJO 7: UN CLIENTE PAGA PARA COLAR SU CANCIÓN — PASOS 2 Y 3
  // paySong() — Apertura de Ventana Popup y Creación de Pago Seguro en Stripe
  // ========================================================

  // ========================================================
  // PAGO DE CANCIÓN
  // ========================================================
  paySong(track: any) {
    const width = 550;
    const height = 800;
    const left = (window.screen.width / 2) - (width / 2);
    const top = (window.screen.height / 2) - (height / 2);

    // Abrimos ventana emergente vacía con spinner inmediato para no ser bloqueados por el navegador
    const paymentWindow = window.open('', 'StripePayment', `width=${width},height=${height},left=${left},top=${top}`);
    if (paymentWindow) {
      paymentWindow.document.write(`
        <html>
          <head>
            <title>Conectando con Stripe...</title>
            <style>
              body { background: #0c0b0e; color: #fff; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; display: flex; flex-direction: column; justify-content: center; align-items: center; height: 100vh; margin: 0; }
              .spinner { border: 4px solid rgba(255,255,255,0.05); border-top: 4px solid #1db954; border-radius: 50%; width: 45px; height: 45px; animation: spin 1s linear infinite; margin-bottom: 24px; }
              @keyframes spin { 0% { transform: rotate(0deg); } 100% { transform: rotate(360deg); } }
              h3 { font-size: 1.25rem; font-weight: 600; margin: 0 0 8px 0; color: #ffffff; }
              p { color: #8a898e; font-size: 0.95rem; margin: 0; }
            </style>
          </head>
          <body>
            <div class="spinner"></div>
            <h3>Conectando con Stripe Checkout</h3>
            <p>Por favor, no cierres esta ventana...</p>
          </body>
        </html>
      `);
    }

    const payload = {
      barId: this.barId,
      title: track.name,
      trackId: track.id,
      artist: track.artists[0].name,
      previewUrl: track.preview_url || '',
      albumArtUrl: track.album?.images?.[0]?.url || '',
      durationMs: track.duration_ms || 0
    };

    this.musicService.paySong(payload).subscribe({
      next: (res: any) => {
        if (res && res.url) {
          if (paymentWindow) {
            paymentWindow.location.href = res.url;
          }
        } else {
          if (paymentWindow) paymentWindow.close();
          alert('Error: No se ha podido obtener la URL de pago de Stripe.');
        }
      },
      error: (err) => {
        console.error("Error al iniciar pago de canción:", err);
        if (paymentWindow) paymentWindow.close();
        alert('Error al conectar con el servidor de pagos de Stripe: ' + (err.error?.message || ''));
      }
    });
  }

  // ========================================================
  // 💰 FLUJO 7: UN CLIENTE PAGA PARA COLAR SU CANCIÓN — PASO 7
  // handlePaymentMessage() — Recepción de Mensaje Seguro postMessage desde la Popup
  // ========================================================
  handlePaymentMessage = (event: MessageEvent) => {
    if (event.data && event.data.type === 'payment-success') {
      console.log("[PAGO] Mensaje recibido en ventana principal. Recargando cola...", event.data.payload);
      this.queueService.getQueue(this.barId).subscribe((freshQueue: any[]) => {
        const oldLength = this.queue.length;
        this.queue = freshQueue;
        this.localPaidQueue = freshQueue
          .filter((q: any) => q.paid === true)
          .map((q: any) => this.queueItemToVisual(q));
        this.nextTracks = freshQueue
          .filter((q: any) => !q.paid)
          .map((q: any) => this.queueItemToVisual(q));
        this.cdr.detectChanges();

        if (oldLength === 0 && freshQueue.length > 0 && this.sdkDeviceId) {
          console.log("[PAGO] Cola estaba vacía, iniciando reproducción inmediata.");
          this.currentPlayingId = null;
          this.playQueuePosition0();
        }
      });
    }
  };

  // ========================================================
  // --- 6. CONTROLES Y UTILIDADES GENERALES ---
  // ========================================================

  // ========================================================
  // UTILIDADES
  // ========================================================
  formatTime(ms: number): string {
    if (!ms || ms < 0) return '0:00';
    const totalSeconds = Math.floor(ms / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return `${minutes}:${seconds < 10 ? '0' : ''}${seconds}`;
  }

  connectSpotify() {
    this.spoti.redirectToSpotify();
  }

  get displayedTrack(): any {
    if (this.currentTrackPlaying) {
      return this.currentTrackPlaying;
    }
    if (this.queue.length > 0) {
      return this.queueItemToVisual(this.queue[0]);
    }
    return null;
  }

  get totalDuration(): number {
    if (this.currentDuration > 0) return this.currentDuration;
    if (this.displayedTrack && this.displayedTrack.duration_ms) return this.displayedTrack.duration_ms;
    return 0;
  }

  get progressTime(): number {
    return this.currentProgress;
  }

  togglePlay() {
    if (!this.sdkPlayer) return;

    if (!this.currentTrackPlaying && this.queue.length > 0) {
      console.log("[CONTROL] No hay track sonando pero sí canciones en la cola. Iniciando canción 0.");
      this.currentPlayingId = null; // Resetear para forzar play
      this.playQueuePosition0();
    } else {
      this.sdkPlayer.togglePlay().then(() => {
        console.log("[CONTROL] Play/Pause alternado");
      }).catch((err: any) => {
        console.error("Error al alternar reproducción:", err);
        // Si falla por desincronización, reintentar reproduciendo pos 0
        if (this.queue.length > 0) {
          this.currentPlayingId = null;
          this.playQueuePosition0();
        }
      });
    }
  }

  nextTrack() {
    console.log("[CONTROL] Siguiente canción pulsada por el usuario.");
    this.finishCurrentSong();
  }

  private queueItemToVisual(q: any) {
    return {
      id: q.spotifyTrackId,
      uri: `spotify:track:${q.spotifyTrackId}`,
      name: q.title,
      artists: [{ name: q.artist }],
      album: { images: [{ url: q.albumArtUrl }] },
      duration_ms: q.durationMs
    };
  }

  // ========================================================
  // --- 7. CIERRE DE SESIÓN Y DESTRUCCIÓN ---
  // ========================================================

  logout() {
    if (this.sdkPlayer) {
      try {
        console.log("[LOGOUT] Pausando reproductor de Spotify de forma segura.");
        this.sdkPlayer.pause();
        this.sdkPlayer.removeListener('initialization_error');
        this.sdkPlayer.removeListener('authentication_error');
        this.sdkPlayer.removeListener('account_error');
        this.sdkPlayer.removeListener('playback_error');
        this.sdkPlayer.removeListener('player_state_changed');
        this.sdkPlayer.removeListener('ready');
        this.sdkPlayer.removeListener('not_ready');
      } catch (e) {
        console.warn("Error al pausar/desvincular el reproductor en logout:", e);
      }
    }
    localStorage.removeItem('user');
    localStorage.removeItem('spotiToken');
    sessionStorage.removeItem('spotiToken');
    sessionStorage.removeItem('clientId');
    sessionStorage.removeItem('userEmail');
    this.router.navigate(['/login']);
  }

  ngOnDestroy() {
    if (this.queueSub) this.queueSub.unsubscribe();
    clearInterval(this.progressInterval);
    window.removeEventListener('message', this.handlePaymentMessage);
    if (this.sdkPlayer) {
      try {
        console.log("[DESTROY] Desvinculando listeners del reproductor de Spotify.");
        this.sdkPlayer.removeListener('initialization_error');
        this.sdkPlayer.removeListener('authentication_error');
        this.sdkPlayer.removeListener('account_error');
        this.sdkPlayer.removeListener('playback_error');
        this.sdkPlayer.removeListener('player_state_changed');
        this.sdkPlayer.removeListener('ready');
        this.sdkPlayer.removeListener('not_ready');
      } catch (e) {
        console.warn("Error al desvincular listeners en destroy:", e);
      }
    }
  }
}
