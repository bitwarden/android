import Foundation
import CoreData

struct Cipher:Identifiable,Codable{
    enum CodingKeys : CodingKey {
        case id
        case name
        case login
    }
    
    var id:String
    var name:String?
    var userId:String?
    var login:Login
}

struct Login:Codable{
    var username:String?
    var totp:String?
    var uris:[LoginUri]?
}

struct LoginUri:Codable{
    var uri:String?
}

extension Cipher{
    func toCipherEntity(moContext: NSManagedObjectContext) -> CipherEntity{
        let entity = CipherEntity(context: moContext)
        entity.id = id        
        entity.name = name
        entity.userId = userId ?? "unknown"
        entity.username = login.username
        entity.totp = login.totp
        
        if let uris = login.uris, let encodedData = try? JSONEncoder().encode(uris) {
            entity.loginUris = String(data: encodedData, encoding: .utf8)
        }
        
        return entity
    }
}
