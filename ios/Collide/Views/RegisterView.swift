import SwiftUI

struct RegisterView: View {
    var authViewModel: AuthViewModel
    var onNavigateToLogin: () -> Void

    @State private var displayName = ""
    @State private var email = ""
    @State private var password = ""

    var body: some View {
        ZStack {
            VStack(spacing: 16) {
                Spacer()

                Text("Create Account")
                    .font(.title)
                    .fontWeight(.bold)

                Spacer().frame(height: 16)

                TextField("Display Name", text: $displayName)
                    .textFieldStyle(.roundedBorder)
                    .textContentType(.name)

                TextField("Email", text: $email)
                    .textFieldStyle(.roundedBorder)
                    .textContentType(.emailAddress)
                    .keyboardType(.emailAddress)
                    .autocapitalization(.none)

                SecureField("Password", text: $password)
                    .textFieldStyle(.roundedBorder)
                    .textContentType(.newPassword)

                if let error = authViewModel.errorMessage {
                    Text(error)
                        .foregroundColor(.red)
                        .font(.caption)
                }

                Button("Sign Up") {
                    authViewModel.signUp(email: email, password: password, displayName: displayName)
                }
                .buttonStyle(.borderedProminent)
                .frame(maxWidth: .infinity)
                .disabled(authViewModel.authState == .loading)

                Button("Already have an account? Sign In") {
                    onNavigateToLogin()
                }
                .font(.callout)

                Spacer()
            }
            .padding()

            if authViewModel.authState == .loading {
                ProgressView()
            }
        }
    }
}
