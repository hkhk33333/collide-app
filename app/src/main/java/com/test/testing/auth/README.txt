# How to Enable Firebase Authentication

To enable email/password authentication in your Firebase project, follow these steps:

1. Go to the [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Click on "Authentication" in the left sidebar
4. Click on the "Get Started" button if you haven't set up authentication yet
5. Click on the "Sign-in method" tab
6. Find "Email/Password" in the list and click on it
7. Toggle the "Enable" switch to turn it on
8. Click "Save"

That's it! Your Firebase project is now configured to allow email/password authentication.

## Testing the Authentication

1. Run your app
2. You should see the login screen
3. Click "Don't have an account? Sign Up" to create a new account
4. Enter a name, email and password (at least 6 characters)
5. Click "Sign Up"
6. The app should authenticate you and show the map

If you want to sign out, click the new "Sign Out" button at the bottom of the screen.

## What's happening in the code

1. We created an AuthViewModel to manage authentication state and operations
2. We added login and registration screens
3. We connected authentication to Firebase
4. We integrated it with the map and location functionality
5. Now each user's location is tied to their authenticated account

Your location data is now stored securely with your user ID as the key. 