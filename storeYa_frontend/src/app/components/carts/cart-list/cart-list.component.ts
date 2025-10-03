import { Component, OnInit, OnDestroy } from '@angular/core';
import { CartItem, CartService } from '../../../core/services/cart.service';
import { Subject, takeUntil } from 'rxjs';
import { CartItemComponent } from '../cart-item/cart-item.component';
import { CommonModule } from '@angular/common';
import { VndPipe } from '../../../shared/pipes/truncate.pipe';
import { HeaderComponent } from '../../../shared/layout/navbar/navbar.component';

@Component({
  selector: 'app-cart-list',
  templateUrl: './cart-list.component.html',
  styleUrls: ['./cart-list.component.scss'],
  standalone: true,
  imports: [CommonModule, CartItemComponent, VndPipe, HeaderComponent],
  
})
export class CartListComponent implements OnInit {
  private destroy$ = new Subject<void>();
  cartItems: CartItem[] = [];
  total = 0;
  private userId?: number;

  constructor(private cartService: CartService) { }

  ngOnInit() {
    this.cartService.loadCart().subscribe({
      next: () => console.log('Cart loaded'),
      error: err => console.error('Load cart failed', err)
    });

    this.cartService.cart$
      .pipe(takeUntil(this.destroy$))
      .subscribe(items => {
        this.cartItems = items;
        this.total = this.cartService.getTotal();
      });
  }
  removeItem(productId: number) {
    this.cartService.removeItem(productId).subscribe({
      next: cart => {
        // cartService sẽ emit cart$. Cập nhật cartItems ở đây optional
        this.cartItems = cart?.items ?? [];
        this.total = this.cartService.getTotal();
      },
      error: err => console.error('Failed to remove item', err)
    });
  }



  updateQuantity(event: { id: number; qty: number }) {
    this.cartService.updateQuantity(event.id, event.qty).subscribe();
  }

  checkout() {
    if (this.total === 0) return;
    alert(`Thanh toán tổng: $${this.total}`);
    this.cartService.clearCart().subscribe();
  }

}
