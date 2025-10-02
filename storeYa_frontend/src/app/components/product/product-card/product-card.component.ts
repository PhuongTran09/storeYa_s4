import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { Router } from '@angular/router';
import { Product } from '../../../core/services/product.service';
import { VndPipe } from '../../../shared/pipes/truncate.pipe';

@Component({
  selector: 'product-card',
  templateUrl: './product-card.component.html',
  styleUrls: ['./product-card.component.scss'],
  standalone: true,
  imports: [CommonModule, VndPipe]
})
export class ProductCardComponent {
  @Input() product!: Product;

  constructor(private router: Router) {}

  goToDetail() {
    this.router.navigate(['/product', this.product.id]);
  }

  addToCart(product: Product, event?: MouseEvent) {
    if (event) {
      event.stopPropagation(); // tránh trigger click card
    }
    console.log("Add to cart:", product);
    // TODO: gọi service add cart
  }
}
