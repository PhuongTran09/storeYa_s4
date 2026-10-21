import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CartItem } from '../../../core/services/cart.service';
import { VndPipe } from '../../../shared/pipes/truncate.pipe';
import { RouterLink } from "@angular/router";

@Component({
  selector: 'app-cart-item',
  standalone: true,
  imports: [CommonModule, VndPipe, RouterLink],
  templateUrl: './cart-item.component.html',
  styleUrls: ['./cart-item.component.scss']
})
export class CartItemComponent {
  @Input() item!: CartItem;
  @Output() updateQty = new EventEmitter<{ id: number; qty: number }>();
  @Output() remove = new EventEmitter<number>();

  decreaseQty() {
    if (this.item.quantity > 1) {
      this.updateQty.emit({ id: this.item.id, qty: this.item.quantity - 1 });
    }
  }

  increaseQty() {
    this.updateQty.emit({ id: this.item.id, qty: this.item.quantity + 1 });
  }

  onRemove() {
    this.remove.emit(this.item.id);
  }
}
