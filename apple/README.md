# Layout to PDF for SwiftUI · Teamz Lab

Native SwiftUI → Core Graphics PDF, iOS 16+ and macOS 13+. The root `Package.swift` exposes `LayoutToPDF`. Add the repository URL in Xcode's **Add Package Dependencies**, select a reviewed revision, then add the `LayoutToPDF` product.

```swift
import LayoutToPDF

// On the main actor, with a dedicated print view:
let result = try LayoutPDF.render(InvoiceView(), options: .a4)
try result.write(to: destinationURL)
// result.data, result.pageCount
```

`PDFOptions` supports paper dimensions in points, margin, content width and maximum pages. `.a5.landscape` is available. Fonts and ordinary SwiftUI text can remain vector/selectable; the actual rendering of effects and images belongs to Apple's ImageRenderer. The tests verify selectable text for the supplied fixtures, not every possible view.

Use a non-lazy VStack for document content. Load data/images before rendering. WebViews, video and platform-backed views are outside ImageRenderer's supported content. Long content is sliced geometrically and may split a row. For semantic pagination, compose explicit document pages or use a document-layout engine. UIKit-only views and AppKit-only views do not have an adapter here.

```bash
swift test
xcodegen generate --spec apple/Example/project.yml
# Open apple/Example/LayoutPDFDemo.xcodeproj and select your signing team.
```

The sample includes invoice/report generation, PDFKit preview and the system Save PDF flow. Production host apps own destination permissions and security-scoped bookmarks.

[Teamz Lab builds Android, iOS and Flutter apps](https://teamzlab.com/?utm_source=github&utm_medium=opensource&utm_campaign=layout_to_pdf#contact). Apache-2.0, without mandatory branding in your PDFs.
