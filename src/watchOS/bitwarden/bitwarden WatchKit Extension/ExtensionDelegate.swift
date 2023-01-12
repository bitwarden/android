import WatchKit
import FirebaseCore

class ExtensionDelegate: NSObject, WKExtensionDelegate {

    func applicationDidFinishLaunching() {
#if !DEBUG
        FirebaseApp.configure()
#endif
    }
}
