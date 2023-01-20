import SwiftUI

@main
struct bitwardenApp: App {
    @WKExtensionDelegateAdaptor(ExtensionDelegate.self) var delegate
    
    @SceneBuilder var body: some Scene {
        WindowGroup {
            NavigationView {
                CipherListView()
            }
        }

        WKNotificationScene(controller: NotificationController.self, category: "myCategory")
    }
}
