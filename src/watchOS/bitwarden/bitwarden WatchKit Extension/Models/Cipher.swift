import Foundation
import CoreData

struct WatchDTO:Codable{
    var ciphers:[Cipher]
}

struct Cipher:Identifiable,Codable{
    var id:String
    var name:String?
    var organizationUseTotp:Bool
    var login:Login
}

struct Login:Codable{
    var username:String?
    var totp:String?
}

extension Cipher{
    func toCipherEntity(moContext: NSManagedObjectContext) -> CipherEntity{
        let entity = CipherEntity(context: moContext)
        entity.id = id        
        entity.name = name
        entity.organizationUseTotp = organizationUseTotp
        entity.username = login.username
        entity.totp = login.totp
        return entity
    }
}
