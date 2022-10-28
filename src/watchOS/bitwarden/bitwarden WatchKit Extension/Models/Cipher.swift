//
//  Cipher.swift
//  bitwarden WatchKit Extension
//
//  Created by Federico Andrés Maccaroni on 25/08/2022.
//

import Foundation

struct WatchDTO:Codable{
    var ciphers:[Cipher]
}

struct Cipher:Identifiable,Codable{
    var id:String
    var name:String?
    var organizationUseTotp:Bool
    var login:Login
}

struct Login:Codable{
    var username:String?
    var totp:String?
}

//extension Cipher{
//    static let samples = [
//        Cipher(name: "Facebook", login: Login(totp: "111 111")),
//        Cipher(name: "Twitter", login: Login(totp: "345 654")),
//        Cipher(name: "Amazon", login: Login(totp: "325 867")),
//        Cipher(name: "Google", login: Login(totp: "092 234")),
//        Cipher(name: "Bitwarden", login: Login(totp: "123 464")),
//        Cipher(name: "Authy", login: Login(totp: "452 322")),
//        Cipher(name: "Wave", login: Login(totp: "333 333")),
//        Cipher(name: "Super Mario", login: Login(totp: "432 654")),
//        Cipher(name: "Outlook", login: Login(totp: "293 549"))
//    ]
//}
