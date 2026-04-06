import SwiftUI

struct FriendListView: View {
    var friendService: FriendService
    var onNavigateToAddFriend: () -> Void
    var onNavigateBack: () -> Void
    var onViewFriendLocation: (String) -> Void

    @State private var friends: [FriendshipModel]?
    @State private var snackbarMessage: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Friends")
                .font(.title)
                .fontWeight(.bold)
                .padding(.horizontal)

            // User ID card
            if let userId = friendService.getCurrentUserId() {
                VStack(alignment: .leading, spacing: 4) {
                    Label {
                        VStack(alignment: .leading) {
                            Text("Your User ID:")
                                .font(.caption)
                            Text(userId)
                                .font(.subheadline)
                                .fontWeight(.bold)
                                .textSelection(.enabled)
                            Text("(Share this with friends who want to add you)")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    } icon: {
                        Image(systemName: "person.fill")
                    }
                }
                .padding()
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(Color(.secondarySystemBackground))
                .cornerRadius(12)
                .padding(.horizontal)
            }

            // Add Friend button
            Button {
                onNavigateToAddFriend()
            } label: {
                Label("Add Friend", systemImage: "plus")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .padding(.horizontal)

            if let friends {
                if friends.isEmpty {
                    VStack(spacing: 8) {
                        Text("You don't have any friends yet.")
                            .font(.body)
                        Text("Tap the + button to add friends.")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.top, 32)
                } else {
                    List {
                        let incoming = friends.filter { $0.status == .PENDING && $0.direction == .INCOMING }
                        if !incoming.isEmpty {
                            Section("Incoming Friend Requests") {
                                ForEach(incoming) { friend in
                                    incomingRow(friend)
                                }
                            }
                        }

                        let outgoing = friends.filter { $0.status == .PENDING && $0.direction == .OUTGOING }
                        if !outgoing.isEmpty {
                            Section("Pending Requests") {
                                ForEach(outgoing) { friend in
                                    outgoingRow(friend)
                                }
                            }
                        }

                        let accepted = friends.filter { $0.status == .ACCEPTED }
                        Section("Your Friends") {
                            if accepted.isEmpty {
                                Text("You don't have any accepted friends yet")
                                    .foregroundColor(.secondary)
                            } else {
                                ForEach(accepted) { friend in
                                    acceptedRow(friend)
                                }
                            }
                        }
                    }
                    .listStyle(.insetGrouped)
                }
            } else {
                VStack {
                    ProgressView()
                    Text("Loading friends...")
                        .font(.caption)
                }
                .frame(maxWidth: .infinity)
                .padding(.top, 32)
            }

            Spacer()

            Button("Back to Map") {
                onNavigateBack()
            }
            .buttonStyle(.bordered)
            .frame(maxWidth: .infinity)
            .padding(.horizontal)
            .padding(.bottom)
        }
        .overlay(alignment: .bottom) {
            if let message = snackbarMessage {
                Text(message)
                    .padding()
                    .background(.regularMaterial)
                    .cornerRadius(8)
                    .padding(.bottom, 60)
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                            snackbarMessage = nil
                        }
                    }
            }
        }
        .onAppear {
            friendService.observeFriendships { friendships in
                friends = friendships
            }
        }
    }

    @ViewBuilder
    private func incomingRow(_ friend: FriendshipModel) -> some View {
        HStack {
            VStack(alignment: .leading) {
                Text(friend.displayName)
                    .font(.body)
                Text("Wants to be your friend \u{2022} \(formatDate(friend.requestedAt))")
                    .font(.caption)
                    .foregroundColor(.accentColor)
            }
            Spacer()
            Button {
                friendService.acceptFriendRequest(friendId: friend.userId) { _, message in
                    snackbarMessage = message
                }
            } label: {
                Label("Accept", systemImage: "checkmark")
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.small)
        }
    }

    @ViewBuilder
    private func outgoingRow(_ friend: FriendshipModel) -> some View {
        HStack {
            VStack(alignment: .leading) {
                Text(friend.displayName)
                    .font(.body)
                Text("Waiting for approval \u{2022} \(formatDate(friend.requestedAt))")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            Spacer()
            ProgressView()
                .controlSize(.small)
        }
    }

    @ViewBuilder
    private func acceptedRow(_ friend: FriendshipModel) -> some View {
        HStack {
            VStack(alignment: .leading) {
                Text(friend.displayName)
                    .font(.body)
                Text("Friends since: \(formatDate(friend.updatedAt))")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            Spacer()
            Button("View Location") {
                onViewFriendLocation(friend.userId)
            }
            .buttonStyle(.bordered)
            .controlSize(.small)
        }
    }

    private func formatDate(_ timestamp: Int64) -> String {
        let date = Date(timeIntervalSince1970: TimeInterval(timestamp) / 1000)
        let formatter = DateFormatter()
        formatter.dateFormat = "MMM d, yyyy"
        return formatter.string(from: date)
    }
}
