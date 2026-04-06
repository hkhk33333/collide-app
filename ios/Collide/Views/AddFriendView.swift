import SwiftUI

struct AddFriendView: View {
    var friendService: FriendService
    var onNavigateBack: () -> Void

    @State private var userId = ""
    @State private var isLoading = false
    @State private var snackbarMessage: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Add a Friend")
                .font(.title)
                .fontWeight(.bold)

            Text("Enter the user ID of the person you want to add as a friend. You can find this in their profile.")
                .font(.subheadline)
                .foregroundColor(.secondary)

            TextField("User ID", text: $userId)
                .textFieldStyle(.roundedBorder)
                .autocapitalization(.none)
                .disabled(isLoading)

            Button("Send Friend Request") {
                guard !userId.trimmingCharacters(in: .whitespaces).isEmpty else { return }
                isLoading = true
                friendService.sendFriendRequest(targetUserId: userId) { success, message in
                    isLoading = false
                    snackbarMessage = message
                    if success {
                        userId = ""
                    }
                }
            }
            .buttonStyle(.borderedProminent)
            .frame(maxWidth: .infinity)
            .disabled(userId.trimmingCharacters(in: .whitespaces).isEmpty || isLoading)

            Button("Back") {
                onNavigateBack()
            }
            .buttonStyle(.bordered)
            .frame(maxWidth: .infinity)

            Spacer()
        }
        .padding()
        .overlay(alignment: .bottom) {
            if let message = snackbarMessage {
                Text(message)
                    .padding()
                    .background(.regularMaterial)
                    .cornerRadius(8)
                    .padding(.bottom, 20)
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                            snackbarMessage = nil
                        }
                    }
            }
        }
    }
}
