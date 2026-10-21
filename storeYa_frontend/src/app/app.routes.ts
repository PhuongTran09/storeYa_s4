import { Routes } from '@angular/router';
import { authGuard, paymentResultGuard } from './core/guards/auth.guards';
import { NotFoundComponent } from './page/not-found/not-found.component';
import { ProductDetailComponent } from './components/product/product-detail/product-detail.component';
import { CartListComponent } from './components/carts/cart-list/cart-list.component';
import { ProfileComponent } from './components/profile/profile.component';
import { PaymentsComponent } from './components/payments/payments.component';
import { UserListPayComponent } from './components/payments/user-list-pay/user-list-pay.component';
import { PaymentResultComponent } from './page/payment-result/payment-result.component';

export const routes: Routes = [
  { path: '', redirectTo: 'home', pathMatch: 'full' },

  // Public
  {
    path: '',
    children: [
      {
        path: 'home',
        loadComponent: () =>
          import('./page/home/home.component').then(m => m.HomeComponent)
      },

      { path: 'product/:id', component: ProductDetailComponent },
      // These pages all call authenticated APIs. Without the guard an anonymous
      // visitor reached the component and got a wall of 401s instead of the login modal.
      { path: 'cart', canActivate: [authGuard], component: CartListComponent },
      { path: 'profile', canActivate: [authGuard], component: ProfileComponent },
      { path: 'payments', canActivate: [authGuard], component: PaymentsComponent },
      { path: 'history-payment', canActivate: [authGuard], component: UserListPayComponent },


      {
        canActivate: [paymentResultGuard],
        path: 'payment-result', component: PaymentResultComponent
      }

    ]

  },

  // Admin
  {
    path: 'admin',
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./page/dashboard/dashboard.component').then(m => m.DashboardComponent)
      },
    ]
  },

  {
    path: '404',
    loadComponent: () =>
      import('./page/not-found/not-found.component').then(m => m.NotFoundComponent)
  },
  { path: '**', redirectTo: '404' }
];
