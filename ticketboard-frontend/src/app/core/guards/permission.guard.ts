import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const permissionGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const requiredPermission = route.data?.['permission'] as string;
  const requiredPermissions = route.data?.['permissions'] as string[];
  const requireAll = route.data?.['requireAll'] as boolean ?? false;

  if (!authService.isAuthenticated()) {
    router.navigate(['/login'], { queryParams: { returnUrl: state.url } });
    return false;
  }

  if (!requiredPermission && !requiredPermissions) {
    return true;
  }

  let hasAccess = false;

  if (requiredPermission) {
    hasAccess = authService.hasPermission(requiredPermission);
  } else if (requiredPermissions) {
    if (requireAll) {
      hasAccess = authService.hasAllPermissions(...requiredPermissions);
    } else {
      hasAccess = authService.hasAnyPermission(...requiredPermissions);
    }
  }

  if (!hasAccess) {
    router.navigate(['/unauthorized']);
    return false;
  }

  return true;
};