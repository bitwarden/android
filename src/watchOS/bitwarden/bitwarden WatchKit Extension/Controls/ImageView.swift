import Foundation
import SwiftUI
import Combine

/// Image view to be used on watchOS < 8
///
/// - Note: Based on:  https://stackoverflow.com/questions/60710997/images-disappear-in-list-as-i-scroll-swiftui-swift
///
struct ImageView<PlaceholderView: View>: View {
    @ObservedObject var imageLoader:ImageLoader
    var imgMaxWidth:CGFloat
    var imgMaxHeight:CGFloat
    var placeholder: PlaceholderView
    
    init(withURL url:String, maxWidth mw: CGFloat, maxHeight mh: CGFloat, @ViewBuilder _ placeholder: () -> PlaceholderView) {
        imageLoader = ImageLoader(urlString:url)
        self.imgMaxWidth = mw
        self.imgMaxHeight = mh
        self.placeholder = placeholder()
    }

    var body: some View {
        if imageLoader.image == nil {
            placeholder
        } else {
            Image(uiImage: imageLoader.image! )
                .resizable()
                .aspectRatio(contentMode: .fit)
                .frame(maxWidth:imgMaxWidth, maxHeight:imgMaxHeight)
        }
    }
}

class ImageLoader: ObservableObject {
    @Published var image: UIImage?
    var urlString: String?
    var imageCache = ImageCache.getImageCache()
    
    init(urlString: String?) {
        self.urlString = urlString
        loadImage()
    }
    
    func loadImage() {
        if loadImageFromCache() {
            return
        }
        
        loadImageFromUrl()
    }
    
    func loadImageFromCache() -> Bool {
        guard let urlString = urlString else {
            return false
        }
        
        guard let cacheImage = imageCache.get(forKey: urlString) else {
            return false
        }
        
        image = cacheImage
        return true
    }
    
    func loadImageFromUrl() {
        guard let urlString = urlString else {
            return
        }
        
        let url = URL(string: urlString)!
        let task = URLSession.shared.dataTask(with: url, completionHandler: getImageFromResponse(data:response:error:))
        task.resume()
    }
    
    
    func getImageFromResponse(data: Data?, response: URLResponse?, error: Error?) {
        guard error == nil else {
            return
        }
        guard let data = data else {
            return
        }
        
        DispatchQueue.main.async {
            guard let loadedImage = UIImage(data: data) else {
                return
            }
            
            self.imageCache.set(forKey: self.urlString!, image: loadedImage)
            self.image = loadedImage
        }
    }
}

class ImageCache {
    var cache = NSCache<NSString, UIImage>()
    
    func get(forKey: String) -> UIImage? {
        return cache.object(forKey: NSString(string: forKey))
    }
    
    func set(forKey: String, image: UIImage) {
        cache.setObject(image, forKey: NSString(string: forKey))
    }
}

extension ImageCache {
    private static var imageCache = ImageCache()
    static func getImageCache() -> ImageCache {
        return imageCache
    }
}
