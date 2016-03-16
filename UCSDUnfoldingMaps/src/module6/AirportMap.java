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
 * 		 Mar 16, 2016
 *
 */
public class AirportMap extends PApplet {
	
	UnfoldingMap map;
	private List<Marker> airportList;
	private HashMap<Integer, Marker> airportHashMap;
	private List<Marker> routeList;
	private HashMap<String, Marker> routeHashMap;
	
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
		airportHashMap = new HashMap<Integer, Marker>();
		routeList = new ArrayList<Marker>();
		routeHashMap = new HashMap<String, Marker>();
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
				airportHashMap.put(Integer.parseInt(feature.getId()), m);
				
				// put airport in hashmap with OpenFlights unique id for key
				airports.put(Integer.parseInt(feature.getId()), feature.getLocation());
			}
		}
		
		
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
			routeHashMap.put((String)route.getProperty("source") + "-" + (String)route.getProperty("destination"), sl);
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
		
		Marker hitMarker = map.getFirstHitMarker(mouseX, mouseY);
	    if (hitMarker != null && hitMarker instanceof AirportMarker) {
	        // Select current marker
	    	lastSelected = (AirportMarker)hitMarker;
	        lastSelected.setSelected(true);
	    }
		
	    return;
	}
	
	@Override
	public void mouseClicked()
	{
		if (lastClicked != null) {
			lastClicked.setClicked(false);
			lastClicked = null;
		}
		
		hideMarkers();
		
		Marker hitMarker = map.getFirstHitMarker(mouseX, mouseY);
	    if (hitMarker != null && hitMarker instanceof AirportMarker) {
	        // Select current marker
	    	lastClicked = (AirportMarker)hitMarker;
	    	lastClicked.setHidden(false);
	    	lastClicked.setClicked(true);
	        
	    	HashSet<Integer> conn = connectedTo.get(lastClicked.airportId());
	    	
	    	if (conn.size() == 0) return;
			if (lastClicked.getOneHop() == null) {
				lastClicked.setOneHop(conn.size(), airports.keySet().size());
			}
			HashSet<Integer> connections = new HashSet<Integer>(conn);
			
	    	for (Integer c : conn) {
	    		AirportMarker am = (AirportMarker)airportHashMap.get(c);
	    		if (am == null) continue;
	    		am.setColor(50);
	    		am.setHidden(false); // set color?
	    		
	    		SimpleLinesMarker lm = getRoute(Integer.toString(lastClicked.airportId()), Integer.toString(am.airportId()));
				if (lm != null) {
					lm.setColor(100);
					lm.setHidden(false);
				}
				
				HashSet<Integer> connSub = connectedTo.get(am.airportId());
				
				if (connSub.size() > 0) {				
					for (Integer cs : connSub) {
						AirportMarker amSub = (AirportMarker)airportHashMap.get(cs);
						if (amSub == null) continue;
						
						connections.add(cs);
						amSub.setHighlightColor(150);
						amSub.setHidden(false); // set color?

					}
				}
	    	}
	    	
	    	if (lastClicked.getTwoHop() == null) {
				lastClicked.setTwoHops(connections.size(), airports.size());
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
		for(Marker m : map.getMarkers()) {
			m.setHidden(true);
		}
	}
	
	public SimpleLinesMarker getRoute(String src, String des) {
		return (SimpleLinesMarker)routeHashMap.get(src + "-" + des);
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

		fill(0, 0, 0);
		textAlign(LEFT, CENTER);

		text("Size ~ Connectivity", xbase+25, ybase+50);
		text("Elevation (Sea level)", xbase+25, ybase+75);
		
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
				
	}
}
