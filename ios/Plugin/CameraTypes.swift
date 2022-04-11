//
//  CameraTypes.swift
//  Plugin
//
//  Created by sultanmyrza on 2022/3/2.
//  Copyright © 2022 Max Lynch. All rights reserved.
//

import Foundation
import UIKit

// MARK: - Public

public enum CameraDirection: String {
    case rear = "REAR"
    case front = "FRONT"
}

public enum CameraResultType: String {
    case base64
    case uri
    case dataURL = "dataUrl"
}

struct CameraPromptText {
    let title: String
    let cameraAction: String
    let cancelAction: String

    init(title: String? = nil, cameraAction: String? = nil, cancelAction: String? = nil) {
        self.title = title ?? "Camera"
        self.cameraAction = cameraAction ?? "Start Preview"
        self.cancelAction = cancelAction ?? "Cancel"
    }
}

public struct CameraSettings {
    var direction: CameraDirection = .rear
    var resultType: CameraResultType = .base64
    var userPromptText = CameraPromptText()
    var jpegQuality: CGFloat = 1.0
    var width: CGFloat = 0
    var height: CGFloat = 0
    var shouldResize = false
    var shouldCorrectOrientation = true
    var presentationStyle = UIModalPresentationStyle.fullScreen
}

public enum PreviewCameraError: String, Error {
    case previewAlreadyStarted
    case previewAlreadyStopped
    case previewNotStarted
    case recordAlreadyStarted
    case recordAlreadyStopped
    case recordNotStarted
    case defaultDeviceUsedToCaptureDataBusy
    case setupCameraFailure

    var message: String {
        switch self {
        case .previewAlreadyStarted:
            return "Preview camera already started."
        case .previewAlreadyStopped:
            return "Preview camera already stopped."
        case .previewNotStarted:
            return "Preview camera not started yet."
        case .recordAlreadyStarted:
            return "Record video already started."
        case .recordAlreadyStopped:
            return "Record video already stopped."
        case .recordNotStarted:
            return "Record video not started yet."
        case .defaultDeviceUsedToCaptureDataBusy:
            return "Default device used to capture data is used by other process"
        case .setupCameraFailure:
            return "Setup camera failure"
        }
    }
}

// MARK: - Internal

internal enum CameraPermissionType: String, CaseIterable {
    case camera
}

internal enum CameraPropertyListKeys: String, CaseIterable {
    case photoLibraryAddUsage = "NSPhotoLibraryAddUsageDescription"
    case photoLibraryUsage = "NSPhotoLibraryUsageDescription"
    case cameraUsage = "NSCameraUsageDescription"

    var link: String {
        switch self {
        case .photoLibraryAddUsage:
            return "https://developer.apple.com/library/content/documentation/General/Reference/InfoPlistKeyReference/Articles/CocoaKeys.html#//apple_ref/doc/uid/TP40009251-SW73"
        case .photoLibraryUsage:
            return "https://developer.apple.com/library/content/documentation/General/Reference/InfoPlistKeyReference/Articles/CocoaKeys.html#//apple_ref/doc/uid/TP40009251-SW17"
        case .cameraUsage:
            return "https://developer.apple.com/library/content/documentation/General/Reference/InfoPlistKeyReference/Articles/CocoaKeys.html#//apple_ref/doc/uid/TP40009251-SW24"
        }
    }

    var missingMessage: String {
        return "You are missing \(self.rawValue) in your Info.plist file." +
            " Camera will not function without it. Learn more: \(self.link)"
    }
}
