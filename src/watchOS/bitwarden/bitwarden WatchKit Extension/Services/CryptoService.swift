import Foundation
import CryptoKit

public class CryptoService{
    static let ENCRYPTION_KEY: String = "encryptionKey"
    
    private(set) var key: SymmetricKey? = nil
    
    init(){
        key = loadKey()
    }
    
    func encrypt(_ plainText: String?) -> Data? {
        guard let plainText = plainText, let key = key else {
            return nil
        }
        
        let nonce = randomData(lengthInBytes: 12)
        
        let plainData = plainText.data(using: .utf8)
        let sealedData = try! AES.GCM.seal(plainData!, using: key, nonce: AES.GCM.Nonce(data:nonce))
        return sealedData.combined
    }
    
    func decrypt<T: Decodable>(_ combinedEncryptedData: Data, _ type: T.Type) -> T? {
        guard let key = key else {
            return nil
        }
         
        let sealedBox = try! AES.GCM.SealedBox(combined: combinedEncryptedData)
        let decryptedData = try! AES.GCM.open(sealedBox, using: key)
        
        do {
            let item = try JSONDecoder().decode(type, from: decryptedData)
            return item
        } catch {
            Log.e("Fail to decode item for keychain: \(error)")
            return nil
        }
    }
    
    func decrypt(_ combinedEncryptedData: Data) -> Data? {
        guard let key = key else {
            return nil
        }
         
        let sealedBox = try! AES.GCM.SealedBox(combined: combinedEncryptedData)
        let decryptedData = try! AES.GCM.open(sealedBox, using: key)
        return decryptedData
    }

    func loadKey() -> SymmetricKey{
        if let encKey = KeychainHelper.standard.read(CryptoService.ENCRYPTION_KEY) {
            return SymmetricKey(data: encKey)
        }
        
        // First time so we need to generate the key
        let newKey = SymmetricKey(size: .bits256)
        let keyData = newKey.withUnsafeBytes({ body in
            return Data(Array(body))
        })
        KeychainHelper.standard.save(keyData, CryptoService.ENCRYPTION_KEY)
        return newKey
    }
    
    func randomData(lengthInBytes: Int) -> Data {
        var data = Data(count: lengthInBytes)
        _ = data.withUnsafeMutableBytes {
          SecRandomCopyBytes(kSecRandomDefault, lengthInBytes, $0.baseAddress!)
        }
        return data
    }
    
    static func clearKey() {
        KeychainHelper.standard.delete(CryptoService.ENCRYPTION_KEY)
    }
}

public extension Data {
    init?(hexString: String) {
      let len = hexString.count / 2
      var data = Data(capacity: len)
      var i = hexString.startIndex
      for _ in 0..<len {
        let j = hexString.index(i, offsetBy: 2)
        let bytes = hexString[i..<j]
        if var num = UInt8(bytes, radix: 16) {
          data.append(&num, count: 1)
        } else {
          return nil
        }
        i = j
      }
      self = data
    }
    /// Hexadecimal string representation of `Data` object.
    var hexadecimal: String {
        return map { String(format: "%02x", $0) }
            .joined()
    }
}
