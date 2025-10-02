import { CommonModule } from '@angular/common';
import { Component, Input, Output, EventEmitter, NgModule, OnChanges } from '@angular/core';
import { NgModel } from '@angular/forms';

@Component({
  selector: 'app-pagination',
  templateUrl: './pagination.component.html',
  standalone: true,
  imports: [CommonModule],
})
export class PaginationComponent implements OnChanges {
  @Input() currentPage: number = 1;
  @Input() totalPages: number = 1;
  @Input() maxPages: number = 5;
  @Output() pageChange = new EventEmitter<number>();

  pages: number[] = [];

  ngOnChanges() { this.updatePages(); }

  updatePages() {
    let start = Math.max(this.currentPage - Math.floor(this.maxPages / 2), 1);
    let end = start + this.maxPages - 1;
    if (end > this.totalPages) { end = this.totalPages; start = Math.max(end - this.maxPages + 1, 1); }
    this.pages = Array.from({length: end-start+1}, (_, i) => i + start);
  }

  changePage(page: number) {
    if (page >= 1 && page <= this.totalPages && page !== this.currentPage) {
      this.pageChange.emit(page);
    }
  }
}
