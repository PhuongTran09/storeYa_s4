import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { apiUrl } from '../../../environment/api.config';
import { PageResponse } from '../../shared/response/page-response';

const BASE_URL = apiUrl.BASE_URL + '/products';

export interface Product {
  id?: number;
  name: string;
  description?: string;
  price: number;
  stock: number;
  imageUrls?: string[];
  category?: { id: number; name?: string };
}


@Injectable({ providedIn: 'root' })
export class ProductService {
  private http = inject(HttpClient);
  allproducts(): Observable<Product[]> {
    return this.http.get<Product[]>(`${BASE_URL}/public/all`);
  }
  // Lấy tất cả product
  getProducts(page: number = 0, size: number = 5): Observable<PageResponse<Product>> {
    return this.http.get<PageResponse<Product>>(BASE_URL, {
      params: new HttpParams().set('page', page.toString()).set('size', size.toString())
    });
  }
  deleteProduct(id: number): Observable<void> {
    return this.http.delete<void>(`${BASE_URL}/${id}`);
  }
  addProduct(product: Partial<Product>): Observable<Product> {
    return this.http.post<Product>(`${BASE_URL}`, product);
  }


  updateProduct(id: number, product: Partial<Product>): Observable<Product> {
    return this.http.put<Product>(`${BASE_URL}/${id}`, product);
  }

  deleteCloudinaryFile(url: string) {
    return this.http.post(`${BASE_URL}/cloudinary/delete`, { url });


  }
  getProductById(id: number): Observable<Product> {
    return this.http.get<Product>(`${BASE_URL}/public/${id}`);
  }

  getProductsByCategory(categoryId?: number) {
    let url = `${BASE_URL}/products`;
    if (categoryId) {
      url += `?categoryId=${categoryId}`;
    }
    return this.http.get<Product[]>(url);
  }


}
