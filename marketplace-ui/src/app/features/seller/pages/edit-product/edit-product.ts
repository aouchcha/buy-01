import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { Navbar } from '../../../../layout/navbar/navbar';
import { Product } from '../../../../core/services/product';
import { ToastService } from '../../../../core/services/toast.service';

@Component({
  selector: 'app-edit-product',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink, Navbar],
  templateUrl: './edit-product.html',
  styleUrl: './edit-product.scss',
})
export class EditProduct implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly productService = inject(Product);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);

  private productId!: number;

  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly notFound = signal(false);

  readonly editProductForm = this.fb.group({
    name: ['', Validators.required],
    description: ['', Validators.required],
    price: [null as number | null, [Validators.required, Validators.min(0.01)]],
    quantity: [null as number | null, [Validators.required, Validators.min(1)]],
  });

  ngOnInit(): void {
    this.productId = Number(this.route.snapshot.paramMap.get('id'));

    this.productService.getById(this.productId).subscribe({
      next: (product) => {
        this.editProductForm.patchValue({
          name: product.name,
          description: product.description,
          price: product.price,
          quantity: product.quantity,
        });
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.notFound.set(true);
        this.toast.error('Unable to load this product.');
      },
    });
  }

  submit(): void {
    if (this.editProductForm.invalid) {
      this.editProductForm.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    const { name, description, price, quantity } = this.editProductForm.getRawValue();

    this.productService
      .update(this.productId, {
        name: name!,
        description: description!,
        price: price!,
        quantity: quantity!,
      })
      .subscribe({
        next: () => {
          this.saving.set(false);
          this.toast.success('Product updated.');
          this.router.navigate(['/seller']);
        },
        error: () => {
          this.saving.set(false);
          this.toast.error('Unable to update product.');
        },
      });
  }
}
