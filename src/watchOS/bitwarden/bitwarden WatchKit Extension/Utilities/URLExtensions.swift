import Foundation

extension URL {
    static func createFullUri(from uriString:String?) -> URL? {
        guard let uriString = uriString else {
            return nil
        }
        
        let hasHttpScheme = uriString.starts(with: "http://") || uriString.starts(with: "https://")
        if !hasHttpScheme && !uriString.contains("://") && uriString.contains(".") {
            if let uri = URL(string: "http://\(uriString)") {
                return uri
            }
        }
        guard let uri2 = URL(string: uriString) else {
            return nil
        }
        
        return uri2
    }
    
    var host:String? {
        if let components = URLComponents(url: self, resolvingAgainstBaseURL: false){
            return components.host
        }
        return nil
    }
}
