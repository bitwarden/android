import Foundation
import CoreData

enum DecoderConfigurationError: Error {
  case missingManagedObjectContext
}

@objc(CipherEntity)
public class CipherEntity: NSManagedObject, Codable {
    enum CodingKeys: CodingKey {
        case id, name, organizationUseTotp, username, totp
     }
    
    public func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(name, forKey: .name)
        try container.encode(organizationUseTotp, forKey: .organizationUseTotp)
        try container.encode(username, forKey: .username)
        try container.encode(totp, forKey: .totp)
    }

    public required convenience init(from decoder: Decoder) throws {
        guard let context = decoder.userInfo[CodingUserInfoKey.managedObjectContext] as? NSManagedObjectContext else {
            throw DecoderConfigurationError.missingManagedObjectContext
        }

        self.init(context: context)

        let container = try decoder.container(keyedBy: CodingKeys.self)
        self.id = try container.decode(String.self, forKey: .id)
        self.name = try container.decode(String?.self, forKey: .name)
        self.organizationUseTotp = try container.decode(Bool.self, forKey: .organizationUseTotp)
        self.username = try container.decode(String?.self, forKey: .username)
        self.totp = try container.decode(String?.self, forKey: .totp)
    }
}

extension CodingUserInfoKey {
  static let managedObjectContext = CodingUserInfoKey(rawValue: "managedObjectContext")!
}
