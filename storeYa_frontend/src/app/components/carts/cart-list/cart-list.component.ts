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
export class CartListComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  cartItems: CartItem[] = [];
  total = 0;

  constructor(private cartService: CartService) { }

  ngOnInit() {
    // Load cart từ backend
    this.cartService.loadCart().subscribe({
      next: () => console.log('🛒 Cart loaded successfully'),
      error: err => console.error('❌ Load cart failed:', err)
    });

    // Theo dõi thay đổi giỏ hàng
    this.cartService.cart$
      .pipe(takeUntil(this.destroy$))
      .subscribe(items => {
        this.cartItems = items;
        this.total = this.cartService.getTotal();
      });
  }

  updateQuantity(event: { id: number; qty: number }) {
    const sub = this.cartService.updateQuantity(event.id, event.qty).subscribe({
      next: () => {
        console.log(`🔄 Updated quantity for item ${event.id}`);
        sub.unsubscribe();
      },
      error: err => {
        console.error('❌ Update quantity failed:', err);
        sub.unsubscribe();
      }
    });
  }

  removeItem(itemId: number) {
    this.cartService.removeItem(itemId).subscribe();
  }
  

  checkout() {
    if (this.total === 0) return;
    alert(`Thanh toán tổng cộng: ${this.total.toLocaleString('vi-VN')} VND`);
    this.cartService.clearCart().subscribe({
      next: () => console.log('✅ Cart cleared'),
      error: err => console.error('❌ Clear cart failed:', err)
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
