import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { MailService } from '../../core/services/mail.service';
import { AuthService } from '../../core/services/auth.service';
import { EmailMessage, DirectoryContact, MailAttachmentRef, MailFolder, SmtpConfig } from '../../core/models/api.models';
import { ToastService } from '../../shared/components/toast/toast.service';

export type MailFilter = 'ALL' | 'UNREAD' | 'STARRED';
export type MailSort = 'NEWEST' | 'OLDEST';
export type ComposeMode = 'NEW' | 'REPLY' | 'REPLY_ALL' | 'FORWARD';
export type RecipientField = 'TO' | 'CC' | 'BCC';

export interface RecipientChip {
  email: string;
  name?: string;
  registered?: boolean;
}

@Component({
  selector: 'app-mail',
  standalone: true,
  imports: [CommonModule, FormsModule, MatIconModule, MatTooltipModule, MatMenuModule],
  templateUrl: './mail.component.html',
  styleUrls: ['./mail.component.scss']
})
export class MailComponent implements OnInit {
  public activeFolder = computed(() => this.mailService.activeFolder());
  public messages = computed(() => this.mailService.messages());
  public selectedMessage = computed(() => this.mailService.selectedMessage());
  public counts = computed(() => this.mailService.counts());
  public smtpConfig = computed(() => this.mailService.smtpConfig());
  public isLoading = computed(() => this.mailService.isLoading());

  public searchQuery = signal<string>('');
  public messageFilter = signal<MailFilter>('ALL');
  public sortMode = signal<MailSort>('NEWEST');

  public filteredMessages = computed(() => {
    let list = this.messages();
    const q = this.searchQuery().trim().toLowerCase();
    if (q) {
      list = list.filter((m) =>
        (m.subject || '').toLowerCase().includes(q) ||
        (m.senderName || '').toLowerCase().includes(q) ||
        (m.senderEmail || '').toLowerCase().includes(q) ||
        (m.recipientTo || '').toLowerCase().includes(q) ||
        this.stripHtml(m.body).toLowerCase().includes(q)
      );
    }
    const filter = this.messageFilter();
    if (filter === 'UNREAD') {
      list = list.filter((m) => !m.read);
    } else if (filter === 'STARRED') {
      list = list.filter((m) => m.starred);
    }
    return [...list].sort((a, b) => {
      const cmp = a.createdAt < b.createdAt ? -1 : 1;
      return this.sortMode() === 'OLDEST' ? cmp : -cmp;
    });
  });

  public unreadInFolder = computed(() => this.filteredMessages().filter((m) => !m.read).length);

  // ── Multi-select state ─────────────────────────────────────
  public selectedIds = signal<Set<number>>(new Set());
  public selectedMessages = computed(() => this.messages().filter((m) => this.selectedIds().has(m.id)));
  public isAllSelected = computed(() => this.filteredMessages().length > 0 && this.filteredMessages().every((m) => this.selectedIds().has(m.id)));
  public hasSelection = computed(() => this.selectedIds().size > 0);

  public toggleSelect(msg: EmailMessage, event: Event): void {
    event.stopPropagation();
    const next = new Set(this.selectedIds());
    if (next.has(msg.id)) {
      next.delete(msg.id);
    } else {
      next.add(msg.id);
    }
    this.selectedIds.set(next);
  }

  public toggleSelectAll(): void {
    const visible = this.filteredMessages();
    const next = new Set(this.selectedIds());
    if (this.isAllSelected()) {
      visible.forEach((m) => next.delete(m.id));
    } else {
      visible.forEach((m) => next.add(m.id));
    }
    this.selectedIds.set(next);
  }

  public clearSelection(): void {
    this.selectedIds.set(new Set());
  }

  // ── Compose modal state ────────────────────────────────────
  public showComposeModal = signal<boolean>(false);
  public composeMode = signal<ComposeMode>('NEW');
  public showCcBcc = signal<boolean>(false);
  public composeForm: { subject: string; body: string } = {
    subject: '',
    body: ''
  };
  public isSending = signal<boolean>(false);

  // ── Registered mail directory for To/Cc/Bcc suggestions ────
  public directory = signal<DirectoryContact[]>([]);

  public recipientInput = signal<Record<'TO' | 'CC' | 'BCC', string>>({ TO: '', CC: '', BCC: '' });
  public recipientChips = signal<Record<'TO' | 'CC' | 'BCC', RecipientChip[]>>({ TO: [], CC: [], BCC: [] });

  // ── Compose attachments ────────────────────────────────────
  public attachments = signal<MailAttachmentRef[]>([]);
  public uploadingNames = signal<string[]>([]);
  public attachmentsSize = computed(() => this.attachments().reduce((sum, a) => sum + a.sizeBytes, 0));

  public composeTitle = computed(() => {
    switch (this.composeMode()) {
      case 'REPLY': return 'Reply';
      case 'REPLY_ALL': return 'Reply All';
      case 'FORWARD': return 'Forward';
      default: return 'Compose';
    }
  });

  // ── SMTP config modal state ────────────────────────────────
  public showSmtpModal = signal<boolean>(false);
  public smtpForm: SmtpConfig = {
    smtpHost: 'smtp.office365.com',
    smtpPort: 587,
    username: 'dev-mail@aurionpro.com',
    password: '',
    fromEmail: 'dev-mail@aurionpro.com',
    fromName: 'TicketBoard Mailer',
    encryptionType: 'TLS'
  };
  public testRecipientInput = signal<string>('');
  public isTestingSmtp = signal<boolean>(false);
  public isSavingSmtp = signal<boolean>(false);
  public testResultStatus = signal<'NONE' | 'SUCCESS' | 'FAILED'>('NONE');

  constructor(
    public mailService: MailService,
    public authService: AuthService,
    private toastService: ToastService
  ) {}

  ngOnInit(): void {
    this.mailService.loadMessages('INBOX');
    this.mailService.getDirectory().subscribe({
      next: (res) => {
        if (res && res.data) this.directory.set(res.data);
      }
    });
    this.mailService.getSmtpConfig().subscribe({
      next: (res) => {
        if (res && res.data) {
          this.smtpForm = { ...res.data, password: '' };
        }
      }
    });
  }

  public selectFolder(folder: MailFolder): void {
    this.searchQuery.set('');
    this.messageFilter.set('ALL');
    this.clearSelection();
    this.mailService.loadMessages(folder);
  }

  public folderList: Array<MailFolder | 'STARRED'> = ['INBOX', 'STARRED', 'SENT', 'DRAFTS', 'ARCHIVE', 'SPAM', 'TRASH'];

  public onFolderClick(f: MailFolder | 'STARRED'): void {
    if (f === 'STARRED') {
      this.selectStarredOnly();
    } else {
      this.selectFolder(f);
    }
  }

  public isSidebarActive(f: MailFolder | 'STARRED'): boolean {
    return f !== 'STARRED' && this.activeFolder() === f;
  }

  public selectStarredOnly(): void {
    this.searchQuery.set('');
    this.messageFilter.set('ALL');
    this.clearSelection();
    this.mailService.loadMessages('INBOX', undefined, true);
  }

  public refreshFolder(): void {
    const starredMode = false;
    this.mailService.loadMessages(this.activeFolder());
    if (starredMode) { /* no-op */ }
  }

  public selectMessage(msg: EmailMessage): void {
    this.mailService.getMessageById(msg.id).subscribe();
  }

  // ── List-level actions ─────────────────────────────────────
  public toggleStar(msg: EmailMessage, event: Event): void {
    event.stopPropagation();
    this.mailService.toggleStar(msg.id).subscribe();
  }

  public toggleRead(msg: EmailMessage, event: Event): void {
    event.stopPropagation();
    this.mailService.toggleRead(msg.id).subscribe();
  }

  public markAllRead(): void {
    const unread = this.filteredMessages().filter((m) => !m.read);
    if (unread.length === 0) {
      this.toastService.info('No unread emails in this folder.');
      return;
    }
    unread.forEach((m) => this.mailService.toggleRead(m.id).subscribe());
    this.toastService.success(`Marked ${unread.length} email${unread.length === 1 ? '' : 's'} as read.`);
  }

  // ── Single-message actions (reading pane) ──────────────────
  public toggleCurrentStar(): void {
    const msg = this.selectedMessage();
    if (msg) this.mailService.toggleStar(msg.id).subscribe();
  }

  public toggleCurrentRead(): void {
    const msg = this.selectedMessage();
    if (msg) this.mailService.toggleRead(msg.id).subscribe();
  }

  public moveMessage(folder: MailFolder): void {
    const msg = this.selectedMessage();
    if (!msg) return;
    this.mailService.updateFolder(msg.id, folder).subscribe({
      next: () => this.toastService.success(`Email moved to ${this.getFolderLabel(folder)}.`)
    });
  }

  public deleteCurrentMessage(): void {
    const msg = this.selectedMessage();
    if (!msg) return;
    this.mailService.deleteMessage(msg.id).subscribe({
      next: () => this.toastService.success('Email moved to Trash.')
    });
  }

  public prevMessage(): void {
    this.adjacentMessage(-1);
  }

  public nextMessage(): void {
    this.adjacentMessage(1);
  }

  private adjacentMessage(dir: number): void {
    const list = this.filteredMessages();
    if (list.length === 0) return;
    const current = this.selectedMessage();
    const idx = current ? list.findIndex((m) => m.id === current.id) : -1;
    const nextIdx = (idx + dir + list.length) % list.length;
    this.selectMessage(list[nextIdx]);
  }

  // ── Bulk actions ───────────────────────────────────────────
  public bulkToggleRead(read: boolean): void {
    const items = this.selectedMessages();
    const target = items.filter((m) => m.read !== read);
    if (target.length === 0) {
      this.toastService.info('No matching emails to update.');
      return;
    }
    target.forEach((m) => this.mailService.toggleRead(m.id).subscribe());
    this.toastService.success(`Marked ${target.length} email${target.length === 1 ? '' : 's'} as ${read ? 'read' : 'unread'}.`);
    this.clearSelection();
  }

  public bulkToggleStar(starred: boolean): void {
    const items = this.selectedMessages();
    const target = items.filter((m) => m.starred !== starred);
    if (target.length === 0) {
      this.toastService.info('No matching emails to update.');
      return;
    }
    target.forEach((m) => this.mailService.toggleStar(m.id).subscribe());
    this.toastService.success(`${starred ? 'Starred' : 'Unstarred'} ${target.length} email${target.length === 1 ? '' : 's'}.`);
    this.clearSelection();
  }

  public bulkMove(folder: MailFolder): void {
    const items = this.selectedMessages();
    if (items.length === 0) return;
    items.forEach((m) => this.mailService.updateFolder(m.id, folder).subscribe());
    this.toastService.success(`Moved ${items.length} email${items.length === 1 ? '' : 's'} to ${this.getFolderLabel(folder)}.`);
    this.clearSelection();
  }

  public bulkArchive(): void {
    this.bulkMove('ARCHIVE');
  }

  public bulkDelete(): void {
    const items = this.selectedMessages();
    if (items.length === 0) return;
    items.forEach((m) => this.mailService.deleteMessage(m.id).subscribe());
    this.toastService.success(`Moved ${items.length} email${items.length === 1 ? '' : 's'} to Trash.`);
    this.clearSelection();
  }

  // ── Compose ────────────────────────────────────────────────
  public openComposeModal(mode: ComposeMode = 'NEW', src?: EmailMessage): void {
    this.composeMode.set(mode);
    this.attachments.set([]);
    this.uploadingNames.set([]);
    this.recipientInput.set({ TO: '', CC: '', BCC: '' });
    this.recipientChips.set({ TO: [], CC: [], BCC: [] });
    if (src) {
      const originalBody = this.stripHtml(src.body);
      if (mode === 'REPLY' || mode === 'REPLY_ALL') {
        this.showCcBcc.set(mode === 'REPLY_ALL');
        this.seedChips('TO', src.senderEmail);
        if (mode === 'REPLY_ALL') this.seedChips('CC', src.recipientCc || '');
        this.composeForm = {
          subject: src.subject.startsWith('Re:') ? src.subject : `Re: ${src.subject}`,
          body: `\n\n\nOn ${this.formatLongDate(src.createdAt)}, ${src.senderName || src.senderEmail} wrote:\n> ${originalBody.split('\n').join('\n> ')}`
        };
      } else {
        this.showCcBcc.set(false);
        this.composeForm = {
          subject: src.subject.startsWith('Fwd:') ? src.subject : `Fwd: ${src.subject}`,
          body: `\n\n\n-------- Forwarded Message --------\nFrom: ${src.senderName || src.senderEmail} <${src.senderEmail}>\nDate: ${this.formatLongDate(src.createdAt)}\nTo: ${src.recipientTo}\nSubject: ${src.subject}\n\n${originalBody}`
        };
      }
    } else {
      this.showCcBcc.set(false);
      this.composeForm = {
        subject: '',
        body: ''
      };
    }
    this.showComposeModal.set(true);
  }

  // ── Recipient chips (multi-recipient, Gmail/Outlook style) ─
  public chips(field: RecipientField): RecipientChip[] {
    return this.recipientChips()[field];
  }

  public tokenOf(field: RecipientField): string {
    return (this.recipientInput()[field] || '').trim().toLowerCase();
  }

  public sugOptions(field: RecipientField): DirectoryContact[] {
    const token = this.tokenOf(field);
    const existing = this.recipientChips()[field].map((c) => c.email.toLowerCase());
    return this.directory()
      .filter((c) => c && c.email &&
        (!token
          || c.email.toLowerCase().includes(token)
          || (c.fullName || '').toLowerCase().includes(token)) &&
        !existing.includes(c.email.toLowerCase()))
      .slice(0, 6);
  }

  public isRegisteredToken(field: RecipientField): boolean {
    const token = this.tokenOf(field);
    if (!token) return false;
    return this.directory().some((c) => c && c.email && c.email.toLowerCase() === token);
  }

  public pickRecipient(field: RecipientField, contact: DirectoryContact): void {
    this.addChip(field, { email: contact.email, name: contact.fullName, registered: true });
    this.recipientInput.update((r) => ({ ...r, [field]: '' }));
  }

  public onRecipientInput(field: RecipientField, event: Event): void {
    this.recipientInput.update((r) => ({ ...r, [field]: (event.target as HTMLInputElement).value }));
  }

  public onRecipientKey(field: RecipientField, event: Event): void {
    const ke = event as KeyboardEvent;
    if (ke.key === 'Enter' || ke.key === ',') {
      ke.preventDefault();
      this.commitInput(field);
    } else if (ke.key === 'Backspace' && !this.tokenOf(field)) {
      const chips = this.recipientChips()[field];
      if (chips.length) {
        this.removeChip(field, chips.length - 1);
      }
    }
  }

  public commitInput(field: RecipientField): void {
    const token = this.tokenOf(field);
    if (!token) return;
    const registered = this.directory().find((c) => c && c.email && c.email.toLowerCase() === token);
    this.addChip(field, { email: token, name: registered?.fullName, registered: !!registered });
    this.recipientInput.update((r) => ({ ...r, [field]: '' }));
  }

  public removeChip(field: RecipientField, index: number): void {
    this.recipientChips.update((s) => ({
      ...s,
      [field]: s[field].filter((_, i) => i !== index)
    }));
  }

  public addChip(field: RecipientField, chip: RecipientChip): void {
    const exists = this.recipientChips()[field].some((c) => c.email.toLowerCase() === chip.email.toLowerCase());
    if (exists) return;
    this.recipientChips.update((s) => ({ ...s, [field]: [...s[field], chip] }));
  }

  public seedChips(field: RecipientField, commaString: string): void {
    commaString.split(',').map((s) => s.trim()).filter(Boolean).forEach((email) => {
      const contact = this.directory().find((c) => c && c.email.toLowerCase() === email.toLowerCase());
      this.addChip(field, { email, name: contact?.fullName, registered: !!contact });
    });
  }

  public buildRecipients(field: RecipientField): string {
    const chips = this.recipientChips()[field].map((c) => c.email);
    const value = (this.recipientInput()[field] || '').trim();
    if (value) chips.push(value);
    return chips.join(', ');
  }

  public openReply(msg: EmailMessage): void {
    this.openComposeModal('REPLY', msg);
  }

  public openReplyAll(msg: EmailMessage): void {
    this.openComposeModal('REPLY_ALL', msg);
  }

  public openForward(msg: EmailMessage): void {
    this.openComposeModal('FORWARD', msg);
  }

  public closeComposeModal(): void {
    this.showComposeModal.set(false);
  }

  public discardCompose(): void {
    this.showComposeModal.set(false);
    this.toastService.info('Compose discarded.');
  }

  public sendEmail(): void {
    const recipientTo = this.buildRecipients('TO');
    if (!recipientTo || !this.composeForm.subject || !this.composeForm.body) {
      this.toastService.error('Please fill in Recipient, Subject, and Body.');
      return;
    }
    this.isSending.set(true);
    this.mailService.sendEmail({
      recipientTo,
      recipientCc: this.showCcBcc() ? this.buildRecipients('CC') || undefined : undefined,
      recipientBcc: this.showCcBcc() ? this.buildRecipients('BCC') || undefined : undefined,
      subject: this.composeForm.subject,
      body: this.composeForm.body,
      attachments: this.attachments()
    }).subscribe({
      next: (res) => {
        this.isSending.set(false);
        if (res && res.success) {
          this.toastService.success('Email dispatched via SMTP.');
          this.closeComposeModal();
        }
      },
      error: () => {
        this.isSending.set(false);
        this.toastService.error('Failed to send email.');
      }
    });
  }

  public saveDraft(): void {
    const recipientTo = this.buildRecipients('TO');
    if (!recipientTo && !this.composeForm.subject) {
      this.toastService.error('Specify recipient or subject to save draft.');
      return;
    }
    this.isSending.set(true);
    this.mailService.saveDraft({
      recipientTo: recipientTo || '',
      recipientCc: this.showCcBcc() ? this.buildRecipients('CC') || undefined : undefined,
      recipientBcc: this.showCcBcc() ? this.buildRecipients('BCC') || undefined : undefined,
      subject: this.composeForm.subject,
      body: this.composeForm.body,
      attachments: this.attachments()
    }).subscribe({
      next: () => {
        this.isSending.set(false);
        this.toastService.success('Draft saved.');
        this.closeComposeModal();
      },
      error: () => this.isSending.set(false)
    });
  }

  // ── Attachment helpers ─────────────────────────────────────
  public onFilesPicked(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = input.files ? Array.from(input.files) : [];
    input.value = '';
    this.addAttachmentFiles(files);
  }

  public addAttachmentFiles(files: File[]): void {
    for (const file of files) {
      this.uploadingNames.update((list) => [...list, file.name]);
      this.mailService.uploadAttachment(file).subscribe({
        next: (res) => {
          this.uploadingNames.update((list) => list.filter((n) => n !== file.name));
          if (res && res.data) {
            this.attachments.update((list) => [...list, res.data]);
            if (res.data.isDrive) {
              this.toastService.info(`${this.formatBytes(file.size)} exceeds the 25 MB limit — linked via Drive instead.`);
            }
          }
        },
        error: () => {
          this.uploadingNames.update((list) => list.filter((n) => n !== file.name));
          this.toastService.error(`Failed to upload ${file.name}.`);
        }
      });
    }
  }

  public removeAttachment(ref: MailAttachmentRef): void {
    this.attachments.update((list) => list.filter((a) => a.id !== ref.id));
  }

  public formatBytes(bytes: number): string {
    if (!bytes && bytes !== 0) return '';
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
    return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;
  }

  public attachmentMeta(msg: EmailMessage): MailAttachmentRef[] {
    if (!msg || !msg.attachmentsJson) return [];
    try {
      const parsed = JSON.parse(msg.attachmentsJson);
      if (Array.isArray(parsed)) return parsed;
      return [];
    } catch {
      return [];
    }
  }

  public fileIcon(ref: MailAttachmentRef): string {
    const ext = (ref.fileName.split('.').pop() || '').toLowerCase();
    if (['pdf'].includes(ext)) return 'picture_as_pdf';
    if (['zip', 'rar', '7z', 'tar', 'gz'].includes(ext)) return 'folder_zip';
    if (['png', 'jpg', 'jpeg', 'gif', 'svg', 'webp', 'bmp'].includes(ext)) return 'image';
    if (['mp3', 'wav', 'ogg', 'm4a'].includes(ext)) return 'music_note';
    if (['mp4', 'mkv', 'mov', 'avi', 'webm'].includes(ext)) return 'movie';
    if (['txt', 'md', 'log', 'csv'].includes(ext)) return 'description';
    if (['doc', 'docx', 'rtf'].includes(ext)) return 'article';
    if (['xls', 'xlsx'].includes(ext)) return 'table_chart';
    if (['ppt', 'pptx'].includes(ext)) return 'co_present';
    return 'insert_drive_file';
  }

  public openAttachment(ref: MailAttachmentRef): void {
    if (ref.isDrive) {
      if (ref.driveUrl) window.open(ref.driveUrl, '_blank');
      return;
    }
    this.mailService.downloadAttachment(ref.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = ref.fileName;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: () => this.toastService.error('Attachment could not be downloaded.')
    });
  }

  // ── SMTP config ────────────────────────────────────────────
  public openSmtpModal(): void {
    this.testResultStatus.set('NONE');
    this.testRecipientInput.set(this.authService.currentUser()?.email || 'admin@ticketboard.com');
    this.showSmtpModal.set(true);
  }

  public closeSmtpModal(): void {
    this.showSmtpModal.set(false);
  }

  public saveSmtpSettings(): void {
    this.isSavingSmtp.set(true);
    this.mailService.saveSmtpConfig(this.smtpForm).subscribe({
      next: (res) => {
        this.isSavingSmtp.set(false);
        if (res && res.success) {
          this.toastService.success('SMTP configuration saved successfully.');
          this.closeSmtpModal();
        }
      },
      error: () => {
        this.isSavingSmtp.set(false);
        this.toastService.error('Failed to save SMTP configuration.');
      }
    });
  }

  public testSmtpConnection(): void {
    const testEmail = this.testRecipientInput().trim();
    if (!testEmail) {
      this.toastService.error('Please enter a test recipient email address.');
      return;
    }
    this.isTestingSmtp.set(true);
    this.testResultStatus.set('NONE');
    this.mailService.testSmtpConfig(testEmail, this.smtpForm).subscribe({
      next: (res) => {
        this.isTestingSmtp.set(false);
        if (res && res.data && res.data.success) {
          this.testResultStatus.set('SUCCESS');
          this.toastService.success('SMTP connection test succeeded!');
        } else {
          this.testResultStatus.set('FAILED');
          this.toastService.error('SMTP connection test failed.');
        }
      },
      error: () => {
        this.isTestingSmtp.set(false);
        this.testResultStatus.set('FAILED');
        this.toastService.error('SMTP test error occurred.');
      }
    });
  }

  // ── Presentational helpers ─────────────────────────────────
  public getFolderIcon(folder: string): string {
    switch (folder) {
      case 'INBOX': return 'inbox';
      case 'SENT': return 'send';
      case 'DRAFTS': return 'drafts';
      case 'STARRED': return 'star';
      case 'ARCHIVE': return 'archive';
      case 'SPAM': return 'report_problem';
      case 'TRASH': return 'delete';
      default: return 'folder';
    }
  }

  public getFolderLabel(folder: string): string {
    const map: Record<string, string> = {
      INBOX: 'Inbox',
      SENT: 'Sent',
      DRAFTS: 'Drafts',
      STARRED: 'Starred',
      ARCHIVE: 'Archive',
      SPAM: 'Spam',
      TRASH: 'Trash'
    };
    return map[folder] || folder;
  }

  public getFolderUnreadCount(folder: string): number {
    const c = this.counts();
    if (!c) return 0;
    switch (folder) {
      case 'INBOX': return c.inboxUnread;
      case 'SENT': return c.sentTotal;
      case 'DRAFTS': return c.draftsTotal;
      case 'STARRED': return c.starredTotal;
      case 'ARCHIVE': return c.archiveTotal;
      case 'SPAM': return c.spamTotal;
      case 'TRASH': return c.trashTotal;
      default: return 0;
    }
  }

  public isMessageUnread(msg: EmailMessage): boolean {
    return !msg.read;
  }

  public avatarInitial(msg: EmailMessage): string {
    const name = msg.senderName || msg.senderEmail || '?';
    return name.charAt(0).toUpperCase();
  }

  public avatarHue(msg: EmailMessage): string {
    const name = msg.senderName || msg.senderEmail || '';
    let sum = 0;
    for (let i = 0; i < name.length; i++) sum += name.charCodeAt(i);
    return `avatar-hue-${sum % 6}`;
  }

  public hasCc(msg: EmailMessage): boolean {
    return !!(msg.recipientCc && msg.recipientCc.trim());
  }

  public stripHtml(html: string): string {
    const doc = document.createElement('div');
    doc.innerHTML = html || '';
    return (doc.textContent || '').replace(/\s+/g, ' ').trim();
  }

  public formatShortDate(iso: string): string {
    if (!iso) return '';
    const d = new Date(iso);
    const today = new Date();
    const yest = new Date(today);
    yest.setDate(today.getDate() - 1);
    const sameYear = d.getFullYear() === today.getFullYear();
    if (d.toDateString() === today.toDateString()) {
      return d.toLocaleTimeString([], { hour: 'numeric', minute: '2-digit' });
    }
    if (d.toDateString() === yest.toDateString()) return 'Yesterday';
    return d.toLocaleDateString([], { month: 'short', day: 'numeric', ...(sameYear ? {} : { year: 'numeric' }) });
  }

  public formatLongDate(iso: string): string {
    if (!iso) return '';
    return new Date(iso).toLocaleString([], {
      weekday: 'short',
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: 'numeric',
      minute: '2-digit'
    });
  }

  public currentPosition(): number {
    const current = this.selectedMessage();
    if (!current) return 0;
    const idx = this.filteredMessages().findIndex((m) => m.id === current.id);
    return idx >= 0 ? idx + 1 : 0;
  }
}