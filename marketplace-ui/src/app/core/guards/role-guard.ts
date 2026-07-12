// role.guard.ts
import { CanActivateFn } from '@angular/router';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { Auth } from '../services/auth';
import { Role } from '../models/user';

export const roleGuard: CanActivateFn = (route, state) => {
    const authService = inject(Auth);
    const router = inject(Router);

    const requiredRoles = route.data['roles'];
    const userRole = authService.getUserRole();
    console.log("===> requiredRoles", requiredRoles);
    console.log("===> userRole",userRole);

    if (!authService.isLoggedIn()) {
        router.navigate(['/login']);
        return false;
    }
    if (userRole != null) {
        if (requiredRoles && !requiredRoles.includes(userRole.toLowerCase())) {
            router.navigate(['/unauthorized']);
            return false;
        }
    } else{
        router.navigate(['/unauthorized']);
        return false;
    }

    return true;
};