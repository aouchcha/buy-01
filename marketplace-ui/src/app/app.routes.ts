import { Routes } from '@angular/router';

import { ProductList } from "./features/products/pages/product-list/product-list";

export const routes: Routes = [
    {
    path: '',
        component: ProductList
    },
    // {
    //     path: 'login',
    //     component: LoginComponent
    // },
    // {
    //     path: 'register',
    //     component: RegisterComponent
    // },
    // {
    //     path: 'products/:id',
    //     component: ProductDetailsComponent
    // },
    // {
    //     path: 'seller',
    //     canActivate: [AuthGuard, RoleGuard],
    //     data: { role: 'SELLER' },
    //     component: DashboardComponent
    // }
];
