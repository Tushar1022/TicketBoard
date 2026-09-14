import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatDialogModule } from '@angular/material/dialog';
import { AuthService } from '../../../core/services/auth.service';
import { LookupDataService, CategoryInfo } from '../../../core/services/lookup-data.service';
import { LookupData } from '../../../core/models/api.models';

@Component({
  selector: 'app-master-data',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatSelectModule,
    MatTooltipModule,
    MatSnackBarModule,
    MatTableModule,
    MatSlideToggleModule,
    MatDialogModule
  ],
  templateUrl: './master-data.component.html',
  styleUrls: ['./master-data.component.scss']
})
export class MasterDataComponent implements OnInit {

  public categories = signal<CategoryInfo[]>([]);
  public selectedCategory = signal<CategoryInfo | null>(null);
  public data = signal<LookupData[]>([]);
  public loading = signal<boolean>(false);
  public searchQuery = signal<string>('');
  public categorySearchQuery = signal<string>('');

  public editingId = signal<number | null>(null);
  public editForm = signal<{ value: string; label: string; colorCode: string; displayOrder: number }>({
    value: '', label: '', colorCode: '#3b82f6', displayOrder: 1
  });

  public showAddForm = signal<boolean>(false);
  public newEntry = signal<{ value: string; label: string; colorCode: string; displayOrder: number }>({
    value: '', label: '', colorCode: '#3b82f6', displayOrder: 1
  });

  public displayedColumns = ['value', 'label', 'color', 'order', 'status', 'actions'];

  constructor(
    public lookupDataService: LookupDataService,
    private snackBar: MatSnackBar,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.categories.set(this.lookupDataService.appCategories);
    if (this.lookupDataService.appCategories.length > 0) {
      this.selectCategory(this.lookupDataService.appCategories[0]);
    }
  }

  getFilteredCategories(): CategoryInfo[] {
    const q = this.categorySearchQuery().toLowerCase().trim();
    if (!q) return this.categories();
    return this.categories().filter(c => 
      c.name.toLowerCase().includes(q) || 
      c.code.toLowerCase().includes(q) ||
      c.description.toLowerCase().includes(q)
    );
  }

  selectCategory(cat: CategoryInfo): void {
    this.selectedCategory.set(cat);
    this.showAddForm.set(false);
    this.editingId.set(null);
    this.loadData(cat.code);
  }

  loadData(categoryCode: string): void {
    this.loading.set(true);
    this.lookupDataService.getAll(categoryCode).subscribe({
      next: (res) => {
        const items = res.data || [];
        items.sort((a, b) => (a.displayOrder || 0) - (b.displayOrder || 0));
        this.data.set(items);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('Failed to load lookup items', 'Close', { duration: 3000 });
      }
    });
  }

  getFilteredData(): LookupData[] {
    const query = this.searchQuery().toLowerCase().trim();
    if (!query) return this.data();
    return this.data().filter(item =>
      item.value.toLowerCase().includes(query) ||
      item.label.toLowerCase().includes(query)
    );
  }

  startEdit(item: LookupData): void {
    this.editingId.set(item.id);
    this.editForm.set({
      value: item.value,
      label: item.label,
      colorCode: item.colorCode || '#3b82f6',
      displayOrder: item.displayOrder || 1
    });
  }

  cancelEdit(): void {
    this.editingId.set(null);
  }

  saveEdit(item: LookupData): void {
    const cat = this.selectedCategory();
    if (!cat) return;
    const form = this.editForm();

    this.lookupDataService.update(item.id, {
      category: cat.code,
      value: form.value,
      label: form.label,
      colorCode: form.colorCode,
      displayOrder: form.displayOrder,
      isActive: item.isActive
    }).subscribe({
      next: () => {
        this.snackBar.open('Lookup entry updated successfully', 'Close', { duration: 2500 });
        this.editingId.set(null);
        this.loadData(cat.code);
      },
      error: (err) => this.snackBar.open(err.error?.message || 'Update failed', 'Close', { duration: 4000 })
    });
  }

  toggleActive(item: LookupData): void {
    const cat = this.selectedCategory();
    if (!cat) return;

    this.lookupDataService.update(item.id, {
      category: cat.code,
      value: item.value,
      label: item.label,
      colorCode: item.colorCode,
      displayOrder: item.displayOrder,
      isActive: !item.isActive
    }).subscribe({
      next: () => this.loadData(cat.code),
      error: (err) => this.snackBar.open(err.error?.message || 'Status toggle failed', 'Close', { duration: 4000 })
    });
  }

  deleteEntry(item: LookupData): void {
    if (item.isDefault) {
      this.snackBar.open('System default items cannot be deleted', 'Close', { duration: 3000 });
      return;
    }

    const cat = this.selectedCategory();
    if (!cat) return;

    this.lookupDataService.delete(item.id).subscribe({
      next: () => {
        this.snackBar.open('Lookup entry removed', 'Close', { duration: 2500 });
        this.loadData(cat.code);
      },
      error: (err) => this.snackBar.open(err.error?.message || 'Delete failed', 'Close', { duration: 4000 })
    });
  }

  openAddForm(): void {
    this.showAddForm.set(true);
    this.newEntry.set({
      value: '',
      label: '',
      colorCode: '#3b82f6',
      displayOrder: (this.data().length || 0) + 1
    });
  }

  cancelAdd(): void {
    this.showAddForm.set(false);
  }

  addEntry(): void {
    const cat = this.selectedCategory();
    if (!cat) return;

    const entry = this.newEntry();
    if (!entry.value || !entry.label) {
      this.snackBar.open('System Code Value and Display Label are required', 'Close', { duration: 3000 });
      return;
    }

    const formattedValue = entry.value.toUpperCase().replace(/\s+/g, '_');

    this.lookupDataService.create({
      category: cat.code,
      value: formattedValue,
      label: entry.label,
      colorCode: entry.colorCode,
      displayOrder: entry.displayOrder
    }).subscribe({
      next: () => {
        this.snackBar.open('New lookup entry added successfully', 'Close', { duration: 2500 });
        this.showAddForm.set(false);
        this.loadData(cat.code);
      },
      error: (err) => this.snackBar.open(err.error?.message || 'Creation failed', 'Close', { duration: 4000 })
    });
  }

  isEditing(item: LookupData): boolean {
    return this.editingId() === item.id;
  }
}