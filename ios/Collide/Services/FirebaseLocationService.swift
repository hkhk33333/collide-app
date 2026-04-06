import Foundation
import FirebaseAuth
import FirebaseDatabase
import Observation

@Observable
class FirebaseLocationService {
    private let database = Database.database(
        url: "https://locshare-93a7b-default-rtdb.europe-west1.firebasedatabase.app/"
    )
    private lazy var locationsRef = database.reference(withPath: "locations")
    private lazy var usersRef = database.reference(withPath: "users")
    private lazy var friendshipsRef = database.reference(withPath: "friendships")
    private var friendshipHandle: DatabaseHandle?

    func sendLocationUpdate(latitude: Double, longitude: Double) {
        guard let user = Auth.auth().currentUser else { return }

        let locationData: [String: Any] = [
            "latitude": latitude,
            "longitude": longitude,
            "timestamp": Int64(Date().timeIntervalSince1970 * 1000),
            "accuracy": 0,
            "provider": "ios",
            "displayName": user.displayName ?? "Unknown User"
        ]

        locationsRef.child(user.uid).setValue(locationData)
        updateUserProfile(user: user)
    }

    private func updateUserProfile(user: FirebaseAuth.User) {
        let userData: [String: Any] = [
            "uid": user.uid,
            "email": user.email ?? "",
            "displayName": user.displayName ?? "Unknown User",
            "lastActive": Int64(Date().timeIntervalSince1970 * 1000)
        ]
        usersRef.child(user.uid).setValue(userData)
    }

    /// Observe locations for current user + accepted friends.
    /// Listens on friendships to auto-refresh when friendship status changes.
    func observeAllLocations(onChange: @escaping ([String: LocationModel]) -> Void) {
        guard let currentUserId = Auth.auth().currentUser?.uid else {
            onChange([:])
            return
        }

        // Remove existing listener
        if let handle = friendshipHandle {
            friendshipsRef.child(currentUserId).removeObserver(withHandle: handle)
        }

        friendshipHandle = friendshipsRef.child(currentUserId).observe(.value) { [weak self] snapshot in
            self?.refreshAllLocations(
                currentUserId: currentUserId,
                friendshipsSnapshot: snapshot,
                onChange: onChange
            )
        }
    }

    func removeObservers() {
        guard let currentUserId = Auth.auth().currentUser?.uid,
              let handle = friendshipHandle else { return }
        friendshipsRef.child(currentUserId).removeObserver(withHandle: handle)
        friendshipHandle = nil
    }

    private func refreshAllLocations(
        currentUserId: String,
        friendshipsSnapshot: DataSnapshot,
        onChange: @escaping ([String: LocationModel]) -> Void
    ) {
        var locations: [String: LocationModel] = [:]

        // Step 1: Get own location
        locationsRef.child(currentUserId).getData { error, mySnapshot in
            if let mySnapshot, mySnapshot.exists() {
                locations[currentUserId] = LocationModel.from(snapshot: mySnapshot, userId: currentUserId)
            }

            // Step 2: Find accepted friends
            var acceptedFriends: [String] = []
            for child in friendshipsSnapshot.children {
                guard let friendSnapshot = child as? DataSnapshot,
                      let status = friendSnapshot.childSnapshot(forPath: "status").value as? String,
                      status == "ACCEPTED" else { continue }
                acceptedFriends.append(friendSnapshot.key)
            }

            if acceptedFriends.isEmpty {
                onChange(locations)
                return
            }

            // Step 3: Get each friend's location
            var processed = 0
            for friendId in acceptedFriends {
                self.locationsRef.child(friendId).getData { error, friendSnapshot in
                    if let friendSnapshot, friendSnapshot.exists() {
                        locations[friendId] = LocationModel.from(snapshot: friendSnapshot, userId: friendId)
                    }

                    processed += 1
                    if processed == acceptedFriends.count {
                        onChange(locations)
                    }
                }
            }
        }
    }
}
