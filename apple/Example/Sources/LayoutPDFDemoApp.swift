import SwiftUI
import PDFKit
import UniformTypeIdentifiers
import LayoutToPDF

@main
struct LayoutPDFDemoApp: App {
    var body: some Scene { WindowGroup { ExportStudio() } }
}

struct PDFFile: FileDocument {
    static var readableContentTypes: [UTType] { [.pdf] }
    var data: Data
    init(data: Data) { self.data = data }
    init(configuration: ReadConfiguration) throws { data = configuration.file.regularFileContents ?? Data() }
    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper { FileWrapper(regularFileWithContents: data) }
}
struct ExportStudio: View {
    @State private var result: PDFExport?
    @State private var status = "Ready to export."
    @State private var saving = false
    private let green = Color(red: 0.09, green: 0.24, blue: 0.20)
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    Text("TEAMZ LAB · SWIFTUI").font(.caption.bold()).tracking(2).foregroundStyle(green)
                    Text("Layouts in.\nDocuments out.").font(.system(size: 38, weight: .bold)).foregroundStyle(green)
                    Text("Native SwiftUI export with selectable text and real A4 pages.").foregroundStyle(.secondary)
                    VStack(alignment: .leading, spacing: 14) {
                        Text("Try an export").font(.title2.bold())
                        Button("Export SwiftUI invoice") { export(long: false) }
                            .accessibilityIdentifier("export_invoice").buttonStyle(.borderedProminent)
                        Button("Export multi-page report") { export(long: true) }
                            .accessibilityIdentifier("export_report").buttonStyle(.bordered)
                        Text(status).font(.caption).accessibilityIdentifier("export_status")
                    }.frame(maxWidth: .infinity, alignment: .leading).padding(20).background(.white, in: RoundedRectangle(cornerRadius: 18))
                    if let result {
                        PDFPreview(data: result.data).frame(height: 460).accessibilityIdentifier("pdf_preview")
                        Button("Save PDF") { saving = true }.buttonStyle(.borderedProminent)
                        Text("\(result.pageCount) page(s) · Local generation").font(.caption).foregroundStyle(.secondary)
                    }
                    Text("SwiftUI → Core Graphics → PDF\nNo server. No account. No API key.").font(.caption).foregroundStyle(.secondary)
                }.padding(24)
            }.background(Color(red: 0.96, green: 0.95, blue: 0.93)).tint(green)
                .navigationTitle("Layout to PDF").navigationBarTitleDisplayMode(.inline)
                .fileExporter(isPresented: $saving, document: PDFFile(data: result?.data ?? Data()), contentType: .pdf, defaultFilename: "layout-to-pdf") { outcome in
                    switch outcome { case .success: status = "Saved PDF"; case .failure(let error): status = error.localizedDescription }
                }
        }
    }
    private func export(long: Bool) {
        do {
            let output = try LayoutPDF.render(InvoiceDocument(long: long))
            let url = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0].appendingPathComponent(long ? "swiftui-report.pdf" : "swiftui-invoice.pdf")
            try output.write(to: url)
            result = output
            status = "PDF ready · \(output.pageCount) page(s)"
        } catch { status = error.localizedDescription }
    }
}
struct InvoiceDocument: View {
    var long: Bool
    var body: some View {
        VStack(alignment: .leading, spacing: 20) {
            Text("TEAMZ LAB").font(.headline).foregroundStyle(Color(red: 0.15, green: 0.45, blue: 0.35))
            Text(long ? "Project report" : "Invoice").font(.system(size: 44, weight: .bold))
            Text("SAMPLE DOCUMENT / 001").font(.caption)
            Divider()
            ForEach(0..<(long ? 60 : 3), id: \.self) { i in
                HStack { Text("\(i + 1). \(long ? "Project milestone" : "Design service")"); Spacer(); Text(long ? "Complete" : "$100") }
                    .font(.system(size: 18)).padding(.vertical, 8)
            }
            Divider()
            Text(long ? "END OF REPORT" : "TOTAL  $300").font(.title2.bold())
            Text("Generated locally from SwiftUI. Demo values only.").font(.caption)
        }.foregroundStyle(Color(red: 0.09, green: 0.24, blue: 0.20)).padding(20).background(.white)
    }
}
struct PDFPreview: UIViewRepresentable {
    var data: Data
    func makeUIView(context: Context) -> PDFView { let view = PDFView(); view.autoScales = true; return view }
    func updateUIView(_ view: PDFView, context: Context) { view.document = PDFDocument(data: data) }
}
