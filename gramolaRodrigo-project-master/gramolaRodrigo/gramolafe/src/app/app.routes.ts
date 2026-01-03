import { Routes } from '@angular/router';
import { verifiedGuard } from './verified.guard';

export const routes: Routes = [
	{ path: 'forgot-password', loadComponent: () => import('./forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent) },
	{ path: 'reset-password', loadComponent: () => import('./reset-password/reset-password.component').then(m => m.ResetPasswordComponent) },
	{ path: 'confirm-email', loadComponent: () => import('./confirm-email/confirm-email.component').then(m => m.ConfirmEmailComponent) },
		{ path: 'subscribe', loadComponent: () => import('./subscribe/subscribe.component').then(m => m.SubscribeComponent), canActivate: [verifiedGuard] },
];
