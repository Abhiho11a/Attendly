# Attendly

> A modern, offline-first Android attendance tracker designed to make college attendance management simple, intelligent, and stress-free.

Attendly is an Android application built for students who want a faster and more reliable way to manage their class timetable, track attendance, understand attendance requirements, and stay on top of their academic schedule.

Instead of depending on spreadsheets, handwritten calculations, or repeatedly checking attendance manually, Attendly brings timetable management, attendance tracking, analytics, reminders, AI-assisted timetable import, backup and restore, and a home-screen widget into one application.

---

## ✨ Why Attendly?

Managing college attendance can become surprisingly difficult when students have to:

- Remember different class schedules every day
- Manually calculate attendance percentages
- Figure out how many classes they can safely miss
- Recalculate attendance whenever a class is attended, absent, or cancelled
- Enter large timetables manually
- Keep track of attendance across multiple subjects
- Remember important classes and attendance-related reminders
- Protect their attendance data while still being able to back it up

Attendly is designed around these everyday problems.

> **Spend less time managing attendance and more time focusing on college.**

---

## 🚀 Features

### 📅 Smart Timetable Management

Create and manage your college timetable directly inside Attendly.

The timetable acts as the foundation for the rest of the application, allowing Attendly to understand your daily classes and provide relevant attendance information.

Students can organize their classes and quickly view their daily schedule without relying on external calendar applications or spreadsheets.

---

### 🤖 AI-Powered Timetable Import

Manually entering an entire college timetable can be time-consuming.

Attendly provides an AI-assisted timetable import workflow powered by Google Gemini that can interpret timetable information and help convert it into structured timetable data.

This significantly reduces the effort required during the initial setup of the application.

Instead of manually entering every subject and class period, students can use the AI-assisted workflow to speed up timetable creation.

---

### ✅ Attendance Tracking

Track attendance for individual classes and subjects.

Attendly maintains attendance records locally and allows students to update the status of their classes.

The application uses the recorded attendance history to automatically calculate attendance statistics.

This removes the need for students to manually maintain attendance spreadsheets or repeatedly perform calculations.

---

### 📊 Attendance Percentage & Analytics

No more manual attendance calculations.

Attendly automatically calculates attendance information from the recorded class history.

Students can quickly understand:

- Current attendance percentage
- Subject-wise attendance
- Attendance progress
- Attendance trends
- Attendance status relative to their required percentage

The goal is to turn raw attendance records into information that is easy to understand and act upon.

---

### 🎯 Required Attendance Calculation

One of the most useful parts of Attendly is helping students understand attendance requirements.

Instead of only showing the current percentage, Attendly helps answer practical questions such as:

> **How many more classes do I need to attend?**

and

> **How many classes can I miss while maintaining the required attendance?**

This makes attendance percentage actionable rather than just informational.

Students can use these calculations to make better decisions about their upcoming classes.

---

### 🔔 Notifications & Reminders

Attendly includes notification and reminder functionality designed around the student's timetable and attendance workflow.

The notification system is designed to work independently of the main Compose UI lifecycle so that scheduled reminders can be handled by Android's background scheduling mechanisms.

This allows Attendly to provide reminders without requiring the user to keep the application open.

Notification delivery is subject to Android notification permissions and device-specific background restrictions.

---

### 📱 Home Screen Widget

Attendly includes a home-screen widget that provides quick access to useful attendance information without requiring the student to open the application every time.

The widget is designed to make frequently needed attendance information available directly from the Android home screen.

---

### 💾 Local & Offline-First Data

Attendly is designed primarily as a local, offline-first application.

Core attendance and timetable information is stored on the user's device rather than requiring a continuously connected cloud service.

This means the core attendance functionality is designed to remain useful when:

- Wi-Fi is unavailable
- Mobile data is unavailable
- Internet connectivity is unreliable

The application does not require a cloud database simply to record everyday attendance.

---

### 🔄 Backup & Restore

Attendly provides data backup and restore functionality so users can create a portable backup of their application data.

This helps protect against accidental data loss and provides a way to restore important attendance and timetable information when needed.

The backup system works around the user's locally stored Attendly data.

---

### 👤 Personalized Profile

Attendly includes a lightweight user profile and registration experience.

Users can provide a display name, which is used to personalize the application experience.

For example, the home screen can provide contextual greetings such as:

- Good morning, Rahul
- Good afternoon, Rahul
- Good evening, Rahul

The display name can also be updated later through the application settings.

---

### 🌐 Lightweight Backend Registration

Attendly uses a lightweight backend service for installation registration and basic installation counting.

The backend stores registration information such as:

- Installation ID
- Display name
- Registration timestamp
- Updated timestamp

The backend does **not** store the user's:

- Attendance records
- Timetable
- Attendance history
- Local reminder configuration
- Core application data

This keeps the core application offline-first while still allowing the project to support basic installation registration.

---

## 🏗️ Architecture

Attendly follows a layered Android architecture designed to keep the user interface, application logic, local persistence, and background functionality separated.

A simplified view of the application architecture is:

```text
┌─────────────────────────────────────┐
│          Jetpack Compose UI         │
├─────────────────────────────────────┤
│         ViewModels / State          │
├─────────────────────────────────────┤
│        Repositories / Logic         │
├─────────────────────────────────────┤
│           Room Database             │
├─────────────────────────────────────┤
│    Local Attendance & Timetable     │
└─────────────────────────────────────┘
```

Background notification functionality operates independently from the Compose UI lifecycle.

The lightweight registration service follows a separate architecture:

```text
Attendly Android App
        │
        │ HTTPS
        ▼
Node.js + Express Backend
        │
        ▼
MongoDB Atlas
        │
        ▼
Registration Records
```

The backend is intentionally not responsible for the user's attendance database.

---

## 🧩 Technology Stack

### Android

- **Kotlin**
- **Jetpack Compose**
- **Android SDK**
- **Room Database**
- **ViewModel**
- **Kotlin Coroutines**
- **Android Notifications**
- **Android App Widgets**

### AI

- **Google Gemini API**
- AI-assisted timetable import

### Backend

- **Node.js**
- **Express.js**
- **MongoDB Atlas**
- **Mongoose**
- **REST API**
- **Render**

### Development & Version Control

- **Gradle**
- **Git**
- **GitHub**

---

## 🔐 Privacy & Data Design

Attendly separates core student data from lightweight registration data.

### Core Student Data

The main attendance and timetable data remains local to the Android device.

```text
Timetable
Attendance
Attendance History
Reminder Configuration
Backup Data
        │
        ▼
   Local Device
```

### Registration Data

Only the lightweight registration information required by the registration service is sent to the backend.

```text
Installation ID
Display Name
Registration Time
Updated Time
        │
        ▼
Registration Backend
```

This separation prevents the registration backend from becoming a dependency for everyday attendance tracking.

---

## ⚡ Offline-First Philosophy

Attendly is built around an important principle:

> **Core attendance functionality should not stop working just because the internet is unavailable.**

The user should be able to manage attendance and timetable information locally without requiring a cloud connection for every action.

Internet connectivity is only needed for features that genuinely depend on external services, such as:

- AI-assisted timetable processing
- Backend registration

The core attendance and timetable system is designed to remain local.

---

## 🔄 Application Flow

A simplified first-launch flow looks like this:

```text
First Launch
     │
     ▼
Display Name Registration
     │
     ▼
Installation ID Generated
     │
     ▼
Backend Registration
     │
     ▼
Attendly Home
```

After registration, the application primarily operates using local data:

```text
Timetable
    │
    ▼
Attendance Tracking
    │
    ├───────────────┐
    ▼               ▼
Analytics       Notifications
    │
    ▼
Attendance Insights
```

---

## 🛠️ Project Structure

A simplified project structure looks like:

```text
Attendly/
│
├── app/
│   └── src/
│       ├── main/
│       │   ├── java/
│       │   ├── res/
│       │   └── AndroidManifest.xml
│       │
│       ├── test/
│       └── androidTest/
│
├── gradle/
│   └── wrapper/
│
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── settings.gradle.kts
├── .gitignore
└── README.md
```

The exact internal package structure may evolve as the application continues to develop.

---

## 📲 Getting Started

### Requirements

To build Attendly locally, you will need:

- Android Studio
- Compatible JDK
- Android SDK
- A physical Android device or emulator
- Internet access for Gradle dependency resolution and external features that require network connectivity

### Clone the Repository

```bash
git clone https://github.com/Abhiho11a/Attendly.git
```

Navigate into the project:

```bash
cd Attendly
```

Open the project in Android Studio.

Allow Gradle synchronization to complete and then run the application on a connected Android device or emulator.

---

## 🔑 Configuration

Some Attendly features require external configuration, such as AI API access or backend configuration.

Private configuration values must **never** be committed to the repository.

Do not commit:

- API keys
- Passwords
- MongoDB connection strings
- Signing keys
- Keystore files
- Local machine configuration

Use local configuration files and environment-specific configuration for development secrets.

---

## 🧪 Beta Testing

Attendly is currently being developed and tested as a beta application.

Beta testers are encouraged to report:

- 🐛 Bugs and crashes
- 🔔 Notification problems
- 🎨 UI/UX issues
- ⚡ Performance problems
- 🤖 AI timetable import problems
- 📊 Incorrect attendance calculations
- 💾 Backup/restore problems
- 💡 Feature suggestions
- ❓ Anything confusing or inconvenient

When reporting a bug, please include:

```text
Device:
Android Version:
Attendly Version:
What Happened:
What You Expected:
Steps to Reproduce:
```

Including device and Android version information makes debugging significantly easier.

---

## 🗺️ Roadmap

Attendly is an evolving project. Future improvements may include:

- More advanced attendance insights
- Improved timetable import reliability
- More customizable notification preferences
- Improved home-screen widgets
- Enhanced backup and restore workflows
- Additional personalization
- More robust background scheduling
- Improved accessibility
- Performance and battery optimizations
- Expanded testing across Android versions and device manufacturers

The roadmap will continue to evolve based on real-world usage and user feedback.

---

## 🎯 Project Goals

### 1. Simplicity

Attendance management should not feel complicated.

### 2. Reliability

The application should provide dependable attendance calculations, local data persistence, and reminders.

### 3. Privacy

Core attendance and timetable information should remain under the user's control and should not require a cloud database for normal usage.

### 4. Practicality

Every feature should solve a genuine student problem rather than exist simply because it is technically interesting.

---

## 📦 Releases

Installable Android builds are distributed through GitHub Releases.

The source code remains in this repository, while APK builds are attached to individual releases.

Beta builds are distributed as **Pre-release** versions while Attendly is being tested and refined.

---

## 🤝 Contributing

Attendly is primarily a personal engineering project, but feedback, bug reports, and practical suggestions are welcome.

If you find an issue:

1. Reproduce the problem.
2. Collect the relevant device and Android version information.
3. Open an issue with clear reproduction steps.
4. Include screenshots, logs, or other useful information where appropriate.

---

## 📄 License

The licensing terms for this project will be defined separately.

---

## ❤️ Built for Students

Attendly started with a simple idea:

> **Attendance management should be something students can understand at a glance, not something they have to constantly calculate and maintain manually.**

From timetable management and AI-assisted import to attendance analytics, reminders, widgets, backup and restore, and offline-first storage, Attendly aims to turn attendance tracking into a smoother everyday experience.

**Built with Kotlin, Jetpack Compose, and a focus on solving practical student problems.**
