import 'package:flutter_test/flutter_test.dart';
import 'package:layout_to_pdf_example/main.dart';

void main() {
  testWidgets('shows both export paths', (tester) async {
    await tester.pumpWidget(const ExportApp());
    await tester.scrollUntilVisible(find.text('Export widget PDF'), 150);
    expect(find.text('Export widget PDF'), findsOneWidget);
    await tester.scrollUntilVisible(find.text('Build text report'), 100);
    expect(find.text('Build text report'), findsOneWidget);
  });
}
