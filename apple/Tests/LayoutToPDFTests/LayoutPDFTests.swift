import XCTest
import SwiftUI
import PDFKit
@testable import LayoutToPDF

final class LayoutPDFTests: XCTestCase {
    func testA4ContainsSelectableTextAndCorrectDimensions() async throws {
        try await MainActor.run {
            let result = try LayoutPDF.render(Text("Layout PDF invoice 001").font(.system(size: 24)))
            let pdf = try XCTUnwrap(PDFDocument(data: result.data))
            XCTAssertEqual(result.pageCount, 1)
            XCTAssertEqual(pdf.pageCount, 1)
            XCTAssertEqual(pdf.page(at: 0)?.bounds(for: .mediaBox).size, CGSize(width: 595, height: 842))
            XCTAssertTrue(pdf.string?.contains("Layout PDF invoice 001") == true)
        }
    }
    func testLongContentPaginatesAndKeepsLastRow() async throws {
        try await MainActor.run {
            let result = try LayoutPDF.render(VStack(alignment: .leading, spacing: 12) {
                ForEach(0..<80) { i in Text("Report row \(i)").font(.system(size: 18)) }
            })
            let pdf = try XCTUnwrap(PDFDocument(data: result.data))
            XCTAssertGreaterThan(result.pageCount, 1)
            XCTAssertEqual(result.pageCount, pdf.pageCount)
            XCTAssertTrue(pdf.page(at: 0)?.string?.contains("Report row 0") == true)
            XCTAssertTrue(pdf.page(at: pdf.pageCount - 1)?.string?.contains("Report row 79") == true)
        }
    }
    func testA5Landscape() async throws {
        try await MainActor.run {
            let result = try LayoutPDF.render(Text("Landscape"), options: PDFOptions.a5.landscape)
            let pdf = try XCTUnwrap(PDFDocument(data: result.data))
            XCTAssertEqual(pdf.page(at: 0)?.bounds(for: .mediaBox).size, CGSize(width: 595, height: 420))
        }
    }
    func testInvalidMarginsAndPageLimits() async throws {
        try await MainActor.run {
            XCTAssertThrowsError(try LayoutPDF.render(Text("Invalid"), options: PDFOptions(margin: 500)))
            XCTAssertThrowsError(try LayoutPDF.render(Color.green.frame(height: 3000), options: PDFOptions(maxPages: 1)))
        }
    }
}
