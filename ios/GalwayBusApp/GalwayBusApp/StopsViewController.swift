//
//  FirstViewController.swift
//  GalwayBusApp
//
//  Created by jooreill on 26/05/2019.
//  Copyright © 2019 Surrus. All rights reserved.
//

import UIKit
import SharedCode

class FirstViewController: UIViewController, UITableViewDataSource, UITableViewDelegate {
    @IBOutlet weak var tableView: UITableView!
    
    var listStops: [BusStop] = []
    
    override func viewDidLoad() {
        super.viewDidLoad()

        tableView.delegate = self
        tableView.dataSource = self
        
        let g = GalwayBusRepository()
        g.getBusStops(success: { data in
            self.listStops = data
            self.tableView.reloadData()
            return KotlinUnit()
        })
        
        
        g.getNearestStops(latitude: 53.2743394, longitude: -9.0514163, success: { data in
            return KotlinUnit()
        })
    }

    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return listStops.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "LabelCell", for: indexPath)
        
        cell.textLabel?.text = listStops[indexPath.row].short_name + " (" + listStops[indexPath.row].irish_short_name + ")"
        
        return cell
    }
    

}

