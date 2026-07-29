# Tabula Via

Tabula Via is a comprehensive Android application designed to help educators manage their classrooms, track student progress, and maintain records efficiently. Built with modern Android development practices, it offers a seamless experience with offline support and cloud synchronization.

## Features

- **Authentication**: Secure login using Google Sign-In integrated with Firebase Authentication.
- **Class Management**: Create and manage multiple classes.
- **Student Tracking**: Maintain detailed lists of students for each class.
- **Skill Assessment**: Define and track specific skills or competencies for students and classes.
- **Attendance Management**: A dedicated dashboard for recording and editing student attendance sessions.
- **Activity Logging**: Track both individual and group activities, with specialized views for each.
- **Reporting**: Generate and view reports based on student performance and attendance.
- **Cloud Sync**: Robust background synchronization using WorkManager to keep local data in sync with Firebase Cloud Storage, ensuring data is safe and accessible across devices.
- **Offline First**: Full offline capability allowing educators to work in environments without internet connectivity.

## Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Local Database**: [Room](https://developer.android.com/training/data-storage/room)
- **Backend/Auth**: [Firebase](https://firebase.google.com/) (Authentication & Cloud Storage)
- **Background Tasks**: [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- **Navigation**: [Compose Navigation](https://developer.android.com/jetpack/compose/navigation)
- **Dependency Injection**: standard Android ViewModel and Repository patterns.

## Project Structure

- `db/`: Database configuration and Room migration logic.
- `dao/`: Data Access Objects for Room.
- `model/`: Data entities and domain models.
- `repository/`: Single source of truth for data, handling local and remote data operations.
- `viewmodel/`: UI logic and state management.
- `ui/`: Composable screens, components, and themes.
- `worker/`: Background workers for handling data synchronization.
- `utils/`: Common helper functions and extensions.

## Setup

1. **Firebase Configuration**:
   - Create a project in the [Firebase Console](https://console.firebase.google.com/).
   - Add an Android app with the package name `edu.jm.tabulavia`.
   - Download the `google-services.json` file and place it in the `app/` directory.
   - Enable Google Sign-In in the Firebase Authentication settings.

2. **Google Cloud Console**:
   - Ensure you have the `default_web_client_id` configured in your resources for Credential Manager to work correctly with Google Sign-In.

3. **Build**:
   - Open the project in Android Studio.
   - Sync Gradle and build the application.

## License

This project's license information is MIT.
