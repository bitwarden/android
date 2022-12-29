import Foundation
import WatchConnectivity

struct NotificationMessage: Identifiable {
    let id = UUID()
    let text: String
}

final class WatchConnectivityManager: NSObject, ObservableObject {
    static let shared = WatchConnectivityManager()
    @Published var notificationMessage: NotificationMessage? = nil
    
    private let WATCH_DTO_APP_CONTEXT_KEY = "watchDto"
    private let TRIGGER_SYNC_ACTION_KEY = "triggerSync"
    private let ACTION_MESSAGE_KEY = "actionMessage"
    
    private override init() {
        super.init()
        
        if WCSession.isSupported() {
            WCSession.default.delegate = self
            WCSession.default.activate()
        }
    }
        
    func send(_ message: String) {
        guard WCSession.default.activationState == .activated else {
          return
        }
        #if os(iOS)
        guard WCSession.default.isWatchAppInstalled else {
            return
        }
        #else
        guard WCSession.default.isCompanionAppInstalled else {
            return
        }
        #endif
        
        guard WCSession.default.isReachable else {
            return
        }
        
        WCSession.default.sendMessage([ACTION_MESSAGE_KEY : TRIGGER_SYNC_ACTION_KEY], replyHandler: nil) { error in
            print("Cannot send message: \(String(describing: error))")
        }
    }
}

extension WatchConnectivityManager: WCSessionDelegate {
    func session(_ session: WCSession, didReceiveMessage message: [String : Any]) {
        DispatchQueue.main.async { [weak self] in
            self?.notificationMessage = NotificationMessage(text: "testing this didReceiveMessage")
        }
        
        if let notificationText = message[ACTION_MESSAGE_KEY] as? String {
            DispatchQueue.main.async { [weak self] in
                self?.notificationMessage = NotificationMessage(text: notificationText)
            }
        }
    }
    
    func session(_ session: WCSession, didReceiveMessage message: [String : Any], replyHandler: @escaping ([String : Any]) -> Void) {
        DispatchQueue.main.async { [weak self] in
            self?.notificationMessage = NotificationMessage(text: "testing this didReceiveMessage")
        }
        let returnMessage: [String : Any] = [
               "key1" : "s"
            ]

        replyHandler(returnMessage)
        
//        if let notificationText = message[kMessageKey] as? String {
//            DispatchQueue.main.async { [weak self] in
//                self?.notificationMessage = NotificationMessage(text: notificationText)
//            }
//        }

    }
    
    func session(_ session: WCSession, didReceiveUserInfo userInfo: [String : Any] = [:]) {
        DispatchQueue.main.async { [weak self] in
            self?.notificationMessage = NotificationMessage(text: "testing this didReceiveUserInfo")
        }
    }
    
    func session(_ session: WCSession,
                 activationDidCompleteWith activationState: WCSessionActivationState,
                 error: Error?) {
        DispatchQueue.main.async { [weak self] in
            self?.notificationMessage = NotificationMessage(text: "testing this activationDidCompleteWith")
        }
        
    }
    
    func session(_ session: WCSession, didReceiveApplicationContext applicationContext: [String : Any]) {
        DispatchQueue.main.async { [weak self] in
            self?.notificationMessage = NotificationMessage(text: "testing this didReceiveApplicationContext")
        }
        if let notificationText = applicationContext[WATCH_DTO_APP_CONTEXT_KEY] as? String {
            
//            let decoder = JSONDecoder()
//            do {
//                let ciphers = try decoder.decode(Cipher.self, from: notificationText.data(using: .utf8)!)
//
                DispatchQueue.main.async { [weak self] in
                    let index1 = notificationText.index(notificationText.startIndex, offsetBy: 0)
                    let index2 = notificationText.index(notificationText.startIndex, offsetBy: 6)
                    let indexRange = index1...index2
                    let subString = notificationText[indexRange] // eil

                    self?.notificationMessage = NotificationMessage(text: String(subString))
                }
//            }
//            catch {
//                print(error)
//            }
        }
    }
    
    #if os(iOS)
    func sessionDidBecomeInactive(_ session: WCSession) {}
    func sessionDidDeactivate(_ session: WCSession) {
        session.activate()
    }
    #endif
}
