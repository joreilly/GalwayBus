import SwiftUI
import MapKit
import SharedCode

struct ContentView : View {
    @ObservedObject var busRouteViewModel: BusRouteViewModel
    @ObservedObject var busStopViewModel: BusStopViewModel
    @ObservedObject var nearbyStopsViewModel: NearbyStopsViewModel
    
    var body: some View {
        TabView {
            NearbyView(nearbyStopsViewModel: nearbyStopsViewModel)
                .tabItem {
                    VStack {
                        Image(systemName: "location")
                        Text("Nearby")
                    }
                }
            RouteListView(busRouteViewModel: busRouteViewModel)
                .tabItem {
                    VStack {
                        Image(systemName: "heart")
                        Text("Routes")
                    }
                }
        }
    }
}



extension BusStop: Identifiable { }

struct NearbyView : View {
    @ObservedObject var nearbyStopsViewModel: NearbyStopsViewModel
    @State var region = MKCoordinateRegion(center: .init(latitude: 0, longitude: 0),
                                           latitudinalMeters: 500, longitudinalMeters: 500)

    var body: some View {

        VStack {
            Map(coordinateRegion: $region,
                interactionModes: MapInteractionModes.all,
                showsUserLocation: true,
                annotationItems: nearbyStopsViewModel.listStops) { (busStop) -> MapPin in
                let coordinate = CLLocationCoordinate2D(latitude: busStop.latitude!.doubleValue, longitude: busStop.longitude!.doubleValue)
                    return MapPin(coordinate: coordinate)
            }
            
            
            List(nearbyStopsViewModel.listStops, id: \.stop_id) { busStop in
                BusStopView(busStop: busStop, nearbyStopsViewModel: nearbyStopsViewModel)
            }
            
            
        }
        .onAppear(perform: {
            region.center = CLLocationCoordinate2D(latitude: 53.2743394, longitude: -9.0514163)

            nearbyStopsViewModel.fetch()
        }).listStyle(PlainListStyle())
    }
}


struct BusStopView : View {
    var busStop: BusStop
    @ObservedObject var nearbyStopsViewModel: NearbyStopsViewModel
    
    let maroonColor = Color(red: 107.0/255.0, green: 13.0/255.0, blue: 14.0/255.0)
    
    var body: some View {
        HStack {
            Image("ic_bus").resizable().frame(width: 48, height: 48.0)
            VStack(alignment: .leading) {
                Text(busStop.longName).font(.headline)
                Text(busStop.stopRef).font(.subheadline)
            }
            Spacer()
            
            Button(action: {}, label: {
                (nearbyStopsViewModel.favorites.contains(busStop.stopRef) ?
                    Image(systemName: "heart.fill") :Image(systemName: "heart"))
                    .imageScale(.large).foregroundColor(maroonColor)
            }).onTapGesture {
                nearbyStopsViewModel.toggleFavorite(stopRef: busStop.stopRef)
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
        .navigationBarTitle(Text("Routes"), displayMode: .large)
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







