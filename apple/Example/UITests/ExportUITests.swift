import XCTest

final class ExportUITests: XCTestCase {
    func testInvoiceAndReport() {
        let app = XCUIApplication()
        app.launch()
        XCTAssertTrue(app.buttons["export_invoice"].waitForExistence(timeout: 10))
        app.buttons["export_invoice"].tap()
        XCTAssertTrue(app.staticTexts["PDF ready · 1 page(s)"].waitForExistence(timeout: 10))
        let screenshot = XCTAttachment(screenshot: app.screenshot())
        screenshot.name = "swiftui-invoice-iphone"
        screenshot.lifetime = .keepAlways
        add(screenshot)
        app.buttons["export_report"].tap()
        let status = app.staticTexts["export_status"]
        XCTAssertTrue(status.label.hasPrefix("PDF ready"))
        XCTAssertFalse(status.label.contains("· 1 page"))
    }
}
