import Foundation

class StateService {
    static let shared: StateService = StateService()
    
    let CURRENT_STATE_KEY = "current_state_key"
    
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
            var stateVal = newState.rawValue
            let data = Data(bytes: &stateVal, count: MemoryLayout.size(ofValue: stateVal))

            KeychainHelper.standard.save(data, CURRENT_STATE_KEY)
        }
    }
}
