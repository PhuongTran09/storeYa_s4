import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Product, ProductService } from '../../../core/services/product.service';
import { environment } from '../../../../environment/api.config';
import { Category, CategoryService } from '../../../core/services/category.service';
import { ToastService } from '../../../core/services/toast.service';
import { firstValueFrom } from 'rxjs';
import { ConfirmDialogComponent } from '../../../shared/confirm-dialog/confirm-dialog.component';

interface FileWithPreview extends File {
  preview: string;
}


@Component({
  selector: 'app-product-add',
  standalone: true,
  imports: [FormsModule, CommonModule, ConfirmDialogComponent],
  templateUrl: './product-add.component.html',
  styleUrls: ['./product-add.component.scss']
})
export class ProductAddComponent {
  @Input() product: Product | null = null;
  @Output() saved = new EventEmitter<void>();
  @Output() closeForm = new EventEmitter<void>();

  formData: any = {
    name: '',
    description: '',
    price: "",
    stock: "",
    imageUrls: [] as string[],
    categoryId: ""
  };

  selectedFiles: FileWithPreview[] = [];
  uploading = false;

  categories: Category[] = [];
  showConfirm = false;





  constructor(
    private productService: ProductService,
    private categoryService: CategoryService,
    private toast: ToastService

  ) { }





  ngOnInit() {
    if (this.product) {
      this.formData = {
        name: this.product.name,
        description: this.product.description ?? '',
        price: this.product.price,
        stock: this.product.stock,
        imageUrls: this.product.imageUrls ?? [],
        categoryId: this.product.category?.id ?? 1
      };

      // Load ảnh cũ vào selectedFiles (size = 0 -> ảnh cũ)
      this.selectedFiles = (this.product.imageUrls ?? []).map(url => ({
        name: url.split('/').pop() || 'image',
        preview: url,
        size: 0,
        type: 'image/*'
      } as FileWithPreview));
    }

    this.categoryService.getAll().subscribe({
      next: (res) => this.categories = res,
      error: (err) => console.error('Lỗi load categories', err)
    });
  }

  async uploadFilesToCloudinary(): Promise<string[]> {
    const filesToUpload = this.selectedFiles.filter(f => f.size > 0); // chỉ file mới

    const uploadPromises = filesToUpload.map(file => {
      const data = new FormData();
      data.append('file', file);
      data.append('upload_preset', environment.cloudinary.uploadPreset);
      data.append('folder', environment.cloudinary.folder);

      return fetch(`https://api.cloudinary.com/v1_1/${environment.cloudinary.cloudName}/image/upload`, {
        method: 'POST',
        body: data
      })
        .then(res => {
          if (!res.ok) throw new Error('Upload lỗi Cloudinary');
          return res.json();
        })
        .then(json => json.secure_url);
    });

    return Promise.all(uploadPromises);
  }


  saveProduct() {
    if (!this.formData.name?.trim()) return;
    this.showConfirm = true;
  }
  async onConfirmSave() {
    this.uploading = true;
    this.showConfirm = false;
    this.toast.show(this.product ? 'Updating product...' : 'Creating product...', 'loading');

    try {
      // 1. Giữ ảnh cũ
      const keptOldUrls = this.selectedFiles
        .filter(f => f.size === 0)
        .map(f => f.preview);

      // 2. Upload ảnh mới
      const uploadedUrls = await this.uploadFilesToCloudinary();

      const payload: Partial<Product> = {
        name: this.formData.name,
        description: this.formData.description,
        price: this.formData.price,
        stock: this.formData.stock,
        imageUrls: [...keptOldUrls, ...uploadedUrls],
        category: { id: this.formData.categoryId }
      };

      if (this.product) {
        await firstValueFrom(this.productService.updateProduct(this.product.id!, payload));
        this.toast.show('Product updated!', 'success');
      } else {
        await firstValueFrom(this.productService.addProduct(payload));
        this.toast.show('Product created!', 'success');
      }

      this.saved.emit();
      this.closeForm.emit();
    } catch (err) {
      console.error('Save product lỗi:', err);
      this.toast.show('Save failed!', 'error');
    } finally {
      this.uploading = false;
    }
  }




  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement | null;
    if (input?.files && input.files.length > 0) {
      const newFiles: FileWithPreview[] = Array.from(input.files).map(file =>
        Object.assign(file, { preview: URL.createObjectURL(file) })
      );
      this.selectedFiles = [...this.selectedFiles, ...newFiles].slice(0, 5);
      input.value = ''; // input chắc chắn ko null nhờ if kiểm tra
    }
  }

  onCancelSave() {
    this.showConfirm = false;
  }



  async removeFile(index: number) {
    const file = this.selectedFiles[index];

    // Nếu file đã có URL (ảnh cũ hoặc đã upload)
    if (file.preview.startsWith('http')) {
      try {
        await this.productService.deleteCloudinaryFile(file.preview).toPromise();
        console.log('Xóa ảnh trên Cloudinary thành công:', file.preview);
      } catch (err) {
        console.error('Xóa ảnh trên Cloudinary lỗi:', err);
      }
    }

    // Xóa khỏi selectedFiles local
    this.selectedFiles.splice(index, 1);
  }
  cancel() {
    this.closeForm.emit();
  }

}
