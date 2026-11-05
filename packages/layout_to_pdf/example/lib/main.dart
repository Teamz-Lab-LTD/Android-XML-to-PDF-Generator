import 'package:flutter/material.dart';
import 'package:layout_to_pdf/layout_to_pdf.dart';
import 'package:printing/printing.dart';

void main() => runApp(const ExportApp());

class ExportApp extends StatelessWidget {
  const ExportApp({super.key});
  @override
  Widget build(BuildContext context) => MaterialApp(
    title: 'Layout to PDF',
    debugShowCheckedModeBanner: false,
    theme: ThemeData(
      colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xFF173D32)),
      scaffoldBackgroundColor: const Color(0xFFF4F2EC),
      useMaterial3: true,
    ),
    home: const ExportStudio(),
  );
}

class ExportStudio extends StatefulWidget {
  const ExportStudio({super.key});
  @override
  State<ExportStudio> createState() => _ExportStudioState();
}

class _ExportStudioState extends State<ExportStudio> {
  final captureKey = GlobalKey();
  PdfExport? result;
  String status = 'Ready to export.';
  bool busy = false;
  Future<void> export(bool report) async {
    setState(() {
      busy = true;
      status = 'Generating PDF…';
    });
    try {
      final output = report
          ? await buildReportPdf(
              title: 'Project report',
              sections: [
                ReportSection(
                  'Delivery notes',
                  List.generate(
                    70,
                    (i) =>
                        'Milestone ${i + 1}: verified document output, clear API examples and reproducible tests.',
                  ),
                ),
              ],
            )
          : await captureToPdf(captureKey);
      if (mounted) {
        setState(() {
          result = output;
          status = 'PDF ready · ${output.pageCount} page(s)';
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          status = 'Export failed: $e';
        });
      }
    } finally {
      if (mounted) {
        setState(() {
          busy = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('Layout to PDF')),
    body: SafeArea(
      child: Center(
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 720),
          child: ListView(
            padding: const EdgeInsets.all(24),
            children: [
              const Text(
                'TEAMZ LAB · FLUTTER',
                style: TextStyle(
                  fontSize: 12,
                  fontWeight: FontWeight.bold,
                  letterSpacing: 2,
                ),
              ),
              const SizedBox(height: 18),
              const Text(
                'One widget.\nA real document.',
                style: TextStyle(
                  fontSize: 36,
                  height: 1.1,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF173D32),
                ),
              ),
              const SizedBox(height: 12),
              const Text(
                'Capture the rendered card, or build a selectable-text report with flowing pages.',
              ),
              const SizedBox(height: 24),
              PdfCapture(
                captureKey: captureKey,
                child: const ColoredBox(
                  color: Colors.white,
                  child: Padding(
                    padding: EdgeInsets.all(24),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'SAMPLE RECEIPT',
                          style: TextStyle(
                            color: Color(0xFF27745A),
                            fontSize: 12,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        SizedBox(height: 20),
                        Text(
                          'Made with Flutter',
                          style: TextStyle(
                            color: Color(0xFF173D32),
                            fontSize: 26,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        SizedBox(height: 20),
                        Divider(),
                        SizedBox(height: 12),
                        Text(
                          'Design consultation          \$120\nDevelopment                     \$280\nDocumentation                  \$100',
                          style: TextStyle(height: 2),
                        ),
                        SizedBox(height: 16),
                        Text(
                          'TOTAL   \$500',
                          style: TextStyle(fontWeight: FontWeight.bold),
                        ),
                        SizedBox(height: 12),
                        Text(
                          'Demo values only. No payment is due.',
                          style: TextStyle(fontSize: 11),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 20),
              FilledButton(
                key: const Key('capture'),
                onPressed: busy ? null : () => export(false),
                child: const Text('Export widget PDF'),
              ),
              OutlinedButton(
                key: const Key('report'),
                onPressed: busy ? null : () => export(true),
                child: const Text('Build text report'),
              ),
              Text(status, key: const Key('status')),
              if (result != null) ...[
                const SizedBox(height: 16),
                FilledButton(
                  onPressed: () => Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (_) => Scaffold(
                        appBar: AppBar(title: const Text('PDF preview')),
                        body: PdfPreview(
                          build: (_) async => result!.bytes,
                          canChangeOrientation: false,
                          canChangePageFormat: false,
                          canDebug: false,
                          pdfFileName: 'layout-to-pdf.pdf',
                        ),
                      ),
                    ),
                  ),
                  child: const Text('Preview, print or share'),
                ),
              ],
              const SizedBox(height: 16),
              const Text(
                'Capture exports pixels. Reports export text.\nBoth are generated locally.',
                style: TextStyle(fontSize: 12),
              ),
            ],
          ),
        ),
      ),
    ),
  );
}
