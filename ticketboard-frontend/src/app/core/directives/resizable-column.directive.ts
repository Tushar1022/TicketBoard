import { AfterViewInit, Directive, ElementRef, Input, OnDestroy } from '@angular/core';

@Directive({
  selector: 'th[appResizable]',
  standalone: true,
})
export class ResizableColumnDirective implements AfterViewInit, OnDestroy {
  @Input() minWidth = 60;

  private thEl!: HTMLElement;
  private handleEl!: HTMLElement;
  private startX = 0;
  private startWidth = 0;

  constructor(private el: ElementRef<HTMLElement>) {}

  ngAfterViewInit(): void {
    this.thEl = this.el.nativeElement;
    this.thEl.style.position = 'relative';

    this.handleEl = document.createElement('div');
    this.handleEl.className = 'col-resize-handle';
    this.handleEl.style.cssText =
      'position:absolute;top:0;right:-3px;width:6px;height:100%;cursor:col-resize;z-index:10;';
    this.thEl.appendChild(this.handleEl);
    this.handleEl.addEventListener('mousedown', this.onMouseDown);
  }

  private onMouseDown = (e: MouseEvent): void => {
    e.preventDefault();
    e.stopPropagation();
    this.startX = e.clientX;
    this.startWidth = this.thEl.offsetWidth;
    document.addEventListener('mousemove', this.onMouseMove);
    document.addEventListener('mouseup', this.onMouseUp);
    document.body.style.cursor = 'col-resize';
    document.body.style.userSelect = 'none';
  };

  private onMouseMove = (e: MouseEvent): void => {
    const width = Math.max(this.minWidth, this.startWidth + e.clientX - this.startX);
    this.thEl.style.width = width + 'px';
    this.thEl.style.minWidth = width + 'px';
  };

  private onMouseUp = (): void => {
    document.removeEventListener('mousemove', this.onMouseMove);
    document.removeEventListener('mouseup', this.onMouseUp);
    document.body.style.cursor = '';
    document.body.style.userSelect = '';
  };

  ngOnDestroy(): void {
    document.removeEventListener('mousemove', this.onMouseMove);
    document.removeEventListener('mouseup', this.onMouseUp);
    this.handleEl?.removeEventListener('mousedown', this.onMouseDown);
  }
}
