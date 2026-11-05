# Changelog

## 3.0.0 source preview

- Broaden the project to Layout to PDF by Teamz Lab; keep the repository URL and Java package.
- Remove obsolete storage permission gating, Dexter and RxJava from the Android core.
- Add content-URI results, page counts, A4/A5 sizing, landscape, margins and continuous pagination.
- Close streams/documents, atomically write private outputs, validate paths and restore source View bounds.
- Handle document-picker cancellation and Activity recreation with a retained staged PDF.
- Add optional Jetpack Compose capture, a portable Flutter capture/report package and a SwiftUI package.
- Replace the sample home with runnable export/preview flows, device tests, CI and current documentation.

This is a source-level preview, not a claim that 3.0.0 has been published to a package registry. Read [migration notes](docs/migration.md) before adopting.
