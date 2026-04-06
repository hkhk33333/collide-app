import SwiftUI
import MapKit

struct MapScreenView: View {
    let allLocations: [String: LocationModel]
    let currentUserId: String?
    @Binding var friendToFocus: String?
    var onNavigateToFriends: () -> Void
    var onSignOut: () -> Void

    @State private var cameraPosition: MapCameraPosition = .automatic

    var body: some View {
        ZStack {
            Map(position: $cameraPosition) {
                ForEach(Array(allLocations.values)) { location in
                    Annotation(
                        location.displayName,
                        coordinate: CLLocationCoordinate2D(
                            latitude: location.latitude,
                            longitude: location.longitude
                        )
                    ) {
                        markerView(for: location)
                    }
                }
            }
            .mapControls {
                MapUserLocationButton()
                MapCompass()
            }
            .onChange(of: friendToFocus) { _, newValue in
                if let friendId = newValue,
                   let location = allLocations[friendId] {
                    withAnimation {
                        cameraPosition = .region(MKCoordinateRegion(
                            center: CLLocationCoordinate2D(
                                latitude: location.latitude,
                                longitude: location.longitude
                            ),
                            latitudinalMeters: 1000,
                            longitudinalMeters: 1000
                        ))
                    }
                    friendToFocus = nil
                }
            }

            // Bottom controls
            VStack {
                Spacer()

                HStack {
                    // Center on my location
                    Button {
                        if let userId = currentUserId,
                           let myLocation = allLocations[userId] {
                            withAnimation {
                                cameraPosition = .region(MKCoordinateRegion(
                                    center: CLLocationCoordinate2D(
                                        latitude: myLocation.latitude,
                                        longitude: myLocation.longitude
                                    ),
                                    latitudinalMeters: 1000,
                                    longitudinalMeters: 1000
                                ))
                            }
                        }
                    } label: {
                        Image(systemName: "location.fill")
                            .padding()
                            .background(.regularMaterial)
                            .clipShape(Circle())
                    }

                    Spacer()

                    // Friends button
                    Button {
                        onNavigateToFriends()
                    } label: {
                        Image(systemName: "person.2.fill")
                            .padding()
                            .background(.regularMaterial)
                            .clipShape(Circle())
                    }

                    // Sign out button
                    Button {
                        onSignOut()
                    } label: {
                        Image(systemName: "rectangle.portrait.and.arrow.right")
                            .padding()
                            .background(.regularMaterial)
                            .clipShape(Circle())
                    }
                }
                .padding()
            }
        }
    }

    @ViewBuilder
    private func markerView(for location: LocationModel) -> some View {
        let isMe = location.userId == currentUserId
        VStack(spacing: 2) {
            Image(systemName: isMe ? "person.circle.fill" : "person.circle")
                .font(.title2)
                .foregroundColor(isMe ? .blue : .red)
            Text(location.displayName)
                .font(.caption2)
                .padding(2)
                .background(.regularMaterial)
                .cornerRadius(4)
        }
    }
}
