# StudySync — Student Note Sharing Application

[![License](https://img.shields.io/badge/license-see--LICENSE-lightgrey)](https://github.com/WahidurRahman77/StudySync-Student-note-sharing-Application/blob/main/LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android-brightgreen)](#)
[![Firebase](https://img.shields.io/badge/service-Firebase-yellow)](#)
[![Cloudinary](https://img.shields.io/badge/service-Cloudinary-blue)](#)
[![Build status](https://img.shields.io/badge/build-pending-lightgrey)](#)
[![Play Store](https://img.shields.io/badge/Play%20Store-pending-orange)](#)

StudySync is an Android application built in Kotlin that allows students to share, discover, and manage study notes and academic resources. The app integrates Firebase for authentication and push notifications and Cloudinary for media storage, making it a practical and scalable note-sharing platform for university communities.

---

## Table of contents
- [About](#about)
- [Key features](#key-features)
- [Tech stack](#tech-stack)
- [Repository layout](#repository-layout)
- [How it fits together](#how-it-fits-together)
- [Quick start](#quick-start)
- [Firebase & Cloudinary setup](#firebase--cloudinary-setup)
- [Environment variables & configuration](#environment-variables--configuration)
- [Running locally](#running-locally)
- [Testing & debugging tips](#testing--debugging-tips)
- [Contributing](#contributing)
- [License](#license)
- [Contact](#contact)

---

## About
StudySync is designed for students to upload and access academic material in a simple, organized, and user-friendly way. It supports social learning workflows such as saving notes, storing profile media, and receiving notifications related to academic updates, new uploads, or account activity.

The project includes a modular Android app structure and integrates cloud services for secure and scalable media handling.

<div style="display:flex;gap:48px;align-items:center;justify-content:center;flex-wrap:wrap;">
  <img src="./Resource%20pic/Dashboard.jpg" alt="Dashboard"
       style="width:30%;max-width:420px;height:460px;object-fit:cover;border-radius:10px;box-shadow:0 6px 18px rgba(0,0,0,0.12);display:block;" />
  <img src="./Resource%20pic/LOGIN%20PAGE.jpg" alt="Login Page"
       style="width:30%;max-width:420px;height:460px;object-fit:cover;border-radius:10px;box-shadow:0 6px 18px rgba(0,0,0,0.12);display:block;" />
  <img src="./Resource%20pic/Dashboard.jpg" alt="Admin Dashboard"
       style="width:30%;max-width:420px;height:460px;object-fit:cover;border-radius:10px;box-shadow:0 6px 18px rgba(0,0,0,0.12);display:block;" />
</div>

## Key features
- Firebase Authentication
- Firebase Cloud Messaging (push notifications)
- Cloudinary integration for media upload and delivery
- Student profile management with profile image uploads
- Clean Kotlin Android structure with feature-oriented screens
- Built using Gradle Kotlin DSL
- Suitable for academic portals and student collaboration workflows
## In APP Features

| Feature | Category | Description | Users |
|---|---|---|---|
| Secure Authentication & Registration | Authentication | Student-ID validation at signup, duplicate-account prevention, and Firebase Auth password recovery. | All users |
| Role-Based Access Control | Access & Permissions | Students request course access; status remains Pending until Admin approval. | Students & Admins |
| Course Feed & Sharing | Collaboration | Interactive course wall for announcements, nested discussions, and attachments (images / PDFs). | Students & Admins |
| Bookmark System (My Notes) | Productivity | Personal, course-wise saved notes with sorting (Date, A→Z) and quick access. | Students |
| In‑App Document Viewer | Content Viewing | Native rendering for large PDFs & high-res images with pinch-to-zoom and external-open option. | All users |
| File Upload & Cloud Storage | Storage | Media and documents uploaded to Cloudinary for scalable delivery and CDN-backed performance. | Students & Admins |
| Real‑Time Notifications | Notifications | Instant in-app badge counters plus system push notifications via Firebase Cloud Messaging. | All users |
| Global Search & Filters | Discovery | Fast search across Departments, Courses, Feeds and user directory with filter controls. | All users |
| Admin Analytics Console | Admin Tools | Live dashboard with engagement metrics, access approval/revocation, and activity monitoring. | Admins |
| Moderation Tools | Admin Tools | Content moderation, user management, and audit logs for safe community operations. | Admins |
| Security & Data Practices | Security | Keep secrets out of source control; use Firebase rules and Cloudinary signed delivery for sensitive assets. | All users |

## Tech stack
- Language: Kotlin
- Platform: Android
- Build system: Gradle (Kotlin DSL)
- Services:
  - Firebase Authentication
  - Firebase Cloud Messaging
  - Cloudinary

Key implementation files:
- `ApplicationDemo2/app/src/main/java/com/example/applicationdemo/CloudinaryHelper.kt`
- `ApplicationDemo2/app/src/main/java/com/example/applicationdemo/FirebaseManager.kt`
- `ApplicationDemo2/app/src/main/java/com/example/applicationdemo/MainActivity.kt`
- `ApplicationDemo2/app/src/main/java/com/example/applicationdemo/ProfileScreen.kt`
- `ApplicationDemo2/app/src/main/java/com/example/applicationdemo/ProfileUploadScreen.kt`
- `ApplicationDemo2/app/src/main/java/com/example/applicationdemo/model/DataModels.kt`

---

## Repository layout
```text
StudySync-Student-note-sharing-Application/
├── LICENSE
├── README.md
├── ApplicationDemo2/
│   ├── build.gradle.kts
│   ├── gradle.properties
│   ├── gradlew
│   ├── gradlew.bat
│   ├── settings.gradle.kts
│   └── app/
│       ├── build.gradle.kts
│       ├── google-services.json
│       └── src/
│           └── main/
│               ├── AndroidManifest.xml
│               ├── java/com/example/applicationdemo/
│               │   ├── CloudinaryHelper.kt
│               │   ├── FirebaseManager.kt
│               │   ├── MainActivity.kt
│               │   ├── MyFirebaseMessagingService.kt
│               │   ├── NotificationHelper.kt
│               │   ├── ProfileScreen.kt
│               │   ├── ProfileUploadScreen.kt
│               │   ├── model/
│               │   │   └── DataModels.kt
│               │   ├── navigation/
│               │   └── ui/
│               └── res/
```

### How it fits together
- `MainActivity.kt` acts as the application entry point and coordinates the app flow.
- `FirebaseManager.kt` handles Firebase-related logic such as authentication and notification registration.
- `CloudinaryHelper.kt` manages media upload and linking to media URLs.
- `ProfileScreen.kt` and `ProfileUploadScreen.kt` handle user profile and media upload experiences.
- `MyFirebaseMessagingService.kt` and `NotificationHelper.kt` support push notification handling.

---

## Quick start

1. Clone the repository:
```bash
git clone https://github.com/WahidurRahman77/StudySync-Student-note-sharing-Application.git
cd StudySync-Student-note-sharing-Application/ApplicationDemo2
```

2. Open the project in Android Studio.

3. Ensure Firebase configuration is in place.

4. Build and run the app on an emulator or Android device.

### Command-line build
```bash
./gradlew assembleDebug
./gradlew installDebug
```

---

## Firebase & Cloudinary setup

### Firebase
- Go to Firebase Console: https://console.firebase.google.com/
- Create a new Firebase project or use an existing one.
- Register your Android app.
- Download `google-services.json` and place it at:
  - `ApplicationDemo2/app/google-services.json`
- Enable:
  - Firebase Authentication
  - Firebase Cloud Messaging (optional, for push notifications)

### Cloudinary
- Create a Cloudinary account: https://cloudinary.com/
- Copy your Cloud name, API key, and API secret.
- Configure them securely in your development environment or project config.
- Ensure the app retrieves these values safely at runtime before uploading media.

> Important: never commit secret values directly to the repository.

---

## Environment variables & configuration
Recommended setup for local development:

```properties
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_API_KEY=your_api_key
CLOUDINARY_API_SECRET=your_api_secret
```

Store them in a local file such as:
- `~/.gradle/gradle.properties`, or
- a non-committed local config file within the project.

Firebase config is normally stored in `google-services.json`, which is already part of the Android app setup.

---

## Running locally
1. Open `ApplicationDemo2/` in Android Studio.
2. Wait for Gradle sync to complete.
3. Connect a physical device or run an Android emulator.
4. Click Run.

Optional CLI steps:
```bash
cd ApplicationDemo2
./gradlew clean assembleDebug
```

---

## Testing & debugging tips
- Use Android Studio Logcat for runtime debugging.
- Check Firebase Console for Authentication and FCM logs.
- Validate uploads in the Cloudinary dashboard.
- Confirm that the `google-services.json` file matches the Firebase project.
- Verify network permissions and app configuration if uploads or notifications fail.

---

## Contributing
Contributions are welcome. To contribute:

1. Fork the repository.
2. Create a feature branch.
3. Make your changes.
4. Test on a device or emulator.
5. Open a pull request with a clear summary.

See the `CONTRIBUTORS.md` file for contribution guidelines.

---

## License
This project is licensed under the terms of the `LICENSE` file included in this repository.

Repository license: https://github.com/WahidurRahman77/StudySync-Student-note-sharing-Application/blob/main/LICENSE

---

## Contact
- Author: WahidurRahman77
- GitHub: https://github.com/WahidurRahman77/StudySync-Student-note-sharing-Application

If you want to expand the app further, a strong next step would be adding:
- note categories and faculty filters,
- PDF upload support,
- student collaboration features,
- search and recommendation improvements,
- admin moderation tools.

---

Thank you for checking out StudySync.
