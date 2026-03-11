# NumScan

Android camera app that scans phone numbers in real-time using ML Kit OCR and CameraX.

## Features
- Real-time phone number detection via camera
- International formats: +1, +44, +66 and more
- One-tap copy to clipboard
- Neon scan UI with Material Design 3

## Build

./gradlew assembleDebug

APK: app/build/outputs/apk/debug/app-debug.apk

## Stack
- CameraX 1.3.1
- ML Kit Text Recognition 16.0.0
- Material Design 3
- Kotlin

## Structure

MainActivity.kt - Launch screen
ScanActivity.kt - Camera and detection screen
PhoneNumberEngine.kt - Phone number parsing