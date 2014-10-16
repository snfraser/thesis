import ngat.astrometry.*;
import ngat.util.*;

import java.util.*;

/** Calculates the time above horizon for various input params over year.*/
public class UpTimes {

    public static void main(String args[]) {

	try {
	CommandTokenizer ct = new  CommandTokenizer("--");
	ct.parse(args);

	ConfigurationProperties config = ct.getMap();
	double lat = Math.toRadians(config.getDoubleValue("lat"));
	double lon = Math.toRadians(config.getDoubleValue("lon"));

	Position.setViewpoint(lat, lon);

	double dec0 = Math.toRadians(config.getDoubleValue("dec"));
	double dd   = Math.toRadians(config.getDoubleValue("delta"));
	int    nd   = config.getIntValue("number");


	Calendar cal = Calendar.getInstance();
	cal.set(Calendar.MONTH, 0);
	cal.set(Calendar.DATE, 1);
	cal.set(Calendar.HOUR, 0);
	cal.set(Calendar.MINUTE, 0);
	cal.set(Calendar.SECOND, 0);
	cal.set(Calendar.MILLISECOND, 0);
	long start = cal.getTime().getTime();

	long time = start;
	while (time < start +365*86400*1000L) {

	    System.err.printf("%1$tD %1$tT", new Date(time));

	    for (int i = 0; i < nd; i++) {
		
		double dec = dec0 + i*dd;
		Position target = new Position(0, dec);

		double rise = target.getRiseTime();
		double set  = target.getSetTime();
		double len  = rise-set;
		
		System.err.printf("%1$6.2f" , Math.toDegrees(len)/15.0); 
	    
	    } 

	    System.err.printf("\n");
	    
	    time += 86400*1000L;

	}

	} catch (Exception e) {
	    e.printStackTrace();
	    return;
	}
    }

}
