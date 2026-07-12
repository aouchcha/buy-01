import { Routes } from '@angular/router';

import { ProductList } from "./features/products/pages/product-list/product-list";
import { Login } from "./features/auth/pages/login/login"
import { Register } from "./features/auth/pages/register/register"
import { noAuthGuard } from './core/guards/no-auth-guard';
import { Profile } from './features/profile/pages/profile/profile';
import { authGuard } from './core/guards/auth-guard';
import { roleGuard } from './core/guards/role-guard';
import { Dashboard } from './features/seller/pages/dashboard/dashboard'
import { Role } from './core/models/user'
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
    {
        path: 'seller',
        canActivate: [authGuard, roleGuard],
        data: { roles: ['seller'] },
        component: Dashboard
    }
];
