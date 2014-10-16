import ngat.phase2.*;
import ngat.astrometry.*;
import java.util.*;


public class Metrics {
   
    ISite site;
    AstrometrySiteCalculator astro;
    long sunset;
    long sunrise;

    long lon;

    /** Create a metrics for the site with specified time in night as midpoint.*/
    public Metrics(ISite site, long time) throws Exception {

	this.site = site;

	astro = new BasicAstrometrySiteCalculator(site);

	SolarCalculator sunTrack = new SolarCalculator();
	Coordinates sun = sunTrack.getCoordinates(time);
	sunset  = time - astro.getTimeSinceLastSet(sun, 0.0, time);
	sunrise = time + astro.getTimeUntilNextRise(sun, 0.0, time);
	lon = sunrise - sunset;

    }

    public double scoreElev(Agroup g, long time) throws Exception {

	TargetTrackCalculator track = getTrack(g, time);
	double maxel = astro.getMaximumAltitude(track, sunset, sunrise);
	Coordinates c = track.getCoordinates(time);
	double elev  = astro.getAltitude(c, time);

	return (elev/maxel);

    }  

    public double scorePriority(Agroup g, long time) throws Exception {
	
	double p = 0;
	switch (g.priority) {
	case -10:
	    p = 0;
	    break;
	case -1:
	    p = 3;
	    break;
	default:
	    p = g.priority;
	    break;
	}

	return p;

    } 

    public double scoreSeeMatch(Agroup g, long time) throws Exception {

	switch (g.seecat) {
	case Agroup.GOOD:
	    return 1.0;
	case Agroup.AVER:
	    return 0.5;
	case Agroup.POOR:
	    return 0.25;
	case Agroup.USAB:
	    return 0.1;
	}
	return 0.0;
    }  

    public double scoreLunMatch(Agroup g, long time) throws Exception {

	if (g.lunar == Agroup.DARK && dark(time))
	    return 1.0;

	if (g.lunar == Agroup.BRIGHT && dark(time))
	    return 0.5;

	// request bright and is bright
	return 1.0;

    }  


    public double scoreRN(Agroup g, long time) throws Exception{

	double rn = g.rn;
	if (rn == 0.0)
	    rn = 1.0;
	return 1.0/rn ;

    }  

    public double scoreTD(Agroup group, long time) throws Exception {

	// calculate demand for g...

	long tt = 0L;
	long t = sunset;	    
	while (t < sunrise) {
	    
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

	return gdmd;

    }


    private TargetTrackCalculator getTrack(Agroup g, long t) throws Exception {

	XExtraSolarTarget target = new XExtraSolarTarget("tgt");
	target.setRa(g.ra);
	target.setDec(g.dec);
	TargetTrackCalculator track = new BasicTargetCalculator(target, site);

	return track;
    }

    /** True if its moon-dark at t.*/	
    private boolean dark(long t) throws Exception {

	LunarCalculator moonTrack = new LunarCalculator(site);
	Coordinates moon = moonTrack.getCoordinates(t);

	if (astro.getAltitude(moon, t) < 0.0)
	    return true;

	return false;

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