import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:layout_to_pdf_example/main.dart' as demo;

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();
  testWidgets('capture, report and preview on a real Flutter engine', (
    tester,
  ) async {
    demo.main();
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.byKey(const Key('capture')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('capture')));
    await tester.pumpAndSettle();
    for (
      var i = 0;
      i < 60 && find.textContaining('PDF ready').evaluate().isEmpty;
      i++
    ) {
      await tester.pump(const Duration(milliseconds: 500));
    }
    final captureStatus = tester
        .widget<Text>(find.byKey(const Key('status')))
        .data!;
    expect(captureStatus, 'PDF ready · 1 page(s)', reason: captureStatus);
    await tester.ensureVisible(find.byKey(const Key('report')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('report')));
    await tester.pumpAndSettle();
    for (
      var i = 0;
      i < 60 && find.textContaining('PDF ready').evaluate().isEmpty;
      i++
    ) {
      await tester.pump(const Duration(milliseconds: 500));
    }
    final status = tester.widget<Text>(find.byKey(const Key('status'))).data!;
    expect(status, startsWith('PDF ready'));
    expect(status, isNot('PDF ready · 1 page(s)'));
    await tester.ensureVisible(find.text('Preview, print or share'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Preview, print or share'));
    await tester.pumpAndSettle(const Duration(seconds: 1));
    expect(find.text('PDF preview'), findsOneWidget);
  });
}
