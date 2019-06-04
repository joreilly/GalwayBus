import SwiftUI
import SharedCode


struct ContentView : View {
    var listRoutes: [BusRoute] = []

    var body: some View {
        NavigationView {
            List(listRoutes.identified(by: \.timetableId)) { route in
                RouteRow(route: route)
            }
            .navigationBarTitle(Text("Routes"), displayMode: .large)
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


