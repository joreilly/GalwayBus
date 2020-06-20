//
//  AppDelegate.swift
//  GalwayBus
//
//  Created by John O'Reilly on 20/06/2020.
//  Copyright © 2020 John O'Reilly. All rights reserved.
//

import Cocoa
import SwiftUI
import SharedCode

@NSApplicationMain
class AppDelegate: NSObject, NSApplicationDelegate {

    var window: NSWindow!


    func applicationDidFinishLaunching(_ aNotification: Notification) {
        // Create the SwiftUI view that provides the window contents.
        let repository = GalwayBusRepository()
        let busRouteViewModel = BusRouteViewModel(repository: repository)
        let busStopViewModel = BusStopViewModel(repository: repository)

        
        let contentView = ContentView(busRouteViewModel: busRouteViewModel, busStopViewModel: busStopViewModel)

        // Create the window and set the content view. 
        window = NSWindow(
            contentRect: NSRect(x: 0, y: 0, width: 480, height: 300),
            styleMask: [.titled, .closable, .miniaturizable, .resizable, .fullSizeContentView],
            backing: .buffered, defer: false)
        window.center()
        window.setFrameAutosaveName("Main Window")
        window.contentView = NSHostingView(rootView: contentView)
        window.makeKeyAndOrderFront(nil)
    }

    func applicationWillTerminate(_ aNotification: Notification) {
        // Insert code here to tear down your application
    }


}

