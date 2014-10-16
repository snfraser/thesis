import java.util.*;
import java.io.*;
import java.text.*;

import ngat.util.*;
import ngat.phase2.*;
import ngat.astrometry.*;

/** A LAS scheduler.*/
public class LasScheduler {
    
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

    long horizon;

    int nn; // number of sequences to generate

    double overunfrac;

    long hoveruntime;

    double bgthresh;

    public LasScheduler(ConfigurationProperties cfg) throws Exception {
	
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

	// number of seq gens
	nn = cfg.getIntValue("nn", 100);

	// horizon in hours
	horizon = (long)(3600.0*1000.0*cfg.getDoubleValue("horizon"));

	// fraction probablity of BGs being used if no PRIMs
	double bgfrac = cfg.getDoubleValue("bgfrac", 5.0);
	bgthresh = 1.0/bgfrac;

	// minimum allowable frac of group within horizon for overrun.
	overunfrac = cfg.getDoubleValue("overfrac", 0.8);

	// maximum overun fraction of horizon
	double hoverunfrac =  cfg.getDoubleValue("hfrac", 0.1);
	hoveruntime = (long)(hoverunfrac*(double)horizon);

    }
    
    private void run() throws Exception {

	// statistical gathering	
	PopulationStatistics ss_el = new PopulationStatistics();
	PopulationStatistics ss_pr = new PopulationStatistics();
	PopulationStatistics ss_sm = new PopulationStatistics();
	PopulationStatistics ss_lm = new PopulationStatistics();
	PopulationStatistics ss_rn = new PopulationStatistics();
	PopulationStatistics ss_td = new PopulationStatistics();
	PopulationStatistics ss_xt = new PopulationStatistics();

	for (int irun = 0; irun < nrun; irun++) {

	    List runlist = new Vector();

	    gmap = loader.load(mapfile);

	    // initialize score tallies
	    long xtot = 0L;
	    	    
	    double sig_el = 0.0;
	    double sig_pr = 0.0;
	    double sig_sm = 0.0;
	    double sig_lm = 0.0;
	    double sig_rn = 0.0;
	    double sig_td = 0.0;
	    
	    long lon = end - start;

	    // start at sunset then pick best group sequence and jump forwards
	    long t = start;
	    
	    while (t < end) {
		
		// select best sequence from t to t+h.
		List seq = bestSequence(t, t + horizon);

		// prevents manic behaviour if we run out of groups at EON
		if (seq.size() == 0 && (end - t < horizon)) {
		    System.err.printf("Run %4d, NO_SEQ_FRAG at %tT %tF until EON \n", irun, t, t);
		    t = end;
		    continue;
		} 

		System.err.printf("Run %4d, SEQ_FRAG for %tT %tF +(%4d)m : [", irun, t, t, (horizon/60000)); 

		long rxt = 0L;				
		for (int i = 0; i < seq.size(); i++) {
		    Agroup group = (Agroup)seq.get(i);
		    System.err.printf("%s, ", group.name);
		    rxt += group.xt;
		}

		System.err.printf("] RT = %4dm \n",(rxt/60000));

		// step thro sequence...
		Iterator is = seq.iterator();
		while (is.hasNext()) {

		    Agroup group = (Agroup)is.next();
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
		    //		    double f_td = metrics.scoreTD(group, t);
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

		    // history update
		    group.last = t;

		    // add group to runlist
		    runlist.add(group);

		    // random variation of XT
		    long xt = (long)((double)group.xt*(1.0 + (Math.random()-0.5)*0.1));
		    t += xt;
		}
		
	    }

	    //System.err.println("LON: "+((double)lon/3600000.0)+"h, Total exec time: "+((double)xtot/3600000.0)+"h Frac: "+((double)xtot/(double)lon));	
	    double xtf = (double)xtot/(double)lon;
	    System.err.printf("Run: %4d, SCORES  %6.4f %6.4f %6.4f %6.4f %6.4f %6.4f %6.4f\n", irun, sig_el, sig_pr, sig_sm, sig_lm, sig_rn, sig_td, xtf);

	    ss_el.addSample(sig_el);
	    ss_pr.addSample(sig_pr);
	    ss_sm.addSample(sig_sm);
	    ss_lm.addSample(sig_lm);
	    ss_rn.addSample(sig_rn);
	    ss_td.addSample(sig_td);
	    ss_xt.addSample(xtf);
	
	    // print runlist
	    System.err.printf("Run %4d, LIST \n", irun);
	    Iterator irl = runlist.iterator();
	    while (irl.hasNext()) {
		Agroup g = (Agroup)irl.next();
		System.err.println(":"+g);
	    }


	} //next run

	// print stats results Cat Avg Max Min Std
	System.err.printf("EL: Avg: %6.4f Max: %6.4f Min: %6.4f Std: %6.4f\n", ss_el.getAverage(), ss_el.getMaximum(), ss_el.getMinimum(), ss_el.getStandardDeviation());
	System.err.printf("PR: Avg: %6.4f Max: %6.4f Min: %6.4f Std: %6.4f\n", ss_pr.getAverage(), ss_pr.getMaximum(), ss_pr.getMinimum(), ss_pr.getStandardDeviation());
	System.err.printf("SM: Avg: %6.4f Max: %6.4f Min: %6.4f Std: %6.4f\n", ss_sm.getAverage(), ss_sm.getMaximum(), ss_sm.getMinimum(), ss_sm.getStandardDeviation());
	System.err.printf("LM: Avg: %6.4f Max: %6.4f Min: %6.4f Std: %6.4f\n", ss_lm.getAverage(), ss_lm.getMaximum(), ss_lm.getMinimum(), ss_lm.getStandardDeviation());
	System.err.printf("RN: Avg: %6.4f Max: %6.4f Min: %6.4f Std: %6.4f\n", ss_rn.getAverage(), ss_rn.getMaximum(), ss_rn.getMinimum(), ss_rn.getStandardDeviation());
	System.err.printf("TD: Avg: %6.4f Max: %6.4f Min: %6.4f Std: %6.4f\n", ss_td.getAverage(), ss_td.getMaximum(), ss_td.getMinimum(), ss_td.getStandardDeviation());
	System.err.printf("XT: Avg: %6.4f Max: %6.4f Min: %6.4f Std: %6.4f\n", ss_xt.getAverage(), ss_xt.getMaximum(), ss_xt.getMinimum(), ss_xt.getStandardDeviation());

    }

    private List bestSequence(long t1, long t2) throws Exception {
	
	double bestScore = -999.99;
	List bestSeq = null;
	
	// create a bunch of sequences and see which is best
	for (int i = 0; i < nn; i++) {

	    List seq = new Vector();
	    double score = createSequence(t1, t2, seq);
	    
	    if (score > bestScore) {
		bestSeq = seq;
	    }
	}

	return bestSeq;
    }

    private double createSequence(long t1, long t2, List seq) throws Exception {

	// first save the group histories as we will need to restore them
	List hlist = new Vector();

	Iterator glist = gmap.values().iterator();
	while (glist.hasNext()) {
	    Agroup g = (Agroup)glist.next();
	    History h = new History(g);
	    hlist.add(h);
	}
	
	// start at t1, pick a random group from gmap which is feasible,
	// score it and add to tally and list, incr time by xt
	double tally = 0.0;
	long t = t1;	
	while (t < t2) {
	
	    // at this step build a list of candidates
	    List candidates = new Vector();
	    List bg = new Vector();

	    Iterator clist = gmap.values().iterator();
	    while (clist.hasNext()) {
		Agroup g = (Agroup)clist.next();
		if (cando(g, t)) {
		    // check it wont overrun by more than xfrac
		    double xfrac = (double)(t2 - t) / g.xt;
		    long overuntime = t + g.xt - t2;
		    if (xfrac > overunfrac || overuntime < hoveruntime) {
  		
			if (g.priority == -10)
			    bg.add(g);
			else
			    candidates.add(g);
		    }
		}
	    }

	    Agroup g = null;
	    if (candidates.size() != 0) {
		// pick a random candidate
		int ii = (int)(Math.floor(Math.random()*(double)candidates.size()));
		g = (Agroup)candidates.get(ii);
		seq.add(g);
	    } else if
		  // only use a BG if we have to, then only sometimes - can be better to sit quietly
		  (bg.size() != 0 && Math.random() < bgthresh) {
		int jj = (int)(Math.floor(Math.random()*(double)bg.size()));
                g = (Agroup)bg.get(jj);
                seq.add(g);
	    }
	    
	    if (g != null) {	    
		tally += score(g,t)*g.xt;
		g.last = t; // record g was done at t
		t += g.xt; // advance time
	    } else {
		t += 5*60000L;
	    }

	}

	// restore the group histories weve just changed
	for (int i = 0; i < hlist.size(); i++) {
	    ((History)hlist.get(i)).restoreGroupHistory();
        }

	return tally/(double)(t-t1);
	
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

	//return metrics.scoreSeeMatch(group, t)/(double)group.xt;
	return metrics.scoreElev(group, t);
	//	return metrics.scoreElev(group, t)/(double)group.xt;

    }


    public static void main(String args[]) {

	try {

	    ConfigurationProperties cfg = CommandTokenizer.use("--").parse(args);

	    LasScheduler las = new LasScheduler(cfg);

	    las.run();

	    // results


	} catch (Exception e) {
	    e.printStackTrace();
	}

    }


}