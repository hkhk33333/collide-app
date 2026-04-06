# Collide iOS

Minimal iOS port of the Collide location-sharing app. Connects to the same Firebase backend as the Android app.

## Requirements

- Xcode 15+ (for iOS 17 APIs)
- iOS 17.0+ deployment target
- Firebase project: `locshare-93a7b`

## Setup Instructions

### 1. Create Xcode Project

1. Open Xcode > **File > New > Project**
2. Choose **iOS > App**
3. Product Name: `Collide`
4. Interface: **SwiftUI**
5. Language: **Swift**
6. Set minimum deployment target to **iOS 17.0**

### 2. Add Source Files

Drag all Swift files from `ios/Collide/` into the Xcode project navigator (check "Copy items if needed").

### 3. Add Firebase SDK

1. **File > Add Package Dependencies**
2. Paste: `https://github.com/firebase/firebase-ios-sdk.git`
3. Select version: **Up to Next Major**
4. Add these product modules:
   - `FirebaseAuth`
   - `FirebaseDatabase`

### 4. Configure Firebase

1. Go to [Firebase Console](https://console.firebase.google.com/) > Project `locshare-93a7b`
2. Click **Add app > iOS**
3. Enter your Bundle Identifier (from Xcode project settings)
4. Download `GoogleService-Info.plist`
5. Drag it into Xcode project root (check "Copy items if needed")

### 5. Set Privacy Descriptions

In your Xcode project target, go to **Info** tab and add:

| Key | Value |
|-----|-------|
| `NSLocationWhenInUseUsageDescription` | Collide needs your location to share it with friends |

### 6. Build & Run

Select a simulator (iPhone 15 or later recommended) and press **Cmd+R**.

## Features (Minimal Port)

- Email/password authentication (shared with Android users)
- Real-time location sharing on Apple Maps
- Friend requests (send, accept, view list)
- View friend locations on map

## Not Yet Implemented

- Google Sign-In
- Background location updates
- Push notifications

## Architecture

| File | Purpose |
|------|---------|
| `CollideApp.swift` | App entry, Firebase init |
| `ContentView.swift` | Auth gate + screen router |
| `AuthViewModel.swift` | Firebase Auth state |
| `LocationManager.swift` | CoreLocation wrapper |
| `FirebaseLocationService.swift` | Location sync with Firebase RTDB |
| `FriendService.swift` | Friend request operations |
| `MapScreenView.swift` | MapKit map with markers |
| `FriendListView.swift` | Friend list with sections |
| `AddFriendView.swift` | Send friend request form |
