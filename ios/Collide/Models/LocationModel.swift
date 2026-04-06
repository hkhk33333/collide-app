import Foundation
import FirebaseDatabase

struct LocationModel: Identifiable {
    let id: String // userId
    let latitude: Double
    let longitude: Double
    let userId: String
    let timestamp: Int64
    let displayName: String

    static func from(snapshot: DataSnapshot, userId: String) -> LocationModel {
        let value = snapshot.value as? [String: Any] ?? [:]
        return LocationModel(
            id: userId,
            latitude: value["latitude"] as? Double ?? 0.0,
            longitude: value["longitude"] as? Double ?? 0.0,
            userId: userId,
            timestamp: value["timestamp"] as? Int64 ?? 0,
            displayName: value["displayName"] as? String ?? "Unknown User"
        )
    }
}
