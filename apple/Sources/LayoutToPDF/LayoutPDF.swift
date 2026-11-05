import CoreGraphics
import Foundation
import SwiftUI

/// Page dimensions are PDF points (72 points = 1 inch).
public struct PDFOptions {
    public var pageSize: CGSize
    public var margin: CGFloat
    public var contentWidth: CGFloat
    public var maxPages: Int

    public init(pageSize: CGSize = CGSize(width: 595, height: 842), margin: CGFloat = 24,
                contentWidth: CGFloat = 547, maxPages: Int = 100) {
        self.pageSize = pageSize
        self.margin = margin
        self.contentWidth = contentWidth
        self.maxPages = maxPages
    }
    public static var a4: PDFOptions { PDFOptions() }
    public static var a5: PDFOptions { PDFOptions(pageSize: CGSize(width: 420, height: 595), contentWidth: 372) }
    public var landscape: PDFOptions {
        PDFOptions(pageSize: CGSize(width: max(pageSize.width, pageSize.height), height: min(pageSize.width, pageSize.height)),
                   margin: margin, contentWidth: contentWidth, maxPages: maxPages)
    }
}

public struct PDFExport {
    public let data: Data
    public let pageCount: Int
    /// Atomic write. Directory selection and user permissions belong to the host app.
    public func write(to url: URL) throws { try data.write(to: url, options: .atomic) }
}

public enum PDFExportError: Error, LocalizedError {
    case invalidOptions, emptyContent, tooManyPages, rendererUnavailable
    public var errorDescription: String? {
        switch self {
        case .invalidOptions: return "Use finite positive page/content dimensions and margins that leave printable space."
        case .emptyContent: return "The SwiftUI content has no printable size."
        case .tooManyPages: return "The document exceeds maxPages. Split it into smaller documents."
        case .rendererUnavailable: return "The system could not render this SwiftUI content."
        }
    }
}

/// Local SwiftUI-to-PDF rendering. No network, screenshot service, or third-party dependency.
///
/// Pass a dedicated print view with loaded data. WebViews, video, platform-backed views and
/// unmaterialized lazy-list content are not supported by SwiftUI ImageRenderer.
@MainActor
public enum LayoutPDF {
    public static func render<Content: View>(_ content: Content, options: PDFOptions = .a4) throws -> PDFExport {
        let pw = options.pageSize.width, ph = options.pageSize.height, margin = options.margin
        guard pw.isFinite, ph.isFinite, pw > 0, ph > 0,
              margin.isFinite, margin >= 0, margin * 2 < min(pw, ph),
              options.contentWidth.isFinite, options.contentWidth > 0, options.maxPages > 0 else {
            throw PDFExportError.invalidOptions
        }
        let renderer = ImageRenderer(content: content
            .frame(width: options.contentWidth)
            .fixedSize(horizontal: false, vertical: true)
            .environment(\.colorScheme, .light))
        renderer.proposedSize = ProposedViewSize(width: options.contentWidth, height: nil)
        renderer.isOpaque = true
        let buffer = NSMutableData()
        guard let consumer = CGDataConsumer(data: buffer as CFMutableData) else { throw PDFExportError.rendererUnavailable }
        var mediaBox = CGRect(origin: .zero, size: options.pageSize)
        guard let context = CGContext(consumer: consumer, mediaBox: &mediaBox, nil) else { throw PDFExportError.rendererUnavailable }
        var failure: PDFExportError?
        var pages = 0
        renderer.render { size, draw in
            guard size.width.isFinite, size.height.isFinite, size.width > 0, size.height > 0 else {
                failure = .emptyContent; return
            }
            let scale = (pw - margin * 2) / size.width
            let pageHeight = ph - margin * 2
            let count = ceil(size.height * scale / pageHeight)
            guard count.isFinite, count <= CGFloat(options.maxPages) else { failure = .tooManyPages; return }
            pages = Int(count)
            for page in 0..<pages {
                context.beginPDFPage(nil)
                context.setFillColor(CGColor(gray: 1, alpha: 1))
                context.fill(mediaBox)
                context.saveGState()
                context.clip(to: CGRect(x: margin, y: margin, width: pw - margin * 2, height: pageHeight))
                context.translateBy(x: margin, y: ph - margin - size.height * scale + CGFloat(page) * pageHeight)
                context.scaleBy(x: scale, y: scale)
                draw(context)
                context.restoreGState()
                context.endPDFPage()
            }
        }
        context.closePDF()
        if let failure { throw failure }
        guard pages > 0 else { throw PDFExportError.rendererUnavailable }
        return PDFExport(data: buffer as Data, pageCount: pages)
    }
}
