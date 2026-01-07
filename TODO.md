# TODO: Implement Toast Notification for Payment Success

## Tasks
- [ ] Modify payment-form.component.ts: Add showSuccess and successMessage variables, replace success message logic with toast
- [ ] Modify payment-form.component.html: Add toast notification HTML at the end
- [ ] Modify payment-form.component.css: Add toast notification styles

## Details
- Replace the current success message and modal close with a toast that appears for 4 seconds
- Toast should show "¡Pago aceptado! \"${songTitle}\" añadida a la cola."
- Toast styled like Spotify green, floating in top-right corner
