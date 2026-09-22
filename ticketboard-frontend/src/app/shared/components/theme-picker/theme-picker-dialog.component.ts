import { Component, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule } from '@angular/material/dialog';
import { ThemePickerComponent } from './theme-picker.component';

@Component({
  selector: 'tb-theme-picker-dialog',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatButtonModule, MatDialogModule, ThemePickerComponent],
  templateUrl: './theme-picker-dialog.component.html',
  styleUrls: ['./theme-picker-dialog.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ThemePickerDialogComponent {}