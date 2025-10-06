import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { BehaviorSubject, tap } from 'rxjs';
import { apiUrl } from '../../../environment/api.config';

const BASE_URL = apiUrl.BASE_URL + '/carts';

export interface CartItem {
  id: number;
  productId: number;
  productName: string;
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

  constructor(private http: HttpClient) {}

  /** Chuẩn hóa dữ liệu item để không bị undefined */
  private mapCartItem(item: any): CartItem {
    const product = item.product ?? {};

    return {
      id: item.id,
      productId: item.productId ?? product.id,
      productName: item.productName ?? product.name ?? 'Sản phẩm không tên',
      price: item.price ?? product.price ?? 0,
      quantity: item.quantity ?? 1,
      image:
        item.image ??
        (product.images?.length ? product.images[0].url : 'assets/img/no-image.png')
    };
  }

  /** Load giỏ hàng */
  loadCart() {
    return this.http.get<{ items: any[] }>(`${BASE_URL}`).pipe(
      tap(res => {
        this.cartItems = (res?.items ?? []).map(i => this.mapCartItem(i));
        this.cartSubject.next(this.cartItems);
      })
    );
  }

  /** Thêm sản phẩm vào giỏ */
  addToCart(productId: number, quantity: number) {
    const params = new HttpParams()
      .set('productId', productId)
      .set('quantity', quantity);

    return this.http.post<{ items: any[] }>(`${BASE_URL}/items`, null, { params }).pipe(
      tap(res => {
        this.cartItems = (res?.items ?? []).map(i => this.mapCartItem(i));
        this.cartSubject.next(this.cartItems);
      })
    );
  }

  /** Cập nhật số lượng */
  updateQuantity(itemId: number, quantity: number) {
    const params = new HttpParams().set('quantity', quantity);

    return this.http.put<{ items: any[] }>(`${BASE_URL}/items/update/${itemId}`, null, { params }).pipe(
      tap(res => {
        this.cartItems = (res?.items ?? []).map(i => this.mapCartItem(i));
        this.cartSubject.next(this.cartItems);
      })
    );
  }

  /** Xóa item khỏi giỏ */
  removeItem(itemId: number) {
    return this.http.delete<{ items: any[] }>(`${BASE_URL}/items/${itemId}`).pipe(
      tap(res => {
        this.cartItems = (res?.items ?? []).map(i => this.mapCartItem(i));
        this.cartSubject.next(this.cartItems);
      })
    );
  }

  /** Xóa toàn bộ giỏ */
  clearCart() {
    return this.http.delete(`${BASE_URL}/clear`).pipe(
      tap(() => {
        this.cartItems = [];
        this.cartSubject.next([]);
      })
    );
  }

  /** Tổng tiền giỏ hàng */
  getTotal() {
    return this.cartItems.reduce((sum, item) => sum + item.price * item.quantity, 0);
  }
}
