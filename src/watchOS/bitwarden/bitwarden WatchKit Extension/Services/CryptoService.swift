//
//  CryptoService.swift
//  bitwarden WatchKit Extension
//
//  Created by Federico Andrés Maccaroni on 19/10/2022.
//

import Foundation
import CryptoKit

public class CryptoService{
    //let keyStr = "d5a423f64b607ea7c65b311d855dc48f36114b227bd0c7a3d403f6158a9e4412"
    
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
        
        print(String(decoding: decryptedData, as: UTF8.self))
        
        do {
            let item = try JSONDecoder().decode(type, from: decryptedData)
            return item
        } catch {
            assertionFailure("Fail to decode item for keychain: \(error)")
            return nil
        }
    }
    
    func decrypt(_ combinedEncryptedData: Data) -> Data? {
        guard let key = key else {
            return nil
        }
         
        let sealedBox = try! AES.GCM.SealedBox(combined: combinedEncryptedData)
        let decryptedData = try! AES.GCM.open(sealedBox, using: key)
        
        print(String(decoding: decryptedData, as: UTF8.self))
        return decryptedData
    }



    
    
/*
    func encrypt(_ plainText: String?) -> Data? {
        //let key = SymmetricKey(data: Data(hexString:keyStr)!)
        guard let plainText = plainText, let key = key else {
            return nil
        }
        
        let nonce = randomData(lengthInBytes: 12)
        
        let plainData = plainText.data(using: .utf8)
        let sealedData = try! AES.GCM.seal(plainData!, using: key, nonce: AES.GCM.Nonce(data:nonce))
        return sealedData.combined
        
        //let ciphertext = Data(base64Encoded: "LzpSalRKfL47H5rUhqvA")
        //let nonce = Data(hexString: "131348c0987c7eece60fc0bc") // = initialization vector
        //let tag = Data(hexString: "5baa85ff3e7eda3204744ec74b71d523")
        //let sealedBox = try! AES.GCM.SealedBox(nonce: AES.GCM.Nonce(data: nonce),
        //                                      ciphertext: ciphertext!,
        //                                       tag: tag)
        
        
        //print("Nonce: \(sealedData.nonce.withUnsafeBytes { Data(Array($0)).hexadecimal })")
        //print("Tag: \(sealedData.tag.hexadecimal)")
        //print("Data: \(sealedData.ciphertext.base64EncodedString())")
        
    }
    
    func decrypt<T: Decodable>(_ combinedEncryptedData: Data, _ type: T.Type) -> T? {
        //let nonce = Data(hexString: "131348c0987c7eece60fc0bc") // = initialization vector
        //let tag = Data(hexString: "5baa85ff3e7eda3204744ec74b71d523")

        //let sealedBox = try! AES.GCM.SealedBox(nonce: AES.GCM.Nonce(data: nonce),
        //                                       ciphertext: encryptedText.data(using: .utf8)!,
        //                                       tag: tag)
        
        guard let key = key else {
            return nil
        }
         
        let sealedBox = try! AES.GCM.SealedBox(combined: combinedEncryptedData)
        let decryptedData = try! AES.GCM.open(sealedBox, using: key)
        
        print(String(decoding: decryptedData, as: UTF8.self))
        
        do {
            let item = try JSONDecoder().decode(type, from: decryptedData)
            return item
        } catch {
            assertionFailure("Fail to decode item for keychain: \(error)")
            return nil
        }

    }
 
 */
    
    func loadKey() -> SymmetricKey{
        if let encKey = KeychainHelper.standard.read("encryptionKey") {
            return SymmetricKey(data: encKey)
        }
        
        // First time so we need to generate the key
        let newKey = SymmetricKey(size: .bits256)
        let keyData = newKey.withUnsafeBytes({ body in
            return Data(Array(body))
        })
        KeychainHelper.standard.save(keyData, "encryptionKey")
        return newKey
    }
    
    func randomData(lengthInBytes: Int) -> Data {
        var data = Data(count: lengthInBytes)
        _ = data.withUnsafeMutableBytes {
          SecRandomCopyBytes(kSecRandomDefault, lengthInBytes, $0.baseAddress!)
        }
        return data
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
