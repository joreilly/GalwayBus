import SwiftUI
import SharedCode

struct ContentView : View {
    @ObservedObject var busRouteViewModel: BusRouteViewModel
    @ObservedObject var busStopViewModel: BusStopViewModel
    
    var body: some View {
        TabView {
            RouteListView(busRouteViewModel: busRouteViewModel)
                .tabItem {
                    VStack {
                        Text("Routes")
                    }
                }
            BusStopListView(busStopViewModel: busStopViewModel)
                .tabItem {
                    VStack {
                        Text("Stops")
                    }
                }
        }
    }
}


struct RouteListView : View {
    @ObservedObject var busRouteViewModel: BusRouteViewModel
    
    var body: some View {
        
        List(busRouteViewModel.listRoutes, id: \.timetableId) { route in
            RouteRow(route: route)
        }
        .onAppear(perform: busRouteViewModel.fetch)
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

struct BusStopListView : View {
    @ObservedObject var busStopViewModel: BusStopViewModel
    
    var body: some View {
        
        List(busStopViewModel.listStops, id: \.stop_id) { busStop in
            BusStopRow(busStop: busStop)
        }
        .onAppear(perform: busStopViewModel.fetch)
    }
}


struct BusStopRow : View {
    var busStop: BusStop
    
    var body: some View {
        HStack {
            VStack(alignment: .leading) {
                Text(busStop.shortName).font(.headline)
                Text(busStop.irishShortName).font(.subheadline)
            }
        }
    }
}







