import SwiftUI
import Combine
import SharedCode



class BusRouteViewModel: BindableObject {
    var listRoutes: [BusRoute] = [] {
        didSet {
            didChange.send(self)
        }
    }
    
    var didChange = PassthroughSubject<BusRouteViewModel, Never>()
    
    let repository: GalwayBusRepository
    init(repository: GalwayBusRepository) {
        self.repository = repository
    }
    
    func fetch() {
        repository.fetchBusRoutes(success: { data in
            self.listRoutes = data
            return KotlinUnit()
        })
    }
}

struct ContentView : View {
    @EnvironmentObject var busRouteViewModel: BusRouteViewModel

    var body: some View {
        NavigationView {
            List(busRouteViewModel.listRoutes.identified(by: \.timetableId)) { route in
                RouteRow(route: route)
            }
            .navigationBarTitle(Text("Routes"), displayMode: .large)
            .onAppear() {
                self.busRouteViewModel.fetch()
            }
        }
    }
}


struct RouteRow : View {
    var route: BusRoute
    
    var body: some View {
        HStack {
            Image("ic_bus")
            
            VStack(alignment: .leading) {
                Text(route.timetableId).font(.headline)
                Text(route.longName).font(.subheadline)
            }
        }
    }
}


