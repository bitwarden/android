import Foundation

struct CipherMock {
    static let ciphers:[Cipher] = [
        Cipher(id: "0", name: "MySite", userId: "123123", login: Login(username: "test@testing.com", totp: "otpauth://account?period=10&secret=LLLLLLLLLLLLLLLL", uris: cipherLoginUris)),
        Cipher(id: "1", name: "GitHub", userId: "123123", login: Login(username: "thisisatest@testing.com", totp: "LLLLLLLLLLLLLLLL", uris: cipherLoginUris)),
        Cipher(id: "2", name: "No user", userId: "123123", login: Login(username: nil, totp: "otpauth://account?period=10&digits=8&algorithm=sha256&secret=LLLLLLLLLLLLLLLL", uris: cipherLoginUris)),
        Cipher(id: "3", name: "Site 2", userId: "123123", login: Login(username: "longtestemail000000@fastmailasdfasdf.com", totp: "otpauth://account?period=10&digits=7&algorithm=sha512&secret=LLLLLLLLLLLLLLLL", uris: cipherLoginUris)),
        Cipher(id: "4", name: "Really long name for a site that is used for a totp", userId: "123123", login: Login(username: "user3", totp: "steam://LLLLLLLLLLLLLLLL", uris: cipherLoginUris)),
        Cipher(id: "5", name: "Short", userId: "123123", login: Login(username: "u", totp: "steam://LLLLLLLLLLLLLLLL", uris: cipherLoginUris))
    ]
    
    static let cipherLoginUris:[LoginUri] = [
        LoginUri(uri: "github.com"),
        LoginUri(uri: "example2.com")
    ]
}
