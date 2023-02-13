import Foundation

enum BWState : Int, Codable {
    case valid = 0
    case needLogin = 1
    case needPremium = 2
    case needSetup = 3
    case need2FAItem = 4
    case syncing = 5
    //    case needUnlock = 6
    case needDeviceOwnerAuth = 7
    case debug = 255
    
    var isDestructive: Bool {
        return self == .needSetup || self == .needLogin || self == .needPremium || self == .need2FAItem
    }
}
