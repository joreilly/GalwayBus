import Combine
import SharedCode


class BusRouteViewModel: ObservableObject {
    @Published private(set) var listRoutes: [BusRoute] = []
    
    
    private let repository: GalwayBusRepository
    init(repository: GalwayBusRepository) {
        self.repository = repository
    }
    
    func fetch() {
        repository.fetchBusRoutes(success: { data in
            self.listRoutes = data
        })
    }
}

    
class BusStopViewModel: ObservableObject {
    @Published var listStops = [BusStop]()
    @Published var busList = [Bus]()
    
    private let repository: GalwayBusRepository
    init(repository: GalwayBusRepository) {
        self.repository = repository
    }
    
    func fetch() {
        repository.getBusStops(success: { data in
            self.listStops = data
        })
    }
    
    func fetchBusList() {
        repository.fetchBusListForRoute(routeId: "401", success: { data in
            self.busList = data
        })
    }

}



