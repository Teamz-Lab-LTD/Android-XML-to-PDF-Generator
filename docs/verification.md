# Verification and platform coverage

Local checks on 2026-11-05. This is evidence for the tested revision and fixtures, not a promise that every layout, printer or OS version works. CI results are linked from the repository badge.

| Target | Evidence |
|---|---|
| Android XML/Views | JVM contract tests, debug/release builds and lint pass. Pixel 8a / Android 16 verified A4/A5 dimensions, multipage output, validation and View-bound restoration. All 13 instrumentation tests pass on Pixel 8a, including shared-picker cancellation after actual Activity recreation. |
| Jetpack Compose / Android | Pixel 8a export opened with PdfRenderer; checked page dimensions and nonblank pixels. Raster output. |
| Flutter / Android | Physical Pixel integration test passed: widget capture, multipage text report, preview. |
| Flutter / iOS | Physical iPhone 13 mini integration test passed for the same flows. |
| Flutter / macOS | Native desktop integration test passed for the same flows. |
| Flutter / web | Release web build passed. Chrome manually verified widget capture (1 page), text report (3 pages), and rendered PDF preview. OS print-dialog output was not tested. |
| Flutter / Windows, Linux | Portable implementation and example hosts included; not runtime-tested here. |
| SwiftUI / macOS | Four PDFKit tests pass: page bounds, selectable fixture text, pagination including last-row preservation, landscape and invalid-option/page-limit handling. |
| SwiftUI / iOS | iPhone 13 mini UI test passed: invoice PDF preview and long report generation. |

## Reproduce

Android needs JDK 17 and Android SDK 36:

```bash
./gradlew :xml-to-pdf:testDebugUnitTest :sample:assembleDebug :sample:assembleRelease \
  :sample:lintDebug :xml-to-pdf:lintDebug :compose-to-pdf:lintDebug
./gradlew :sample:connectedDebugAndroidTest
./gradlew :xml-to-pdf:publishReleasePublicationToMavenLocal \
  :compose-to-pdf:publishReleasePublicationToMavenLocal
```

Keep a physical test device unlocked. Instrumentation requires the app and system document picker to be visible. The release sample is minified with R8. `python3 scripts/release_smoke.py` independently verified XML invoice export, PdfRenderer preview, multipage report and Compose export on Pixel 8a. It uses the ordinary release APK with no test-only keep rules. Set `ANDROID_SERIAL` when multiple devices are attached.

Flutter was checked with Flutter 3.38.5 / Dart 3.10.4:

```bash
cd packages/layout_to_pdf
flutter pub get
flutter analyze
flutter test
cd example
flutter pub get
flutter test
flutter test integration_test/export_test.dart -d <device-id>
flutter build web
```

Apple was checked with Xcode 26.0.1:

```bash
swift test
xcodebuild -scheme LayoutToPDF -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath artifacts/apple-ci CODE_SIGNING_ALLOWED=NO build
# Optional physical iPhone demo (configure your own signing team):
cd apple/Example
xcodegen generate
xcodebuild -project LayoutPDFDemo.xcodeproj -scheme LayoutPDFDemo \
  -destination 'id=<your-device-id>' DEVELOPMENT_TEAM=<your-team> test
```

No signing identities, device identifiers, personal documents or private client source are included. README screenshots show fictional sample documents. The hero is generated artwork.

## Remaining limits

No printer fleet, OEM-wide Android matrix, very large-document stress benchmark, PDF/A, PDF accessibility-tagging, or multilingual shaping certification has been performed. Test the fonts and languages used by your application. Lifecycle recovery covers Activity recreation; process-death recovery is not implemented. Continuous capture slicing can divide a row.
