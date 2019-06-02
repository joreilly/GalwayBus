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
        guard let cell = tableView.dequeueReusableCell(withIdentifier: "BusRouteCell", for: indexPath) as? BusRouteTableViewCell else {
            fatalError("The dequeued cell is not an instance of BusRouteTableViewCell.")
        }
        
        let route = listRoutes[indexPath.row]
        
        cell.nameLabel.text = String(route.timetableId)
        cell.routeNameLabel.text = route.longName
        
        return cell
    }
    
}

