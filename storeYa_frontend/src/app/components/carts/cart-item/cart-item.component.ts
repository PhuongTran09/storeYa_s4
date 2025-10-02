import { CommonModule } from '@angular/common';
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-cart-item',
  templateUrl: './cart-item.component.html',
  styleUrls: ['./cart-item.component.scss'],
  standalone: true,
  imports: [CommonModule, FormsModule],
})
export class CartItemComponent {
  @Input() item!: { id: number; name: string; price: number; quantity: number; image?: string };
  @Output() removeItem = new EventEmitter<number>();
  @Output() quantityChange = new EventEmitter<{ id: number; qty: number }>();

  remove() {
    this.removeItem.emit(this.item.id);
  }

  onQtyChange(qty: number) {
    if (qty > 0) this.quantityChange.emit({ id: this.item.id, qty });
  }
}
