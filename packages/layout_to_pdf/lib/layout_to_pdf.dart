/// Export painted Flutter widgets as PDF bytes. Saving/printing belongs to the host app.
library;

import 'dart:math' as math;
import 'dart:typed_data';
import 'dart:ui' as ui;
import 'package:flutter/rendering.dart';
import 'package:flutter/widgets.dart';
import 'package:pdf/pdf.dart';
import 'package:pdf/widgets.dart' as pw;

export 'package:pdf/pdf.dart' show PdfPageFormat;
export 'report.dart' show ReportSection, buildReportPdf;

/// Wrap exactly the document area, excluding toolbars and export controls.
class PdfCapture extends StatelessWidget {
  const PdfCapture({super.key, required this.captureKey, required this.child});
  final GlobalKey captureKey;
  final Widget child;
  @override
  Widget build(BuildContext context) =>
      RepaintBoundary(key: captureKey, child: child);
}

/// The portable result: no filesystem, network or platform-channel dependency.
class PdfExport {
  const PdfExport({required this.bytes, required this.pageCount});
  final Uint8List bytes;
  final int pageCount;
}

/// Captures an already painted boundary. Off-screen lazy content is not materialized.
///
/// Call after a frame and after fonts/images have loaded. PDF output is raster;
/// use the `pdf` package's document widgets for searchable text and semantic tables.
Future<PdfExport> captureToPdf(
  GlobalKey key, {
  PdfPageFormat? pageFormat,
  double pixelRatio = 2,
  double margin = 24,
  int maxPixels = 16000000,
  int maxPages = 100,
}) async {
  if (!pixelRatio.isFinite || pixelRatio <= 0 || maxPixels <= 0) {
    throw ArgumentError(
        'pixelRatio and maxPixels must be positive and finite.');
  }
  final object = key.currentContext?.findRenderObject();
  if (object is! RenderRepaintBoundary || !object.hasSize) {
    throw StateError('Mount PdfCapture and wait for its first painted frame.');
  }
  final size = object.size;
  if (size.isEmpty || !size.width.isFinite || !size.height.isFinite) {
    throw StateError('The capture area needs a finite, nonzero size.');
  }
  if ((size.width * pixelRatio).ceil() * (size.height * pixelRatio).ceil() >
      maxPixels) {
    throw ArgumentError(
        'Capture exceeds maxPixels. Lower pixelRatio or export smaller sections.');
  }
  final format = pageFormat ?? PdfPageFormat.a4;
  _validatePage(format, margin, maxPages);
  final image = await object.toImage(pixelRatio: pixelRatio);
  try {
    // Crop in image space before embedding so a tall page is not repeated in full in every PDF page.
    final usableWidth = format.width - margin * 2;
    final usableHeight = format.height - margin * 2;
    final sliceHeight = (usableHeight * image.width / usableWidth).floor();
    if (sliceHeight < 1) {
      throw ArgumentError('Printable area is too short for this capture.');
    }
    final count = (image.height / sliceHeight).ceil();
    if (count > maxPages) {
      throw ArgumentError('Export exceeds maxPages. Split the document.');
    }
    final document = pw.Document();
    for (var top = 0; top < image.height; top += sliceHeight) {
      final height = math.min(sliceHeight, image.height - top);
      final recorder = ui.PictureRecorder();
      final canvas = ui.Canvas(recorder);
      canvas.drawColor(const ui.Color(0xFFFFFFFF), ui.BlendMode.src);
      canvas.drawImageRect(
          image,
          ui.Rect.fromLTWH(
              0, top.toDouble(), image.width.toDouble(), height.toDouble()),
          ui.Rect.fromLTWH(0, 0, image.width.toDouble(), height.toDouble()),
          ui.Paint());
      final picture = recorder.endRecording();
      final slice = await picture.toImage(image.width, height);
      picture.dispose();
      try {
        final png = await slice.toByteData(format: ui.ImageByteFormat.png);
        if (png == null) {
          throw StateError('Unable to encode the captured image.');
        }
        final memory = pw.MemoryImage(png.buffer.asUint8List());
        document.addPage(pw.Page(
          pageFormat: format,
          margin: pw.EdgeInsets.all(margin),
          build: (_) => pw.Align(
              alignment: pw.Alignment.topLeft,
              child: pw.Image(memory,
                  width: usableWidth,
                  height: height * usableWidth / image.width)),
        ));
      } finally {
        slice.dispose();
      }
    }
    return PdfExport(bytes: await document.save(), pageCount: count);
  } finally {
    image.dispose();
  }
}

void _validatePage(PdfPageFormat format, double margin, int maxPages) {
  if (!format.width.isFinite ||
      !format.height.isFinite ||
      format.width <= 0 ||
      format.height <= 0 ||
      !margin.isFinite ||
      margin < 0 ||
      margin * 2 >= math.min(format.width, format.height) ||
      maxPages < 1) {
    throw ArgumentError(
        'Use a finite page size, nonnegative margins and positive maxPages.');
  }
}
