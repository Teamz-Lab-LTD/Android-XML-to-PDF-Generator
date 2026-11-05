# Integrating with an AI coding assistant

Use the real API rather than asking a model to invent a PDF builder.

1. Identify your source: Android XML/View, rendered Compose, rendered Flutter widget, a text report, or SwiftUI.
2. Read the corresponding quickstart and compile the sample before adapting it.
3. Use PDF points for paper size. Choose A4/A5, margins, content width and a page limit explicitly.
4. Build a dedicated light print layout. Wait for fonts, images and data before capture.
5. Test the PDF: opens successfully, expected page count and dimensions, first/last content present, correct language, readable colors.
6. Test cancel, rotation and sharing separately. Never treat an attempted save as success.

## A useful prompt

> Integrate Layout to PDF using the API from this repository. Use [platform/export path]. Export [document] as A4 with 24-point margins. Preserve the existing data model. Include a test that opens the generated PDF and checks page count, dimensions and nonblank content. Do not invent API methods. Do not claim off-screen lazy content or selectable text for bitmap capture. Keep saving/printing user-controlled.

This repository has no AI service, model dependency or API-key requirement. Its value is the documented behavior and verification, whether a person or a coding assistant performs the integration.
