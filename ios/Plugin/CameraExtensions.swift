//
//  CameraExtensions.swift
//  Plugin
//
//  Created by sultanmyrza on 2022/3/2.
//  Copyright © 2022 Max Lynch. All rights reserved.
//

import Foundation
import AVFoundation

internal protocol CameraAuthorizationState {
    var authorizationState: String { get }
}

extension AVAuthorizationStatus: CameraAuthorizationState {
    var authorizationState: String {
        switch self {
        case .denied, .restricted:
            return "denied"
        case .authorized:
            return "granted"
        case .notDetermined:
            fallthrough
        @unknown default:
            return "prompt"
        }
    }
}
