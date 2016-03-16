package module6;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import de.fhpotsdam.unfolding.data.Feature;
import de.fhpotsdam.unfolding.data.PointFeature;
import processing.core.PConstants;
import processing.core.PGraphics;

/** 
 * A class to represent AirportMarkers on a world map.
 *   
 * @author Adam Setters and the UC San Diego Intermediate Software Development
 * MOOC team
 * @author Vu Nguyen
 * Date: Mar 14, 2016
 *       Mar 16, 2016
 */
public class AirportMarker extends CommonMarker {
	public static final float ALTITUDE_HIGH = 528; // 0.1
	public static final float ALTITUDE_EXTREME = 5282; // 1 mile
	
	//public static List<SimpleLinesMarker> routes;
	private HashSet<Integer> connectedTo;
	private String oneHop;
	private String twoHops;
	
	public AirportMarker(Feature city) {
		super(((PointFeature)city).getLocation(), city.getProperties());
		
		HashMap<String, Object> properties = city.getProperties();
		properties.put("id", city.getId());
		setProperties(properties);
	}
	

	@Override
	public void drawMarker(PGraphics pg, float x, float y) {
		colorDetermine(pg);
		float size = sizeDetermine();
		pg.ellipse(x, y, size, size);
	}

	@Override
	public void showTitle(PGraphics pg, float x, float y) {
		String title = toString();
		pg.pushStyle();
		
		pg.rectMode(PConstants.CORNER);
		
		pg.stroke(110);
		pg.fill(255,255,255);
		pg.rect(x, y + 15, pg.textWidth(title) + 6, 50, 5);
		
		pg.textAlign(PConstants.LEFT, PConstants.TOP);
		pg.fill(0);
		pg.text(title, x + 3 , y +18);
		
		pg.popStyle();
	}
	
	@Override
	public void showInfo(PGraphics pg, float x, float y) {
		float nx = 50;
		float ny = 50;
		String title = getInfo();
		pg.pushStyle();
		
		pg.rectMode(PConstants.CORNER);
		
		pg.stroke(110);
		pg.fill(255,255,255);
		pg.rect(nx, ny + 15, pg.textWidth(title) + 6, 50, 5);
		
		pg.textAlign(PConstants.LEFT, PConstants.TOP);
		pg.fill(0);
		pg.text(title, nx + 3 , ny +18);
		
		pg.popStyle();
	}
	
	private void colorDetermine(PGraphics pg) {
		float alt = getAltitude();
		
		if (alt < ALTITUDE_HIGH) {
			pg.fill(255, 255, 0);
		}
		else if (alt < ALTITUDE_EXTREME) {
			pg.fill(0, 0, 255);
		}
		else {
			pg.fill(255, 0, 0);
		}
	}
	
	private float sizeDetermine() {
		int size = connectedTo.size();  
		if (size < 25) {
			return 4;
		} else if (size < 150) {
			return 8;
		} else {
			return 10 + size/25;
		}
	}
	
	public void setOneHop(int oH, int total) {
		this.oneHop = Double.toString(Math.round((double)oH/total * 100)) + "%";
	}
	
	public void setTwoHops(int tH, int total) {
		this.twoHops = Double.toString(Math.round((double)tH/total * 100)) + "%";;
	}
	
	public String getOneHop() {
		return this.oneHop;
	}
	
	public String getTwoHop() {
		return this.twoHops;
	}
	
	public String toString()
	{
		String title = getCode() + " - " + getName() + 
						"\nis connected to " + getCoverage() + 
						" other airports.\nElevation: " + getAltitude() + "ft."; 
		return title.replace("\"", "");
	}
	
	public String getInfo() {
		String title = getCode() + " - " + getName() +
						"\nOne-Hop coverage: " + this.oneHop +
						"\nTwo-Hop coverage: " + this.twoHops;
		
		return title.replace("\"", "");
	}
	
	public String getCode() {
		return (String) getProperty("code");
	}
	
	public String getName() {
		return (String) getProperty("name");	
	}
	
	public String getCity() {
		return (String) getProperty("city");	
		
	}
	
	public String getCountry() {
		return (String) getProperty("country");	
		
	}
	
	public float getAltitude() {
		return Float.parseFloat(getProperty("altitude").toString());
	}
	
	public String getCoverage() {
		if (connectedTo == null) {
			return "NA";
		}
		return Integer.toString(connectedTo.size());
	}
	
	public void setConnectedTo(HashSet<Integer> ct) {
		this.connectedTo = ct;
	}
	
	public int airportId() {
		return Integer.parseInt((String) this.getProperty("id"));
	}
}
