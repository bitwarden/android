//
//  bitwardenApp.swift
//  bitwarden WatchKit Extension
//
//  Created by Federico Andrés Maccaroni on 25/08/2022.
//

import SwiftUI

@main
struct bitwardenApp: App {
    @SceneBuilder var body: some Scene {
        WindowGroup {
            NavigationView {
                ContentView()
            }
        }

        WKNotificationScene(controller: NotificationController.self, category: "myCategory")
    }
}
