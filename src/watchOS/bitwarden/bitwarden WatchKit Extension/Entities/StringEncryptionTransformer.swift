import Foundation
import UIKit

@objc(StringEncryptionTransformer)
class StringEncryptionTransformer : ValueTransformer {
    var cryptoService: CryptoService = CryptoService()
        
    override public class func allowsReverseTransformation() -> Bool {
        return true
    }
    
    override func transformedValue(_ value: Any?) -> Any?{
        var toEncrypt: String
        
        switch value {
        case let aString as String:
            toEncrypt = aString
        default:
            return nil
        }
        
        if let encryptedData = cryptoService.encrypt(toEncrypt) {
            return encryptedData
        }

        return nil
    }
    
    override func reverseTransformedValue(_ value: Any?) -> Any?{
        if let encryptedData = value as? Data {
            if let decryptedData = cryptoService.decrypt(encryptedData) {
                return String(decoding: decryptedData, as: UTF8.self)
            }
        }
        
        return nil
    }
}

extension StringEncryptionTransformer {
    static let name = NSValueTransformerName(rawValue: String(describing: StringEncryptionTransformer.self))

    /// Registers the value transformer with `ValueTransformer`.
    public static func register() {
        let transformer = StringEncryptionTransformer()
        ValueTransformer.setValueTransformer(transformer, forName: name)
    }
}
