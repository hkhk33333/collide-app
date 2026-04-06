import Foundation
import FirebaseAuth
import Observation

enum AuthState {
    case initial
    case loading
    case authenticated
    case unauthenticated
}

@Observable
class AuthViewModel {
    var currentUser: User?
    var authState: AuthState = .initial
    var errorMessage: String?

    init() {
        currentUser = Auth.auth().currentUser
        authState = currentUser != nil ? .authenticated : .unauthenticated
    }

    func signIn(email: String, password: String) {
        guard !email.isEmpty, !password.isEmpty else {
            errorMessage = "Email and password cannot be empty"
            return
        }

        authState = .loading
        errorMessage = nil

        Auth.auth().signIn(withEmail: email, password: password) { [weak self] result, error in
            guard let self else { return }
            if let error {
                self.errorMessage = error.localizedDescription
                self.authState = .unauthenticated
            } else {
                self.currentUser = Auth.auth().currentUser
                self.authState = .authenticated
            }
        }
    }

    func signUp(email: String, password: String, displayName: String) {
        guard !email.isEmpty, !password.isEmpty else {
            errorMessage = "Email and password cannot be empty"
            return
        }
        guard password.count >= 6 else {
            errorMessage = "Password must be at least 6 characters"
            return
        }

        authState = .loading
        errorMessage = nil

        Auth.auth().createUser(withEmail: email, password: password) { [weak self] result, error in
            guard let self else { return }
            if let error {
                self.errorMessage = error.localizedDescription
                self.authState = .unauthenticated
                return
            }

            // Update display name
            let changeRequest = Auth.auth().currentUser?.createProfileChangeRequest()
            changeRequest?.displayName = displayName
            changeRequest?.commitChanges { _ in
                self.currentUser = Auth.auth().currentUser
                self.authState = .authenticated
            }
        }
    }

    func signOut() {
        try? Auth.auth().signOut()
        currentUser = nil
        authState = .unauthenticated
    }
}
