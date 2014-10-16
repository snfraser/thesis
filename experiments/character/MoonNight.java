import ngat.astrometry.*;
import ngat.util.*;
import java.util.*;
import java.text.*;

public class MoonNight {

    public static void main(String args[]) {

	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
	SimpleTimeZone UTC = new SimpleTimeZone(0,"UTC");
	sdf.setTimeZone(UTC);

	System.setProperty("astrometry.impl", "ngat.astrometry.TestCalculator");

	try {
	    ConfigurationProperties cfg = CommandTokenizer.use("--").parse(args);
	    Date d1 = cfg.getDateValue("year", "yyyy");

	    Calendar cal = Calendar.getInstance();
	    cal.setTime(d1);

	    System.err.println(d1.toGMTString());

	    double lat  = Math.toRadians(cfg.getDoubleValue("lat"));
	    double lon  = Math.toRadians(cfg.getDoubleValue("long"));
	    Site site = new Site("Test",lat,lon);

	    long t1 = cal.getTime().getTime();
	    t1 += 12*3600*1000L;

	    long t = t1;
	    while (t < t1 + 365*86400*1000L) {
		// new day at noon
		
		long st = t;
		double night  = 0.0;
		double lunight= 0.0;
		double anight = 0.0;
		double alunight= 0.0;
		while (st < t + 24*3600*1000L) {
		    Position sun    = Astrometry.getSolarPosition(st);
		    double   sunel  = sun.getAltitude(st, site);
		    Position moon   = Astrometry.getLunarPosition(st);
		    double   moonel = moon.getAltitude(st, site);
		    
		    if (sunel < 0.0) {
			night += 300000.0;
			if (moonel > 0.0)
			    lunight += 300000.0;
		    } 
		    if (sunel < Math.toRadians(-18.0)) {
			anight += 300000.0;
			if (moonel > 0.0)
			    alunight += 300000.0;
		    }
		    
		    st += 5*60*1000L; // 5 minutes
		}
		// date night_length moon_up_at_night
		System.err.println(sdf.format(new Date(t))+" "+
				   (night/3600000.0)+" "+
				   (lunight/3600000.0)+" "+
				   (anight/3600000.0)+" "+
				   (alunight/3600000.0));
		t += 24*3600*1000L;
	    }

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}
