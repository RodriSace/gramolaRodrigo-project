import { Component, OnInit, OnDestroy, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
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
  @ViewChild('audioPlayer') audioPlayer!: ElementRef<HTMLAudioElement>;

  nowPlaying: any = null;
  upNext: any[] = [];
  
  private songEndTimer: any;
  private fetchInterval: any;
  private queueChangedSub?: Subscription;
  
  remainingSeconds: number | null = null;
  audioStatus: string | null = null;
  API_URL = API_URL;
  private endHandled = false;

  constructor(private songService: SongService) { }

  ngOnInit(): void {
    this.fetchInterval = setInterval(() => this.fetchQueue(), 4000);
    this.queueChangedSub = this.songService.queueChanged$.subscribe(() => {
      this.fetchQueue();
    });
    
    // Desbloqueo inicial
    document.addEventListener('click', () => {
      if (this.audioPlayer && this.audioPlayer.nativeElement.paused && this.nowPlaying) {
        this.audioPlayer.nativeElement.play().catch(() => {});
      }
    }, { once: true });
  }

  ngAfterViewInit(): void {
    if (this.audioPlayer) {
      const audio = this.audioPlayer.nativeElement;
      
      audio.addEventListener('ended', () => {
        this.audioStatus = 'Finalizado';
        this.handleTrackEnd();
      });

      audio.addEventListener('error', (e) => {
        console.error('Error audio, reintentando...', e);
        setTimeout(() => this.handleTrackEnd(), 2000);
      });
      
      audio.addEventListener('timeupdate', () => {
        if(audio.duration) {
           this.remainingSeconds = Math.ceil(audio.duration - audio.currentTime);
        }
      });
    }
    this.fetchQueue();
  }

  ngOnDestroy(): void {
    if (this.songEndTimer) clearTimeout(this.songEndTimer);
    if (this.fetchInterval) clearInterval(this.fetchInterval);
    if (this.queueChangedSub) this.queueChangedSub.unsubscribe();
  }

  fetchQueue() {
    this.songService.getQueue().subscribe({
      next: (data) => {
        // SEGURIDAD: Si ya estamos tocando algo, NO cambiamos aunque la cola cambie.
        // Esperamos a que la canción termine sola.
        if (this.nowPlaying != null) {
            return;
        }

        if (data.length > 0) {
          const nextSong = data[0];
          this.nowPlaying = nextSong;
          this.resolveAndPlay(nextSong);
        }
      },
      error: (err) => console.error('Error cola', err)
    });
  }

  resolveAndPlay(song: any) {
    if (!this.audioPlayer) return;
    const audio = this.audioPlayer.nativeElement;
    this.endHandled = false;
    this.audioStatus = 'Cargando...';

    // Resolver URL (Backend o Directa)
    if (song.previewUrl && song.previewUrl.startsWith('http')) {
        this.setAudioSrcAndPlay(song.previewUrl);
    } else {
        fetch(`${API_URL}/api/deezer/url/${song.songId}`)
            .then(res => res.json())
            .then(data => {
                if (data.url) this.setAudioSrcAndPlay(data.url);
                else throw new Error("URL vacía");
            })
            .catch(() => this.handleTrackEnd());
    }
  }

  setAudioSrcAndPlay(url: string) {
    const audio = this.audioPlayer.nativeElement;
    audio.src = url;
    audio.load();

    const playPromise = audio.play();
    if (playPromise !== undefined) {
      playPromise.then(() => {
        this.audioStatus = 'Reproduciendo';
        audio.muted = false; 
      }).catch(() => {
        this.audioStatus = 'Reproduciendo (Mute)';
        audio.muted = true;
        audio.play().catch(() => setTimeout(() => this.handleTrackEnd(), 2000));
      });
    }
  }

  private handleTrackEnd() {
    if (this.endHandled) return;
    this.endHandled = true;
    
    // CORRECCIÓN CRÍTICA:
    // Pedimos la siguiente canción al backend y usamos LA QUE NOS DEVUELVE.
    // No llamamos a fetchQueue() porque la cola ya estará vacía.
    this.songService.playNext().subscribe({
      next: (nextSong) => {
        if (nextSong) {
            console.log("Siguiente canción recibida:", nextSong.title);
            this.nowPlaying = nextSong; // Actualizamos la canción actual
            this.upNext = [];
            this.resolveAndPlay(nextSong); // Y la reproducimos directamente
        } else {
            // Si no devuelve nada (raro), forzamos recarga
            this.nowPlaying = null;
            this.fetchQueue();
        }
      },
      error: () => setTimeout(() => this.handleTrackEnd(), 2000)
    });
  }

  formatTime(seconds: number | null): string {
    if (seconds === null) return '--:--';
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m}:${s < 10 ? '0' : ''}${s}`;
  }
}