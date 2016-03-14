package module6;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import de.fhpotsdam.unfolding.UnfoldingMap;
import de.fhpotsdam.unfolding.data.PointFeature;
import de.fhpotsdam.unfolding.data.ShapeFeature;
import de.fhpotsdam.unfolding.marker.Marker;
import de.fhpotsdam.unfolding.marker.SimpleLinesMarker;
import de.fhpotsdam.unfolding.utils.MapUtils;
import de.fhpotsdam.unfolding.geo.Location;
import parsing.ParseFeed;
import processing.core.PApplet;

/** An applet that shows airports (and routes)
 * on a world map.  
 * @author Adam Setters and the UC San Diego Intermediate Software Development
 * MOOC team
 * @author Vu Nguyen
 * Date: Mar 14, 2016
 *
 */
public class AirportMap extends PApplet {
	
	UnfoldingMap map;
	private List<Marker> airportList;
	List<Marker> routeList;
	
	public HashMap<Integer, Location> airports;
	public HashMap<Integer, HashSet<Integer>> connectedTo;
	
	private AirportMarker lastSelected;
	private AirportMarker lastClicked;
	
	public void setup() {
		// setting up PAppler
		size(1200, 700, OPENGL);
		
		// setting up map and default events
		map = new UnfoldingMap(this, 200, 50, 900, 600);
		MapUtils.createDefaultEventDispatcher(this, map);
		
		// get features from airport data
		List<PointFeature> features = ParseFeed.parseAirports(this, "airports.dat");
		// get features from route data
		List<ShapeFeature> routes = ParseFeed.parseRoutes(this, "routes.dat");
		
		// list for markers, hashmap for quicker access when matching with routes
		airportList = new ArrayList<Marker>();
		airports = new HashMap<Integer, Location>();
		connectedTo = new HashMap<Integer, HashSet<Integer>>();
		
		// populate routes info
		for(ShapeFeature route : routes) {
			
			// get source and destination airportIds
			int source = Integer.parseInt((String)route.getProperty("source"));
			int dest = Integer.parseInt((String)route.getProperty("destination"));
			
			// get locations for airports on route
			HashSet<Integer> tos;
			if(!connectedTo.containsKey(source)) {
				tos = new HashSet<Integer>(dest);
				connectedTo.put(source, tos);
			} else {
				tos = connectedTo.get(source);
				tos.add(dest);
				connectedTo.put(source, tos);
			}
		}
		
		// create markers from features
		for(PointFeature feature : features) {
			if (connectedTo.containsKey(Integer.parseInt(feature.getId()))) {
				AirportMarker m = new AirportMarker(feature);
			
				//m.setRadius(5);
				m.setConnectedTo(connectedTo.get(Integer.parseInt(feature.getId())));
				airportList.add(m);
			
				// put airport in hashmap with OpenFlights unique id for key
				airports.put(Integer.parseInt(feature.getId()), feature.getLocation());
			}
		}
		
		routeList = new ArrayList<Marker>();
		for (ShapeFeature route : routes) {
			int source = Integer.parseInt((String)route.getProperty("source"));
			int dest = Integer.parseInt((String)route.getProperty("destination"));
			
			// get locations for airports on route
			if(airports.containsKey(source) && airports.containsKey(dest)) {
				route.addLocation(airports.get(source));
				route.addLocation(airports.get(dest));
			}
			
			SimpleLinesMarker sl = new SimpleLinesMarker(route.getLocations(), route.getProperties());
			sl.setHidden(true);
			routeList.add(sl);
		}
		
		map.addMarkers(routeList);
		map.addMarkers(airportList);
		
	}
	
	public void draw() {
		background(150);
		map.draw();
		addKey();
	}
	
	@Override
	public void mouseMoved()
	{
		// clear the last selection
		if (lastSelected != null) {
			lastSelected.setSelected(false);
			lastSelected = null;
		
		}
		selectMarkerIfHover(airportList);
	}
	
	// If there is a marker selected 
	private void selectMarkerIfHover(List<Marker> airports)
	{
		// Abort if there's already a marker selected
		if (lastSelected != null) {
			return;
		}
		
		for (Marker ap : airports) 
		{
			CommonMarker airport = (CommonMarker)ap;
			if (airport.isInside(map,  mouseX, mouseY)) {
				lastSelected = (AirportMarker)airport;
				airport.setSelected(true);
				return;
			}
		}
	}
	
	@Override
	public void mouseClicked()
	{
		lastClicked = null;
		hideMarkers();
		
		for (Marker marker : airportList) {
			if (marker.isInside(map, mouseX, mouseY)) {
				lastClicked = (AirportMarker)marker;

				HashSet<Integer> conn = connectedTo.get(lastClicked.airportId());
				lastClicked.setHidden(false);
				
				for (Marker mhide : airportList) {
					AirportMarker am = (AirportMarker)mhide;
					// is am connected to lastClicked?
									
					boolean isConnected = conn.contains(am.airportId());
					
					if (isConnected) {
						am.setHidden(false);
						SimpleLinesMarker lm = getRoute(lastClicked, am);
						if (lm != null) {
							lm.setHidden(false);
						}
					}
				}
			}
		}
		
		if (lastClicked == null) {
			unhideMarkers();
		}
	}
	
	// loop over and unhide all markers
	private void unhideMarkers() {
		for(Marker marker : airportList) {
			marker.setHidden(false);
		}
	}
	
	// loop over and hide all markers
	private void hideMarkers() {
		for(Marker marker : airportList) {
			marker.setHidden(true);
		}
		for(Marker route : routeList) {
			route.setHidden(true);
		}
	}
	
	public SimpleLinesMarker getRoute(Marker src, Marker des) {
		for (int i = 0; i < routeList.size(); i++) {
			SimpleLinesMarker lm = (SimpleLinesMarker)routeList.get(i);
			Location s = airports.get(((AirportMarker)src).airportId());
			Location d = airports.get(((AirportMarker)des).airportId());
			
			List<Location> locs = lm.getLocations();

			if (locs.size() > 1) {
				if ((s == locs.get(0) && d == locs.get(1)) ||
						s == locs.get(1) && d == locs.get(0)) {
					return lm;
				}
			}
		}
		return null;
	}
	
	private void addKey() {	
		// Remember you can use Processing's graphics methods here
		fill(255, 250, 240);
		
		int xbase = 25;
		int ybase = 50;
		
		rect(xbase, ybase, 175, 200);
		
		fill(0);
		textAlign(LEFT, CENTER);
		textSize(12);
		text("Airport Key", xbase+25, ybase+25);
		
		fill(150, 30, 30);
		/*int tri_xbase = xbase + 35;
		int tri_ybase = ybase + 50;
		triangle(tri_xbase, tri_ybase-CityMarker.TRI_SIZE, tri_xbase-CityMarker.TRI_SIZE, 
				tri_ybase+CityMarker.TRI_SIZE, tri_xbase+CityMarker.TRI_SIZE, 
				tri_ybase+CityMarker.TRI_SIZE);*/

		fill(0, 0, 0);
		textAlign(LEFT, CENTER);
//		text("Airport Marker", tri_xbase + 15, tri_ybase);
//		
//		text("Land Quake", xbase+50, ybase+70);
//		text("Ocean Quake", xbase+50, ybase+90);
		text("Size ~ Connectivity", xbase+25, ybase+50);
		text("Elevation (Sea level)", xbase+25, ybase+75);
		
		/*fill(255, 255, 255);
		ellipse(xbase+35, 
				ybase+70, 
				10, 
				10);
		rect(xbase+35-5, ybase+90-5, 10, 10);*/
		
		fill(color(255, 255, 0));
		ellipse(xbase+35, ybase+95, 12, 12);
		fill(color(0, 0, 255));
		ellipse(xbase+35, ybase+115, 12, 12);
		fill(color(255, 0, 0));
		ellipse(xbase+35, ybase+135, 12, 12);
		
		textAlign(LEFT, CENTER);
		fill(0, 0, 0);
		text("Up to 528ft", xbase+50, ybase+95);
		text("528ft to 5282ft", xbase+50, ybase+115);
		text("Over 5282ft", xbase+50, ybase+135);
		
		/*fill(255, 255, 255);
		int centerx = xbase+35;
		int centery = ybase+200;
		ellipse(centerx, centery, 12, 12);

		strokeWeight(2);
		line(centerx-8, centery-8, centerx+8, centery+8);
		line(centerx-8, centery+8, centerx+8, centery-8);*/
		
	}
}
