import { Component } from '@angular/core';
import { ProductCardComponent } from '../../components/product/product-card/product-card.component';
import { HeaderComponent } from '../../shared/layout/navbar/navbar.component';
import { Product, ProductService } from '../../core/services/product.service';
import { CommonModule } from '@angular/common';
import { Category, CategoryService } from '../../core/services/category.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [ProductCardComponent, HeaderComponent, CommonModule],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent {

  products: Product[] = [];
  selectedCategoryId: number | null = null; // category đang chọn
  filteredProducts: Product[] = [];
  categories: Category[] = [];

  constructor(private productService: ProductService, private categoryService: CategoryService) { }

  ngOnInit(): void {
    this.loadProducts();
    this.loadCategories();
  }

  loadCategories(): void {
    this.categoryService.getAll().subscribe({
      next: (data) => {
        this.categories = data;
      },
      error: (err) => console.error('Lấy category lỗi:', err),
    });
  }

  loadProducts(): void {
    this.productService.allproducts().subscribe({
      next: (data) => {
        this.products = data;
        this.filterProducts();
      },
      error: (err) => console.error('Lấy product lỗi:', err),
    });
  }

  // Lọc product theo category
  filterProducts(): void {
    if (!this.selectedCategoryId) {
      this.filteredProducts = this.products;
    } else {
      this.filteredProducts = this.products.filter(
        p => p.category?.id === this.selectedCategoryId
      );
    }
  }

  // Khi đổi category
  onCategoryChange(categoryId: number) {
    this.selectedCategoryId = categoryId;
    this.filterProducts();
  }
}
