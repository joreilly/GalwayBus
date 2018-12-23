//
//  ViewController.swift
//  GalwayBus
//
//  Created by jooreill on 22/12/2018.
//  Copyright © 2018 jooreill. All rights reserved.
//

import UIKit
import SharedCode


class ViewController: UIViewController {

    override func viewDidLoad() {
        super.viewDidLoad()
        
        let label = UILabel(frame: CGRect(x: 0, y: 0, width: 300, height: 21))
        label.center = CGPoint(x: 160, y: 285)
        label.textAlignment = .center
        label.font = label.font.withSize(25)
        label.text = CommonKt.createApplicationScreenMessage()
        view.addSubview(label)

        var g: GalwayBusApi = GalwayBusApi()
        
        
    }


}

