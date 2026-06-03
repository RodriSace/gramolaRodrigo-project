import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

/**
 * COMPONENTE RAÍZ DE LA APLICACIÓN (AppComponent)
 * 
 * ¿Qué es y para qué sirve?
 * Es el componente principal e inicializador de Angular. Actúa como el lienzo base
 * o contenedor principal de toda la SPA (Single Page Application).
 * 
 * Su única responsabilidad en esta arquitectura es pintar el marcado base y declarar
 * el marcador '<router-outlet></router-outlet>' en su plantilla HTML.
 * De esta forma, el enrutador de Angular inyectará dinámicamente en este hueco las distintas
 * páginas (Login, Register, Music, Subscription, etc.) según cambie la URL.
 */
@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  // Título por defecto asignado al proyecto Angular
  title = 'gramolafe';
}

