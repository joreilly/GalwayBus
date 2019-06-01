//
//  SecondViewController.swift
//  GalwayBusApp
//
//  Created by jooreill on 26/05/2019.
//  Copyright © 2019 Surrus. All rights reserved.
//

import UIKit
import SharedCode

class SecondViewController: UIViewController, UITableViewDataSource, UITableViewDelegate {
    @IBOutlet weak var tableView: UITableView!
    
    var listRoutes: [BusRoute] = []
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        tableView.delegate = self
        tableView.dataSource = self
        
        let g = GalwayBusRepository()
        g.fetchBusRoutes(success: { data in
            self.listRoutes = data
            self.tableView.reloadData()
            return KotlinUnit()
        })
    }


    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return listRoutes.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "LabelCell", for: indexPath)
        
        cell.textLabel?.text = String(listRoutes[indexPath.row].timetableId)
        
        return cell
    }
    
}

