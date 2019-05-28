//
//  FirstViewController.swift
//  GalwayBusApp
//
//  Created by jooreill on 26/05/2019.
//  Copyright © 2019 Surrus. All rights reserved.
//

import UIKit
import main

class FirstViewController: UIViewController {

    override func viewDidLoad() {
        super.viewDidLoad()
        
        let g = GalwayBusRepository()
        g.fetchBusStops2(success: { data in
            return KotlinUnit()
        })
    }


}

