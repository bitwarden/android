import Foundation

class EnvironmentService{
    static let shared: EnvironmentService = EnvironmentService()
    
    let BASE_URL_KEY = "base_url"
    let ICONS_URL_KEY = "icons_url"
    let DEFAULT_ICONS_URL = "https://icons.bitwarden.net"
    
    private init(){}
    
    var baseUrl:String? {
        guard let urlData = KeychainHelper.standard.read(BASE_URL_KEY) else {
            return nil
        }
        
        return String(decoding: urlData, as: UTF8.self)
    }
    
    var iconsUrl:String {
        guard let urlData = KeychainHelper.standard.read(ICONS_URL_KEY) else {
            return baseUrl == nil ? DEFAULT_ICONS_URL : "\(baseUrl!)/icons"
        }
        
        return String(decoding: urlData, as: UTF8.self)
    }
}
