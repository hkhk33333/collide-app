import SwiftUI

enum AuthScreen {
    case login, register
}

enum AppScreen {
    case map, friends, addFriend
}

struct ContentView: View {
    @State private var authViewModel = AuthViewModel()
    @State private var authScreen: AuthScreen = .login
    @State private var currentScreen: AppScreen = .map
    @State private var locationManager = LocationManager()
    @State private var locationService = FirebaseLocationService()
    @State private var friendService = FriendService()
    @State private var allLocations: [String: LocationModel] = [:]
    @State private var friendToFocus: String?

    var body: some View {
        Group {
            switch authViewModel.authState {
            case .authenticated:
                mainContent
            case .initial, .loading, .unauthenticated:
                authContent
            }
        }
    }

    @ViewBuilder
    private var authContent: some View {
        switch authScreen {
        case .login:
            LoginView(
                authViewModel: authViewModel,
                onNavigateToRegister: { authScreen = .register }
            )
        case .register:
            RegisterView(
                authViewModel: authViewModel,
                onNavigateToLogin: { authScreen = .login }
            )
        }
    }

    @ViewBuilder
    private var mainContent: some View {
        switch currentScreen {
        case .map:
            MapScreenView(
                allLocations: allLocations,
                currentUserId: authViewModel.currentUser?.uid,
                friendToFocus: $friendToFocus,
                onNavigateToFriends: { currentScreen = .friends },
                onSignOut: {
                    locationService.removeObservers()
                    locationManager.stopUpdating()
                    authViewModel.signOut()
                }
            )
            .onAppear {
                locationManager.requestPermission()
                locationManager.startUpdating()
                locationManager.onLocationUpdate = { coordinate in
                    locationService.sendLocationUpdate(
                        latitude: coordinate.latitude,
                        longitude: coordinate.longitude
                    )
                }
                locationService.observeAllLocations { locations in
                    allLocations = locations
                }
            }

        case .friends:
            FriendListView(
                friendService: friendService,
                onNavigateToAddFriend: { currentScreen = .addFriend },
                onNavigateBack: { currentScreen = .map },
                onViewFriendLocation: { friendId in
                    friendToFocus = friendId
                    currentScreen = .map
                }
            )

        case .addFriend:
            AddFriendView(
                friendService: friendService,
                onNavigateBack: { currentScreen = .friends }
            )
        }
    }
}
