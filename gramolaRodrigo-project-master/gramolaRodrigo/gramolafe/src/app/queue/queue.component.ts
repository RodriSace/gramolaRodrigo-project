import { Component, OnInit, OnDestroy, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { SongService } from '../song.service';
import { AuthService } from '../auth.service';
import { Subscription } from 'rxjs';

interface QueuedSong {
  songId: string;
  title: string;
  artist: string;
  albumCover: string;
  previewUrl: string;
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
  audioStatus = 'Iniciando...';
  remainingSeconds: number | null = null;
  
  public nextTrackUrl: string | null = null;
  private preloadedSongId: string | null = null; // Seguimiento del ID precargado
  private isTransitioning = false;
  private fetchInterval: any;
  private queueChangedSub?: Subscription;

  constructor(
    private songService: SongService, 
    private authService: AuthService,
    private sanitizer: DomSanitizer
  ) { }

  ngOnInit(): void {
    this.authService.checkSubscriptionStatus().subscribe(active => {
      if (active) {
        this.fetchQueue();
        this.fetchInterval = setInterval(() => this.fetchQueue(), 5000);
      } else { this.audioStatus = 'Suscripción inactiva'; }
    });
    this.queueChangedSub = this.songService.queueChanged$.subscribe(() => this.fetchQueue());
  }

  ngAfterViewInit(): void {
    if (this.audioPlayer) {
      const audio = this.audioPlayer.nativeElement;
      audio.addEventListener('ended', () => this.handleTrackEnd());
      audio.addEventListener('timeupdate', () => {
        if(audio.duration) this.remainingSeconds = Math.ceil(audio.duration - audio.currentTime);
      });
    }
  }

  ngOnDestroy(): void {
    if (this.fetchInterval) clearInterval(this.fetchInterval);
    if (this.queueChangedSub) this.queueChangedSub.unsubscribe();
  }

  fetchQueue() {
    if (this.isTransitioning) return;
    this.songService.getQueue().subscribe((data: QueuedSong[]) => {
      if (!data || data.length === 0) return;
      const incoming = data[0];

      // Sincronizar canción actual si el servidor cambió
      if (!this.nowPlaying || this.nowPlaying.songId !== incoming.songId) {
        this.nowPlaying = { ...incoming };
        this.upNext = data.slice(1);
        this.setAudioSrcAndPlay(this.nowPlaying.previewUrl);
        this.nextTrackUrl = null;
        this.preloadedSongId = null;
      } else {
        this.upNext = data.slice(1);
      }

      // GESTIÓN DE PRECARGA: Si lo que sigue no es lo que tenemos en memoria, actualizar
      if (this.upNext.length > 0) {
        const nextInLine = this.upNext[0];
        if (nextInLine.songId !== this.preloadedSongId) {
          this.nextTrackUrl = nextInLine.previewUrl;
          this.preloadedSongId = nextInLine.songId;
        }
      } else {
        this.nextTrackUrl = null;
        this.preloadedSongId = null;
      }
    });
  }

  setAudioSrcAndPlay(url: string) {
    if (!url) return;
    const audio = this.audioPlayer.nativeElement;
    audio.src = url;
    audio.load();
    audio.play().then(() => this.audioStatus = 'En antena').catch(() => {
      this.audioStatus = 'Muteado';
      audio.muted = true; audio.play();
    });
  }

  private handleTrackEnd() {
    if (this.isTransitioning) return;
    this.isTransitioning = true;
    this.audioStatus = 'Siguiente pista...';

    // Usar precarga solo si coincide con el orden actual de la cola
    if (this.nextTrackUrl && this.upNext.length > 0 && this.upNext[0].songId === this.preloadedSongId) {
      const nextSong = this.upNext[0];
      this.nowPlaying = { ...nextSong };
      this.upNext = this.upNext.slice(1);
      this.setAudioSrcAndPlay(this.nextTrackUrl);
      this.nextTrackUrl = null;
      this.preloadedSongId = null;
    }

    this.songService.playNext().subscribe(() => {
      setTimeout(() => { this.isTransitioning = false; this.fetchQueue(); }, 1000);
    });
  }

  getSafeUrl(url: string): SafeUrl { return this.sanitizer.bypassSecurityTrustUrl(url); }
}