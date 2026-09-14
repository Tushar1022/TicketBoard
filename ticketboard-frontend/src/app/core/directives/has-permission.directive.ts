import { Directive, Input, OnInit, OnDestroy, TemplateRef, ViewContainerRef } from '@angular/core';
import { AuthService } from '../services/auth.service';

@Directive({
  selector: '[hasPermission]',
  standalone: true
})
export class HasPermissionDirective implements OnInit, OnDestroy {
  private permission: string = '';
  private alreadyRendered = false;

  @Input()
  set hasPermission(permission: string) {
    this.permission = permission;
    this.evaluate();
  }

  constructor(
    private templateRef: TemplateRef<any>,
    private viewContainer: ViewContainerRef,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.evaluate();
  }

  ngOnDestroy(): void {
    this.viewContainer.clear();
  }

  private evaluate(): void {
    if (this.authService.hasPermission(this.permission)) {
      if (!this.alreadyRendered) {
        this.viewContainer.createEmbeddedView(this.templateRef);
        this.alreadyRendered = true;
      }
    } else {
      this.viewContainer.clear();
      this.alreadyRendered = false;
    }
  }
}