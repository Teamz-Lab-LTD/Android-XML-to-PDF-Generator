# Layout to PDF for Flutter · Teamz Lab

Two local export paths: capture a painted widget as a raster PDF, or build a flowing text report. Returns bytes; saving, previewing, sharing and printing stay in your app.

This package is currently distributed from this repository, not pub.dev:

```yaml
dependencies:
  layout_to_pdf:
    git:
      url: https://github.com/Teamz-Lab-LTD/Android-XML-to-PDF-Generator.git
      ref: master # Pin a reviewed commit for production.
      path: packages/layout_to_pdf
```

```dart
final key = GlobalKey();
// In your widget tree:
PdfCapture(captureKey: key, child: InvoiceCard());
// After the frame is painted and images/fonts have loaded:
final result = await captureToPdf(key, pageFormat: PdfPageFormat.a4);
// result.bytes and result.pageCount
```

For searchable reports:

```dart
final result = await buildReportPdf(
  title: 'Delivery report',
  sections: [ReportSection('Completed', ['Design approved.', 'Export verified.'])],
  // fontBytes: bundledTtfBytes, // Supply fonts for non-Latin content.
);
```

Use `printing`'s `PdfPreview`, `Printing.layoutPdf` or `Printing.sharePdf` with the returned bytes. The runnable [example](example/lib/main.dart) includes both export paths and PDF preview.

**Limits:** widget capture is raster, requires a painted `RepaintBoundary`, and does not materialize off-screen lazy-list items. Defaults: 2× capture scale, 16 megapixels, 100 pages. Captured content is geometrically sliced; a row can cross a page boundary. Reports use `pdf` paragraph flow and a repeated header/footer. Font shaping is inherited from that package; test actual languages. Report generation uses `compute` on native platforms; web runs it on the main thread. Large captures still require UI-thread rasterization.

The portable Dart API has no `dart:io` or native channels and is intended for Android, iOS, web, macOS, Windows and Linux. See the root [verification matrix](../../docs/verification.md) for actual tested targets. A target is not declared device-tested merely because Flutter supports it.

The `image` dependency is constrained below 4.11.0 because that release failed compilation with the resolved `archive` 4.0.9 on the tested Flutter SDK. Review that bound when upgrading dependencies.

Built by [Teamz Lab](https://teamzlab.com/?utm_source=github&utm_medium=opensource&utm_campaign=layout_to_pdf#contact). Apache-2.0. No required watermark, account or network call.
