import Combine
import SharedCode
import KMPNativeCoroutinesAsync



@MainActor
class NearbyStopsViewModel: ObservableObject {
    @Published private(set) var listStops: [BusStop] = []
    @Published public var favorites: Set<String> = []
    
    
    private let repository: GalwayBusRepository
    init(repository: GalwayBusRepository) {
        self.repository = repository
                
        Task {
            do {
                let stream = asyncStream(for: repository.favoritesNative)
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
            let result = try await asyncFunction(for: repository.fetchNearestStopsNative(latitude: 53.2743394, longitude: -9.0514163))
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
    
    func toggleFavorite(stopRef: String) {
        repository.toggleFavorite(stopRef: stopRef)
    }
}


class BusRouteViewModel: ObservableObject {
    @Published private(set) var listRoutes: [BusRoute] = []
    
    
    private let repository: GalwayBusRepository
    init(repository: GalwayBusRepository) {
        self.repository = repository
    }
    
    func fetch() async {
        do {
            let busRoutes = try await asyncFunction(for: repository.fetchBusRoutesNative())
            self.listRoutes = busRoutes
        } catch {
            print("Failed with error: \(error)")
        }
    }
}

    

