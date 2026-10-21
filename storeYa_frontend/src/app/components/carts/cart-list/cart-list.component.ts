import {Component, OnDestroy, OnInit} from '@angular/core';
import {CartItem, CartService} from '../../../core/services/cart.service';
import {Subject, takeUntil} from 'rxjs';
import {CartItemComponent} from '../cart-item/cart-item.component';
import {CommonModule} from '@angular/common';
import {VndPipe} from '../../../shared/pipes/truncate.pipe';
import {HeaderComponent} from '../../../shared/layout/navbar/navbar.component';
import {RouterLink} from "@angular/router";
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-cart-list',
  templateUrl: './cart-list.component.html',
  styleUrls: ['./cart-list.component.scss'],
  standalone: true,
  imports: [CommonModule, CartItemComponent, VndPipe, HeaderComponent, RouterLink],
})
export class CartListComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  cartItems: CartItem[] = [];
  total = 0;

  constructor(private cartService: CartService, private toast: ToastService) { }

  ngOnInit() {
    // Load cart từ backend
    this.cartService.loadCart().subscribe({
    });

    // Theo dõi thay đổi giỏ hàng
    this.cartService.cart$
      .pipe(takeUntil(this.destroy$))
      .subscribe(items => {
        this.cartItems = items;
        this.cartService.total$.subscribe(total => {
          this.total = total;
        });
      });
  }

  updateQuantity(event: { id: number; qty: number }) {
    const sub = this.cartService.updateQuantity(event.id, event.qty).subscribe({
      next: () => {
        console.log(`Updated quantity for item ${event.id}`);
        sub.unsubscribe();
      },
      error: () => {
        this.toast.show('Mặt hàng trong đã hết hàng', 'warning');
        sub.unsubscribe();
      }
    });
  }

  removeItem(itemId: number) {
    this.cartService.removeItem(itemId).subscribe();
  }




  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
