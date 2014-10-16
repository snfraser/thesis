import ngat.astrometry.*;
import ngat.util.*;

import java.util.*;

/** Calculates the nightlength for various input params over year.*/
public class NightLength {

    public static void main(String args[]) {

	try {
	CommandTokenizer ct = new  CommandTokenizer("--");
	ct.parse(args);

	ConfigurationProperties config = ct.getMap();
	double lat = Math.toRadians(config.getDoubleValue("lat"));
	double lon = Math.toRadians(config.getDoubleValue("lon"));

	Position.setViewpoint(lat, lon);

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
	    
	    Position sun = Astrometry.getSolarPosition(time);

	    double rise = sun.getRiseTime();
	    double set  = sun.getSetTime();
	    double len = rise-set;

	    System.err.printf("%1$tD %1$tT %2$6.2f \n" ,
			      new Date(time),
			      Math.toDegrees(len)/15.0); 

	    time += 86400*1000L;

	}

	} catch (Exception e) {
	    e.printStackTrace();
	    return;
	}
    }

}
