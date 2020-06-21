import SwiftUI
import MapKit
import SharedCode

struct MapView {
    var busList: [Bus]
    
    func makeMapView() -> MKMapView {
        MKMapView(frame: .zero)
    }
    
    func updateMapView(_ view: MKMapView, context: Context) {
        let coordinate = CLLocationCoordinate2D(latitude: 53.2743394, longitude: -9.0514163)

        let span = MKCoordinateSpan(latitudeDelta: 0.04, longitudeDelta: 0.04)
        let region = MKCoordinateRegion(center: coordinate, span: span)
        view.showsZoomControls = true
        view.setRegion(region, animated: false)
        
        view.removeAnnotations(view.annotations)
        
        for bus in busList {
            let annotation = MKPointAnnotation()

            let centerCoordinate = CLLocationCoordinate2D(latitude: bus.latitude, longitude: bus.longitude)
            annotation.coordinate = centerCoordinate
            annotation.title = bus.vehicle_id
            view.addAnnotation(annotation)
        }
    }
    
}

#if os(macOS)

extension MapView: NSViewRepresentable {
    func makeNSView(context: Context) -> MKMapView {
        makeMapView()
    }
    
    func updateNSView(_ nsView: MKMapView, context: Context) {
        updateMapView(nsView, context: context)
    }
}

#else

extension MapView: UIViewRepresentable {
    func makeUIView(context: Context) -> MKMapView {
        makeMapView()
    }
    
    func updateUIView(_ uiView: MKMapView, context: Context) {
        updateMapView(uiView, context: context)
    }
}

#endif

