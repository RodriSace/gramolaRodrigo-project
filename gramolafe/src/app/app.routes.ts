import { Routes } from '@angular/router';
import { RegisterComponent } from './components/register/register.component';
import { LoginComponent } from './components/login/login.component';
import { MusicComponent } from './components/music/music.component';
import { PaymentSuccessComponent } from './components/payment-success/payment-success.component';
import { ConfirmComponent } from './components/confirm/confirm.component';
import { SubscriptionComponent } from './components/subscription/subscription.component';
import { CallBackComponent } from './components/callback/callback.component';
import { ResetPasswordComponent } from './components/reset-password/reset-password.component';

export const routes: Routes = [
    { path: 'register', component: RegisterComponent },
    { path: 'login', component: LoginComponent },
    { path: 'music', component: MusicComponent },
    { path: 'payment-success', component: PaymentSuccessComponent },
    { path: 'confirm', component: ConfirmComponent },
    { path: 'subscription', component: SubscriptionComponent },
    { path: 'payment', component: SubscriptionComponent }, // Alias para cumplir con el enunciado
    { path: 'callback', component: CallBackComponent },
    { path: 'reset-password', component: ResetPasswordComponent },
    { path: '', redirectTo: '/login', pathMatch: 'full' }
];
