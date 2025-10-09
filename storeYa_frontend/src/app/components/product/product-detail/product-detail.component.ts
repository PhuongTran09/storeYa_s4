import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ProductService, Product } from '../../../core/services/product.service';
import { FormsModule } from '@angular/forms';
import { HeaderComponent } from '../../../shared/layout/navbar/navbar.component';
import { VndPipe } from '../../../shared/pipes/truncate.pipe';
import { CartService } from '../../../core/services/cart.service';  // 👈 import CartService
import { ToastComponent } from '../../../shared/toast/toast.component';
import { ToastService } from '../../../core/services/toast.service';
import { finalize } from 'rxjs';

@Component({
  selector: 'app-product-detail',
  templateUrl: './product-detail.component.html',
  styleUrls: ['./product-detail.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, HeaderComponent, VndPipe],
})
export class ProductDetailComponent implements OnInit, OnDestroy {
  product!: Product | null;
  currentImage = 0;
  intervalId: any;
  animating = false;
  direction: 'left' | 'right' = 'right';
  quantity = 1;
  saving: boolean | undefined;

  constructor(
    private route: ActivatedRoute,
    private productService: ProductService,
    private cartService: CartService,   // 👈 inject service
    private toast: ToastService  // 👈 inject ToastService
  ) { }

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadProduct(+id);
    }
  }

  ngOnDestroy() {
    if (this.intervalId) {
      clearInterval(this.intervalId);
    }
  }

  loadProduct(id: number) {
    this.productService.getProductById(id).subscribe({
      next: (res) => {
        this.product = res;
        this.startAutoSlide();
      },
      error: (err) => {
        console.error('Không tải được sản phẩm', err);
      },
    });
  }

  startAutoSlide() {
    if (this.product?.imageUrls && this.product.imageUrls.length > 1) {
      this.intervalId = setInterval(() => {
        this.nextImage();
      }, 3000);
    }
  }

  nextImage() {
    if (!this.product?.imageUrls || this.animating) return;

    this.direction = 'right';
    this.animating = true;
    this.currentImage =
      (this.currentImage + 1) % this.product.imageUrls.length;

    setTimeout(() => (this.animating = false), 700);
  }

  prevImage() {
    if (!this.product?.imageUrls || this.animating) return;

    this.direction = 'left';
    this.animating = true;
    this.currentImage =
      (this.currentImage - 1 + this.product.imageUrls.length) %
      this.product.imageUrls.length;

    setTimeout(() => (this.animating = false), 700);
  }

  increaseQty() {
    this.quantity++;
  }

  decreaseQty() {
    if (this.quantity > 1) {
      this.quantity--;
    }
  }
  addToCart() {
    if (!this.product) return;

    // Hiển thị toast loading
    this.toast.show('Đang thêm sản phẩm vào giỏ...', 'loading');
    this.saving = true; // optional nếu bạn muốn disable nút

    this.cartService.addToCart(this.product.id ?? 0, this.quantity)
      .pipe(finalize(() => (this.saving = false))) // reset trạng thái nút
      .subscribe({
        next: () => {
          // Thêm thành công → toast success
          this.toast.show('🛒 Đã thêm sản phẩm vào giỏ!', 'success');
        },
        error: (err) => {
          // Thêm thất bại → toast error
          this.toast.show('❌ Thêm sản phẩm thất bại!', 'error');
          console.error(err);
        }
      });
  }




}
