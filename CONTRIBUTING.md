# Contributing

A small reproducible fix is more useful than a speculative rewrite. Open an issue with the export path, OS/framework versions, a minimal example and the expected document. Remove private data from PDFs and screenshots.

Run the relevant checks in [docs/verification.md](docs/verification.md). Add a regression test for rendering, storage or lifecycle changes. Keep the Android XML module independent of Compose. Keep Flutter free of platform-specific file I/O. Never add network calls, analytics or a watermark to exported documents.

Documentation must distinguish bitmap capture from text/vector rendering. Do not advertise an OS as tested without recording the actual test. For native dependencies, preserve supported minimum OS versions or explain the breaking change.

Commercial integrations: [Teamz Lab](https://teamzlab.com/?utm_source=github&utm_medium=opensource&utm_campaign=layout_to_pdf#contact). Community contributions remain welcome under Apache-2.0; hiring Teamz Lab is not required.
