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
        /*
        case let aBool as Bool:
            toEncrypt = aBool.description
        */
        switch value {
        case let aString as String:
            toEncrypt = aString
        default:
            return nil
        }
        
        if let encryptedData = cryptoService.encrypt(toEncrypt) {
            return String(decoding: encryptedData, as: UTF8.self)
        }

        return nil
    }
    
    override func reverseTransformedValue(_ value: Any?) -> Any?{
        if let encryptedString = value as? String {
            if let decryptedData = cryptoService.decrypt(encryptedString.data(using: .utf8)!) {
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

