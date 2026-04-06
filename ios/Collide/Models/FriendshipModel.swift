import Foundation

enum FriendshipStatus: String {
    case PENDING
    case ACCEPTED
    case REJECTED
}

enum FriendshipDirection: String {
    case OUTGOING
    case INCOMING
}

struct FriendshipModel: Identifiable {
    let id: String // userId
    let userId: String
    let displayName: String
    let status: FriendshipStatus
    let direction: FriendshipDirection
    let requestedAt: Int64
    let updatedAt: Int64
}
