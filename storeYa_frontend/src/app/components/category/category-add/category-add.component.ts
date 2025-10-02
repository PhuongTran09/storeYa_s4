import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Category, CategoryService } from '../../../core/services/category.service';
import { ToastService } from '../../../core/services/toast.service';
import { finalize } from 'rxjs/operators';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';

@Component({
  selector: 'app-category-add',
  standalone: true,
  imports: [CommonModule, FormsModule, ConfirmDialogComponent],
  templateUrl: './category-add.component.html',
  styleUrls: ['./category-add.component.scss']
})
export class CategoryAddComponent {
  @Input() category: Category | null = null;
  @Output() saved = new EventEmitter<void>();
  @Output() closeForm = new EventEmitter<void>();

  formData: Partial<Category> = { name: '' };
  saving = false;
  showConfirm = false;

  constructor(
    private categoryService: CategoryService,
    private toast: ToastService
  ) { }

  ngOnInit(): void {
    if (this.category) {
      this.formData = { name: this.category.name };
    }
  }

  // 👉 Bấm nút Save → bật confirm
  saveCategory() {
    if (!this.formData.name?.trim()) return;
    this.showConfirm = true;
  }

  onConfirmSave() {
    this.showConfirm = false;
    this.saving = true;
    this.toast.show(this.category ? 'Updating category...' : 'Creating category...', 'loading');

    const request$ = this.category
      ? this.categoryService.update(this.category.id, this.formData)
      : this.categoryService.create(this.formData);

    request$
      .pipe(finalize(() => (this.saving = false))) // luôn reset saving
      .subscribe({
        next: () => {
          this.toast.show(this.category ? 'Category updated!' : 'Category created!', 'success');
          this.saved.emit();
          this.closeForm.emit();
        },
        error: () => {
          this.toast.show(this.category ? 'Update failed!' : 'Create failed!', 'error');
        }
      });
  }

  onCancelSave() {
    this.showConfirm = false;
  }

  cancel() {
    this.closeForm.emit();
  }
}
