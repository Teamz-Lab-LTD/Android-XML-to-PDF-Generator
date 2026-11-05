// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "LayoutToPDF",
    platforms: [.iOS(.v16), .macOS(.v13)],
    products: [.library(name: "LayoutToPDF", targets: ["LayoutToPDF"])],
    targets: [
        .target(name: "LayoutToPDF", path: "apple/Sources/LayoutToPDF"),
        .testTarget(name: "LayoutToPDFTests", dependencies: ["LayoutToPDF"], path: "apple/Tests/LayoutToPDFTests")
    ]
)
