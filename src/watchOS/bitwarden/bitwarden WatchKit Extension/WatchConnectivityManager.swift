import Combine
import Foundation
import WatchConnectivity

struct WatchConnectivityMessage {
    var state: BWState?
    var debugText: String?
}

final class WatchConnectivityManager: NSObject, ObservableObject {
    static let shared = WatchConnectivityManager()
    
    let watchConnectivitySubject = CurrentValueSubject<WatchConnectivityMessage, Error>(WatchConnectivityMessage(state: nil))
    
    private let WATCH_DTO_APP_CONTEXT_KEY = "watchDto"
    private let TRIGGER_SYNC_ACTION_KEY = "triggerSync"
    private let ACTION_MESSAGE_KEY = "actionMessage"
    
    var messageQueue = ArrayQueue<[String : Any]>()
    
    private override init() {
        super.init()
        
        if WCSession.isSupported() {
            WCSession.default.delegate = self
            WCSession.default.activate()
        }
    }
    
    var isSessionActivated: Bool {
        return WCSession.default.isCompanionAppInstalled && WCSession.default.activationState == .activated
    }
    
    func triggerSync() {
        send([ACTION_MESSAGE_KEY : TRIGGER_SYNC_ACTION_KEY])
    }
        
    func send(_ message: [String : Any]) {
        guard WCSession.default.activationState == .activated else {
            messageQueue.enqueue(message)
            return
        }
        
        guard WCSession.default.isCompanionAppInstalled else {
            return
        }
        
        WCSession.default.sendMessage(message) { error in
            Log.e("Cannot send message: \(String(describing: error))")
        }
    }
}

extension WatchConnectivityManager: WCSessionDelegate {
    func session(_ session: WCSession, didReceiveMessage message: [String : Any]) {
    }
    
    func session(_ session: WCSession, didReceiveMessage message: [String : Any], replyHandler: @escaping ([String : Any]) -> Void) {
    }
    
    func session(_ session: WCSession,
                 activationDidCompleteWith activationState: WCSessionActivationState,
                 error: Error?) {
        guard !messageQueue.isEmpty, activationState == .activated else {
            return
        }
        
        repeat {
            send(messageQueue.dequeue()!)
        } while !messageQueue.isEmpty
    }
    
    func session(_ session: WCSession, didReceiveApplicationContext applicationContext: [String : Any]) {
        // in order for the delivery to be faster the time is added to the key to make each application context update have a different key
        // and update faster
        let watchDtoKey = applicationContext.keys.first { k in
            k.starts(with: WATCH_DTO_APP_CONTEXT_KEY)
        }
        
        do {
            guard let dtoKey = watchDtoKey,
                  let nsRawData = applicationContext[dtoKey] as? NSData,
                  KeychainHelper.standard.hasDeviceOwnerAuth() else {
                return
            }
            
            let decoder = MessagePackDecoder()
            decoder.userInfo[MessagePackDecoder.dataSpecKey] = DataSpecBuilder()
                .append("state")
                .appendArray("ciphers", DataSpecBuilder()
                    .append("id")
                    .append("name")
                    .appendObj("login", DataSpecBuilder()
                        .append("username")
                        .append("totp")
                        .appendArray("uris", DataSpecBuilder()
                            .append("uri")
                            .build())
                        .build())
                    .build())
                .appendObj("userData", DataSpecBuilder()
                    .append("id")
                    .append("email")
                    .append("name")
                    .build())
                .appendObj("environmentData", DataSpecBuilder()
                    .append("base")
                    .append("icons")
                    .build())
                .build()
            
            let rawData = try nsRawData.decompressed(using: .lzfse)

            let watchDTO = try decoder.decode(WatchDTO.self, from: Data(referencing: rawData))
            
            let previousUserId = StateService.shared.getUser()?.id
            
            if previousUserId != watchDTO.userData?.id {
                self.watchConnectivitySubject.send(WatchConnectivityMessage(state: .syncing))
            }
            
            StateService.shared.currentState = watchDTO.state
            StateService.shared.setUser(user: watchDTO.userData)
//            StateService.shared.setVaultTimeout(watchDTO.settingsData?.vaultTimeoutInMinutes, watchDTO.settingsData?.vaultTimeoutAction ?? .lock)
            EnvironmentService.shared.baseUrl = watchDTO.environmentData?.base
            EnvironmentService.shared.setIconsUrl(url: watchDTO.environmentData?.icons)
            
            if watchDTO.state.isDestructive {
                CipherService.shared.deleteAll(nil) {
                    self.watchConnectivitySubject.send(WatchConnectivityMessage(state: nil))
                }
            }
            
            if watchDTO.state == .valid, var ciphers = watchDTO.ciphers {
                // we need to track the to which user the ciphers belong to, so we add the user here to all ciphers
                // note: it's not being sent directly from the phone to increase performance on the communication
                ciphers.indices.forEach { i in
                    ciphers[i].userId = watchDTO.userData!.id
                }
                
                CipherService.shared.saveCiphers(ciphers) {
                    if let previousUserId = previousUserId,
                       let currentUserid = watchDTO.userData?.id,
                       previousUserId != currentUserid {
                        CipherService.shared.deleteAll(previousUserId) {}
                    }
                    self.watchConnectivitySubject.send(WatchConnectivityMessage(state: nil))
                }
            }
        }
        catch {
            Log.e(error)
        }
    }
}
