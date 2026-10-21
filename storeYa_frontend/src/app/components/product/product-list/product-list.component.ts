import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Product, ProductService } from '../../../core/services/product.service';
import { TruncatePipe } from '../../../shared/pipes/truncate.pipe';
import { ProductAddComponent } from '../product-add/product-add.component';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';
import { ToastService } from '../../../core/services/toast.service';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule, TruncatePipe, ProductAddComponent, PaginationComponent, ConfirmDialogComponent],
  templateUrl: './product-list.component.html',
  styleUrls: ['./product-list.component.scss']
})
export class ProductListComponent implements OnInit {
  // Backend pagination
  pageNumber = 0;      // 0-based, dùng để gọi API
  pageSize = 8;        // số item mỗi trang
  totalPages = 0;      // tổng số trang từ backend

  // Frontend pagination
  currentPage = 1;     // 1-based, dùng cho PaginationComponent

  products: Product[] = [];

  showAddForm = false;
  selectedProduct: Product | null = null;
  confirmDeleteId: number | null = null;

  constructor(private productService: ProductService, private toast: ToastService) { }

  ngOnInit(): void {
    this.loadProducts(this.pageNumber);
  }

  loadProducts(page: number) {
    this.productService.getProducts(page, this.pageSize).subscribe(res => {
      if (res.content.length === 0 && page > 0) {
        this.loadProducts(page - 1);
        return;
      }
      this.products = res.content;
      this.pageNumber = res.pageNumber;
      this.totalPages = res.totalPages;
      this.currentPage = res.pageNumber + 1;
    });
  }

  // PaginationComponent emit
  onPageChange(page: number) {
    this.loadProducts(page - 1); // convert 1-based -> 0-based API
  }

  // Add/Edit product
  openAddProduct() {
    this.selectedProduct = null;
    this.showAddForm = true;
  }

  openEditProduct(product: Product) {
    this.selectedProduct = product;
    this.showAddForm = true;
  }

  closeAddProduct() {
    this.showAddForm = false;
  }

  reloadProducts() {
    this.loadProducts(this.pageNumber);
    this.showAddForm = false;
  }
  askDeleteProduct(id: number) {
    this.confirmDeleteId = id;
  }

  deleteProduct() {
    if (!this.confirmDeleteId) return;

    this.toast.show('Deleting product...', 'loading');
    this.productService.deleteProduct(this.confirmDeleteId).subscribe({
      next: () => {
        this.toast.show('Product deleted!', 'success');
        this.loadProducts(this.pageNumber);
        this.confirmDeleteId = null;
      },
      error: (err) => {
        console.error('Delete product error:', err);
        this.toast.show('Delete failed!', 'error');
        this.confirmDeleteId = null;
      },
    });
  }

  // Update product
  updateProduct(id: any, updatedData: Partial<Product>): void {
    this.productService.updateProduct(id, updatedData).subscribe({
      next: (updatedProduct) => {
        const index = this.products.findIndex(p => p.id === id);
        if (index !== -1) this.products[index] = updatedProduct;
      },
      error: (err) => console.error('Cập nhật product lỗi:', err)
    });
  }
  cancelDelete() {
    this.confirmDeleteId = null;
  }
}
