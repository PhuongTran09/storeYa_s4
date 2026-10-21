import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CategoryAddComponent } from '../category-add/category-add.component';
import { Category, CategoryService } from '../../../core/services/category.service';
import { ToastService } from '../../../core/services/toast.service';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-category-list',
  standalone: true,
  imports: [CommonModule, CategoryAddComponent, ConfirmDialogComponent],
  templateUrl: './category-list.component.html',
  styleUrls: ['./category-list.component.scss']
})
export class CategoryListComponent implements OnInit {
  categories: Category[] = [];

  showAddForm = false;
  selectedCategory: Category | null = null;

  confirmDeleteId: number | null = null;

  constructor(
    private categoryService: CategoryService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.loadCategories();
  }

  loadCategories() {
    this.categoryService.getAll().subscribe({
      next: (res) => (this.categories = res),
      error: (err) => console.error('Load categories error:', err),
    });
  }

  openAddCategory() {
    this.selectedCategory = null;
    this.showAddForm = true;
  }

  openEditCategory(category: Category) {
    this.selectedCategory = { ...category };
    this.showAddForm = true;
  }

  closeAddCategory() {
    this.showAddForm = false;
  }

  reloadCategories() {
    this.loadCategories();
  }

  askDeleteCategory(id: number) {
    this.confirmDeleteId = id;
  }

  deleteCategory() {
    if (!this.confirmDeleteId) return;

    this.toast.show('Deleting category...', 'loading');
    this.categoryService.delete(this.confirmDeleteId).subscribe({
      next: () => {
        this.toast.show('Category deleted!', 'success');
        this.loadCategories();
        this.confirmDeleteId = null;
      },
      error: (err) => {
        console.error('Delete category error:', err);
        this.toast.show('Delete failed!', 'error');
        this.confirmDeleteId = null;
      },
    });
  }

  cancelDelete() {
    this.confirmDeleteId = null;
  }
}
