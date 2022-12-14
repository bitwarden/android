import SwiftUI

@main
struct bitwardenApp: App {    
    @SceneBuilder var body: some Scene {
        WindowGroup {
            NavigationView {
                CipherListView()
            }
        }

        WKNotificationScene(controller: NotificationController.self, category: "myCategory")
    }
}
