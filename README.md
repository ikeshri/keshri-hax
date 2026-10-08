# Keshri Hax 2.0

A redesigned offline chess position analyzer UI for Android.

## What is included
- Cyber/neon home screen
- White/Black selection with real selected state
- Android MediaProjection permission flow
- Floating analysis panel with status, best move, evaluation, depth and confidence
- Pause / Rescan / Stop controls
- Local TensorFlow Lite piece-recognition pipeline
- Local Stockfish UCI integration
- Android 14+ mediaProjection foreground-service declarations
- GitHub Actions build without requiring gradlew in the repository

## Required binary assets
The project intentionally does not fake binary model/engine files.

Add:
1. `app/src/main/assets/chess_pieces.tflite`
   - 13 output classes:
     0 empty
     1 white pawn
     2 white knight
     3 white bishop
     4 white rook
     5 white queen
     6 white king
     7 black pawn
     8 black knight
     9 black bishop
     10 black rook
     11 black queen
     12 black king
   - Input expected by the sample recognizer: 64x64 RGB float32.

2. `app/src/main/assets/stockfish`
   - A compatible Android/ARM executable Stockfish binary.
   - The binary must be appropriate for the phone architecture.

Without these two assets the APK still builds, but the overlay will report MODEL MISSING or ENGINE MISSING instead of pretending analysis worked.

## Android permission flow
Android 14+ requires the mediaProjection foreground-service type and permission. The app asks for screen-capture consent before starting the service.

Use this app for offline positions, puzzles, and self-game analysis.
