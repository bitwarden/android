import Foundation

class StateService {
    static let shared: StateService = StateService()
    
    let HAS_RUN_BEFORE_KEY = "has_run_before_key"
    let CURRENT_STATE_KEY = "current_state_key"
    let CURRENT_USER_KEY = "current_user_key"
    let LACKED_DEVICE_OWNER_AUTH_LAST_TIME_KEY = "lacked_device_owner_auth_last_time_key"
//    let TIMEOUT_MINUTES_KEY = "timeout_minutes_key"
//    let TIMEOUT_ACTION_KEY = "timeout_action_key"
    
    var hasRunBefore = false
    
    private init(){}
    
    var currentState:BWState {
        get {
            guard let stateData = KeychainHelper.standard.read(CURRENT_STATE_KEY),
                  let strData = String(data: stateData, encoding: .utf8),
                  let intData = Int(strData),
                  let state = BWState(rawValue: intData) else {
                return BWState.needSetup
            }
            
            return state
        }
        set(newState) {
            let stateVal = String(newState.rawValue)
            KeychainHelper.standard.save(stateVal.data(using: .utf8)!, CURRENT_STATE_KEY)
        }
    }
    
    func getUser() -> User? {
        return KeychainHelper.standard.read(CURRENT_USER_KEY, User.self)
    }
    
    func setUser(user: User?) {
        guard let user = user else {
            KeychainHelper.standard.delete(CURRENT_USER_KEY)
            return
        }
        
        KeychainHelper.standard.save(user, key: CURRENT_USER_KEY)
    }
    
    /// Checks the state integrity of the app. If the app has been reinstalled then there are some keychain things that need to be reset.
    func checkIntegrity() {
        if hasRunBefore {
            return
        }
        
        let userDefaults = UserDefaults.standard
        hasRunBefore = userDefaults.bool(forKey: HAS_RUN_BEFORE_KEY)
        
        if !hasRunBefore {
            clear()
            EnvironmentService.shared.clear()
            CryptoService.clearKey()
            
            userDefaults.set(true, forKey: HAS_RUN_BEFORE_KEY)
        }
    }
    
    func clear() {
        currentState = .needSetup
        setUser(user: nil)
    }
    
    var lackedDeviceOwnerAuthLastTime : Bool {
        get {
            return UserDefaults.standard.bool(forKey: LACKED_DEVICE_OWNER_AUTH_LAST_TIME_KEY)
        }
        set(newValue) {
            UserDefaults.standard.set(newValue, forKey: LACKED_DEVICE_OWNER_AUTH_LAST_TIME_KEY)
        }
    }
    
//    var vaultTimeoutInMinutes: Int? {
//        guard let timeoutData = KeychainHelper.standard.read(TIMEOUT_MINUTES_KEY),
//              let strData = String(data: timeoutData, encoding: .utf8),
//              let intVal = Int(strData) else {
//            return nil
//        }
//
//        return intVal
//    }
    
//    var vaultTimeoutAction: VaultTimeoutAction {
//        guard let timeoutActionData = KeychainHelper.standard.read(TIMEOUT_ACTION_KEY),
//              let strData = String(data: timeoutActionData, encoding: .utf8),
//              let intData = Int(strData),
//              let timeoutAction = VaultTimeoutAction(rawValue: intData) else {
//            return .lock
//        }
//
//        return timeoutAction
//    }
    
//    func setVaultTimeout(_ timeoutInMinutes: Int?, _ action: VaultTimeoutAction) {
//        guard let timeoutInMinutes = timeoutInMinutes else {
//            KeychainHelper.standard.delete(TIMEOUT_MINUTES_KEY)
//            return
//        }
//
//        KeychainHelper.standard.save(String(timeoutInMinutes).data(using: .utf8)!, TIMEOUT_MINUTES_KEY)
//        KeychainHelper.standard.save(String(action.rawValue).data(using: .utf8)!, TIMEOUT_ACTION_KEY)
//    }
}
