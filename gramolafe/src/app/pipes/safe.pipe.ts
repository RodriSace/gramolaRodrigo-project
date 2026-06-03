import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

/**
 * FILTRO DE SEGURIDAD PARA RECURSOS EXTERNOS (SafePipe)
 * 
 * ¿Qué es y para qué sirve?
 * Es un Pipe personalizado de Angular anotado con @Pipe y configurado como 'standalone'.
 * Sirve para esquivar las restricciones de seguridad integradas de Angular relativas al enlazado
 * de recursos dinámicos.
 * 
 * Explicación detallada para la defensa ante el profesor:
 * Por seguridad para evitar ataques de Inyección de Scripts Cruzados (XSS), Angular bloquea automáticamente
 * cualquier enlace variable inyectado directamente en el atributo 'src' de elementos sensibles como '<iframe>'.
 * 
 * Si intentásemos incrustar la URL del reproductor o muestras de audio de Spotify sin sanear, Angular daría
 * un error de seguridad en la consola del navegador.
 * 
 * Este Pipe inyecta el servicio 'DomSanitizer' del navegador y llama a 'bypassSecurityTrustResourceUrl'.
 * Esto le indica a Angular: "He comprobado esta URL, es legítima y proviene de Spotify, confía en ella".
 * En el HTML se usa sencillamente aplicando el operador tubería: '<iframe [src]="cancionUrl | safe"></iframe>'.
 */
@Pipe({
  name: 'safe',
  standalone: true
})
export class SafePipe implements PipeTransform {
  
  // Inyectamos DomSanitizer, el escudo de seguridad HTML oficial de Angular para navegadores
  constructor(private sanitizer: DomSanitizer) {}

  /**
   * Método transformador obligatorio de la interfaz PipeTransform.
   * Toma un string URL ordinario y lo transforma en una URL segura para recursos de confianza (SafeResourceUrl).
   * 
   * @param url La dirección web externa (ej. de Spotify) que deseamos incrustar.
   */
  transform(url: string): SafeResourceUrl {
    return this.sanitizer.bypassSecurityTrustResourceUrl(url);
  }
}

