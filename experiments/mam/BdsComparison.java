import java.util.*;
import java.io.*;
import java.text.*;

import ngat.util.*;
import ngat.phase2.*;
import ngat.astrometry.*;

/** A BDS scheduler.*/
public class BdsComparison {
    
    static  SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    static final SimpleTimeZone UTC = new SimpleTimeZone(0, "UTCS");

    ISite site;
    AstrometrySiteCalculator astro;
    Metrics metrics;

    LoadGroups loader;
    File mapfile;
    Map gmap;

    long start;
    long end;

    int nrun;

    double wel = 0.0;
    double wpr = 0.0;
    double wrn = 0.0;
    
    public BdsComparison(ConfigurationProperties cfg) throws Exception {
	
	sdf.setTimeZone(UTC);

	loader = new LoadGroups();
	mapfile = new File(cfg.getProperty("groups"));
	
	double lat = Math.toRadians(cfg.getDoubleValue("latitude", 28.0));
	double lon = Math.toRadians(cfg.getDoubleValue("longitude",-17.0));

	site = new BasicSite("obs", lat, lon);
	astro = new BasicAstrometrySiteCalculator(site);

	//start = sdf.parse(cfg.getProperty("start")).getTime();
	//end   = sdf.parse(cfg.getProperty("end")).getTime();

	start = sdf.parse("2007-11-13 19:00").getTime();
	end   = sdf.parse("2007-11-14 07:00").getTime();
	
	long tmid = (start + end)/2;

	metrics = new Metrics(site, tmid);

	nrun = cfg.getIntValue("runs", 1000);

    }
    
    private void run() throws Exception {

	// each run we step the value of wel up and the value of pr down

	for (int k = 0; k < 50; k++) {

	    wel = (double)k/50.0;
	    wpr = 1.0 - wel;

	    // statistical gathering	
	    PopulationStatistics ss_el = new PopulationStatistics();
	    PopulationStatistics ss_pr = new PopulationStatistics();
	    PopulationStatistics ss_sm = new PopulationStatistics();
	    PopulationStatistics ss_lm = new PopulationStatistics();
	    PopulationStatistics ss_rn = new PopulationStatistics();
	    PopulationStatistics ss_td = new PopulationStatistics();
	    PopulationStatistics ss_xt = new PopulationStatistics();


	for (int irun = 0; irun < nrun; irun++) {

	    gmap = loader.load(mapfile);
	    
	    // initialize score tallies
	    long xtot = 0L;
	    	    
	    double sig_el = 0.0;
	    double sig_pr = 0.0;
	    double sig_sm = 0.0;
	    double sig_lm = 0.0;
	    double sig_rn = 0.0;
	    double sig_td = 0.0;
	    
	    double lon = (double)(end - start);

	    // start at sunset then pick best group and jump forwards
	    long t = start;
	    
	    while (t < end) {
		
		Agroup group = selectGroup(t);
		
		if (group == null) {
		    // 1 minute idle as no group available
		    t += 60000L;
		} else {

		    // work out tallies
		 
		    XExtraSolarTarget star = new XExtraSolarTarget(group.name);
		    star.setRa(group.ra);
		    star.setDec(group.dec);

		    TargetTrackCalculator track = new BasicTargetCalculator(star, site);	
		    Coordinates c = track.getCoordinates(t);
		    double targetElev = astro.getAltitude(c, t);
		    double maxel = astro.getMaximumAltitude(track, start, end);
		    double f_el = metrics.scoreElev(group, t);
		    double f_pr = metrics.scorePriority(group, t);
		    double f_sm = metrics.scoreSeeMatch(group, t);
		    double f_lm = metrics.scoreLunMatch(group, t);
		    double f_rn = metrics.scoreRN(group, t);
		    //double f_td = metrics.scoreTD(group, t);
		    double f_td = 0.0;

		    // System.err.printf("%s %tF %tT -> %tT %3.2fm %3.2f %3.2f :: %4.4f %4.4f %4.4f %4.4f %4.4f %4.4f \n", 
		    //      group.id, t, t, t + group.xt, 
		    //      ((double)group.xt/60000.0),
		    //      Math.toDegrees(targetElev),  Math.toDegrees(maxel),
		    //      f_el, f_pr, f_sm, f_lm, f_rn, f_td);

		    double gxtl = (double)group.xt/lon;
		    sig_el += f_el*gxtl;
		    sig_pr += f_pr*gxtl;
		    sig_sm += f_sm*gxtl;
		    sig_lm += f_lm*gxtl;
		    sig_rn += f_rn*gxtl;
		    sig_td += f_td*gxtl;
		    xtot   += group.xt;

		    // add exec time of selected group
		    group.last = t;

		    // random variation of XT
		    long xt = (long)((double)group.xt*(1.0 + (Math.random()-0.5)*0.1));
		    t += xt;
		}
		
	    }

	    //System.err.println("LON: "+((double)lon/3600000.0)+"h, Total exec time: "+((double)xtot/3600000.0)+"h Frac: "+((double)xtot/(double)lon));	    
	    // System.err.printf("Run: %4d  %6.4f %6.4f %6.4f %6.4f %6.4f %6.4f \n", irun, sig_el, sig_pr, sig_sm, sig_lm, sig_rn, sig_td);

	    ss_el.addSample(sig_el);
	    ss_pr.addSample(sig_pr);
	    ss_sm.addSample(sig_sm);
	    ss_lm.addSample(sig_lm);
	    ss_rn.addSample(sig_rn);
	    ss_td.addSample(sig_td);
	    ss_xt.addSample((double)xtot/lon);
	
	} //next run

	// print stats results Cat Avg Max Min Std
	System.err.printf("%4.2f %4.2f %4.2f EL: Avg: %6.4f Max: %6.4f Min: %6.4f Std: %6.4f\n", wel,wpr,wrn, ss_el.getAverage(), ss_el.getMaximum(), ss_el.getMinimum(), ss_el.getStandardDeviation());
	System.err.printf("%4.2f %4.2f %4.2f PR: Avg: %6.4f Max: %6.4f Min: %6.4f Std: %6.4f\n", wel,wpr,wrn, ss_pr.getAverage(), ss_pr.getMaximum(), ss_pr.getMinimum(), ss_pr.getStandardDeviation());
	System.err.printf("%4.2f %4.2f %4.2f SM: Avg: %6.4f Max: %6.4f Min: %6.4f Std: %6.4f\n", wel,wpr,wrn, ss_sm.getAverage(), ss_sm.getMaximum(), ss_sm.getMinimum(), ss_sm.getStandardDeviation());
	System.err.printf("%4.2f %4.2f %4.2f LM: Avg: %6.4f Max: %6.4f Min: %6.4f Std: %6.4f\n", wel,wpr,wrn, ss_lm.getAverage(), ss_lm.getMaximum(), ss_lm.getMinimum(), ss_lm.getStandardDeviation());
	System.err.printf("%4.2f %4.2f %4.2f RN: Avg: %6.4f Max: %6.4f Min: %6.4f Std: %6.4f\n", wel,wpr,wrn, ss_rn.getAverage(), ss_rn.getMaximum(), ss_rn.getMinimum(), ss_rn.getStandardDeviation());
	System.err.printf("%4.2f %4.2f %4.2f TD: Avg: %6.4f Max: %6.4f Min: %6.4f Std: %6.4f\n", wel,wpr,wrn, ss_td.getAverage(), ss_td.getMaximum(), ss_td.getMinimum(), ss_td.getStandardDeviation());
	System.err.printf("%4.2f %4.2f %4.2f XT: Avg: %6.4f Max: %6.4f Min: %6.4f Std: %6.4f\n", wel,wpr,wrn, ss_xt.getAverage(), ss_xt.getMaximum(), ss_xt.getMinimum(), ss_xt.getStandardDeviation());


	} // next k

    }

    private Agroup selectGroup(long t) throws Exception {

	List candidates = new Vector();
	List bg = new Vector();

	Iterator glist = gmap.values().iterator();
	while (glist.hasNext()) {

	    Agroup group = (Agroup)glist.next();
	    if (cando(group, t)) {
		if (group.priority == -10)
		    bg.add(group);
		else
		    candidates.add(group);
	    }
	}

	//System.err.printf("%tF %tT Found %4d candidates and %4d\n", t, t, candidates.size(), bg.size());

	if (candidates.size() != 0) {
	    
	    double bestScore = -999.99;
	    Agroup bestGroup = null;
	    
	    Iterator clist = candidates.iterator();
	    while (clist.hasNext()) {
		
		Agroup group = (Agroup)clist.next();
		double score = score(group, t);
			
		if (score > bestScore) {
		    bestScore = score;
		    bestGroup = group;
		}
		
	    }
	    return bestGroup;
	}

	if (bg.size() != 0) {
	    
	    double bestScore = -999.99;
	    Agroup bestGroup = null;
	    
	    Iterator clist = bg.iterator();
	    while (clist.hasNext()) {
		
		Agroup group = (Agroup)clist.next();
		double score = score(group, t);
		
		if (score > bestScore) {
		    bestScore = score;
		    bestGroup = group;
		}
		
	    }
	    return bestGroup;
	}

	return null;

	// 	int in = (int)Math.floor(Math.random()*(double)candidates.size());
	// 	bestGroup = (Agroup)candidates.get(in);

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
    

    private double score(Agroup group, long t) throws Exception {
	
	return wel*metrics.scoreElev(group, t)+
	    wpr*metrics.scorePriority(group,t)+
	    wrn*metrics.scoreRN(group,t);
	
	//return metrics.scorePriority(group,t);
	//return metrics.scoreSeeMatch(group, t);
	//return metrics.scoreLunMatch(group,t);
	//return metrics.scoreRN(group,t);
	//return metrics.scoreTD(group,t);

    }


    public static void main(String args[]) {

	try {

	    ConfigurationProperties cfg = CommandTokenizer.use("--").parse(args);

	    BdsComparison bds = new BdsComparison(cfg);

	    bds.run();

	    // results


	} catch (Exception e) {
	    e.printStackTrace();
	}

    }


}