import Combine
import SharedCode
import KMPNativeCoroutinesAsync



@MainActor
class NearbyStopsViewModel: ObservableObject {
    @Published private(set) var listStops: [BusStop] = []
    @Published public var favorites: Set<String> = []
    @Published private(set) var depatures: [GalwayBusDeparture] = []
    
    
    private let repository: GalwayBusRepository
    init(repository: GalwayBusRepository) {
        self.repository = repository
                
        Task {
            do {
                let stream = asyncSequence(for: repository.favorites)
                for try await data in stream {
                    self.favorites = data
                    print(data)
                }
            } catch {
                print("Failed with error: \(error)")
            }
        }
    }
    
    func fetch() async {
        do {
            let result = try await asyncFunction(for: repository.fetchNearestStops(latitude: 53.2743394, longitude: -9.0514163))
            if (result is ResultSuccess<NSArray>) {
                let successResult = result as! ResultSuccess<NSArray>
                self.listStops = successResult.data as! [BusStop]
            } else {
                print(result)
            }
        } catch {
            print("Failed with error: \(error)")
        }
    }
    
    func fetchDeparturees(stopRef: String) {
        Task {
            do {
                let result = try await asyncFunction(for: repository.fetchBusStopDepartures(stopRef: stopRef))
                if (result is ResultSuccess<NSArray>) {
                    let successResult = result as! ResultSuccess<NSArray>
                    self.depatures = successResult.data as! [GalwayBusDeparture]
                    print(self.depatures)
                } else {
                    print(result)
                }
            } catch {
                print("Failed with error: \(error)")
            }
        }
    }
    
    func toggleFavorite(stopRef: String) {
        repository.toggleFavorite(stopRef: stopRef)
    }
}


@MainActor
class BusRouteViewModel: ObservableObject {
    @Published private(set) var listRoutes: [BusRoute] = []
    
    
    private let repository: GalwayBusRepository
    init(repository: GalwayBusRepository) {
        self.repository = repository
    }
    
    func fetch() async {
        do {
            let busRoutes = try await asyncFunction(for: repository.fetchBusRoutes())
            self.listRoutes = busRoutes
        } catch {
            print("Failed with error: \(error)")
        }
    }
}

    

