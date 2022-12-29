import Foundation

extension UInt64 {
    var data: Data {
        var int64 = self
        let int64Data = Data(bytes: &int64, count: MemoryLayout.size(ofValue: self))
        return int64Data
    }
}
