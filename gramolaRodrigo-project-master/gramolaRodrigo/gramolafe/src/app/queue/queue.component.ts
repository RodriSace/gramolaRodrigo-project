import { Component, OnInit, OnDestroy, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { SongService } from '../song.service';
import { AuthService } from '../auth.service';
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
  audioStatus: string | null = 'Iniciando...';
  
  private isTransitioning = false;
  // Cambiado a placehold.co que es más fiable
  private defaultCover = 'https://placehold.co/150x150?text=No+Cover';
  private corsProxy = 'https://corsproxy.io/?';

  constructor(
    private songService: SongService, 
    private authService: AuthService,
    private sanitizer: DomSanitizer
  ) { }

  ngOnInit(): void {
  this.audioStatus = 'Verificando suscripción...';

  this.authService.checkSubscriptionStatus().subscribe({
    next: (active: boolean) => {
      if (active) {
        // ELIMINADO: loadInitialQueue() ya no se llama aquí.
        // Solo llamamos al servidor para obtener la lista real.
        this.fetchQueue(); 
        this.fetchInterval = setInterval(() => this.fetchQueue(), 5000);
      } else {
        this.audioStatus = 'Suscripción inactiva';
      }
    },
    error: () => this.audioStatus = 'Error de conexión con el bar'
  });

  this.queueChangedSub = this.songService.queueChanged$.subscribe(() => {
    this.fetchQueue();
  });
}

  ngAfterViewInit(): void {
    if (this.audioPlayer) {
      const audio = this.audioPlayer.nativeElement;
      audio.addEventListener('ended', () => this.handleTrackEnd());
      audio.addEventListener('error', () => {
        setTimeout(() => this.handleTrackEnd(), 1500);
      });
      audio.addEventListener('timeupdate', () => {
        if(audio.duration) {
           this.remainingSeconds = Math.ceil(audio.duration - audio.currentTime);
        }
      });
    }
  }

  ngOnDestroy(): void {
    if (this.fetchInterval) clearInterval(this.fetchInterval);
    if (this.queueChangedSub) this.queueChangedSub.unsubscribe();
  }

  fetchQueue() {
    if (this.isTransitioning) return;
    this.songService.getQueue().subscribe({
      next: (data: QueuedSong[]) => {
        if (data && data.length > 0) {
          const incoming = data[0];
          // Solo cambiamos si la canción del servidor es distinta a la actual
          if (!this.nowPlaying || this.nowPlaying.songId !== incoming.songId) {
            this.nowPlaying = { ...incoming };
            this.upNext = data.slice(1);
            this.resolveAndPlay(this.nowPlaying);
          } else {
            this.upNext = data.slice(1);
          }
        }
      }
    });
  }

  resolveAndPlay(song: QueuedSong) {
    if (!this.audioPlayer) return;
    this.audioStatus = 'Obteniendo stream...';

    const targetUrl = `https://api.deezer.com/track/${song.songId}`;
    
    fetch(this.corsProxy + encodeURIComponent(targetUrl))
      .then(res => res.json())
      .then(track => {
        if (track && track.preview) {
          this.nowPlaying!.albumCover = track.album.cover_medium;
          this.setAudioSrcAndPlay(track.preview);
        } else {
          this.handleTrackEnd();
        }
      })
      .catch(() => this.handleTrackEnd());
  }

  setAudioSrcAndPlay(url: string) {
    const audio = this.audioPlayer.nativeElement;
    audio.src = url;
    audio.load();
    audio.play()
      .then(() => this.audioStatus = 'En antena')
      .catch(() => {
        this.audioStatus = 'Click para reproducir';
        audio.muted = true; 
        audio.play().catch(() => {});
      });
  }

  private handleTrackEnd() {
    if (this.isTransitioning) return;
    this.isTransitioning = true;
    this.audioStatus = 'Siguiente pista...';

    this.songService.playNext().subscribe({
      next: () => {
        this.fetchQueue();
        setTimeout(() => { this.isTransitioning = false; }, 2000);
      },
      error: () => { this.isTransitioning = false; }
    });
  }

  getSafeUrl(url: string): SafeUrl {
    return this.sanitizer.bypassSecurityTrustUrl(url);
  }

  onImageError(event: any) {
    event.target.src = this.defaultCover;
  }
}