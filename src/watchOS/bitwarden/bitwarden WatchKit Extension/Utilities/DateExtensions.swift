import Foundation

extension Date{
    var epocUtcNowInMs: Int {
        return Int(self.timeIntervalSince1970 * 1_000)
    }

}
