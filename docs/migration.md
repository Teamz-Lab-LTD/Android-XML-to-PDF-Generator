# Migrating from Android 2.6.8

The Java package remains `com.gkemon.XMLtoPDF`. The repository URL remains unchanged. This source revision contains breaking corrections and is prepared as 3.0.0; see README for installation status.

- App-specific saves no longer request storage permissions. Remove READ/WRITE_EXTERNAL_STORAGE declarations if your app has no other use for them. The library no longer includes Dexter or RxJava.
- Shared storage uses the system document picker. Register `XmlToPDFLifecycleObserver` in `onCreate`. Read `SuccessResponse.getUri()` with `ContentResolver`; a shared document may have no filesystem path, so `getFile()` can be null.
- `getPdfDocument()` is deprecated and returns null for new exports. The document is closed before completion; use the file/URI.
- `setPageSize(A4/A5)` now works, before `setFileName`. Sizes are PDF points. The default `AS_LIKE_XML` retains one page per source View when margins are zero.
- `setLandscape`, `setPageMargin` and `setBackgroundColor` are optional builder settings after `setFileName`.
- Tall fixed-paper content is clipped into consecutive page slices. This is geometric pagination, not paragraph/table-aware layout. Supply explicit page Views when a row must not split.
- `setFolderNameOrPath` accepts relative folders or absolute paths inside app files directories. Arbitrary shared filesystem paths are rejected; use the document picker. Paths are no longer silently rewritten.
- `.pdf` is appended only once. Existing app-specific files with the same name are atomically replaced. Choose unique names if you need history.
- Build on the main thread. XML inflation and View drawing use the UI thread; file writing uses a worker. Create a fresh builder for each export. Invalid inputs report `onFailure`, followed by `onFinishPDFGeneration`.
- The save observer retains the staged PDF across Activity recreation. Call `setResultListener` on the new observer to receive the result in the new screen. A process restart does not restore an unfinished export; start it again. Cancel returns a `CancellationException` through `FailureResponse`.
- A second export through the same observer is rejected while its picker is active. Register one observer per Activity.
- An absent PDF viewer does not make a successful file write fail. The URI is still returned and the listener receives a log message.
- Dark-screen styling is not automatically rewritten. Inflate a light print layout or explicitly set text colors. A white page background alone cannot fix white text.
- The renamed internal FileProvider paths resource avoids common host resource-name collisions. The authority stays `${applicationId}.xmlToPdf.provider`.

## Optional modules

Compose is opt-in; existing XML users do not need it. SwiftUI and Flutter are separate packages, not Android binaries wrapped for other operating systems. Their API and rendering constraints are described in their own READMEs.
