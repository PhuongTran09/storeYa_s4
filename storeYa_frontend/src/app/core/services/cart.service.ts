import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, tap } from 'rxjs';
import { apiUrl } from '../../../environment/api.config';

const BASE_URL = apiUrl.BASE_URL + '/carts';

export interface CartItem {
  id: number;
  name: string;
  price: number;
  quantity: number;
  image?: string;
}

@Injectable({
  providedIn: 'root'
})
export class CartService {
  private cartItems: CartItem[] = [];
  private cartSubject = new BehaviorSubject<CartItem[]>([]);
  cart$ = this.cartSubject.asObservable();

  constructor(private http: HttpClient) { }

  /** Lấy giỏ hàng */
  loadCart() {
    return this.http.get<{ items: CartItem[] }>(`${BASE_URL}`).pipe(
      tap(res => {
        this.cartItems = res?.items ?? [];
        this.cartSubject.next(this.cartItems);
      })
    );
  }

  /** Thêm item vào giỏ */
  addToCart(productId: number, quantity: number, name?: string) {
    let params = new HttpParams()
      .set('productId', productId.toString())
      .set('quantity', quantity.toString());

    if (name) params = params.set('name', name);

    return this.http.post<{ items: CartItem[] }>(`${BASE_URL}/items`, null, { params }).pipe(
      tap(res => {
        this.cartItems = res.items;
        this.cartSubject.next(this.cartItems);
      })
    );
  }

  /** Update số lượng */
  updateQuantity(productId: number, quantity: number) {
    let params = new HttpParams()
      .set('productId', productId.toString())
      .set('quantity', quantity.toString());

    return this.http.put<{ items: CartItem[] }>(`${BASE_URL}/update`, null, { params }).pipe(
      tap(res => {
        this.cartItems = res?.items ?? [];
        this.cartSubject.next(this.cartItems);
      })
    );
  }

  /** Xoá item */
  removeFromCart(productId: number) {
    let params = new HttpParams().set('productId', productId.toString());

    return this.http.delete<{ items: CartItem[] }>(`${BASE_URL}/remove`, { params }).pipe(
      tap(res => {
        this.cartItems = res?.items ?? [];
        this.cartSubject.next(this.cartItems);
      })
    );
  }

  /** Clear giỏ */
  clearCart() {
    return this.http.delete(`${BASE_URL}/clear`).pipe(
      tap(() => {
        this.cartItems = [];
        this.cartSubject.next(this.cartItems);
      })
    );
  }

  /** Tổng tiền */
  getTotal() {
    return this.cartItems.reduce((sum, item) => sum + item.price * item.quantity, 0);
  }
}
