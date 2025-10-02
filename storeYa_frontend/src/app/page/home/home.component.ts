import { Component } from '@angular/core';
import { ProductListComponent } from '../../components/product/product-list/product-list.component';
import { HeaderComponent } from '../../shared/layout/navbar/navbar.component';
import { ProductCardComponent } from '../../components/product/product-card/product-card.component';
import { Product, ProductService } from '../../core/services/product.service';
import { CommonModule } from '@angular/common';




@Component({
  selector: 'app-home',
  standalone: true,
  imports: [ProductCardComponent, HeaderComponent, CommonModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss'
})
export class HomeComponent {

  ngOnInit(): void {
    this.loadProducts();
  }
  
  products: Product[] = [];
  constructor(private productService: ProductService) { }
  loadProducts(): void {
    this.productService.allproducts().subscribe({
      next: (data) => this.products = data,
      error: (err) => console.error('Lấy product lỗi:', err),
    });
  }
}
