import SwiftUI
import SharedCode
import MapKit

struct MapView: UIViewRepresentable {
    var busStops: [BusStop]
    
    func makeUIView(context: Context) -> MKMapView {
        MKMapView(frame: .zero)
    }
    
    func updateUIView(_ view: MKMapView, context: Context) {
        let coordinate = CLLocationCoordinate2D(
            latitude: 53.2743394, longitude: -9.0514163)
        let span = MKCoordinateSpan(latitudeDelta: 0.005, longitudeDelta: 0.005)
        let region = MKCoordinateRegion(center: coordinate, span: span)
        view.setRegion(region, animated: true)
        
        
        for stop in busStops {
            let annotation = MKPointAnnotation()
            let centerCoordinate = CLLocationCoordinate2D(latitude: stop.latitude!.doubleValue, longitude: stop.longitude!.doubleValue)
            annotation.coordinate = centerCoordinate
            annotation.title = stop.longName
            //print(String(stop.latitude!) + " " + String(stop.longitude!))
            view.addAnnotation(annotation)
        }
    }
}

