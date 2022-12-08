import Foundation
import CoreData


extension CipherEntity {

    @nonobjc public class func fetchRequest() -> NSFetchRequest<CipherEntity> {
        return NSFetchRequest<CipherEntity>(entityName: "CipherEntity")
    }

    @NSManaged public var id: String
    @NSManaged public var name: String?
    @NSManaged public var userId: String
    @NSManaged public var totp: String?
    @NSManaged public var type: NSObject?
    @NSManaged public var username: String?
    @NSManaged public var loginUris: String?

}

extension CipherEntity : Identifiable {
    func toCipher() -> Cipher{
        
        var loginUrisArray: [LoginUri]?
        if loginUris != nil {
            loginUrisArray = try? JSONDecoder().decode([LoginUri].self, from: loginUris!.data(using: .utf8)!)
        }
        
        return Cipher(id: id,
                      name: name,
                      userId: userId,
                      login: Login(username: username, totp: totp, uris: loginUrisArray))
    }
}
