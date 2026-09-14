import os
import re

base_dir = '/Users/ashwinisunilshinde/Documents/Tushar/TicketBoard/ticketboard-frontend/src/app'

# H3: Add takeUntilDestroyed
components_to_fix = [
    'features/projects/project-detail/project-detail.component.ts',
    'features/work-items/work-item-board/work-item-board.component.ts',
    'features/dashboard/executive/executive-dashboard.component.ts',
    'features/dashboard/developer/my-workspace.component.ts',
    'features/admin/user-management/user-list.component.ts'
]

def add_take_until_destroyed(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    # Imports
    if "import { DestroyRef, inject }" not in content and "import { DestroyRef" not in content:
        content = re.sub(r"import { (.*?) } from '@angular/core';", r"import { \1, DestroyRef, inject } from '@angular/core';", content)
    
    if "takeUntilDestroyed" not in content:
        content = re.sub(r"(import .*?;)", r"\1\nimport { takeUntilDestroyed } from '@angular/core/rxjs-interop';", content, count=1)

    # Class property
    if "private destroyRef = inject(DestroyRef);" not in content:
        content = re.sub(r"(export class .*? implements .*?{|export class .*? {)", r"\1\n  private destroyRef = inject(DestroyRef);", content)

    # Replace subscribe
    def subscribe_replacer(match):
        pre = match.group(1)
        if ".pipe(" in pre:
            return pre.replace(".pipe(", ".pipe(takeUntilDestroyed(this.destroyRef), ") + "subscribe("
        else:
            return pre + ".pipe(takeUntilDestroyed(this.destroyRef)).subscribe("
            
    content = re.sub(r"(.*?)\.subscribe\(", subscribe_replacer, content)
    
    # Empty ngOnDestroy
    content = re.sub(r"ngOnDestroy\(\) {\s*}\s*", "", content)
    content = re.sub(r"ngOnDestroy\(\): void {\s*}\s*", "", content)

    with open(filepath, 'w') as f:
        f.write(content)

for comp in components_to_fix:
    add_take_until_destroyed(os.path.join(base_dir, comp))

# H4: Clear setInterval Timers
def fix_timer(filepath):
    with open(filepath, 'r') as f:
        content = f.read()

    if "implements" in content and "OnDestroy" not in content:
        content = re.sub(r"(export class .*? implements )(.*? {)", r"\1OnDestroy, \2", content)
    elif "implements" not in content:
        content = re.sub(r"(export class .*? )({)", r"\1implements OnDestroy \2", content)

    if "import { OnDestroy" not in content and "import { Component, OnDestroy" not in content:
        content = re.sub(r"import { (.*?) } from '@angular/core';", r"import { \1, OnDestroy } from '@angular/core';", content)

    if "ngOnDestroy" not in content:
        content = re.sub(r"}\s*$", "\n  ngOnDestroy(): void {\n    if (this.timerInterval) { clearInterval(this.timerInterval); }\n  }\n}", content)
    else:
        content = re.sub(r"(ngOnDestroy\(\).*?{)", r"\1\n    if (this.timerInterval) { clearInterval(this.timerInterval); }", content, flags=re.DOTALL)

    with open(filepath, 'w') as f:
        f.write(content)

fix_timer(os.path.join(base_dir, 'features/work-items/work-item-board/work-item-board.component.ts'))
fix_timer(os.path.join(base_dir, 'shared/components/task-detail-dialog/task-detail-dialog.component.ts'))

# H12: Fix LookupDataService
lookup_path = os.path.join(base_dir, 'core/services/lookup-data.service.ts')
with open(lookup_path, 'r') as f:
    content = f.read()
content = content.replace("import { Observable, of } from 'rxjs';", "import { Observable, of, throwError } from 'rxjs';")
content = re.sub(r"catchError\(\(\) => \{[^}]*return of\(\{ success: (true|false), message: '[^']*'(, data: [^,]*)?, timestamp: [^}]*\}\);\s*\}\)", r"catchError(err => { console.error('LookupData error:', err); return throwError(() => err); })", content, flags=re.DOTALL)
content = re.sub(r"catchError\(\(\) => \{.*?(return of\(\{.*?\}\);)\s*\}\)", r"catchError(err => { console.error('LookupData error:', err); return throwError(() => err); })", content, flags=re.DOTALL)
with open(lookup_path, 'w') as f:
    f.write(content)

# M7: Fix isLoading Stuck in ProjectDetailComponent
proj_detail_path = os.path.join(base_dir, 'features/projects/project-detail/project-detail.component.ts')
with open(proj_detail_path, 'r') as f:
    content = f.read()
    
if "forkJoin" not in content:
    content = content.replace("import { ActivatedRoute, RouterModule } from '@angular/router';", "import { ActivatedRoute, RouterModule } from '@angular/router';\nimport { forkJoin } from 'rxjs';")

new_load = """  public loadAllProjectData(): void {
    this.isLoading.set(true);
    this.loadError.set('');

    forkJoin({
      project: this.projectService.getProjectById(this.projectId),
      reqs: this.requirementService.getAllRequirements({ projectId: this.projectId }),
      tasks: this.workItemService.getWorkItems({ projectId: this.projectId }),
      time: this.timeTrackingService.getTimeEntriesByProject(this.projectId),
      releases: this.releaseService.getReleases(this.projectId),
      risks: this.riskService.getRisks(this.projectId),
      issues: this.riskService.getIssuesByProject(this.projectId)
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (res: any) => {
        if (res.project.success && res.project.data) this.project.set(res.project.data);
        if (res.reqs.success && res.reqs.data) this.requirements.set(res.reqs.data);
        if (res.tasks.success && res.tasks.data) {
          this.tasks.set(res.tasks.data);
          if (res.tasks.data.length && !this.selectedTask()) this.selectedTask.set(res.tasks.data[0]);
        }
        if (res.time.success && res.time.data) this.timeEntries.set(res.time.data);
        if (res.releases.success && res.releases.data) this.releases.set(res.releases.data);
        if (res.risks.success && res.risks.data) this.risks.set(res.risks.data);
        if (res.issues.success && res.issues.data) this.issues.set(res.issues.data);

        this.loadComments();
        this.loadAuditLogs();
        this.loadProjectDocuments();
        this.isLoading.set(false);
      },
      error: () => {
        this.loadError.set('Failed to load project details.');
        this.isLoading.set(false);
      }
    });
  }"""

# Using regex to replace the old loadAllProjectData method
content = re.sub(r"public loadAllProjectData\(\): void \{.*?(?=\n\s+public loadComments\(\): void \{)", new_load + "\n\n", content, flags=re.DOTALL)

with open(proj_detail_path, 'w') as f:
    f.write(content)

