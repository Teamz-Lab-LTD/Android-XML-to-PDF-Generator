import 'package:flutter/foundation.dart';
import 'package:pdf/pdf.dart';
import 'package:pdf/widgets.dart' as pw;
import 'layout_to_pdf.dart' show PdfExport;

/// Plain data: safe to send to the worker isolate on native Flutter platforms.
class ReportSection {
  const ReportSection(this.heading, this.paragraphs);
  final String heading;
  final List<String> paragraphs;
}

/// Builds a text-first report with repeated header/footer and flowing paragraphs.
///
/// Supply a bundled TTF font for non-Latin text, and optional fallback TTFs.
/// Script shaping support is inherited from `pdf`. Test your actual language/fonts.
/// Native platforms build in an isolate; Flutter web runs compute on the main thread.
Future<PdfExport> buildReportPdf({
  required String title,
  required List<ReportSection> sections,
  Uint8List? fontBytes,
  List<Uint8List> fallbackFontBytes = const [],
  int maxPages = 100,
}) {
  if (title.trim().isEmpty || sections.isEmpty || maxPages < 1) {
    throw ArgumentError(
        'Provide a title, at least one section and a positive page limit.');
  }
  return compute(
      _buildReport, (title, sections, fontBytes, fallbackFontBytes, maxPages));
}

Future<PdfExport> _buildReport(
    (
      String,
      List<ReportSection>,
      Uint8List?,
      List<Uint8List>,
      int
    ) input) async {
  final (title, sections, fontBytes, fallbackBytes, maxPages) = input;
  final font = fontBytes == null
      ? pw.Font.helvetica()
      : pw.Font.ttf(ByteData.sublistView(fontBytes));
  final fallback = fallbackBytes
      .map((bytes) => pw.Font.ttf(ByteData.sublistView(bytes)))
      .toList();
  final document = pw.Document(title: title, creator: 'Layout to PDF');
  document.addPage(pw.MultiPage(
    pageFormat: PdfPageFormat.a4,
    margin: const pw.EdgeInsets.all(36),
    maxPages: maxPages,
    theme:
        pw.ThemeData.withFont(base: font, bold: font, fontFallback: fallback),
    header: (_) => pw.Container(
        margin: const pw.EdgeInsets.only(bottom: 16),
        child: pw.Text(title,
            style: pw.TextStyle(
                font: font, fontSize: 18, color: PdfColor.fromHex('#173D32')))),
    footer: (context) => pw.Align(
        alignment: pw.Alignment.centerRight,
        child: pw.Text('Page ${context.pageNumber} / ${context.pagesCount}',
            style: const pw.TextStyle(fontSize: 9))),
    build: (_) => [
      for (final section in sections) ...[
        pw.Header(level: 1, text: section.heading),
        for (final paragraph in section.paragraphs)
          pw.Paragraph(text: paragraph),
      ],
    ],
  ));
  final bytes = await document.save();
  final count = document.document.pdfPageList.pages.length;
  // The upstream MultiPage guard is debug-only in some versions. Enforce our contract in release too.
  if (count > maxPages) {
    throw StateError('Report exceeds maxPages. Split it into smaller reports.');
  }
  return PdfExport(bytes: bytes, pageCount: count);
}
