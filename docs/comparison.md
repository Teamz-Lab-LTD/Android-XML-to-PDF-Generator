# Choosing a PDF tool

Reviewed 2026-11-05 from upstream documentation. This is a scope comparison, not a benchmark or claim of superiority. No competitor code or private client code was copied.

| Tool | Useful when | Difference from this repository |
|---|---|---|
| [Android PdfDocument](https://developer.android.com/reference/android/graphics/pdf/PdfDocument) | You want direct Canvas control | The Android module adds source selection, sizing, storage, callbacks and regression examples on top of this platform API. |
| [ViewPrinter](https://github.com/natario1/ViewPrinter) | You need live document editing/preview, View hierarchies and PDF/image output | Its document editor is outside our scope; we focus on exporting an existing layout and separate platform adapters. |
| [PDFCreatorAndroid](https://github.com/tejpratap46/PDFCreatorAndroid) | You assemble PDF pages from Android Views | It describes View-aware page construction. Our simple continuous slicing can split a row; use explicit page Views for semantic boundaries. |
| [compose-to-pdf](https://github.com/jksalcedo/compose-to-pdf) | Your project is Compose-only | Our optional Compose adapter captures the drawn graphics layer as a bitmap and shares the Android storage path with XML. It does not promise vector Compose text. |
| [dart_pdf / printing](https://github.com/DavBfr/dart_pdf) | You need a mature Flutter/Dart document engine and printing integration | We use these tools rather than duplicate their engine. Our Flutter package adds a bounded widget-capture API and a small report recipe. Use upstream directly for advanced tables, forms or rich document design. |
| [SwiftUI ImageRenderer](https://developer.apple.com/documentation/swiftui/imagerenderer) | You control an Apple-only SwiftUI print view | Our Swift package adds paper sizing, pagination, limits, atomic output and PDFKit regression tests. |

## Where Layout to PDF aims to help

- One documented project for existing Android XML/Views, Compose, Flutter and native SwiftUI exports.
- Explicit distinction between pixels and text/vector documents.
- Permission-free app-private Android saves, content-URI results and cancellation/recreation behavior.
- Runnable examples, device evidence, CI and migration notes for people and AI coding assistants.
- Teamz Lab implementation help when an app needs custom layouts, integration or testing.

## Where another tool is a better fit

Use a dedicated document engine for PDF editing, digital signatures, encryption, PDF/A, accessible tagged PDFs, exact table/paragraph pagination or very large reports. This project does not implement those features. No performance or output-quality comparison has been benchmarked against competitors.
