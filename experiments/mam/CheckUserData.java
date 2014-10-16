import java.io.*;
import java.util.*;
import java.text.*;
import ngat.astrometry.*;
import ngat.phase2.*;

public class CheckUserData {

    public static void main(String args[]) {

	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	SimpleTimeZone UTC = new SimpleTimeZone(0, "UTC");
	sdf.setTimeZone(UTC);

	double LAT = Math.toRadians(28.0);
	double LON = Math.toRadians(-17.0);
	
	// ReadUserData userfile mapfile

	try {

	    ISite site = new BasicSite("obs", LAT, LON);
	    AstrometryCalculator astro = new BasicAstrometryCalculator();

	    long sunset = sdf.parse("2007-11-13 19:00").getTime(); 
	    long sunrise = sdf.parse("2007-11-14 07:00").getTime(); 

	    long tmid = (sunset+sunrise)/2;
	    long lon = sunrise - sunset;

	    Metrics metrics = new Metrics(site, tmid);

	    BufferedReader bin = new BufferedReader(new FileReader(args[0]));

	    LoadGroups l = new LoadGroups();

	    Map gmap = l.load(new File(args[1]));

	    long xtot = 0L;

	    double sig_el = 0.0;
	    double sig_pr = 0.0;
	    double sig_sm = 0.0;
	    double sig_lm = 0.0;
	    double sig_rn = 0.0;
	    double sig_td = 0.0;

	    String line = null;
	    while ((line = bin.readLine()) != null) {

		StringTokenizer st = new StringTokenizer(line);
		
		String st1 = st.nextToken();
		String st2 = st.nextToken();
	
		long start = (sdf.parse(st1+" "+st2)).getTime();

		String sid = st.nextToken();
		Agroup g = (Agroup)gmap.get("G_"+sid);

		long end = start + g.xt;

		xtot += g.xt;
		XExtraSolarTarget star = new XExtraSolarTarget("test");
		star.setRa(g.ra);
		star.setDec(g.dec);

		TargetTrackCalculator track = new BasicTargetCalculator(star, site);
		// better double targetMinElev = astro.getMinimumAltitude(track,
		// site, time, time + (long) execTime);
		Coordinates c = track.getCoordinates(start);
		double targetElev = astro.getAltitude(c, site, start);
		double maxel = astro.getMaximumAltitude(track, site, sunset, sunrise);
	
		double f_el = metrics.scoreElev(g, start);
		double f_pr = metrics.scorePriority(g, start);
		double f_sm = metrics.scoreSeeMatch(g, start);
		double f_lm = metrics.scoreLunMatch(g, start);
		double f_rn = metrics.scoreRN(g, start);
		double f_td = metrics.scoreTD(g, start);

		sig_el += f_el*g.xt;
		sig_pr += f_pr*g.xt;
		sig_sm += f_sm*g.xt;
		sig_lm += f_lm*g.xt;
		sig_rn += f_rn*g.xt;
		sig_td += f_td*g.xt;

		System.err.printf("%s %tT -> %tT %3.2f %3.2f :: %4.4f %4.4f %4.4f %4.4f %4.4f %4.4f \n",sid, start, end, 
				  Math.toDegrees(targetElev),  Math.toDegrees(maxel),
				  f_el, f_pr, f_sm, f_lm, f_rn, f_td);
		
	    }
	    


	    System.err.println("LON: "+((double)lon/3600000.0)+"h, Total exec time: "+((double)xtot/3600000.0)+"h Frac: "+
			       ((double)xtot/(double)lon));
	    
	    System.err.printf("H1: %6.4f %6.4f %6.4f %6.4f %6.4f %6.4f %6.4f\n",
			      sig_el/lon, sig_pr/lon, sig_sm/lon, sig_lm/lon, sig_rn/lon, sig_td/lon, 
			      ((double)xtot/(double)lon));
	    
	    
	} catch (Exception e) {
	    e.printStackTrace();
	}

    }


}
