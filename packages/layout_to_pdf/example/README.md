# Layout to PDF demo · Teamz Lab

A runnable Flutter example for widget capture, flowing text reports, preview, printing and sharing. The receipt contains fictional demonstration values.

```bash
flutter pub get
flutter run -d <device-id>
flutter test
flutter test integration_test/export_test.dart -d <device-id>
```

Use `flutter devices` to select Android, iOS, macOS or a browser. Windows/Linux hosts are included; consult the [verification matrix](../../../docs/verification.md) before assuming runtime coverage.

For physical iOS devices, choose your own development team in Xcode. You may put `DEVELOPMENT_TEAM = YOUR_TEAM_ID` in the ignored `ios/Flutter/Signing.xcconfig`. No signing credentials are included.

The package generates bytes locally. `printing` supplies the host preview/print/share controls. The capture and text-report paths have different capabilities; see the [package README](../README.md).
