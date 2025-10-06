import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Output } from '@angular/core';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-sidebar-admin',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './sidebar-admin.component.html',
  styleUrls: ['./sidebar-admin.component.scss']
})
export class SidebarAdminComponent {
  selectedView: 'products' | 'categories' | 'settings' | 'exports' | 'orders' | null = null;

  @Output() viewChange = new EventEmitter<'products' | 'categories' | 'settings' | 'exports' | 'orders'>();

  selectView(view: 'products' | 'categories' | 'settings' | 'exports' | 'orders') {
    this.selectedView = view;
    this.viewChange.emit(view);
  }
}
