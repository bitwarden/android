import Foundation

class BWStateViewModel : ObservableObject{
    @Published var text:String
    @Published var isLoading:Bool = false
    
    init(_ state: BWState, _ defaultText: String?){
        switch state {
        case .needLogin:
            text = "LogInToBitwardenOnYourIPhoneToViewVerificationCodes"
//        case .needUnlock:
//            text = "UnlockBitwardenOnYourIPhoneToViewVerificationCodes"
        case .needPremium:
            text = "ToViewVerificationCodesUpgradeToPremium"
        case .needSetup:
            text = "SetUpBitwardenToViewItemsContainingVerificationCodes"
        case .syncing:
            text = "SyncingItemsContainingVerificationCodes"
            isLoading = true
        case .need2FAItem:
            text = "Add2FactorAutenticationToAnItemToViewVerificationCodes"
        case .needDeviceOwnerAuth:
            text = "SetUpAppleWatchPasscodeInOrderToUseBitwarden"
        default:
            text = defaultText ?? ""
        }
    }
}
