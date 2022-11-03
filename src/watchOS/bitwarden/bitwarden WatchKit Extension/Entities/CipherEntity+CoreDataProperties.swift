import Foundation
import CoreData


extension CipherEntity {

    @nonobjc public class func fetchRequest() -> NSFetchRequest<CipherEntity> {
        return NSFetchRequest<CipherEntity>(entityName: "CipherEntity")
    }

    @NSManaged public var id: String
    @NSManaged public var name: String?
    @NSManaged public var organizationUseTotp: Bool
    @NSManaged public var totp: String?
    @NSManaged public var type: NSObject?
    @NSManaged public var username: String?

}

extension CipherEntity : Identifiable {
    func toCipher() -> Cipher{
        return Cipher(id: id,
                      name: name,
                      organizationUseTotp: organizationUseTotp,
                      login: Login(username: username, totp: totp))
    }
}
