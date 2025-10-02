import { Component } from '@angular/core';

import { SidebarAdminComponent } from '../../shared/admin-layout/sidebar-admin/sidebar-admin.component';
import { NavbarAdminComponent } from '../../shared/admin-layout/navbar-admin/navbar-admin.component';
import { ProductListComponent } from '../../components/product/product-list/product-list.component';
import { CommonModule } from '@angular/common';
import { CategoryListComponent } from '../../components/category/category-list/category-list.component';
import { ToastComponent } from '../../shared/toast/toast.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [SidebarAdminComponent, NavbarAdminComponent, ProductListComponent, CommonModule, CategoryListComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss'
})
export class DashboardComponent {
  // Giá trị mặc định có thể rỗng hoặc 'products'
  currentView: 'products' | 'categories' | 'settings' | 'exports' | null = null;

  show(view: 'products' | 'categories' | 'settings' | 'exports') {
    this.currentView = view;
  }
}
