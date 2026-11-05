import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:layout_to_pdf/layout_to_pdf.dart';

void main() {
  test('text reports flow across pages with a page count', () async {
    final result = await buildReportPdf(title: 'Engineering report', sections: [
      ReportSection(
          'Progress',
          List.generate(
              80,
              (i) =>
                  'Milestone $i: the export was verified against the expected document.')),
    ]);
    expect(ascii.decode(result.bytes.take(5).toList()), '%PDF-');
    expect(result.pageCount, greaterThan(1));
  });
  test('rejects empty reports', () {
    expect(() => buildReportPdf(title: '', sections: []), throwsArgumentError);
  });

  testWidgets('exports rendered content with PDF header', (tester) async {
    final key = GlobalKey();
    await tester.pumpWidget(MaterialApp(
        home: Center(
            child: PdfCapture(
                captureKey: key,
                child: const SizedBox(
                    width: 300,
                    height: 400,
                    child: ColoredBox(
                        color: Colors.white, child: Text('Invoice / 001')))))));
    await tester.pumpAndSettle();
    await tester.runAsync(() async {
      final result = await captureToPdf(key, pixelRatio: 1);
      expect(ascii.decode(result.bytes.take(5).toList()), '%PDF-');
      expect(result.pageCount, 1);
      expect(result.bytes.length, greaterThan(500));
    });
  });
  testWidgets('tall rendered content paginates', (tester) async {
    final key = GlobalKey();
    await tester.pumpWidget(MaterialApp(
        home: SingleChildScrollView(
            child: PdfCapture(
                captureKey: key,
                child: const SizedBox(
                    width: 300,
                    height: 3000,
                    child: ColoredBox(color: Colors.green))))));
    await tester.pumpAndSettle();
    await tester.runAsync(() async {
      final result = await captureToPdf(key, pixelRatio: 1);
      expect(result.pageCount, greaterThan(1));
    });
  });
  test('rejects unattached capture', () async {
    await expectLater(captureToPdf(GlobalKey()), throwsStateError);
  });
  testWidgets('rejects invalid bounds before allocating an image',
      (tester) async {
    final key = GlobalKey();
    await tester.pumpWidget(MaterialApp(
        home: PdfCapture(
            captureKey: key, child: const SizedBox(width: 300, height: 400))));
    await tester.pumpAndSettle();
    await expectLater(
        captureToPdf(key, pixelRatio: double.infinity), throwsArgumentError);
    await expectLater(captureToPdf(key, maxPixels: 1), throwsArgumentError);
    await expectLater(captureToPdf(key, margin: 999), throwsArgumentError);
  });
}
