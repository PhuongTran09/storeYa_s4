import {HttpClient, HttpParams} from '@angular/common/http';
import {Injectable} from '@angular/core';
import {BehaviorSubject, throwError} from 'rxjs';
import {catchError, map, tap} from 'rxjs/operators';
import {apiUrl} from '../../../environment/api.config';

const BASE_URL = apiUrl.BASE_URL + '/carts';

// Interface cho CartItem, không đổi
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
  total$ = this.cart$.pipe(
    map(items => items.reduce((sum, item) => sum + item.price * item.quantity, 0))
  );


  cart$ = this.cartSubject.asObservable();
  /** * "Selector" cho tổng số lượng sản phẩm trong giỏ.
   */
  itemCount$ = this.cart$.pipe(
    map(items => items.reduce((sum, item) => sum + item.quantity, 0))
  );
  // State chính của giỏ hàng
  private cartSubject = new BehaviorSubject<CartItem[]>([]);

  constructor(private http: HttpClient) {
    // Tải giỏ hàng lần đầu khi service được khởi tạo
    this.loadCart().subscribe();
  }

  /** Tải giỏ hàng từ server */
  loadCart() {
    return this.http.get<{ items: any[] }>(`${BASE_URL}`).pipe(
      tap(res => this._updateCartState(res)),
      catchError(err => this._handleError(err))
    );
  }

  /** Thêm sản phẩm vào giỏ */
  addToCart(productId: number, quantity: number) {
    const params = new HttpParams()
      .set('productId', productId)
      .set('quantity', quantity);

    return this.http.post<{ items: any[] }>(`${BASE_URL}/items`, null, { params }).pipe(
      tap(res => this._updateCartState(res)),
      catchError(err => this._handleError(err))
    );
  }

  /** Cập nhật số lượng */
  updateQuantity(itemId: number, quantity: number) {
    const params = new HttpParams().set('quantity', quantity);

    return this.http.put<{ items: any[] }>(`${BASE_URL}/items/update/${itemId}`, null, { params }).pipe(
      tap(res => this._updateCartState(res)),
      catchError(err => this._handleError(err))
    );
  }

  /** Xóa item khỏi giỏ */
  removeItem(itemId: number) {
    return this.http.delete<{ items: any[] }>(`${BASE_URL}/items/${itemId}`).pipe(
      tap(res => this._updateCartState(res)),
      catchError(err => this._handleError(err))
    );
  }

  /** Xóa toàn bộ giỏ */
  clearCart() {
    return this.http.delete(`${BASE_URL}/clear`).pipe(
      tap(() => {
        // Xóa state ở local
        this.cartSubject.next([]);
      }),
      catchError(err => this._handleError(err))
    );
  }

  /** * Chuẩn hóa dữ liệu trả về từ API.
   * Đây là một best practice để phòng chống lỗi từ backend.
   */
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

  /** * Phương thức private để cập nhật state giỏ hàng và thông báo cho các subscribers.
   * Giúp tránh lặp code (DRY principle).
   */
  private _updateCartState(response: { items: any[] } | null) {
    const mappedItems = (response?.items ?? []).map(i => this.mapCartItem(i));
    this.cartSubject.next(mappedItems);
  }

  /**
   * Xử lý lỗi tập trung.
   */
  private _handleError(error: any) {
    console.error('Lỗi từ CartService:', error);
    // Có thể thêm logic để hiển thị thông báo cho người dùng
    return throwError(() => new Error('Có lỗi xảy ra, vui lòng thử lại.'));
  }
}
