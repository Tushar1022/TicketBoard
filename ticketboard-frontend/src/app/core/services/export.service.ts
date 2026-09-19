import { Injectable } from '@angular/core';
import { ExportDocument, ExportFormat, ExportColumn, ExportTable, ExportOptions, ExportOrientation, ExportLayout } from '../models/report.models';
import { BRAND, nowStamp } from '../branding/brand.config';
import { AuthService } from './auth.service';

const DEFAULT_OPTIONS: Required<ExportOptions> = {
  orientation: 'landscape',
  layout: 'standard'
};

@Injectable({ providedIn: 'root' })
export class ExportService {
  constructor(private authService: AuthService) {}

  private get currentUserName(): string {
    return this.authService.currentUser()?.fullName || BRAND.author;
  }

  async export(doc: ExportDocument, format: ExportFormat, baseName?: string, options: ExportOptions = {}): Promise<void> {
    const opts = { ...DEFAULT_OPTIONS, ...options };
    const name = baseName || this.sanitizeFilename(doc.title);
    switch (format) {
      case 'pdf':   return this.exportPdf(doc, name, opts);
      case 'csv':   return this.exportCsv(doc, name);
      case 'excel': return this.exportExcel(doc, name);
      case 'word':  return this.exportWord(doc, name);
      case 'txt':   return this.exportTxt(doc, name);
      case 'ppt':   return this.exportPpt(doc, name);
    }
  }

  private saveBlob(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    setTimeout(() => URL.revokeObjectURL(url), 5000);
  }

  private sanitizeFilename(name: string): string {
    return name.replace(/[^a-zA-Z0-9_\- ]/g, '').replace(/\s+/g, '_').substring(0, 60) || 'report';
  }

  private brandHeader(doc: ExportDocument): string[] {
    return [BRAND.fullName, doc.title, doc.subtitle || '', `Generated: ${nowStamp()} by ${this.currentUserName}`];
  }

  // ───────── PDF ─────────

  private async exportPdf(doc: ExportDocument, baseName: string, opts: Required<ExportOptions>): Promise<void> {
    const jsPDFModule = await import('jspdf');
    const jsPDF = jsPDFModule.jsPDF;
    const autoTableFn = (await import('jspdf-autotable')).default;

    const orientation: ExportOrientation = opts.orientation;
    const custom = opts.layout === 'custom';

    const pdf = new jsPDF({ orientation, unit: 'mm', format: 'a4' });
    const pageW = pdf.internal.pageSize.getWidth();
    const pageH = pdf.internal.pageSize.getHeight();
    const margin = 14;
    const bandH = custom ? 18 : 26;

    // ── Header Band Drawing ──
    const drawHeader = (data: any) => {
      if (data.pageNumber === 1) {
        // Top dark header band
        pdf.setFillColor(30, 41, 59); // #1e293b
        pdf.rect(0, 0, pageW, bandH, 'F');

        // Accent line below header band
        pdf.setFillColor(99, 102, 241); // #6366f1
        pdf.rect(0, bandH, pageW, 1.5, 'F');

        // Title and brand text in header band
        pdf.setFontSize(custom ? 11 : 14);
        pdf.setFont('helvetica', 'bold');
        pdf.setTextColor(255, 255, 255);
        pdf.text(BRAND.fullName.toUpperCase() + ' ENTERPRISE', margin, custom ? 8 : 11);

        pdf.setFontSize(custom ? 9 : 11);
        pdf.setFont('helvetica', 'normal');
        pdf.setTextColor(226, 232, 240);
        pdf.text(doc.title, margin, custom ? 13.5 : 18);

        pdf.setFontSize(8);
        pdf.setTextColor(148, 163, 184);
        pdf.text(`Generated: ${nowStamp()} | Author: ${this.currentUserName}`, pageW - margin, custom ? 13.5 : 18, { align: 'right' });

        let y = bandH + 8;

        // Draw Summary KPI cards if present
        if (doc.summary && doc.summary.length > 0) {
          const kpiCount = doc.summary.length;
          const cardW = Math.min(65, (pageW - margin * 2 - (kpiCount - 1) * 6) / kpiCount);
          const cardH = custom ? 11 : 14;

          doc.summary.forEach((s, idx) => {
            const x = margin + idx * (cardW + 6);
            // Card background & border
            pdf.setFillColor(248, 250, 252);
            pdf.setDrawColor(226, 232, 240);
            pdf.roundedRect(x, y, cardW, cardH, 2, 2, 'FD');

            pdf.setFontSize(custom ? 6 : 7);
            pdf.setFont('helvetica', 'bold');
            pdf.setTextColor(100, 116, 139);
            pdf.text(s.label.toUpperCase(), x + 6, y + (custom ? 4 : 5));

            pdf.setFontSize(custom ? 8.5 : 10);
            pdf.setFont('helvetica', 'bold');
            pdf.setTextColor(15, 23, 42);
            pdf.text(String(s.value), x + 6, y + (custom ? 9 : 11));
          });

          y += cardH + 4;
        }

        // Draw Metadata if present
        if (doc.meta && doc.meta.length > 0) {
          const metaW = pageW - margin * 2;
          const metaCols = orientation === 'landscape' ? 4 : 2;
          const metaCellW = (metaW - (metaCols - 1) * 6) / metaCols;
          doc.meta.forEach((m, i) => {
            const col = i % metaCols;
            const row = Math.floor(i / metaCols);
            const mx = margin + col * (metaCellW + 6);
            const my = y + row * 14;
            pdf.setFillColor(248, 250, 252);
            pdf.setDrawColor(226, 232, 240);
            pdf.roundedRect(mx, my, metaCellW, 13, 1.5, 1.5, 'FD');
            pdf.setFontSize(6.5);
            pdf.setFont('helvetica', 'bold');
            pdf.setTextColor(100, 116, 139);
            pdf.text(m.label.toUpperCase(), mx + 5, my + 4.5);
            pdf.setFont('helvetica', 'normal');
            pdf.setTextColor(15, 23, 42);
            pdf.setFontSize(8.5);
            pdf.text(m.value, mx + 5, my + 10);
          });
          y += Math.ceil(doc.meta.length / metaCols) * 14 + 4;
        }
        return y;
      }
      return undefined;
    };

    const drawFooter = (data: any) => {
      pdf.setFontSize(7);
      pdf.setTextColor(148, 163, 184);
      pdf.text(`${BRAND.fullName} Enterprise Report System`, margin, pageH - 6);
      pdf.text(`Page ${data.pageNumber} of ${pdf.getNumberOfPages()}`, pageW - margin, pageH - 6, { align: 'right' });
      pdf.text(`Confidential & Internal Use Only`, pageW / 2, pageH - 6, { align: 'center' });
      pdf.setTextColor(0);
    };

    for (let i = 0; i < doc.sections.length; i++) {
      const section = doc.sections[i];
      if (i > 0) pdf.addPage();

      let startY = bandH + 8;
      if (i === 0) {
        let headerBottom = 0;
        const returnedY = drawHeader({ pageNumber: 1 });
        if (typeof returnedY === 'number') headerBottom = returnedY;
        startY = headerBottom + 2;
      }

      pdf.setFontSize(custom ? 9 : 10);
      pdf.setFont('helvetica', 'bold');
      pdf.setTextColor(79, 70, 229);
      pdf.text(section.title, margin, startY);

      if (section.description) {
        pdf.setFontSize(8);
        pdf.setFont('helvetica', 'normal');
        pdf.setTextColor(100, 116, 139);
        pdf.text(section.description, margin, startY + 4);
      }

      const headers = section.columns.map((c) => c.label);
      const body = section.rows.map((row) =>
        section.columns.map((c) => this.formatCellValue(row[c.key], c))
      );

      autoTableFn(pdf, {
        head: [headers],
        body,
        startY: startY + (section.description ? 7 : 5),
        styles: {
          fontSize: custom ? 6.5 : 7.5,
          cellPadding: custom ? 2 : 2.5,
          overflow: 'linebreak',
          font: 'helvetica',
          lineColor: [226, 232, 240], // Cell border color
          lineWidth: 0.15
        },
        headStyles: {
          fillColor: [30, 27, 75], // #1e1b4b deep indigo header
          textColor: [255, 255, 255],
          fontStyle: 'bold',
          fontSize: custom ? 7.5 : 8.5
        },
        alternateRowStyles: {
          fillColor: [248, 250, 252] // #f8fafc clean zebra striping
        },
        columnStyles: this.buildPdfColumnStyles(section.columns),
        margin: { left: margin, right: margin },
        didParseCell: (data: any) => {
          // Colorful Status Pills in table cells
          if (data.section === 'body') {
            const val = String(data.cell.raw || '').toUpperCase();
            if (val === 'COMPLETED' || val === 'RESOLVED' || val === 'GREEN' || val === 'UAT_EXIT') {
              data.cell.styles.fillColor = [220, 252, 231]; // #dcfce7
              data.cell.styles.textColor = [22, 101, 52]; // #166534
              data.cell.styles.fontStyle = 'bold';
            } else if (val === 'IN_PROGRESS' || val === 'APPROVED' || val === 'BLUE' || val === 'URGENT') {
              data.cell.styles.fillColor = [224, 231, 255]; // #e0e7ff
              data.cell.styles.textColor = [55, 48, 163]; // #3730a3
              data.cell.styles.fontStyle = 'bold';
            } else if (val === 'BLOCKED' || val === 'RED' || val === 'HIGH') {
              data.cell.styles.fillColor = [254, 226, 226]; // #fee2e2
              data.cell.styles.textColor = [153, 27, 27]; // #991b1b
              data.cell.styles.fontStyle = 'bold';
            } else if (val === 'OPEN' || val === 'AMBER' || val === 'IN_REVIEW') {
              data.cell.styles.fillColor = [254, 243, 199]; // #fef3c7
              data.cell.styles.textColor = [146, 64, 14]; // #92400e
              data.cell.styles.fontStyle = 'bold';
            }
          }
        },
        didDrawPage: (data: any) => {
          drawHeader(data);
          drawFooter(data);
        }
      });
    }

    if (doc.notes) {
      const lastPage = pdf.getNumberOfPages();
      pdf.setPage(lastPage);
      const lastY = pdf.internal.pageSize.getHeight() - 16;
      pdf.setFontSize(8);
      pdf.setFont('helvetica', 'italic');
      pdf.setTextColor(100, 116, 139);
      pdf.text(doc.notes, margin, lastY);
    }

    // Executive Sign-off & Audit Signature Block on final page
    const totalPages = pdf.getNumberOfPages();
    pdf.setPage(totalPages);
    const signY = pageH - 28;

    pdf.setDrawColor(226, 232, 240);
    pdf.line(margin, signY, pageW - margin, signY);

    pdf.setFontSize(7.5);
    pdf.setFont('helvetica', 'bold');
    pdf.setTextColor(71, 85, 105);
    pdf.text('REPORT AUDIT & EXECUTIVE SIGN-OFF', margin, signY + 5);

    pdf.setFontSize(7);
    pdf.setFont('helvetica', 'normal');
    pdf.setTextColor(100, 116, 139);
    pdf.text(`Prepared By: ${this.currentUserName}`, margin, signY + 10);
    pdf.text(`Classification: STRICTLY CONFIDENTIAL & INTERNAL TELEMETRY`, margin, signY + 14);

    pdf.text('Authorized Signature: _______________________', pageW - margin - 65, signY + 10);
    pdf.text('Date: ____ / ____ / ________', pageW - margin - 65, signY + 14);

    this.saveBlob(pdf.output('blob'), `${baseName}.pdf`);
  }

  private buildPdfColumnStyles(columns: ExportColumn[]): Record<string, any> {
    const styles: Record<string, any> = {};
    columns.forEach((col, i) => {
      if (col.align) styles[i] = { halign: col.align };
    });
    return styles;
  }

  // ───────── EXCEL ─────────

  private async exportExcel(doc: ExportDocument, baseName: string): Promise<void> {
    const ExcelJS = (await import('exceljs')).default;
    const wb = new ExcelJS.Workbook();
    wb.creator = BRAND.author;
    wb.created = new Date();

    const ws = wb.addWorksheet(doc.title.substring(0, 31));
    ws.columns = [];

    let row = 1;
    ws.mergeCells(`A${row}:${this.colLetter(6)}${row}`);
    const titleCell = ws.getCell(`A${row}`);
    titleCell.value = BRAND.fullName;
    titleCell.font = { bold: true, size: 16, color: { argb: 'FF4F46E5' } };
    row++;

    ws.mergeCells(`A${row}:${this.colLetter(6)}${row}`);
    const subtitleCell = ws.getCell(`A${row}`);
    subtitleCell.value = doc.title;
    subtitleCell.font = { bold: true, size: 13 };
    row++;

    if (doc.subtitle) {
      ws.mergeCells(`A${row}:${this.colLetter(6)}${row}`);
      ws.getCell(`A${row}`).value = doc.subtitle;
      row++;
    }
    ws.getCell(`A${row}`).value = `Generated: ${nowStamp()} by ${this.currentUserName}`;
    ws.getCell(`A${row}`).font = { size: 9, color: { argb: 'FF64748B' } };
    row += 2;

    if (doc.meta && doc.meta.length > 0) {
      doc.meta.forEach((m) => {
        ws.getCell(`A${row}`).value = m.label;
        ws.getCell(`A${row}`).font = { bold: true };
        ws.getCell(`B${row}`).value = m.value;
        row++;
      });
      row++;
    }

    if (doc.summary && doc.summary.length > 0) {
      ws.getCell(`A${row}`).value = 'Summary';
      ws.getCell(`A${row}`).font = { bold: true, size: 11, color: { argb: 'FF4F46E5' } };
      row++;
      doc.summary.forEach((s) => {
        ws.getCell(`A${row}`).value = s.label;
        ws.getCell(`A${row}`).font = { bold: true };
        ws.getCell(`B${row}`).value = s.value;
        row++;
      });
      row++;
    }

    for (const section of doc.sections) {
      ws.getCell(`A${row}`).value = section.title;
      ws.getCell(`A${row}`).font = { bold: true, size: 11, color: { argb: 'FF4F46E5' } };
      row++;

      const headerRow = ws.getRow(row);
      section.columns.forEach((col, i) => {
        const cell = headerRow.getCell(i + 1);
        cell.value = col.label;
        cell.font = { bold: true, color: { argb: 'FFFFFFFF' } };
        cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: 'FF4F46E5' } };
        cell.alignment = { horizontal: col.align || 'left' };
      });
      headerRow.commit();
      row++;

      section.rows.forEach((r) => {
        const dataRow = ws.getRow(row);
        section.columns.forEach((col, i) => {
          const cell = dataRow.getCell(i + 1);
          cell.value = this.formatCellValue(r[col.key], col);
          cell.alignment = { horizontal: col.align || 'left' };
        });
        dataRow.commit();
        row++;
      });
      row += 2;
    }

    if (doc.notes) {
      ws.getCell(`A${row}`).value = doc.notes;
      ws.getCell(`A${row}`).font = { italic: true, size: 9, color: { argb: 'FF64748B' } };
    }

    const buffer = await wb.xlsx.writeBuffer();
    this.saveBlob(
      new Blob([buffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }),
      `${baseName}.xlsx`
    );
  }

  // ───────── WORD ─────────

  private async exportWord(doc: ExportDocument, baseName: string): Promise<void> {
    const docx = await import('docx');
    const { Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
            WidthType, AlignmentType, HeadingLevel, BorderStyle, ShadingType } = docx;

    const headerRuns = [
      new TextRun({ text: BRAND.fullName, bold: true, size: 32, color: '4F46E5', font: 'Calibri' }),
      new TextRun({ text: '\n' }),
      new TextRun({ text: doc.title, bold: true, size: 28, font: 'Calibri' }),
    ];
    if (doc.subtitle) headerRuns.push(new TextRun({ text: '\n' + doc.subtitle, size: 22, color: '64748B', font: 'Calibri' }));

    const children: any[] = [
      new Paragraph({ children: headerRuns, spacing: { after: 200 } }),
      new Paragraph({
        children: [new TextRun({ text: `Generated: ${nowStamp()} by ${this.currentUserName}`, size: 18, color: '64748B', italics: true, font: 'Calibri' })],
        spacing: { after: 200 }
      })
    ];

    if (doc.meta && doc.meta.length > 0) {
      doc.meta.forEach((m) => {
        children.push(new Paragraph({
          children: [
            new TextRun({ text: `${m.label}: `, bold: true, size: 20, font: 'Calibri' }),
            new TextRun({ text: m.value, size: 20, font: 'Calibri' })
          ],
          spacing: { after: 60 }
        }));
      });
    }

    if (doc.summary && doc.summary.length > 0) {
      children.push(new Paragraph({ spacing: { before: 200 } }));
      children.push(new Paragraph({
        children: [new TextRun({ text: 'Summary', bold: true, size: 24, color: '4F46E5', font: 'Calibri' })],
        heading: HeadingLevel.HEADING_2,
        spacing: { after: 100 }
      }));
      doc.summary.forEach((s) => {
        children.push(new Paragraph({
          children: [
            new TextRun({ text: `${s.label}: `, bold: true, size: 20, font: 'Calibri' }),
            new TextRun({ text: String(s.value), size: 20, font: 'Calibri' })
          ],
          spacing: { after: 60 }
        }));
      });
    }

    for (const section of doc.sections) {
      children.push(new Paragraph({ spacing: { before: 300 } }));
      children.push(new Paragraph({
        children: [new TextRun({ text: section.title, bold: true, size: 24, color: '4F46E5', font: 'Calibri' })],
        heading: HeadingLevel.HEADING_2,
        spacing: { after: 100 }
      }));
      if (section.description) {
        children.push(new Paragraph({
          children: [new TextRun({ text: section.description, italics: true, size: 20, color: '64748B', font: 'Calibri' })],
          spacing: { after: 100 }
        }));
      }

      const headerCells = section.columns.map((col) =>
        new TableCell({
          children: [new Paragraph({ children: [new TextRun({ text: col.label, bold: true, size: 18, color: 'FFFFFF', font: 'Calibri' })] })],
          shading: { type: ShadingType.SOLID, color: '4F46E5' },
          width: { size: Math.round(100 / section.columns.length), type: WidthType.PERCENTAGE }
        })
      );

      const bodyRows = section.rows.map((row) =>
        new TableRow({
          children: section.columns.map((col) =>
            new TableCell({
              children: [new Paragraph({
                children: [new TextRun({ text: this.formatCellValue(row[col.key], col), size: 18, font: 'Calibri' })],
                alignment: col.align === 'right' ? AlignmentType.RIGHT : AlignmentType.LEFT
              })],
              width: { size: Math.round(100 / section.columns.length), type: WidthType.PERCENTAGE }
            })
          )
        })
      );

      children.push(new Table({
        rows: [new TableRow({ children: headerCells, tableHeader: true }), ...bodyRows],
        width: { size: 100, type: WidthType.PERCENTAGE }
      }));
    }

    if (doc.notes) {
      children.push(new Paragraph({ spacing: { before: 300 } }));
      children.push(new Paragraph({
        children: [new TextRun({ text: doc.notes, italics: true, size: 18, color: '64748B', font: 'Calibri' })],
        spacing: { after: 100 }
      }));
    }

    children.push(new Paragraph({
      children: [
        new TextRun({ text: `\n${BRAND.fullName} | ${BRAND.tagline}`, size: 16, color: '94A3B8', font: 'Calibri' }),
        new TextRun({ text: `\n${BRAND.author} | ${BRAND.version}`, size: 16, color: '94A3B8', font: 'Calibri' })
      ],
      spacing: { before: 300 }
    }));

    const document = new Document({
      creator: BRAND.author,
      title: doc.title,
      description: BRAND.tagline,
      sections: [{ children }]
    });

    const blob = await Packer.toBlob(document);
    this.saveBlob(blob, `${baseName}.docx`);
  }

  // ───────── PPT ─────────

  private async exportPpt(doc: ExportDocument, baseName: string): Promise<void> {
    const PptxGenJS = (await import('pptxgenjs')).default;
    const pptx = new PptxGenJS();
    pptx.author = BRAND.author;
    pptx.company = BRAND.company;
    pptx.subject = doc.title;
    pptx.title = doc.title;
    pptx.layout = 'LAYOUT_WIDE';

    const slide1 = pptx.addSlide();
    slide1.background = { color: '4F46E5' };
    slide1.addText(BRAND.fullName, { x: 0.5, y: 0.8, w: '90%', fontSize: 28, bold: true, color: 'FFFFFF', fontFace: 'Calibri' });
    slide1.addText(doc.title, { x: 0.5, y: 1.6, w: '90%', fontSize: 22, color: 'C7D2FE', fontFace: 'Calibri' });
    if (doc.subtitle) slide1.addText(doc.subtitle, { x: 0.5, y: 2.2, w: '90%', fontSize: 16, color: 'E0E7FF', fontFace: 'Calibri' });
    slide1.addText(`Generated: ${nowStamp()} by ${this.currentUserName}`, { x: 0.5, y: 3.0, w: '90%', fontSize: 11, color: 'C7D2FE', fontFace: 'Calibri' });
    slide1.addText(`${BRAND.version} | ${BRAND.tagline}`, { x: 0.5, y: 4.5, w: '90%', fontSize: 10, color: 'A5B4FC', fontFace: 'Calibri' });

    if (doc.meta && doc.meta.length > 0) {
      const metaSlide = pptx.addSlide();
      metaSlide.addText('Project Details', { x: 0.5, y: 0.3, w: '90%', fontSize: 18, bold: true, color: '4F46E5', fontFace: 'Calibri' });
      doc.meta.forEach((m, i) => {
        const yPos = 1.0 + i * 0.4;
        metaSlide.addText(m.label + ':', { x: 0.5, y: yPos, w: 2.5, fontSize: 12, bold: true, fontFace: 'Calibri' });
        metaSlide.addText(m.value, { x: 3.2, y: yPos, w: 6, fontSize: 12, fontFace: 'Calibri' });
      });
    }

    if (doc.summary && doc.summary.length > 0) {
      const sumSlide = pptx.addSlide();
      sumSlide.addText('Summary', { x: 0.5, y: 0.3, w: '90%', fontSize: 18, bold: true, color: '4F46E5', fontFace: 'Calibri' });
      doc.summary.forEach((s, i) => {
        const yPos = 1.0 + i * 0.45;
        sumSlide.addText(s.label, { x: 0.5, y: yPos, w: 3.5, fontSize: 12, bold: true, fontFace: 'Calibri' });
        sumSlide.addText(String(s.value), { x: 4.2, y: yPos, w: 5, fontSize: 12, fontFace: 'Calibri' });
      });
    }

    for (const section of doc.sections) {
      const slide = pptx.addSlide();
      slide.addText(section.title, { x: 0.5, y: 0.3, w: '90%', fontSize: 18, bold: true, color: '4F46E5', fontFace: 'Calibri' });
      if (section.description) {
        slide.addText(section.description, { x: 0.5, y: 0.7, w: '90%', fontSize: 11, italic: true, color: '64748B', fontFace: 'Calibri' });
      }

      const tableRows: any[][] = [];
      const headerRow = section.columns.map((c) => ({
        text: c.label, options: { bold: true, color: 'FFFFFF', fontSize: 9, fontFace: 'Calibri' }
      }));
      tableRows.push(headerRow);

      section.rows.slice(0, 30).forEach((row) => {
        tableRows.push(
          section.columns.map((c) => ({
            text: this.formatCellValue(row[c.key], c),
            options: { fontSize: 8, fontFace: 'Calibri', align: c.align || 'left' }
          }))
        );
      });

      if (section.rows.length > 30) {
        tableRows.push(section.columns.map(() => ({
          text: `... ${section.rows.length - 30} more rows`,
          options: { fontSize: 8, italic: true, color: '64748B', fontFace: 'Calibri' }
        })));
      }

      slide.addTable(tableRows, {
        x: 0.5, y: section.description ? 1.1 : 0.9,
        w: 9.0,
        border: { type: 'solid', pt: 0.5, color: 'E2E8F0' },
        colW: section.columns.map(() => Math.round(900 / section.columns.length)),
        rowH: 0.3
      });
    }

    const footerSlide = pptx.addSlide();
    footerSlide.background = { color: 'F8FAFC' };
    footerSlide.addText(BRAND.fullName, { x: 0.5, y: 1.5, w: '90%', fontSize: 24, bold: true, color: '4F46E5', fontFace: 'Calibri', align: 'center' });
    footerSlide.addText(BRAND.tagline, { x: 0.5, y: 2.2, w: '90%', fontSize: 14, color: '64748B', fontFace: 'Calibri', align: 'center' });
    footerSlide.addText(`${BRAND.version} | Generated ${nowStamp()}`, { x: 0.5, y: 3.0, w: '90%', fontSize: 11, color: '94A3B8', fontFace: 'Calibri', align: 'center' });

    await pptx.writeFile({ fileName: `${baseName}.pptx` });
  }

  // ───────── CSV ─────────

  private async exportCsv(doc: ExportDocument, baseName: string): Promise<void> {
    const lines = this.buildCsvLines(doc);
    const blob = new Blob([lines.join('\n')], { type: 'text/csv;charset=utf-8;' });
    this.saveBlob(blob, `${baseName}.csv`);
  }

  private buildCsvLines(doc: ExportDocument): string[] {
    const lines: string[] = [];
    lines.push(`${BRAND.fullName}`);
    lines.push(doc.title);
    if (doc.subtitle) lines.push(doc.subtitle);
    lines.push(`Generated: ${nowStamp()} by ${this.currentUserName}`);
    lines.push('');

    if (doc.meta && doc.meta.length > 0) {
      doc.meta.forEach((m) => lines.push(`${m.label},${m.value}`));
      lines.push('');
    }

    if (doc.summary && doc.summary.length > 0) {
      lines.push('Summary');
      doc.summary.forEach((s) => lines.push(`${s.label},${s.value}`));
      lines.push('');
    }

    for (const section of doc.sections) {
      lines.push(`--- ${section.title} ---`);
      if (section.description) lines.push(section.description);
      const header = section.columns.map((c) => `"${c.label}"`).join(',');
      lines.push(header);
      section.rows.forEach((row) => {
        const vals = section.columns.map((c) => `"${this.csvEscape(this.formatCellValue(row[c.key], c))}"`);
        lines.push(vals.join(','));
      });
      lines.push('');
    }

    if (doc.notes) {
      lines.push(`Notes: ${doc.notes}`);
    }
    lines.push(`${BRAND.fullName} | ${BRAND.version}`);
    return lines;
  }

  // ───────── TXT ─────────

  private async exportTxt(doc: ExportDocument, baseName: string): Promise<void> {
    const lines = this.buildTxtLines(doc);
    const blob = new Blob([lines.join('\n')], { type: 'text/plain;charset=utf-8;' });
    this.saveBlob(blob, `${baseName}.txt`);
  }

  private buildTxtLines(doc: ExportDocument): string[] {
    const lines: string[] = [];
    const sep = '═'.repeat(80);
    lines.push(sep);
    lines.push(BRAND.fullName.toUpperCase());
    lines.push(doc.title);
    if (doc.subtitle) lines.push(doc.subtitle);
    lines.push(`Generated: ${nowStamp()} by ${this.currentUserName}`);
    lines.push(sep);
    lines.push('');

    if (doc.meta && doc.meta.length > 0) {
      doc.meta.forEach((m) => lines.push(`  ${m.label.padEnd(22)} ${m.value}`));
      lines.push('');
    }

    if (doc.summary && doc.summary.length > 0) {
      lines.push('  SUMMARY');
      lines.push('  ' + '-'.repeat(40));
      doc.summary.forEach((s) => lines.push(`  ${s.label.padEnd(22)} ${String(s.value)}`));
      lines.push('');
    }

    for (const section of doc.sections) {
      lines.push(sep);
      lines.push(`  ${section.title}`);
      if (section.description) lines.push(`  ${section.description}`);
      lines.push('');

      const colWidths = section.columns.map((c) => Math.max(c.label.length, 12));
      const header = section.columns.map((c, i) => c.label.padEnd(colWidths[i])).join('  ');
      lines.push('  ' + header);
      lines.push('  ' + '-'.repeat(header.length));

      section.rows.forEach((row) => {
        const vals = section.columns.map((c, i) => this.formatCellValue(row[c.key], c).padEnd(colWidths[i]));
        lines.push('  ' + vals.join('  '));
      });
      lines.push('');
    }

    if (doc.notes) lines.push(`Notes: ${doc.notes}`);
    lines.push(sep);
    lines.push(`${BRAND.fullName} | ${BRAND.version} | ${BRAND.author}`);
    return lines;
  }

  // ───────── HTML PREVIEW (mirrors the exact document generated per format) ─────────

  renderHtmlPreview(doc: ExportDocument, format: ExportFormat = 'pdf', options: ExportOptions = {}): string {
    const opts = { ...DEFAULT_OPTIONS, ...options };
    switch (format) {
      case 'excel': return this.buildExcelPreview(doc, format, opts);
      case 'word':  return this.buildWordPreview(doc, format, opts);
      case 'csv':   return this.buildTextPreview(doc, format, opts);
      case 'txt':   return this.buildTextPreview(doc, format, opts);
      case 'ppt':   return this.buildPptPreview(doc, format, opts);
      default:      return this.buildPdfPreview(doc, format, opts);
    }
  }

  private buildPdfPreview(doc: ExportDocument, format: ExportFormat, opts: Required<ExportOptions>): string {
    const portrait = opts.orientation === 'portrait';
    const sheetWidth = portrait ? 720 : 1000;
    const sheetMinHeight = portrait ? 1000 : 706;

    const metaBoxes = (doc.meta || []).map((m) =>
      `<div class="kv"><span class="kv-label">${this.esc(m.label)}</span><span class="kv-value">${this.esc(m.value)}</span></div>`
    ).join('');

    const chips = (doc.summary || []).map((s) =>
      `<div class="chip"><span class="chip-label">${this.esc(s.label)}</span><span class="chip-value">${this.esc(String(s.value))}</span></div>`
    ).join('');

    const sections = doc.sections.map((section) => {
      const head = section.columns.map((c) => `<th class="${c.align || 'left'}">${this.esc(c.label)}</th>`).join('');
      const body = section.rows.map((row) =>
        `<tr>${section.columns.map((c) => `<td class="${c.align || 'left'}">${this.esc(this.formatCellValue(row[c.key], c))}</td>`).join('')}</tr>`
      ).join('');
      return `
        <section class="report-section">
          <h3>${this.esc(section.title)}</h3>
          ${section.description ? `<p class="section-desc">${this.esc(section.description)}</p>` : ''}
          <table><thead><tr>${head}</tr></thead><tbody>${body || '<tr><td class="empty" colspan="99">No data available</td></tr>'}</tbody></table>
        </section>`;
    }).join('');

    const html = `<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8" />
<title>${this.esc(doc.title)} — Preview</title>
<style>
  @page { size: A4 ${portrait ? 'portrait' : 'landscape'}; margin: 14mm; }
  * { box-sizing: border-box; }
  body { margin: 0; background: #E2E8F0; color: #1E293B;
         font-family: 'Segoe UI', 'Plus Jakarta Sans', 'Helvetica Neue', Arial, sans-serif; }
  .sheet { position: relative; width: ${sheetWidth}px; min-height: ${sheetMinHeight}px; margin: 24px auto; background: #FFFFFF;
           padding: 30px 34px 26px; box-shadow: 0 10px 40px rgba(30,41,59,.18); overflow: hidden; }
  .sheet::after { content: 'TICKETBOARD'; position: absolute; right: -40px; bottom: 30px;
                  transform: rotate(-28deg); font-size: 80px; font-weight: 800; letter-spacing: 6px;
                  color: rgba(79,70,229,.05); pointer-events: none; white-space: nowrap; z-index: 0; }
  .brand-band { display: flex; justify-content: space-between; align-items: center; padding: 20px 22px;
                background: linear-gradient(100deg, #3730A3 0%, #4338CA 45%, #0284C7 100%);
                border-radius: 10px 10px 0 0; color: #fff; }
  .brand-left { display: flex; align-items: center; gap: 14px; }
  .brand-left .logo { width: 46px; height: 46px; border-radius: 12px; background: #fff; display: flex;
                      align-items: center; justify-content: center; color: #4338CA; font-weight: 900; font-size: 20px; }
  .brand-name { font-size: 19px; font-weight: 800; letter-spacing: .3px; }
  .brand-tag { font-size: 10px; opacity: .82; margin-top: 2px; }
  .brand-right { text-align: right; }
  .brand-right .doctype { font-size: 22px; font-weight: 800; letter-spacing: 2px; }
  .brand-right .doctype-sub { font-size: 11px; opacity: .9; margin-top: 2px; }
  .pill { display: inline-block; margin-top: 6px; padding: 3px 10px; border-radius: 999px;
          background: rgba(255,255,255,.18); font-size: 9px; letter-spacing: 1px; text-transform: uppercase; }
  .accent-line { height: 6px; background: linear-gradient(90deg, #4F46E5 0%, #0284C7 70%, #0D9488 100%); }
  .meta-grid { display: grid; grid-template-columns: repeat(${portrait ? 2 : 4}, 1fr); gap: 8px; margin-top: 14px; }
  .kv { border: 1px solid #CBD5E1; border-left: 3px solid #4F46E5; background: #F8FAFC; padding: 6px 10px; }
  .kv-label { display: block; font-size: 9px; font-weight: 700; color: #94A3B8; letter-spacing: .6px; text-transform: uppercase; }
  .kv-value { display: block; font-size: 13px; font-weight: 600; color: #1E293B; margin-top: 1px; }
  .chips { display: flex; flex-wrap: wrap; gap: 8px; margin: 14px 0 4px; }
  .chip { display: flex; gap: 8px; align-items: baseline; padding: 6px 12px; border-radius: 8px;
          background: #EEF2FF; border: 1px solid #E0E7FF; }
  .chip-label { font-size: 10px; font-weight: 600; color: #6366F1; }
  .chip-value { font-size: 15px; font-weight: 800; color: #3730A3; }
  .report-section { margin-top: 18px; }
  h3 { margin: 0 0 4px; font-size: 13px; font-weight: 800; color: #3730A3; letter-spacing: .4px;
       text-transform: uppercase; }
  .section-desc { margin: 0 0 8px; font-size: 11px; color: #64748B; font-style: italic; }
  table { width: 100%; border-collapse: collapse; font-size: 11px; }
  th { background: #1E1B4B; color: #fff; font-weight: 700; padding: 7px 8px; text-align: left; }
  th.right, td.right { text-align: right; }
  th.center, td.center { text-align: center; }
  td { padding: 6px 8px; border-bottom: 1px solid #E2E8F0; }
  tbody tr:nth-child(even) { background: #F8FAFC; }
  tbody tr:hover { background: #EEF2FF; }
  td.empty { text-align: center; color: #94A3B8; padding: 14px; font-style: italic; }
  .notes { margin-top: 18px; padding: 10px 14px; border-left: 4px solid #0D9488; background: #F0FDFA;
           font-size: 11px; color: #334155; font-style: italic; }
  .footer-bar { margin-top: 22px; border-top: 1px solid #CBD5E1; padding-top: 8px; display: flex;
                justify-content: space-between; font-size: 9px; color: #94A3B8; }
  @media print {
    body { background: #fff; }
    .sheet { margin: 0 auto; box-shadow: none; width: 100%; padding: 0; }
    .no-print { display: none; }
  }
</style>
</head>
<body>
  <div class="sheet">
    <header class="brand-band">
      <div class="brand-left">
        <div class="logo">TB</div>
        <div>
          <div class="brand-name">TicketBoard by Tushar</div>
          <div class="brand-tag">${this.esc(BRAND.tagline)}</div>
        </div>
      </div>
      <div class="brand-right">
        <div class="doctype">${this.esc(doc.title)}</div>
        <div class="doctype-sub">${this.esc(doc.subtitle || '')}</div>
        <span class="pill">${format ? `Preview  •  ${format.toUpperCase()}  •  ${opts.orientation.toUpperCase()}` : 'Document Preview'}</span>
      </div>
    </header>
    <div class="accent-line"></div>
    <div class="meta-grid">${metaBoxes || ''}</div>
    ${chips ? `<div class="chips">${chips}</div>` : ''}
    ${sections}
    ${doc.notes ? `<div class="notes">${this.esc(doc.notes)}</div>` : ''}
    <footer class="footer-bar">
      <span>${this.esc(BRAND.fullName)} | ${this.esc(BRAND.version)}</span>
      <span>Generated: ${this.esc(nowStamp())} by ${this.esc(this.currentUserName)}</span>
      <span>${this.esc(BRAND.tagline)}</span>
    </footer>
  </div>
</body>
</html>`;
    return html;
  }

  // ───────── HTML PREVIEW: EXCEL (mirrors the .xlsx workbook sheet) ─────────

  private buildExcelPreview(doc: ExportDocument, format: ExportFormat, opts: Required<ExportOptions>): string {
    const metaRows = (doc.meta || []).map((m) =>
      `<tr><td class="xl-label">${this.esc(m.label)}</td><td colspan="5" class="xl-value">${this.esc(m.value)}</td></tr>`
    ).join('');

    const sumRows = (doc.summary || []).map((s) =>
      `<tr><td class="xl-label">${this.esc(s.label)}</td><td colspan="5" class="xl-value">${this.esc(String(s.value))}</td></tr>`
    ).join('');

    const sections = doc.sections.map((section) => {
      const head = section.columns.map((c) => `<th class="xl-align-${c.align || 'left'}">${this.esc(c.label)}</th>`).join('');
      const body = section.rows.map((row) =>
        `<tr>${section.columns.map((c) => `<td class="xl-align-${c.align || 'left'}">${this.esc(this.formatCellValue(row[c.key], c))}</td>`).join('')}</tr>`
      ).join('');
      return `
      <tr><td colspan="6" class="xl-section">${this.esc(section.title)}</td></tr>
      ${section.description ? `<tr><td colspan="6" class="xl-desc">${this.esc(section.description)}</td></tr>` : ''}
      <tr>${head}</tr>
      ${body || `<tr><td colspan="6" class="xl-empty">No data available</td></tr>`}`;
    }).join('');

    const body = `
  <div class="xl-sheet">
    <table>
      <tbody>
        <tr><td colspan="6" class="xl-ci">${this.esc(BRAND.fullName)}</td></tr>
        <tr><td colspan="6" class="xl-title">${this.esc(doc.title)}</td></tr>
        ${doc.subtitle ? `<tr><td colspan="6" class="xl-subtitle">${this.esc(doc.subtitle)}</td></tr>` : ''}
        <tr><td colspan="6" class="xl-gen">Generated: ${this.esc(nowStamp())} by ${this.esc(this.currentUserName)}</td></tr>
        <tr class="xl-spacer"><td colspan="6"></td></tr>
        ${metaRows}
        ${doc.meta && doc.meta.length ? '<tr class="xl-spacer"><td colspan="6"></td></tr>' : ''}
        ${doc.summary && doc.summary.length ? `<tr><td colspan="6" class="xl-sumhead">Summary</td></tr>${sumRows}<tr class="xl-spacer"><td colspan="6"></td></tr>` : ''}
        ${sections}
        ${doc.notes ? `<tr><td colspan="6" class="xl-notes">${this.esc(doc.notes)}</td></tr>` : ''}
      </tbody>
    </table>
    <div class="xl-foot">${this.esc(BRAND.fullName)} | ${this.esc(BRAND.version)}</div>
  </div>`;

    const css = `
  * { box-sizing: border-box; }
  body { margin: 0; background: #E2E8F0; color: #1E293B; font-family: 'Segoe UI', Arial, sans-serif; }
  .xl-sheet { width: 760px; margin: 24px auto; padding: 22px; background: #fff; box-shadow: 0 10px 40px rgba(30,41,59,.18); }
  .xl-sheet table { width: 100%; border-collapse: collapse; table-layout: fixed; }
  .xl-sheet td, .xl-sheet th { border: 1px solid #CBD5E1; padding: 5px 8px; font-size: 11px; vertical-align: top; overflow: hidden; font-family: Calibri, 'Segoe UI', Arial, sans-serif; }
  .xl-sheet th { background: #4F46E5; color: #fff; font-weight: 700; text-align: left; }
  .xl-ci { font-size: 16px; font-weight: 700; color: #4F46E5; }
  .xl-title { font-size: 13px; font-weight: 700; }
  .xl-subtitle { color: #475569; }
  .xl-gen { font-size: 9px; color: #64748B; }
  .xl-spacer td { border: 1px solid #CBD5E1; height: 12px; }
  .xl-label { font-weight: 700; background: #F8FAFC; }
  .xl-sumhead { font-weight: 700; font-size: 11px; color: #4F46E5; background: #EEF2FF; }
  .xl-section { background: #EEF2FF; font-weight: 700; color: #3730A3; font-size: 11px; }
  .xl-desc { font-size: 10px; color: #64748B; font-style: italic; }
  .xl-empty { text-align: center; color: #94A3B8; font-style: italic; }
  .xl-notes { font-style: italic; font-size: 9px; color: #64748B; }
  .xl-foot { margin-top: 12px; font-size: 9px; color: #94A3B8; }
  .xl-align-right { text-align: right; }
  .xl-align-center { text-align: center; }
  .xl-align-left { text-align: left; }
  @media print { body { background: #fff; } .xl-sheet { margin: 0 auto; box-shadow: none; width: 100%; } }`;

    return this.wrapPreviewHtml(`${doc.title} — Excel Preview`, css, body);
  }

  // ───────── HTML PREVIEW: WORD (mirrors the .docx document) ─────────

  private buildWordPreview(doc: ExportDocument, format: ExportFormat, opts: Required<ExportOptions>): string {
    const metaParas = (doc.meta || []).map((m) =>
      `<p class="wd-para"><b>${this.esc(m.label)}:</b> ${this.esc(m.value)}</p>`
    ).join('');

    const sumParas = (doc.summary || []).map((s) =>
      `<p class="wd-para"><b>${this.esc(s.label)}:</b> ${this.esc(String(s.value))}</p>`
    ).join('');

    const sections = doc.sections.map((section) => {
      const head = section.columns.map((c) => `<th class="wd-align-${c.align || 'left'}">${this.esc(c.label)}</th>`).join('');
      const body = section.rows.map((row) =>
        `<tr>${section.columns.map((c) => `<td class="wd-align-${c.align || 'left'}">${this.esc(this.formatCellValue(row[c.key], c))}</td>`).join('')}</tr>`
      ).join('');
      return `
      <h2 class="wd-h2">${this.esc(section.title)}</h2>
      ${section.description ? `<p class="wd-desc">${this.esc(section.description)}</p>` : ''}
      <table class="wd-table"><thead><tr>${head}</tr></thead><tbody>${body || '<tr><td class="wd-empty" colspan="99">No data available</td></tr>'}</tbody></table>`;
    }).join('');

    const body = `
  <div class="wd-doc">
    <p class="wd-brand">${this.esc(BRAND.fullName)} <span class="wd-doctitle">${this.esc(doc.title)}</span></p>
    ${doc.subtitle ? `<p class="wd-subtitle">${this.esc(doc.subtitle)}</p>` : ''}
    <p class="wd-gen"><i>Generated: ${this.esc(nowStamp())} by ${this.esc(this.currentUserName)}</i></p>
    ${metaParas}
    ${doc.summary && doc.summary.length ? `<h2 class="wd-h2">Summary</h2>${sumParas}` : ''}
    ${sections}
    ${doc.notes ? `<p class="wd-notes">${this.esc(doc.notes)}</p>` : ''}
    <p class="wd-foot">${this.esc(BRAND.fullName)} | ${this.esc(BRAND.tagline)}<br />${this.esc(BRAND.author)} | ${this.esc(BRAND.version)}</p>
  </div>`;

    const css = `
  * { box-sizing: border-box; }
  body { margin: 0; background: #E2E8F0; color: #1E293B; }
  .wd-doc { width: 720px; min-height: 1000px; margin: 24px auto; padding: 48px 56px; background: #fff; box-shadow: 0 10px 40px rgba(30,41,59,.18); font-family: Calibri, 'Segoe UI', Arial, sans-serif; }
  .wd-brand { font-size: 20px; color: #4F46E5; font-weight: 700; margin: 0 0 2px; }
  .wd-doctitle { color: #1E293B; }
  .wd-subtitle { font-size: 15px; color: #64748B; margin: 4px 0 10px; }
  .wd-gen { font-size: 11px; color: #94A3B8; margin: 0 0 18px; }
  .wd-para { font-size: 13px; margin: 3px 0; color: #1E293B; }
  .wd-h2 { font-size: 16px; font-weight: 700; color: #4F46E5; border-bottom: 1px solid #E2E8F0; padding-bottom: 4px; margin: 22px 0 8px; }
  .wd-desc { font-size: 12px; font-style: italic; color: #64748B; margin: 0 0 8px; }
  .wd-table { width: 100%; border-collapse: collapse; font-size: 12px; }
  .wd-table th { background: #4F46E5; color: #fff; font-weight: 700; padding: 7px 8px; text-align: left; }
  .wd-table td { padding: 6px 8px; border-bottom: 1px solid #E2E8F0; }
  .wd-empty { text-align: center; color: #94A3B8; font-style: italic; padding: 14px; }
  .wd-align-right { text-align: right; }
  .wd-align-center { text-align: center; }
  .wd-notes { font-style: italic; font-size: 12px; color: #64748B; margin-top: 20px; border-left: 3px solid #0D9488; padding-left: 10px; }
  .wd-foot { font-size: 11px; color: #94A3B8; margin-top: 26px; }
  @media print { body { background: #fff; } .wd-doc { margin: 0 auto; box-shadow: none; width: 100%; } }`;

    return this.wrapPreviewHtml(`${doc.title} — Word Preview`, css, body);
  }

  // ───────── HTML PREVIEW: CSV / TXT (exact same lines as the downloaded file) ─────────

  private buildTextPreview(doc: ExportDocument, format: ExportFormat, opts: Required<ExportOptions>): string {
    const lines = format === 'txt' ? this.buildTxtLines(doc) : this.buildCsvLines(doc);
    const escaped = lines.map((l) => this.esc(l));
    const perPage = 46;
    const pages: string[] = [];
    for (let i = 0; i < escaped.length; i += perPage) {
      pages.push(`<div class="tx-page"><pre>${escaped.slice(i, i + perPage).join('\n')}</pre></div>`);
    }

    const label = format === 'txt' ? 'Plain Text (.txt)' : 'CSV Data (.csv)';
    const css = `
  * { box-sizing: border-box; }
  body { margin: 0; background: #E2E8F0; }
  .tx-page { width: 720px; min-height: 940px; margin: 24px auto; padding: 42px 46px; background: #fff; box-shadow: 0 10px 40px rgba(30,41,59,.18); break-after: page; }
  .tx-page pre { margin: 0; font-family: 'Courier New', Courier, monospace; font-size: 12px; line-height: 1.55; white-space: pre; color: #1E293B; }
  @media print { body { background: #fff; } .tx-page { margin: 0 auto; box-shadow: none; width: 100%; padding: 0; } }`;

    return this.wrapPreviewHtml(`${doc.title} — ${label} Preview`, css, pages.join('\n'));
  }

  // ───────── HTML PREVIEW: PPT (mirrors the slide deck) ─────────

  private buildPptPreview(doc: ExportDocument, format: ExportFormat, opts: Required<ExportOptions>): string {
    const metaSlide = doc.meta && doc.meta.length ? `
  <div class="pt-slide">
    <h2 class="pt-head">Project Details</h2>
    ${(doc.meta || []).map((m) => `<div class="pt-row"><span class="pt-lbl">${this.esc(m.label)}:</span><span class="pt-val">${this.esc(m.value)}</span></div>`).join('')}
  </div>` : '';

    const sumSlide = doc.summary && doc.summary.length ? `
  <div class="pt-slide">
    <h2 class="pt-head">Summary</h2>
    ${(doc.summary || []).map((s) => `<div class="pt-row"><span class="pt-lbl">${this.esc(s.label)}</span><span class="pt-val">${this.esc(String(s.value))}</span></div>`).join('')}
  </div>` : '';

    const sectionSlides = doc.sections.map((section) => {
      const head = section.columns.map((c) => `<th>${this.esc(c.label)}</th>`).join('');
      const rows = section.rows.slice(0, 30).map((row) =>
        `<tr>${section.columns.map((c) => `<td>${this.esc(this.formatCellValue(row[c.key], c))}</td>`).join('')}</tr>`
      ).join('');
      const overflow = section.rows.length > 30
        ? `<tr><td class="pt-more" colspan="${section.columns.length}">... ${section.rows.length - 30} more rows</td></tr>` : '';
      return `
  <div class="pt-slide">
    <h2 class="pt-head">${this.esc(section.title)}</h2>
    ${section.description ? `<p class="pt-desc">${this.esc(section.description)}</p>` : ''}
    <table><thead><tr>${head}</tr></thead><tbody>${rows}${overflow}</tbody></table>
  </div>`;
    }).join('');

    const body = `
  <div class="pt-deck">
    <div class="pt-slide pt-title">
      <div class="pt-brand">${this.esc(BRAND.fullName)}</div>
      <div class="pt-doctitle">${this.esc(doc.title)}</div>
      ${doc.subtitle ? `<div class="pt-doctitle-sub">${this.esc(doc.subtitle)}</div>` : ''}
      <div class="pt-gen">Generated: ${this.esc(nowStamp())} by ${this.esc(this.currentUserName)}</div>
      <div class="pt-version">${this.esc(BRAND.version)} | ${this.esc(BRAND.tagline)}</div>
    </div>
    ${metaSlide}
    ${sumSlide}
    ${sectionSlides}
    <div class="pt-slide pt-footer">
      <div class="pt-brand">${this.esc(BRAND.fullName)}</div>
      <div class="pt-tag">${this.esc(BRAND.tagline)}</div>
      <div class="pt-version">${this.esc(BRAND.version)} | Generated ${this.esc(nowStamp())}</div>
    </div>
  </div>`;

    const css = `
  * { box-sizing: border-box; }
  body { margin: 0; background: #E2E8F0; color: #1E293B; font-family: Calibri, 'Segoe UI', Arial, sans-serif; }
  .pt-deck { padding: 12px 0; }
  .pt-slide { width: 1000px; height: 562px; margin: 24px auto; padding: 40px 56px; overflow: hidden; box-shadow: 0 10px 40px rgba(30,41,59,.18); break-after: page; }
  .pt-title { background: linear-gradient(100deg, #3730A3 0%, #4338CA 45%, #0284C7 100%); color: #fff; display: flex; flex-direction: column; justify-content: center; }
  .pt-brand { font-size: 28px; font-weight: 700; }
  .pt-doctitle { font-size: 22px; color: #C7D2FE; margin-top: 10px; }
  .pt-doctitle-sub { font-size: 16px; color: #E0E7FF; margin-top: 6px; }
  .pt-gen { font-size: 11px; color: #C7D2FE; margin-top: 16px; }
  .pt-version { font-size: 10px; color: #A5B4FC; margin-top: 10px; }
  .pt-head { font-size: 18px; font-weight: 700; color: #4F46E5; margin: 0 0 14px; }
  .pt-desc { font-size: 11px; font-style: italic; color: #64748B; margin: 0 0 10px; }
  .pt-row { display: flex; gap: 10px; margin: 4px 0; font-size: 13px; }
  .pt-lbl { font-weight: 700; min-width: 180px; }
  .pt-val { color: #334155; }
  .pt-slide table { width: 100%; border-collapse: collapse; font-size: 11px; }
  .pt-slide th { background: #1E1B4B; color: #fff; font-weight: 700; padding: 6px 8px; text-align: left; }
  .pt-slide td { padding: 5px 8px; border-bottom: 1px solid #E2E8F0; }
  .pt-more { text-align: center; font-style: italic; color: #64748B; }
  .pt-footer { background: #F8FAFC; display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; }
  .pt-footer .pt-brand { color: #4F46E5; }
  .pt-tag { font-size: 14px; color: #64748B; margin-top: 6px; }
  .pt-footer .pt-version { color: #94A3B8; margin-top: 12px; }
  @media print { body { background: #fff; } .pt-slide { margin: 0 auto; box-shadow: none; } }`;

    return this.wrapPreviewHtml(`${doc.title} — PowerPoint Preview`, css, body);
  }

  private wrapPreviewHtml(title: string, css: string, body: string): string {
    return `<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8" />
<title>${this.esc(title)}</title>
<style>${css}</style>
</head>
<body>${body}</body>
</html>`;
  }

  private esc(v: any): string {
    return String(v ?? '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  // ───────── HELPERS ─────────

  private formatCellValue(value: any, col: ExportColumn): string {
    if (value === null || value === undefined) return '';
    const v = String(value);
    switch (col.format) {
      case 'currency': return typeof value === 'number' ? `$${value.toLocaleString('en-US', { minimumFractionDigits: 2 })}` : v;
      case 'percent': return typeof value === 'number' ? `${value.toFixed(1)}%` : v;
      case 'date': return value ? new Date(value).toLocaleDateString() : '';
      case 'boolean': return value ? 'Yes' : 'No';
      case 'number': return typeof value === 'number' ? value.toLocaleString() : v;
      default: return v;
    }
  }

  private csvEscape(val: string): string {
    return val.replace(/"/g, '""');
  }

  private colLetter(col: number): string {
    return String.fromCharCode(64 + col);
  }
}