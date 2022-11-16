import Foundation

enum BWState : Int {
    case valid = 0
    case needLogin = 1
    case needUnlock = 2
    case needPremium = 3
    case needSetup = 4
    case syncing = 5
    case need2FAItem = 6
}
