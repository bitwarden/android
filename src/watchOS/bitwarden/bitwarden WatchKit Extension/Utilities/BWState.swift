import Foundation

enum BWState : Int, Codable {
    case valid = 0
    case needLogin = 1
    case needUnlock = 2
    case needPremium = 3
    case needSetup = 4
    case need2FAItem = 5
    case syncing = 6
    
    var isDestructive: Bool {
        return self == .needSetup || self == .needLogin || self == .needPremium || self == .need2FAItem
    }
}
