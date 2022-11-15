import Foundation
import CryptoKit

class CryptoFunctionService{
    static let shared: CryptoFunctionService = CryptoFunctionService()
    
    enum CryptoHashAlgorithm {
        case Sha1, Sha256, Sha512
    }
    
    enum CryptoError: Error {
        case AlgorithmNotImplemented
    }
    
    func hmac(_ data: Data, _ key: SymmetricKey, algorithm alg: CryptoHashAlgorithm)  -> Data {
        switch alg {
        case .Sha1:
            return Data(HMAC<Insecure.SHA1>.authenticationCode(for: data, using: key))
        case .Sha256:
            return Data(HMAC<SHA256>.authenticationCode(for: data, using: key))
        case .Sha512:
            return Data(HMAC<SHA512>.authenticationCode(for: data, using: key))
        }
    }
}
