import { Component, OnInit, OnDestroy } from '@angular/core';
import { CartItem, CartService } from '../../../core/services/cart.service';
import { Subject, takeUntil } from 'rxjs';
import { CartItemComponent } from '../cart-item/cart-item.component';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-cart-list',
  templateUrl: './cart-list.component.html',
  styleUrls: ['./cart-list.component.scss'],
  standalone: true,
  imports: [CommonModule, CartItemComponent]
})
export class CartListComponent implements OnInit {
  private destroy$ = new Subject<void>();
  cartItems: CartItem[] = [];
  total = 0;
  private userId?: number;

  constructor(private cartService: CartService) { }

  ngOnInit() {
    // Không cần userId
    const token = localStorage.getItem('access_token');
    if (!token) {
      console.warn('⚠️ Chưa login → không load giỏ hàng');
      return;
    }

    // Load cart cho user hiện tại (backend tự lấy từ token)
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

  removeItem(id: number) {
    this.cartService.removeFromCart(id).subscribe();
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
