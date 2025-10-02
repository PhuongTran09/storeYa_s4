import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'truncate',
  standalone: true 
})
export class TruncatePipe implements PipeTransform {
  transform(value: string | null | undefined, limit: number = 300, ellipsis: string = '...'): string {
    if (!value) return '';
    value = value.trim();
    return value.length > limit ? value.substring(0, limit) + ellipsis : value;
  }
}
@Pipe({ name: 'vnd', standalone: true })
export class VndPipe implements PipeTransform {
  transform(value: number | null | undefined): string {
    if (value == null) return '';
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND'
    }).format(value);
  }
}
