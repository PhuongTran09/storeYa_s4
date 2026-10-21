import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { Product } from '../../../core/services/product.service';
import { CartService } from '../../../core/services/cart.service';
import { AuthService } from '../../../core/services/auth.service';
import { NavbarService } from '../../../core/services/navbar.service';
import { ToastService } from '../../../core/services/toast.service';
import { TruncatePipe, VndPipe } from '../../../shared/pipes/truncate.pipe';

@Component({
  selector: 'product-card',
  templateUrl: './product-card.component.html',
  styleUrls: ['./product-card.component.scss'],
  standalone: true,
  imports: [CommonModule, VndPipe, TruncatePipe]
})
export class ProductCardComponent {
  @Input() product!: Product;
  adding = false;

  constructor(
    private router: Router,
    private cartService: CartService,
    private auth: AuthService,
    private navbarService: NavbarService,
    private toast: ToastService
  ) {}

  goToDetail() {
    this.router.navigate(['/product', this.product.id]);
  }

  addToCart(product: Product, event?: MouseEvent) {
    if (event) {
      event.stopPropagation(); // tránh trigger click card
    }

    if (!product.id || this.adding) return;

    if (product.stock <= 0) {
      this.toast.show('Sản phẩm đã hết hàng', 'warning');
      return;
    }

    // /api/carts requires a token, and cards render on the public home page.
    if (!this.auth.getToken()) {
      this.navbarService.openLoginModal();
      this.toast.show('Bạn cần đăng nhập để thêm vào giỏ', 'warning');
      return;
    }

    this.adding = true;
    this.cartService.addToCart(product.id, 1)
      .pipe(finalize(() => (this.adding = false)))
      .subscribe({
        next: () => this.toast.show('🛒 Đã thêm sản phẩm vào giỏ!', 'success'),
        error: () => this.toast.show('❌ Thêm sản phẩm thất bại!', 'error')
      });
  }
}
