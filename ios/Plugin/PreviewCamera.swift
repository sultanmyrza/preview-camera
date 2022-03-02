import Foundation

@objc public class PreviewCamera: NSObject {
    @objc public func echo(_ value: String) -> String {
        print(value)
        return value
    }
}
