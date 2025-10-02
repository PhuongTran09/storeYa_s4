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
  selectedView: 'products' | 'categories' | 'settings' | 'exports' | null = null;

  @Output() viewChange = new EventEmitter<'products' | 'categories' | 'settings' | 'exports'>();

  selectView(view: 'products' | 'categories' | 'settings' | 'exports') {
    this.selectedView = view;
    this.viewChange.emit(view);
  }
}
