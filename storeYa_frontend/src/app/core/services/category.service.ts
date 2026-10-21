import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { apiUrl } from '../../../environment/api.config';

const BASE_URL = apiUrl.BASE_URL + '/categories';

export interface Category {
  id: number;
  name: string;
}

@Injectable({ providedIn: 'root' })
export class CategoryService {
  private http = inject(HttpClient);

  // Lấy tất cả category
  getAll(): Observable<Category[]> {
    return this.http.get<Category[]>(`${BASE_URL}/public/all`);
  }

  // Tạo mới category
  create(category: Partial<Category>): Observable<Category> {
    return this.http.post<Category>(BASE_URL, category);
  }

  // Update category
  update(id: number, category: Partial<Category>): Observable<Category> {
    return this.http.put<Category>(`${BASE_URL}/${id}`, category);
  }

  // Xóa category
  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${BASE_URL}/${id}`);
  }
}
