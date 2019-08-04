import SwiftUI
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



struct ContentView : View {
    @EnvironmentObject var busRouteViewModel: BusRouteViewModel
    
    var body: some View {
        NavigationView {
            List(busRouteViewModel.listRoutes, id: \.timetableId) { route in
                RouteRow(route: route)
            }
            .navigationBarTitle(Text("Routes"), displayMode: .large)
            .onAppear(perform: busRouteViewModel.fetch)
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
                Text(route.longName).font(.subheadline)            }
        }
    }
}




