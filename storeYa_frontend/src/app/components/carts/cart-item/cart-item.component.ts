import { CommonModule } from '@angular/common';
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { TruncatePipe, VndPipe } from '../../../shared/pipes/truncate.pipe';
import { CartItem, CartService } from '../../../core/services/cart.service';
import { RouterLink } from "@angular/router";

@Component({
  selector: 'app-cart-item',
  templateUrl: './cart-item.component.html',
  styleUrls: ['./cart-item.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule, VndPipe, RouterLink],
})
export class CartItemComponent {
  @Input() item!: { id: number; name: string; price: number; quantity: number; image?: string };
  @Output() removeItem = new EventEmitter<number>();
  @Output() quantityChange = new EventEmitter<{ id: number; qty: number }>();
  constructor(private cartService: CartService) { }
  remove() {
    this.removeItem.emit(this.item.id);
  }

  onQtyChange(qty: number) {
    if (qty > 0) this.quantityChange.emit({ id: this.item.id, qty });
  }

  changeQty(item: CartItem, newQty: number) {
    if (newQty < 1) return;

    this.cartService.updateQuantity(item.id, newQty).subscribe({
      next: () => {
        item.quantity = newQty; 
      },
      error: (err) => {
        console.error(err);
      }
    });
  }

}
