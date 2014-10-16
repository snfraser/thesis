import java.util.*;
import java.io.*;
import java.text.*;

import ngat.util.*;
import ngat.phase2.*;
import ngat.astrometry.*;

public class Demand {
    
    static  SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    static final SimpleTimeZone UTC = new SimpleTimeZone(0, "UTCS");

    ISite site;
    AstrometrySiteCalculator astro;

    long start;
    long end;

    Map gmap;

    public Demand(ConfigurationProperties cfg) throws Exception {
	
	LoadGroups loader = new LoadGroups();
	gmap = loader.load(new File(cfg.getProperty("groups")));
	
	double lat = Math.toRadians(cfg.getDoubleValue("latitude", 28.0));
	double lon = Math.toRadians(cfg.getDoubleValue("longitude",-17.0));
	
	site = new BasicSite("obs", lat, lon);
	astro = new BasicAstrometrySiteCalculator(site);
	
	start = sdf.parse(cfg.getProperty("start")).getTime();
	end   = sdf.parse(cfg.getProperty("end")).getTime();
	
    }

    private void run() throws Exception {

	int nm = (int)((double)(end - start)/60000.0) + 1;

	double dmd[]   = new double[nm]; 
	double rndmd[] = new double[nm]; 

	Iterator glist = gmap.values().iterator();
	while (glist.hasNext()) {
	    
	    Agroup group = (Agroup)glist.next();

	    // calculate total time available for this group (in night)
	    long tt = 0L;
	    long t = start;	    
	    while (t < end) {
		
		if (cando(group, t)) 
		    tt += 60000;  
		
		t += 60000L;
	    }
	    
	    // now work out how many attempts we might make
	    int na = 1;
	    switch (group.timing.type) {
	    case Timing.MON:
		na = (int)Math.floor((double)tt/(group.timing.period*3600000.0))+1;
		break;
	    case Timing.INT:
		na = (int)Math.floor((double)tt/(group.timing.period*3600000.0))+1;
		break;
	    default:
		na = 1;
		break;
	    }

	    double gdmd = group.xt/(group.xt + tt/(double)na);
	
	    System.err.printf("Group: %s rn= %2d xt= %3.2fm na= %2d dmd= %3.2f \n", group.name, group.rn, ((double)group.xt/60000), na, gdmd);
	 
	    for (int i = 0; i < nm ; i++) {
		long at = start + i*60000;
		if (cando(group, at)) {
		    dmd[i]   += gdmd;
		    rndmd[i] += gdmd/group.rn;
		}
		at += 60000;
	    }
	    
	} // next group
	
	
	for (int i = 0; i < nm ; i++) {
	    long at = start + i*60000;
	    System.err.printf("%tF %tT %4.2f %4.2f\n", at, at, dmd[i], rndmd[i]);
	}

    }
    
    public static void main(String args[]) {
	
	sdf.setTimeZone(UTC);

	try {
	    
	    ConfigurationProperties cfg = CommandTokenizer.use("--").parse(args);
	    
	    Demand c = new Demand(cfg);
	    
	    c.run();
	
	} catch (Exception e) {
	    e.printStackTrace();
	}

    }
    
    
    private boolean cando(Agroup group, long t) throws Exception {
	
	// sun is up
	SolarCalculator sunTrack = new SolarCalculator();
	Coordinates sun = sunTrack.getCoordinates(t);
	
	if (astro.getAltitude(sun, t) > 0.0)
	    return false;
	
	// bugger already done
	long last = group.last;
	Timing timing = group.timing;
	switch (timing.type) {
	case Timing.MON:
	    long p1 = (long)(0.25*timing.period*3600000.0);
	    if (t - last < p1)
		return false;
	case Timing.INT:
	    long p2 = (long)(timing.period*3600000.0);
	    if (t - last < p2)
		return false;
	case Timing.FLEX:
	    if (last > 0L)
		return false;
	}
	
	// target above horixon 0
	XExtraSolarTarget star = new XExtraSolarTarget(group.name);
	star.setRa(group.ra);
	star.setDec(group.dec);
	
	TargetTrackCalculator track = new BasicTargetCalculator(star, site);	
	Coordinates c = track.getCoordinates(t);
	double targetElev = astro.getAltitude(c, t);
	if (targetElev < 0.0)
	    return false;
	
	// moon bright but wants dark
	LunarCalculator moonTrack = new LunarCalculator(site);
	Coordinates moon = moonTrack.getCoordinates(t);
	
	if (astro.getAltitude(moon, t) > 0.0  && group.lunar == Agroup.DARK)
	    return false;
	
	// twilight but wants night
	if (astro.getAltitude(sun, t) > Math.toRadians(-18.0) && group.tod == Agroup.NIGHT)
	    return false;
	
	return true;
	
    }
    
}
