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
                        Image(systemName: "1.circle")
                        Text("Routes")
                    }
                }
            BusStopListView(busStopViewModel: busStopViewModel)
                .tabItem {
                    VStack {
                        Image(systemName: "2.circle")
                        Text("Stops")
                    }
                }
            MapContainer(busStopViewModel: busStopViewModel)
                .tabItem {
                    VStack {
                        Image(systemName: "3.circle")
                        Text("Maps")
                    }
                }
                .onAppear {
                    self.busStopViewModel.fetch()
                }
        }
    }
}



struct MapContainer: View {
    @ObservedObject var busStopViewModel: BusStopViewModel

    var body: some View {
        MapView(busStops: busStopViewModel.listStops)
    }
}

struct RouteListView : View {
    @ObservedObject var busRouteViewModel: BusRouteViewModel
    
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
                Text(route.longName).font(.subheadline)
            }
        }
    }
}

struct BusStopListView : View {
    @ObservedObject var busStopViewModel: BusStopViewModel
    
    var body: some View {
        
        NavigationView {
            List(busStopViewModel.listStops, id: \.stop_id) { busStop in
                BusStopRow(busStop: busStop)
            }
            .navigationBarTitle(Text("Stops"), displayMode: .large)
            .onAppear(perform: busStopViewModel.fetch)
        }
    }
}


struct BusStopRow : View {
    var busStop: BusStop
    
    var body: some View {
        HStack {
            VStack(alignment: .leading) {
                Text(busStop.longName).font(.headline)
            }
        }
    }
}







