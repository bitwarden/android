import Foundation
import SwiftUI

class CipherDetailsViewModel: ObservableObject{
    @Published var cipher:Cipher    
    
    @Published var totpFormatted:String = ""
    @Published var progress:Double = 1
    @Published var counter:Int
    @Published var iconImageUri:String?
    
    var key: String
    var period: Int
    var timer: Timer? = nil
    
    init(cipher: Cipher) {
        self.cipher = cipher
        self.key = cipher.login.totp!
        self.period = TotpService.shared.getPeriodFrom(key)
        self.counter = period
        self.iconImageUri = IconImageHelper.shared.getLoginIconImage(cipher)
    }
    
    func startGeneration() {
        self.timer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true, block: { [weak self] t in
            guard let self = self else {
                t.invalidate()
                return
            }
            
            let epoc = Int64(Date().timeIntervalSince1970)
            let mod = Int(epoc % Int64(self.period))
            DispatchQueue.main.async {
                self.counter = self.period - mod
                self.progress = Double(self.counter) / Double(self.period)
            }
            
            if mod == 0 || self.totpFormatted == "" {
                do {
                    try self.regenerateTotp()
                } catch {
                    DispatchQueue.main.async {
                        self.totpFormatted = "error"
                        t.invalidate()
                    }
                }
            }
        })
        RunLoop.current.add(timer!, forMode: .common)
        timer?.fire()
    }
    
    func stopGeneration() {
        self.timer?.invalidate()
    }
    
    func regenerateTotp() throws {
        var totpF = try TotpService.shared.GetCodeAsync(key: self.key) ?? ""
        if totpF.count > 4 {
            let halfIndex = totpF.index(totpF.startIndex, offsetBy: totpF.count / 2)
            totpF = "\(totpF[totpF.startIndex..<halfIndex]) \(totpF[halfIndex..<totpF.endIndex])"
        }
        DispatchQueue.main.async {
            self.totpFormatted = totpF
        }
    }
}
