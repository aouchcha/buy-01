import {
  AfterViewInit,
  Component,
  ElementRef,
  OnInit,
  QueryList,
  ViewChildren,
  inject,
  signal,
} from '@angular/core';

import { Navbar } from '../../../../layout/navbar/navbar';
import { ProductDto } from '../../../../core/models/product';
import { Product as ProductService } from '../../../../core/services/product';

@Component({
  selector: 'app-product-list',
  imports: [Navbar],
  templateUrl: './product-list.html',
  styleUrl: './product-list.scss',
})
export class ProductList implements OnInit {
  private readonly productService = inject(ProductService);

  readonly products = signal<ProductDto[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);


  ngOnInit(): void {
    this.productService.getAll().subscribe({
      next: (products) => {        
        this.products.set(products);
        console.log(this.products());
        
        this.loading.set(false);
      },
      error: (error) => {
        console.log(error);
        this.error.set('Could not load birds right now. Please try again later.');
        this.loading.set(false);
      },
    });
  }

 
}