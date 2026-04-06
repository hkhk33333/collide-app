import SwiftUI

struct LoginView: View {
    var authViewModel: AuthViewModel
    var onNavigateToRegister: () -> Void

    @State private var email = ""
    @State private var password = ""

    var body: some View {
        ZStack {
            VStack(spacing: 16) {
                Spacer()

                Text("Welcome Back")
                    .font(.title)
                    .fontWeight(.bold)

                Spacer().frame(height: 16)

                TextField("Email", text: $email)
                    .textFieldStyle(.roundedBorder)
                    .textContentType(.emailAddress)
                    .keyboardType(.emailAddress)
                    .autocapitalization(.none)

                SecureField("Password", text: $password)
                    .textFieldStyle(.roundedBorder)
                    .textContentType(.password)

                if let error = authViewModel.errorMessage {
                    Text(error)
                        .foregroundColor(.red)
                        .font(.caption)
                }

                Button("Sign In") {
                    authViewModel.signIn(email: email, password: password)
                }
                .buttonStyle(.borderedProminent)
                .frame(maxWidth: .infinity)
                .disabled(authViewModel.authState == .loading)

                Button("Don't have an account? Sign Up") {
                    onNavigateToRegister()
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
