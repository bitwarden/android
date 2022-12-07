import Foundation

struct WatchDTO : Codable{
    var state: BWState
    var ciphers: [Cipher]?
    var userData: User?
    var environmentData: EnvironmentUrlDataDto?
//    var settingsData: SettingsDataDto?
    
    init(state: BWState) {
      self.state = state
      self.ciphers = nil
      self.userData = nil
      self.environmentData = nil
    }
}

struct EnvironmentUrlDataDto : Codable {
    var base: String?
    var icons: String?
}

//struct SettingsDataDto : Codable {
//    var vaultTimeoutInMinutes: Int?
//    var vaultTimeoutAction: VaultTimeoutAction
//}
