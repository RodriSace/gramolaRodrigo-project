import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { SongSearchComponent } from '../song-search/song-search.component';
import { QueueComponent } from '../queue/queue.component';
import { API_URL } from '../api.config';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, SongSearchComponent, QueueComponent],
  template: `
    <div class="dashboard-container">
      <div class="dashboard-grid">
        <!-- Panel de búsqueda -->
        <div class="search-panel">
          <h2>Buscar Canciones</h2>
          <app-song-search></app-song-search>
        </div>

        <!-- Panel de cola -->
        <div class="queue-panel">
          <h2>Cola de Reproducción</h2>
          <app-queue></app-queue>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .dashboard-container {
      padding: 2rem;
      max-width: 1400px;
      margin: 0 auto;
    }

    .dashboard-grid {
      display: grid;
      grid-template-columns: 1fr 400px;
      gap: 2rem;
      align-items: start;
    }

    .search-panel, .queue-panel {
      background: rgba(30, 30, 30, 0.95);
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 16px;
      padding: 1.5rem;
      box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.37);
    }

    .search-panel {
      min-height: 600px;
    }

    .queue-panel {
      position: sticky;
      top: 2rem;
      max-height: calc(100vh - 4rem);
      overflow-y: auto;
    }

    h2 {
      color: #bb86fc;
      margin-bottom: 1rem;
      font-size: 1.3rem;
      font-weight: 600;
    }

    @media (max-width: 1024px) {
      .dashboard-grid {
        grid-template-columns: 1fr;
        gap: 1.5rem;
      }

      .queue-panel {
        position: static;
        max-height: none;
      }
    }
  `]
})
export class DashboardComponent implements OnInit, OnDestroy {

  constructor(private http: HttpClient) {}

  ngOnInit(): void {
    // Component initialization
  }

  ngOnDestroy(): void {
    // Component cleanup
  }
}
