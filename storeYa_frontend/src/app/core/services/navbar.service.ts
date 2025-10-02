// src/app/core/services/navbar.service.ts
import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class NavbarService {
  private loginModal$ = new BehaviorSubject<boolean>(false);
  loginModalState$ = this.loginModal$.asObservable();

  openLoginModal() {
    this.loginModal$.next(true);
  }

  closeLoginModal() {
    this.loginModal$.next(false);
  }

}
