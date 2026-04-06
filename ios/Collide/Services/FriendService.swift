import Foundation
import FirebaseAuth
import FirebaseDatabase
import Observation

@Observable
class FriendService {
    private let database = Database.database(
        url: "https://locshare-93a7b-default-rtdb.europe-west1.firebasedatabase.app/"
    )
    private lazy var friendsRef = database.reference(withPath: "friendships")
    private lazy var usersRef = database.reference(withPath: "users")
    private var friendshipsHandle: DatabaseHandle?

    func getCurrentUserId() -> String? {
        Auth.auth().currentUser?.uid
    }

    /// Send a friend request (bidirectional write with rollback on failure)
    func sendFriendRequest(targetUserId: String, completion: @escaping (Bool, String) -> Void) {
        guard let currentUser = Auth.auth().currentUser else {
            completion(false, "You need to be logged in")
            return
        }

        let currentUserId = currentUser.uid

        if currentUserId == targetUserId {
            completion(false, "You cannot add yourself as a friend")
            return
        }

        // Check target user exists
        usersRef.child(targetUserId).getData { [self] error, snapshot in
            guard let snapshot, snapshot.exists() else {
                completion(false, "User does not exist")
                return
            }

            // Check no existing friendship
            self.friendsRef.child(currentUserId).child(targetUserId).getData { error, existingSnapshot in
                if let existingSnapshot, existingSnapshot.exists() {
                    completion(false, "Friend request already exists")
                    return
                }

                let timestamp = Int64(Date().timeIntervalSince1970 * 1000)

                let outgoing: [String: Any] = [
                    "status": FriendshipStatus.PENDING.rawValue,
                    "direction": FriendshipDirection.OUTGOING.rawValue,
                    "requestedAt": timestamp,
                    "updatedAt": timestamp
                ]

                let incoming: [String: Any] = [
                    "status": FriendshipStatus.PENDING.rawValue,
                    "direction": FriendshipDirection.INCOMING.rawValue,
                    "requestedAt": timestamp,
                    "updatedAt": timestamp
                ]

                // Write outgoing record
                self.friendsRef.child(currentUserId).child(targetUserId).setValue(outgoing) { error, _ in
                    if let error {
                        completion(false, "Failed to send friend request: \(error.localizedDescription)")
                        return
                    }

                    // Write incoming record
                    self.friendsRef.child(targetUserId).child(currentUserId).setValue(incoming) { error, _ in
                        if let error {
                            // Rollback outgoing
                            self.friendsRef.child(currentUserId).child(targetUserId).removeValue()
                            completion(false, "Failed to send friend request: \(error.localizedDescription)")
                            return
                        }

                        completion(true, "Friend request sent")
                    }
                }
            }
        }
    }

    /// Accept a friend request (update both sides to ACCEPTED)
    func acceptFriendRequest(friendId: String, completion: @escaping (Bool, String) -> Void) {
        guard let currentUserId = Auth.auth().currentUser?.uid else {
            completion(false, "You need to be logged in")
            return
        }

        let timestamp = Int64(Date().timeIntervalSince1970 * 1000)

        let updates: [String: Any] = [
            "status": FriendshipStatus.ACCEPTED.rawValue,
            "updatedAt": timestamp
        ]

        // Update recipient side
        friendsRef.child(currentUserId).child(friendId).updateChildValues(updates) { [self] error, _ in
            if let error {
                completion(false, "Failed to accept friend request: \(error.localizedDescription)")
                return
            }

            // Update requester side
            self.friendsRef.child(friendId).child(currentUserId).updateChildValues(updates) { error, _ in
                if let error {
                    // Rollback
                    let rollback: [String: Any] = [
                        "status": FriendshipStatus.PENDING.rawValue,
                        "updatedAt": timestamp
                    ]
                    self.friendsRef.child(currentUserId).child(friendId).updateChildValues(rollback)
                    completion(false, "Failed to accept friend request: \(error.localizedDescription)")
                    return
                }

                completion(true, "Friend request accepted")
            }
        }
    }

    /// Observe friendships with real-time updates
    func observeFriendships(onChange: @escaping ([FriendshipModel]) -> Void) {
        guard let currentUserId = Auth.auth().currentUser?.uid else {
            onChange([])
            return
        }

        // Remove existing listener
        if let handle = friendshipsHandle {
            friendsRef.child(currentUserId).removeObserver(withHandle: handle)
        }

        friendshipsHandle = friendsRef.child(currentUserId).observe(.value) { [weak self] snapshot in
            guard let self else {
                onChange([])
                return
            }

            let children = snapshot.children.allObjects as? [DataSnapshot] ?? []

            if children.isEmpty {
                onChange([])
                return
            }

            var friendships: [FriendshipModel] = []
            var processed = 0

            for child in children {
                let friendId = child.key
                guard let statusStr = child.childSnapshot(forPath: "status").value as? String,
                      let directionStr = child.childSnapshot(forPath: "direction").value as? String,
                      let status = FriendshipStatus(rawValue: statusStr),
                      let direction = FriendshipDirection(rawValue: directionStr) else {
                    processed += 1
                    if processed == children.count { onChange(friendships) }
                    continue
                }

                let requestedAt = child.childSnapshot(forPath: "requestedAt").value as? Int64 ?? 0
                let updatedAt = child.childSnapshot(forPath: "updatedAt").value as? Int64 ?? 0

                // Fetch display name
                self.usersRef.child(friendId).child("displayName").getData { error, nameSnapshot in
                    let displayName = nameSnapshot?.value as? String ?? "Unknown"

                    let friendship = FriendshipModel(
                        id: friendId,
                        userId: friendId,
                        displayName: displayName,
                        status: status,
                        direction: direction,
                        requestedAt: requestedAt,
                        updatedAt: updatedAt
                    )
                    friendships.append(friendship)
                    processed += 1

                    if processed == children.count {
                        onChange(friendships)
                    }
                }
            }
        }
    }

    func removeObservers() {
        guard let currentUserId = Auth.auth().currentUser?.uid,
              let handle = friendshipsHandle else { return }
        friendsRef.child(currentUserId).removeObserver(withHandle: handle)
        friendshipsHandle = nil
    }
}
