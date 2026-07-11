import { Routes } from '@angular/router';

import { ProductList } from "./features/products/pages/product-list/product-list";
import { Login } from "./features/auth/pages/login/login"
import { Register } from "./features/auth/pages/register/register"
import { noAuthGuard } from './core/guards/no-auth-guard';
import { Profile } from './features/profile/pages/profile/profile';
import { authGuard } from './core/guards/auth-guard';


export const routes: Routes = [
    {
    path: '',
        component: ProductList
    },
    {
        path: 'login',
        component: Login,
        canActivate: [noAuthGuard],
    },
    {
        path: 'register',
        component: Register,
         canActivate: [noAuthGuard],
    },
    {
        path: 'profile',
        component: Profile,
        canActivate: [authGuard],
    },
    // {
    //     path: 'seller',
    //     canActivate: [AuthGuard, RoleGuard],
    //     data: { role: 'SELLER' },
    //     component: DashboardComponent
    // }
];
