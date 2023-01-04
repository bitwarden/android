import Foundation
import CryptoKit

class TotpService{
    struct CodeConfig{
        let period:Int
        let digits:Int
        let algorithm:CryptoFunctionService.CryptoHashAlgorithm
        let keyB32:String?
    }
    
    static let shared: TotpService = TotpService()
    
    static let STEAM_CHARS = "23456789BCDFGHJKMNPQRTVWXY";
    static let TOTP_DEFAULT_TIMER: Int = 30
    
    func GetCodeAsync(key: String?) throws -> String?{
        guard let key = key, !key.isEmpty else {
            return nil
        }
        
        var config = CodeConfig(period: TotpService.TOTP_DEFAULT_TIMER, digits: 6, algorithm: CryptoFunctionService.CryptoHashAlgorithm.Sha1, keyB32: key)

        let isOtpAuth = key.lowercased().starts(with: "otpauth://");
        let isSteamAuth = key.lowercased().starts(with: "steam://");
        
        if isOtpAuth {
            guard let keyUrl = URLComponents(string: key) else {
                return nil
            }
            
            if let queryItems = keyUrl.queryItems {
                config = getCodeConfigFrom(queryItems, config)
            }
        } else if isSteamAuth {
            let keyIndexOffset = key.index(key.startIndex, offsetBy: 8)

            config = CodeConfig(period: config.period, digits: 5, algorithm: config.algorithm, keyB32: String(key.suffix(from: keyIndexOffset)))
        }
        
        guard let keyB32 = config.keyB32 else {
            return nil
        }

        let keyBytes = try Base32.fromBase32(keyB32)
        if keyBytes.count == 0 {
            return nil
        }
                
        let counter = UInt64(Date().timeIntervalSince1970 / TimeInterval(config.period)).bigEndian
        let hash = CryptoFunctionService.shared.hmac(counter.data, SymmetricKey(data:Data(keyBytes)), algorithm: config.algorithm)
        if hash.count == 0
        {
            return nil;
        }

        let offset = Int(hash[hash.count - 1] & 0xf)
        let binary = Int32(hash[offset] & 0x7f) << 24 | Int32(hash[offset + 1] & 0xff) << 16 | Int32(hash[offset + 2] & 0xff) << 8 | Int32(hash[offset + 3] & 0xff)

        var otp = "";
        if (isSteamAuth) {
            var fullCode = Int(binary & 0x7fffffff)
            for _ in 0..<config.digits {
                let steamCharsIndex = TotpService.STEAM_CHARS.index(TotpService.STEAM_CHARS.startIndex, offsetBy: fullCode % TotpService.STEAM_CHARS.count)
                otp += String(TotpService.STEAM_CHARS[steamCharsIndex])
                fullCode = Int(Double(fullCode) / Double(TotpService.STEAM_CHARS.count))
            }
        } else {
            let rawOtp = UInt32(binary) % UInt32(pow(10, Float(config.digits)))
            otp = String(rawOtp)
            if otp.count != config.digits {
                // Pad left string with zeros
                let prefixedZeros = String(repeatElement("0", count: (config.digits - otp.count)))
                otp = (prefixedZeros + otp)
            }
        }
        return otp
    }
    
    func getCodeConfigFrom(_ queryItems: [URLQueryItem], _ currentConfig: CodeConfig) -> CodeConfig {
        var period:Int?
        var digits:Int?
        var algorithm:CryptoFunctionService.CryptoHashAlgorithm?
        var keyB32:String?
        
        for item in queryItems {
            if item.name == "digits",
               let digitsVal = item.value,
               let digitParam = Int(digitsVal.trimmingCharacters(in: .whitespacesAndNewlines)) {
                if (digitParam > 10){
                    digits = 10;
                } else if (digitParam > 0){
                    digits = digitParam;
                }
            } else if item.name == "period",
                      let periodVal = item.value,
                      let periodParam = Int(periodVal.trimmingCharacters(in: .whitespacesAndNewlines)),
                      periodParam > 0 {
                period = periodParam
            } else if item.name == "secret", let secretVal = item.value {
                keyB32 = secretVal
            } else if item.name == "algorithm", let algorithmVal = item.value {
                if algorithmVal.lowercased() == "sha256" {
                    algorithm = CryptoFunctionService.CryptoHashAlgorithm.Sha256;
                }
                else if algorithmVal.lowercased() == "sha512" {
                    algorithm = CryptoFunctionService.CryptoHashAlgorithm.Sha512;
                }
            }
        }
        
        return CodeConfig(period: period ?? currentConfig.period,
                          digits: digits ?? currentConfig.digits,
                          algorithm: algorithm ?? currentConfig.algorithm,
                          keyB32: keyB32 ?? currentConfig.keyB32)
    }
    
    func getPeriodFrom(_ key: String) -> Int {
        guard key.lowercased().starts(with: "otpauth://"),
              let keyUrl = URLComponents(string: key),
              let queryItems = keyUrl.queryItems  else {
            return TotpService.TOTP_DEFAULT_TIMER
        }
        
        let periodQueryItem = queryItems.first { qi in qi.name == "period" }
        guard let periodValue = periodQueryItem?.value,
              let periodInt = Int(periodValue.trimmingCharacters(in: .whitespacesAndNewlines)),
              periodInt > 0 else {
                  return TotpService.TOTP_DEFAULT_TIMER
              }
        return periodInt
    }
}
