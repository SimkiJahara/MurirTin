# Muri Tin (মুড়ির টিন) - Smart Bus Service Application

## 📋 Table of Contents

- [Overview](#overview)
- [Key Features](#key-features)
- [System Architecture](#system-architecture)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Installation Guide](#installation-guide)
- [Configuration](#configuration)
- [Database Structure](#database-structure)
- [User Roles & Workflows](#user-roles--workflows)
- [Testing Guide](#testing-guide)
- [Troubleshooting](#troubleshooting)
- [Security Best Practices](#security-best-practices)
- [Contributing](#contributing)
- [License](#license)

---

## 🎯 Overview

**Muri Tin** is a comprehensive Android-based smart bus service application that revolutionizes public transportation management in Bangladesh. Built with modern Android development practices, it connects riders, conductors, and bus owners in a seamless ecosystem featuring real-time tracking, automated trip monitoring, and intelligent fare management.

### What Makes Muri Tin Special?

- ✨ **Automatic Trip Monitoring**: No manual intervention needed - the app automatically detects when riders reach their destination
- 🎯 **Smart Fare Adjustment**: Automatically recalculates fares if riders exit early or late
- 🔒 **Secure Boarding**: OTP-based verification ensures only authorized riders board
- 💬 **Real-time Communication**: Built-in chat system for rider-conductor coordination
- 📊 **Comprehensive Analytics**: Detailed insights for bus owners on revenue and performance

---

## 🚀 Key Features

### For Riders (যাত্রী)

| Feature | Description |
|---------|-------------|
| 🚌 **Real-time Bus Tracking** | Live location updates of your assigned bus with estimated arrival time |
| 📍 **Smart Stop Selection** | Find nearby bus stops within 2.5km radius with route matching |
| 🎫 **OTP Boarding Verification** | Secure 4-digit OTP system to verify boarding |
| 🤖 **Automatic Trip Completion** | GPS-based auto-detection when you reach destination (within 100m) |
| 💰 **Dynamic Fare Calculation** | Automatic fare adjustment for early/late exits |
| 💬 **In-App Chat** | Direct messaging with conductor during trip |
| ⭐ **Rating System** | Rate buses, conductors, and overall experience |
| 📱 **Push Notifications** | Real-time updates on request acceptance and trip status |
| 📜 **Trip History** | Complete record of past trips with ratings |

### For Conductors (কন্ডাক্টর)

| Feature | Description                                                         |
|---------|---------------------------------------------------------------------|
| 📋 **Request Management** | View and accept pending trip requests within 30-minute pickup range |
| 📍 **Location Sharing** | Automatic GPS tracking shared with riders                           |
| 🎯 **OTP Verification** | Generate and verify OTPs for rider boarding                         |
| 💵 **Fare Collection** | Mark fare collection status                                         |
| 📅 **Schedule Management** | Create, daily schedules and delete, edit upcoming schedules         |
| 💬 **Rider Communication** | Chat with riders during active trips                                |
| ⏱️ **Time Management** | Track trip duration and manage multiple requests                    |

### For Bus Owners (বাস মালিক)

| Feature                     | Description                                                 |
|-----------------------------|-------------------------------------------------------------|
| 🚍 **Bus Registration**     | Register multiple buses with routes, stops, and fare matrix |
| 👔 **Conductor Management** | Create conductor accounts and assign to buses               |
| 📅 **Schedule view**        | View trip schedules with start/end times and directions     |
| 📊 **Analytics Dashboard**  | Revenue reports, trip counts, and performance metrics       |
| ⭐ **Rating Monitoring**     | Track conductor and bus ratings with detailed reviews       |
| 💰 **Revenue Tracking**     | Daily, weekly, and monthly income analysis                  |
| 📈 **Performance Reports**  | Trip completion rates and conductor efficiency              |

---

## 🏗️ System Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Muri Tin Application                     │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │    Rider     │  │  Conductor   │  │  Bus Owner   │      │
│  │   Interface  │  │  Interface   │  │  Interface   │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
│         │                  │                  │              │
│         └──────────────────┼──────────────────┘              │
│                            │                                 │
│         ┌──────────────────┴──────────────────┐             │
│         │      Business Logic Layer           │             │
│         │  • AuthRepository                   │             │
│         │  • Location Services                │             │
│         │  • Trip Monitoring Service          │             │
│         └──────────────────┬──────────────────┘             │
│                            │                                 │
│         ┌──────────────────┴──────────────────┐             │
│         │       Data Layer                    │             │
│         │  • Firebase Realtime Database       │             │
│         │  • Firebase Authentication          │             │
│         │  • Google Maps Services             │             │
│         │  • GeoFire (Geohashing)             │             │
│         └─────────────────────────────────────┘             │
└─────────────────────────────────────────────────────────────┘
```

### Core Components

1. **Presentation Layer**: Jetpack Compose UI with Material Design 3
2. **Business Logic**: Repository pattern with Kotlin Coroutines
3. **Data Layer**: Firebase Realtime Database with GeoFire indexing
4. **Location Services**: FusedLocationProvider with foreground service
5. **Real-time Updates**: Firebase listeners for live data synchronization

---

## 💻 Technology Stack

### Frontend
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Navigation**: Jetpack Navigation Compose
- **Asynchronous**: Kotlin Coroutines & Flow
- **State Management**: ViewModel with State Hoisting

### Backend & Services
- **Authentication**: Firebase Authentication (Email/Password)
- **Database**: Firebase Realtime Database (Asia Southeast Singapore)
- **Cloud Messaging**: Firebase Cloud Messaging
- **File Storage**: Firebase Storage

### Maps & Location
- **Maps SDK**: Google Maps SDK for Android v18.2.0
- **Location Services**: Google Play Services Location v21.3.0
- **Directions API**: Google Directions API
- **Geocoding**: Google Geocoding API
- **Places**: Google Places SDK v3.5.0
- **Geohashing**: GeoFire Android v3.2.0

### API & Networking
- **HTTP Client**: Retrofit 2.11.0
- **JSON Parsing**: Gson Converter 2.11.0

### Charts & Analytics
- **Data Visualization**: MPAndroidChart v3.1.0

### Build Tools
- **Gradle**: Version 8.1.0
- **Kotlin**: Version 1.9.0
- **Min SDK**: API 24 (Android 7.0)
- **Target SDK**: API 35 (Android 14+)

---

## 📋 Prerequisites

Before you begin, ensure you have:

### Required Software
- **Android Studio**: Arctic Fox (2020.3.1) or later
- **JDK**: Version 11 or higher
- **Android SDK**: API level 24+ (Android 7.0+)
- **Git**: For version control

### Required Accounts
- **Firebase Account**: For backend services
- **Google Cloud Account**: For Maps API and other Google services

### Development Device
- Physical Android device with:
  - Android 7.0 (API 24) or higher
  - GPS enabled
  - Google Play Services installed
  - Minimum 2GB RAM
- **OR** Android Emulator with Google Play Services

### Knowledge Requirements
- Basic understanding of Kotlin programming
- Familiarity with Android development
- Understanding of Firebase services
- Basic knowledge of REST APIs

---

## 📥 Installation Guide

### Step 1: Clone the Repository

```bash
# Clone the repository
git clone <https://github.com/SimkiJahara/MurirTin>

# Navigate to project directory
cd muritin
```

### Step 2: Firebase Setup

#### 2.1 Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click **"Add Project"**
3. Enter project name: `muritin` (or your preferred name)
4. **Optional**: Enable Google Analytics
5. Click **"Create Project"**

#### 2.2 Register Android App

1. In Firebase Console, click **Android icon** (Add app)
2. Enter package name: `com.example.muritin`
3. Enter app nickname: `Muri Tin`
4. **Optional**: Add SHA-1 fingerprint (required for authentication)
5. Download `google-services.json`
6. Place file in `app/` directory of your project

#### 2.3 Enable Firebase Services

**Authentication:**
```
1. Navigate to: Build → Authentication → Get Started
2. Enable sign-in method: Email/Password
3. Save changes
```

**Realtime Database:**
```
1. Navigate to: Build → Realtime Database → Create Database
2. Select location: Asia Southeast (Singapore)
3. Start in: Test Mode (for development)
4. Note your database URL (e.g., https://muritin-xxxxx.firebasedatabase.app/)
```

**Update Database URL in Code:**

```kotlin
// In AuthRepository.kt (line ~25)
private val database: FirebaseDatabase = FirebaseDatabase.getInstance(
    "https://muritin-78a12-default-rtdb.asia-southeast1.firebasedatabase.app/"
)
```

**Set Production Security Rules:**

Copy the rules from `rules.kt' (included in project) comment out lines

### Step 3: Google Maps API Setup

#### 3.1 Enable Required APIs

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Select or create a project
3. Navigate to: **APIs & Services → Library**
4. Enable these APIs:
   - Maps SDK for Android
   - Directions API
   - Places API
   - Geocoding API

#### 3.2 Create API Key

1. Navigate to: **APIs & Services → Credentials**
2. Click **"Create Credentials" → "API Key"**
3. Copy the generated API key
4. Click **"Restrict Key"** (highly recommended)

**Application Restrictions:**
- Restriction type: Android apps
- Package name: `com.example.muritin`
- Add your SHA-1 fingerprint

**API Restrictions:**
- Select: Restrict key
- Choose enabled APIs (Maps SDK, Directions, Places, Geocoding)

#### 3.3 Add API Key to Project

Create or edit `app/src/main/res/values/strings.xml`:

```xml
<resources>
    <string name="app_name">Muri Tin</string>
    <string name="map_api_key">YOUR_GOOGLE_MAPS_API_KEY_HERE</string>
</resources>
```

### Step 4: Get SHA-1 Fingerprint

**For Debug Keystore:**
```bash
keytool -list -v \
  -alias androiddebugkey \
  -keystore ~/.android/debug.keystore \
  -storepass android \
  -keypass android
```

**For Release Keystore:**
```bash
keytool -list -v \
  -alias your_alias \
  -keystore /path/to/your/keystore.jks
```

**Add to Firebase:**
1. Copy SHA-1 fingerprint from output
2. Go to Firebase Console → Project Settings
3. Select your Android app
4. Click **"Add Fingerprint"**
5. Paste SHA-1 and save

### Step 5: Verify Dependencies

Open `app/build.gradle.kts` and verify all dependencies are present (see [build.gradle.kts](build.gradle.kts) in project).

### Step 6: Build and Run

**Using Android Studio:**
```
1. Open project in Android Studio
2. Wait for Gradle sync to complete
3. Connect Android device or start emulator
4. Click Run button (▶️) or press Shift + F10
```

**Using Command Line:**
```bash
# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Run app
adb shell am start -n com.example.muritin/.MainActivity
```

---

## ⚙️ Configuration

### Key Configuration Files

#### 1. AndroidManifest.xml

Ensure these permissions are declared:

```xml
<!-- Essential Permissions -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

#### 2. google-services.json

Place this file in `app/` directory. **Never commit this to version control.**

#### 3. Firebase Security Rules

Use the production-ready security rules from your project's `firebase-security-rules.json` file.

---

## 🗄️ Database Structure

### Collections Overview

```
muritin-database/
├── users/                    # User profiles
├── buses/                    # Bus information
├── busAssignments/           # Conductor-to-bus mappings
├── schedules/                # Trip schedules
├── requests/                 # Trip requests
├── conductorLocations/       # Real-time GPS data
├── messages/                 # Chat messages
├── conductorRatings/         # Conductor performance
└── busRatings/               # Bus ratings
```

### Detailed Schema

#### Users Collection
```json
{
  "users": {
    "$userId": {
      "uid": "string",
      "email": "string",
      "name": "string",
      "phone": "string",
      "nid": "string",
      "age": "number",
      "role": "Rider|Conductor|Owner",
      "createdAt": "timestamp",
      "ownerId": "string (for conductors)"
    }
  }
}
```

#### Buses Collection
```json
{
  "buses": {
    "$busId": {
      "busId": "string",
      "ownerId": "string",
      "name": "string",
      "number": "string",
      "fitnessCertificate": "string",
      "taxToken": "string",
      "stops": ["Stop 1", "Stop 2", "..."],
      "route": {
        "originLoc": {
          "address": "string",
          "latitude": "double",
          "longitude": "double",
          "geohash": "string"
        },
        "stopPointsLoc": [
          {
            "address": "string",
            "latitude": "double",
            "longitude": "double",
            "geohash": "string"
          }
        ],
        "destinationLoc": { "..." }
      },
      "fares": {
        "Stop1": {
          "Stop2": 50,
          "Stop3": 80
        }
      },
      "createdAt": "timestamp"
    }
  }
}
```

#### Requests Collection
```json
{
  "requests": {
    "$requestId": {
      "id": "string",
      "riderId": "string",
      "busId": "string",
      "conductorId": "string",
      "scheduleId": "string",
      "pickup": "string",
      "destination": "string",
      "pickupLatLng": { "lat": 0.0, "lng": 0.0 },
      "destinationLatLng": { "lat": 0.0, "lng": 0.0 },
      "seats": "number",
      "fare": "number",
      "status": "Pending|Accepted|Completed|Cancelled",
      "otp": "string",
      "createdAt": "timestamp",
      "acceptedAt": "timestamp",
      "requestedRoute": { "..." },
      "rideStatus": {
        "otpVerified": "boolean",
        "inBusTravelling": "boolean",
        "boardedAt": "timestamp",
        "earlyExitRequested": "boolean",
        "lateExitRequested": "boolean",
        "actualFare": "number",
        "tripCompleted": "boolean",
        "tripCompletedAt": "timestamp"
      },
      "rating": {
        "conductorRating": "float",
        "busRating": "float",
        "overallRating": "float",
        "comment": "string",
        "timestamp": "timestamp"
      }
    }
  }
}
```

---

## 👥 User Roles & Workflows

### Rider Workflow

1. **Sign Up & Login**
   - Create account with email, phone, NID, age
   - Email verification required

2. **Request Trip**
   - Select pickup location (map or search)
   - Find nearby bus stops (within 2.5km)
   - Select destination from available stops on route
   - Choose number of seats (max 4)
   - View estimated fare
   - Submit request

3. **Wait for Acceptance**
   - Request expires after 3 minutes if not accepted
   - Receive notification when conductor accepts

4. **Board Bus**
   - Receive OTP (4 digits)
   - Share OTP with conductor
   - Conductor verifies OTP
   - Trip monitoring starts automatically

5. **During Trip**
   - View live bus location
   - Chat with conductor
   - Automatic fare adjustment if route changes
   - Auto-completion when reaching destination (within 100m)

6. **Complete Trip**
   - Confirm arrival
   - Rate conductor, bus, and overall experience
   - View updated trip history
   - Contact with conductor upto 5 days if needed if something got lost

### Conductor Workflow

1. **Assignment**
   - Owner creates conductor account
   - Owner assigns conductor to specific bus
   - Conductor receives credentials

2. **View Schedule**
   - Create daily trip schedules
   - Set trip date and time
   - Choose direction (going/returning)
   - View route details and timing
   - Start location sharing

3. **Accept Requests**
   - View pending requests within 30-minute range
   - Check rider details and route
   - Accept request (generates OTP)

4. **Board Rider**
   - Rider provides OTP
   - Verify OTP in app
   - Confirm boarding

5. **During Trip**
   - GPS tracking active
   - Chat with rider if needed
   - Respond to early/late exit requests

6. **Complete Trip**
   - Confirm rider arrival
   - Mark fare as collected
   - View next scheduled trip

### Owner Workflow

1. **Register Bus**
   - Enter bus details (name, number, certificates)
   - Define route with origin, stops, destination
   - Set fare matrix for all stop combinations

2. **Hire Conductor**
   - Create conductor account
   - Provide conductor credentials

3. **Assign Conductor**
   - Link conductor to specific bus
   - One conductor per bus at a time

4**Monitor Performance**
   - View analytics (trips, revenue)
   - Check conductor ratings
   - Track bus performance
   - Generate reports

---

## 🧪 Testing Guide

### Create Test Accounts

**Owner Account:**
```
Email: owner@test.com
Password: password123
Role: Owner
```

**Conductor Account:**
```
Email: conductor@test.com
Password: password123
Role: Conductor
```

**Rider Account:**
```
Email: rider@test.com
Password: password123
Role: Rider
```

### Testing Workflow

**Step 1: Owner Setup**
```
1. Login as owner@test.com
2. Register a bus:
   - Name: Test Bus
   - Number: DHA-123-4567
   - Route: Mirpur → Uttara (add 3-4 stops)
   - Set fares for all stop combinations
3. Create conductor account (conductor@test.com)
4. Assign conductor to bus
```

**Step 2: Conductor Setup**
```
1. Login as conductor@test.com
2. View assigned bus 
3. Create schedule for today:
   - Start: Current time + 10 minutes
   - End: Current time + 2 hours
   - Direction: Going
4. Start location sharing
```

**Step 3: Rider Testing**
```
1. Login as rider@test.com
2. Request trip:
   - Select pickup near conductor's location
   - Select destination on same route
   - Choose 1-2 seats
3. Wait for conductor to accept (~10 seconds)
4. View OTP displayed
5. Track bus location
6. Share OTP with conductor (manual testing)
7. Conductor verifies OTP
8. Check is location permission is given
9. App auto-completes trip near destination
10. Rate the experience
```

### Automated Testing Scenarios

**Location Simulation:**
```bash
# Using ADB to simulate GPS movement
adb shell "settings put secure location_mode 3"
adb emu geo fix <longitude> <latitude>

# Example: Move from Mirpur to Uttara
adb emu geo fix 90.3563 23.8223  # Mirpur
# Wait 30 seconds
adb emu geo fix 90.3897 23.8735  # Uttara
```

---

## 🔧 Troubleshooting

### Common Issues & Solutions

#### Firebase Connection Failed

**Symptoms:**
- App crashes on startup
- Database operations fail
- "Firebase not initialized" error

**Solutions:**
```
1. Verify google-services.json is in app/ directory
2. Check Firebase Database URL in AuthRepository.kt
3. Ensure Firebase rules are set correctly
4. Clean and rebuild project
5. Sync Gradle files
```

#### Google Maps Not Showing

**Symptoms:**
- Blank map screen
- "Authorization failure" error
- Map tiles not loading

**Solutions:**
```
1. Verify API key in strings.xml
2. Check Maps SDK is enabled in Google Cloud Console
3. Add SHA-1 fingerprint to Firebase
4. Ensure billing is enabled on Google Cloud (required for Maps)
5. Check API restrictions match package name
```

#### Location Not Updating

**Symptoms:**
- GPS location not showing
- "Location permission denied" error
- Stale location data

**Solutions:**
```
1. Grant location permissions in device settings
2. Enable GPS/Location services on device
3. Use physical device (emulator GPS can be unreliable)
4. Check FOREGROUND_SERVICE_LOCATION permission (Android 14+)
5. Verify location request settings in TripMonitoringService
```

#### OTP Verification Failed

**Symptoms:**
- "Invalid OTP" error
- OTP not generated
- Boarding verification fails

**Solutions:**
```
1. Ensure request status is "Accepted"
2. Check OTP field in Firebase (should be 4 digits)
3. Verify conductor is assigned to correct bus
4. Ensure schedule is active (current time within schedule)
5. Check Firebase rules allow OTP updates
```

#### Automatic Trip Completion Not Working

**Symptoms:**
- Trip doesn't auto-complete at destination
- Rider has to manually confirm arrival
- Location monitoring stops

**Solutions:**
```
1. Ensure TripMonitoringService is running
2. Check background location permission granted
3. Verify GPS accuracy (should be <100m)
4. Test with actual movement (not just location mock)
5. Check logs for "AutoComplete" tag
```

#### Build Errors

**Generic Solutions:**
```
1. File → Invalidate Caches / Restart
2. Build → Clean Project
3. Build → Rebuild Project
4. Delete .gradle folder and sync
5. Update Gradle wrapper: ./gradlew wrapper --gradle-version 8.1.0
```

**Dependency Conflicts:**
```
1. Check build.gradle.kts matches provided version
2. Update Firebase BOM to latest
3. Ensure Kotlin version matches across project
4. Sync Gradle files
```

---

## 🔒 Security Best Practices

### Never Commit These Files

```gitignore
# Add to .gitignore
google-services.json
local.properties
*.keystore
*.jks
app/src/main/res/values/secrets.xml
```

### API Key Protection

**Development:**
```xml
<!-- Use separate dev/prod keys -->
<string name="map_api_key_dev">DEV_KEY_HERE</string>
<string name="map_api_key_prod">PROD_KEY_HERE</string>
```

**Production:**
```
1. Enable API key restrictions
2. Set daily quotas
3. Monitor usage in Google Cloud Console
4. Use ProGuard for release builds
5. Enable certificate pinning
```

### Firebase Security

**Authentication:**
```
1. Enable email verification
2. Use strong password requirements
3. Implement rate limiting
4. Monitor suspicious login attempts
```

**Database Rules:**
```
1. Never use test mode in production
2. Validate data types and structure
3. Implement proper user role checks
4. Use security expressions for complex rules
5. Regularly audit rules
```

### User Data Protection

```
1. Encrypt sensitive data before storing
2. Implement data retention policies
3. Provide user data export/deletion
4. Follow GDPR guidelines if applicable
5. Regular security audits
```

---

## 🤝 Contributing

### How to Contribute

1. **Fork the Repository**
```bash
git clone https://github.com/SimkiJahara/MurirTin
cd muritin
```

2. **Create Feature Branch**
```bash
git checkout -b feature/your-feature-name
```

3. **Make Changes**
- Follow Kotlin coding conventions
- Add comments for complex logic
- Update documentation if needed
- Test thoroughly

4. **Commit Changes**
```bash
git add .
git commit -m "Add: Brief description of changes"
```

5. **Push to Branch**
```bash
git push origin feature/your-feature-name
```

6. **Create Pull Request**
- Provide clear description of changes
- Reference any related issues
- Include screenshots if UI changes
- Ensure all tests pass

### Code Style Guidelines

**Kotlin:**
- Use 4 spaces for indentation
- Maximum line length: 120 characters
- Use meaningful variable names
- Add KDoc comments for public APIs

**Compose:**
- One composable per file (except small helper composables)
- Use preview annotations for UI testing
- Extract reusable components
- Follow Material Design 3 guidelines

---

## 📄 License

This project is created for **educational purposes** only.


---

## 📞 Contact & Support

### Getting Help

**Documentation Issues:**
- Create an issue in GitHub repository
- Label with "documentation" tag

**Technical Support:**
- Open a GitHub issue with:
  - Detailed problem description
  - Steps to reproduce
  - Screenshots/logs
  - Device/OS information

**Feature Requests:**
- Submit as GitHub issue
- Label with "enhancement" tag
- Describe use case and benefits

---

##  Acknowledgments

### Technologies Used
- **Firebase**: Backend infrastructure and real-time capabilities
- **Google Maps Platform**: Location services and mapping
- **Jetpack Compose**: Modern Android UI framework
- **Material Design 3**: UI component library
- **GeoFire**: Geospatial querying for Firebase
- **Retrofit**: HTTP client for API calls
- **MPAndroidChart**: Data visualization

### Inspiration
This project aims to improve public transportation accessibility in Bangladesh by leveraging modern mobile technology.



*Last Updated: December 2025*