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
            MapContainer(busStopViewModel: busStopViewModel, busRouteViewModel: busRouteViewModel)
                .tabItem {
                    VStack {
                        Text("Map")
                    }
                }
        }
    }
}


struct MapContainer: View {
    @ObservedObject var busStopViewModel: BusStopViewModel
    @ObservedObject var busRouteViewModel: BusRouteViewModel
    
    let timer = Timer.publish(every: 10, on: .main, in: .common).autoconnect()
    

    var body: some View {
        NavigationView {
            List(busRouteViewModel.listRoutes, id: \.timetableId) { route in
                RouteRow(route: route)
            }
            .frame(width: 400)
            .listStyle(SidebarListStyle())
            .onReceive(timer) { _ in
                self.busStopViewModel.fetchBusList()
            }
            .onAppear(perform: {
                self.busRouteViewModel.fetch()
                self.busStopViewModel.fetchBusList()
            })
            
            MapView(busList: busStopViewModel.busList)
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
            Image("ic_bus").resizable().frame(width: 50.0, height: 50.0)
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
                Text(busStop.longName).font(.headline)
            }
        }
    }
}







