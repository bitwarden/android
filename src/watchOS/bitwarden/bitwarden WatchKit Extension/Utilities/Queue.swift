import Foundation

/// This is basic Queue implementation that uses an array internally.
/// For the current use is enough, If intended to use with many items please improve it
/// following https://nitinagam17.medium.com/data-structure-in-swift-queue-part-5-985601071606 Linked list or Two stacks approach
struct ArrayQueue<T> : CustomStringConvertible {
    private var elements: [T] = []
    public init() {}
    
    var isEmpty: Bool {
        elements.isEmpty
    }
    
    var peek: T? {
        elements.first
    }
    
    var description: String {
        if isEmpty {
            return "Queue empty"
        }
        
        return "---- Queue -----\n"
            + elements.map({"\($0)"}).joined(separator: " -> ")
            + "---- Queue end -----"
    }
    
    mutating func enqueue(_ value: T) {
        elements.append(value)
    }
    
    mutating func dequeue() -> T? {
        isEmpty ? nil : elements.removeFirst()
    }
}
