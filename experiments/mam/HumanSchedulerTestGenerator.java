import ngat.util.*;
import ngat.phase2.*;
import ngat.util.logging.*;
import ngat.astrometry.*;

import ngat.oss.simulation.*;
import ngat.icm.*;

import java.io.*;
import java.util.*;
import java.text.*;
import java.rmi.*;

/** Generates a load of scheduling information for a human scheduler to operate on.
 * This can be used to evaluate the human against a despatcher or some other type
 * of scheduling paradigm.
 */
public class HumanSchedulerTestGenerator {

    public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public static SimpleTimeZone UTC = new SimpleTimeZone(0, "UTC");

    static NumberFormat nf;
    static DecimalFormat df;

    static {
        sdf.setTimeZone(UTC);
    }


    /** Generate test stats.*/
    public static void main(String args[]) {

	nf = NumberFormat.getInstance();
	nf.setParseIntegerOnly(true);
	nf.setMaximumIntegerDigits(3);
	nf.setMinimumIntegerDigits(3);
	
	df = new DecimalFormat("###.##");
	//df.setMaximumIntegerDigits(3);
	//df.setMinimumIntegerDigits(3);
	//df.setMaximumFractionDigits(2);
	//df.setMinimumFractionDigits(2);

	try {

	    ConfigurationProperties config = CommandTokenizer.use("--").parse(args);
	    
	    Logger logger = LogManager.getLogger("SIM");
	    logger.setLogLevel(config.getIntValue("log-level", 3));
	    LogHandler con = new ConsoleLogHandler(new SimulationLogFormatter());
	    con.setLogLevel(config.getIntValue("log-level", 3));
	    logger.addHandler(con);

	    // Site.
	    Site site = new Site(config.getProperty("site"), 
				 Math.toRadians(config.getDoubleValue("lat")), 
				 Math.toRadians(config.getDoubleValue("long")));
	    Position.setViewpoint(site.getLatitude(), site.getLongitude());
	    
	    // Start time should be noon.
	    long start = (ScheduleSimulator.sdf.parse(config.getProperty("start"))).getTime();

	    // How many nights
	    int nn = config.getIntValue("nights", 1);
	    long end = start + nn*24*3600*1000L;

	     // Instruments.
	    InstrumentRegistry instruments = (InstrumentRegistry)Naming.lookup("rmi://localhost/InstrumentRegistry");

	     // Exec model
	    File bxf = new File(config.getProperty("exec")); // exec model properties
            BasicExecutionTimingModel execModel = new BasicExecutionTimingModel(site, instruments);
	    PropertiesConfigurator.use(bxf).configure(execModel);

	    // Allow 48 day for this test...
            execModel.setExternalTimeConstraint(end + 48*24*3600*1000L, "End of simulation");

	    // TC window calculator.
	    BasicTimingConstraintWindowCalculator btcwc = new BasicTimingConstraintWindowCalculator(execModel, 5*60*1000L);

	    // ODB Phase2
	    String root = config.getProperty("root");
	 
	    Phase2ModelProvider provider = (Phase2ModelProvider)Naming.
		lookup("rmi://localhost/"+root+"_Phase2ModelProvider");
	    Phase2Model phase2Model = provider.getPhase2Model();

	    // History
            BasicHistoryModel histModel = new BasicHistoryModel();
	    histModel.loadHistory(phase2Model, start);

	    // Accounting
	    BasicAccountingModel accModel = new  BasicAccountingModel();
	    accModel.loadAccounts(phase2Model, start);


	    // Env model.
	    EnvironmentSnapshot env = new EnvironmentSnapshot();
	    env.seeing = Group.EXCELLENT;
	    env.photom = true;

	    // Create maps to store groups from night to night.

	    //Map gid2Name = new HashMap(); // store map from group ID to name.
	    Map name2Gid = new HashMap(); // store map from group name to ID.
	    

	    // Create mapping file for analysis.
	    File mfile = new File(config.getProperty("map"));
	    PrintStream pout = new PrintStream(new FileOutputStream(mfile));
	    
	    // loop over nights
	    long time = 0L;
	    for (int in = 0; in < nn; in++) {

		long sn = start + (long)in*24*3600*1000L;
		long en = sn    + 24*3600*1000L;
		logger.log(1, "Start night: "+in+
			   " from "+ScheduleSimulator.sdf.format(new Date(sn))+
			   " to "+ScheduleSimulator.sdf.format(new Date(en)));

		// Work out sun/moon
		displayEphemeris(site, sn, en);
		
		// work out sunset
		long tt = sn;
		Position sun = Astrometry.getSolarPosition(tt);
		while (tt < en && sun.getAltitude(tt, site) > 0.0) {
		    sun = Astrometry.getSolarPosition(tt);
		    tt += 5*60*1000L;
		}
		// ok sunset
		long sunset = tt;


		double sumxt =  0.0; // total possible execution time over night.
		double sumct =  0.0; // total critical execution time over night.
		double sumnbt = 0.0; // total critical non-background time over night.

		int gg  = 0;
		int gid = 0;
		int xg  = 0; // count executable groups
		int cg  = 0; // count critical groups
		time = sn;

		// find out what can be done
		Iterator ip = phase2Model.listProposals();
		while (ip.hasNext()) {
		    
		    Proposal proposal = (Proposal)ip.next();
		    
		    // VETO: Proposal null.
		    if (proposal == null) continue;
		    
		    logger.log(2, "Check proposal "+proposal.getName());
		
		    // VETO: Proposal outside activation period.        
		    if (proposal.getActivationDate() > time ||
			proposal.getExpiryDate() < time) {
			logger.log(2, "Proposal skipped: PROP_ACTIVATION");
			continue;
		    }
		    
		    // VETO: Proposal used up all allocated time.
		    Accounts acc = accModel.getAccounts(proposal.getFullPath());
		    if (acc == null) {
			logger.log(2, "Proposal skipped: PROP_NO_ACCOUNTS");
			continue;
		    }
		    
		    Account total = acc.getAccount(AccountingModel.ACCOUNT_TOTAL);
		    if (total == null) {
			logger.log(2, "Proposal skipped: PROP_NO_TIME_ALLOC");
			continue;
		    }	
		    
		    if (total.getUsed() >= total.getAllocated()) {
			logger.log(2, "Proposal skipped: Acc.Total="+total);
			logger.log(2, "Proposal skipped: U="+total.getUsed()+" A="+total.getAllocated());
			logger.log(2, "Proposal skipped: PROP_TIME_ALLOC_EXCEEDED");
			continue;
		    }
		    
		    
		    Iterator iGroup = proposal.listAllGroups();		
		    while (iGroup.hasNext()) {
			gg++;
			Group group = (Group)iGroup.next();
		       
			// do we already lnow this group from a previous night ?
			String gpath = group.getFullPath();
			if (name2Gid.containsKey(gpath)) 
			    gid = ((Integer)name2Gid.get(gpath)).intValue();
			else
			    gid = gg;

			ExecutionStatistics hist = histModel.getExecutionStatistics(group);
			long xt = execModel.getExecTime(group);
		
			// work out for each group (type-specific) what windows does it have
			// which start in (t1,t2) - they may end in (t1,t2) or may overrun.
		    
			List windows = btcwc.listFeasibleWindows(group,
								 hist,
								 sn, 
								 en);
		    
			if (windows == null || windows.size() == 0)
			    continue;
		    
		
			logger.log(1, "Group: "+group.getClass().getName()+"/"+group.getName()+" found "+windows.size()+" windows for night: "+in);
			
			int crn = 0;
			Iterator it = windows.iterator();
			while (it.hasNext()) {
		    
			    TimeWindow w = (TimeWindow)it.next(); 
			    logger.log(1, "Found Window: "+w);
			    w.start = Math.max(w.start, sn);                        // later of start of this night or window start
			    w.end = Math.min(w.end, start + (nn+14)*24*3600*1000L); // earliest of window end or 14 nights from start of night 0
			    int rn = btcwc.countRemainingNights(group, w, w.start+5*60*1000L, env, hist);
			    logger.log(1, "Test Window reduced to: "+w+" RN="+rn);
			    if (rn > 0) {
			    	sumxt += (double)xt;
				// criticals
				if (rn == 1) {
				    sumct += (double)xt;
				    if (group.getPriority() != -2)
					sumnbt += (double)xt;
				}

				crn = rn;
				System.err.println("GWIN: G"+gg+" "+
						   group.getClass().getName()+"/"+group.getName()+
						   " XT="+((double)xt/60000)+
						   "M RN="+rn);
				//String sp = "";
				//switch (group.getPriority()){
				//case -2:
				//  sp = "BGR";
				//  break;
				//case -1:
				//  sp = "PHO";
				//  break;
				//default:
				//  sp = nf.format(group.getPriority());
				//}
				
				//String tc = "";
				
				//if (group instanceof MonitorGroup) {
				//  MonitorGroup mg = (MonitorGroup)group;
				//  double period = (double)mg.getPeriod();
				//  double pwin   = (double)mg.getFloatFraction()*period;
				//  tc = "MONITR "+df.format((period/3600000.0))+"H ["+df.format((pwin/3600000.0))+"]";
				//} else if
				///  (group instanceof RepeatableGroup) {
				// RepeatableGroup rg = (RepeatableGroup)group;
				//  double minInt = (double)rg.getMinimumInterval();
				//  int    maxRep = rg.getMaximumRepeats();
				//  tc = "INTVAL "+df.format(minInt/3600000.0)+"H";
				//} else if 
				//  (group instanceof EphemerisGroup) {
				//  tc = "EPHEM  ";
				//    } //else if
				//  (group instanceof FixedGroup) {
				//  tc = "FIXED  ";
				//} else {
				//  tc = "FLEXBL ";				
				//}
				
				//System.err.println("GROUP G"+nf.format(gg)+" "+
				//	   StringUtilities.pad(group.getName(),20,true)+
				//	   StringUtilities.pad(tc,30)+
				//	   " P= "+sp+
				//	   " XT= "+StringUtilities.pad(df.format(((double)xt/60000))+"M",8)+
				//	   " RN= "+nf.format(rn));
				
				
				
			    }
			  
			}
			if (crn > 0) {
			    xg++;
			    String sp = "";
 			    switch (group.getPriority()){
 			    case -2:
 				sp = "BGR";
 				break;
 			    case -1:
 				sp = "STD";
 				break;
 			    default:
 				sp = ""+group.getPriority();
 			    }
			    
 			    String tc = "";
			    
 			    if (group instanceof MonitorGroup) {
 				MonitorGroup mg = (MonitorGroup)group;
 				double period = (double)mg.getPeriod();
 				double pwin   = (double)mg.getFloatFraction()*period;
 				tc = "MONITR "+df.format((period/3600000.0))+"H ["+df.format((pwin/3600000.0))+"]";
 			    } else if
 				(group instanceof RepeatableGroup) {
 				RepeatableGroup rg = (RepeatableGroup)group;
 				double minInt = (double)rg.getMinimumInterval();
 				int    maxRep = rg.getMaximumRepeats();
 				tc = "INTVAL "+df.format(minInt/3600000.0)+"H";
 			    } else if 
 				(group instanceof EphemerisGroup) {
 				tc = "EPHEM  ";
 			    } else if
 				(group instanceof FixedGroup) {
 				tc = "FIXED  ";
 			    } else {
 				tc = "FLEXBL ";				
 			    }

			    String nc = "?";
			    switch (group.getTwilightUsageMode()) {
			    case Group.SKY_ANY:
				nc = "CIVT";
				break;
			    case Group.SKY_BRIGHT_TWILIGHT:
				nc = "NAUT";
				break;
			    case Group.SKY_DARK_TWILIGHT:
				nc = "ASTR";
				break;
			    case Group.SKY_NIGHT:   
				nc = "    ";
				break;
			    }
			    String lc = "?_";
			    switch (group.getMinimumLunar()) {
			    case Group.DARK:
				lc = "DARK";
				break;
			    case Group.BRIGHT:
				lc = "    ";
				break;
			    }
			    String sc ="?";
			    switch (group.getMinimumSeeing()) {
			    case Group.EXCELLENT:
				sc = "EXEL";
				break;
			    case Group.AVERAGE:
				sc = "AVER";
				break;
			    case Group.POOR:
				sc = "POOR";
				break;
			    default:
				sc = "UUUU";
				break;
			    }
			    
			    Iterator iobs = group.listAllObservations();			    
			    Observation obs = (Observation)iobs.next();
			    Source src = obs.getSource();
			    Position tgt = src.getPosition();

			    String srn = "";
			    if (crn == 1) {
				srn = "CRIT";
				cg++;
			    } else {
				// what if its short period interval
				if (group instanceof RepeatableGroup) {
				    RepeatableGroup rg = (RepeatableGroup)group;
				    double minInt = (double)rg.getMinimumInterval();
				    if (minInt < 24*3600*1000L) { 
					srn = "ICRT";
					cg++;
				    } else
					srn = ""+crn;
				} else
				    srn = ""+crn;
				
			    }
			    
 			    System.err.println(nf.format(in)+" GROUP G_"+nf.format(gg)+"   "+
					       " "+StringUtilities.pad(Position.toHMSString(tgt.getRA()), 12, true)+
					       " "+
					       StringUtilities.pad(Position.toDMSString(tgt.getDec()), 12, true)+
					       "  "+
 					       StringUtilities.pad(group.getName(),20,true)+
 					       StringUtilities.pad(tc,30)+
					       " "+lc+" "+sc+" "+nc+" "+
					       " P= "+StringUtilities.pad(sp,3)+
 					       " XT= "+StringUtilities.pad(df.format(((double)xt/60000))+"M",8)+
					       " RN= "+StringUtilities.pad(""+srn,4)+" "+
					       displayTargetEphemeris(site, sunset, en, tgt));
			    
			    // Store the group and its id if it's a new one...
			    if (! name2Gid.containsKey(gpath)) {
				name2Gid.put(gpath, new Integer(gid));
				//gid2name.put(new Integer(gid), gpath);
			    }
				
			    // Dump details to output file(s)...
			    pout.println(nf.format(gid)+" "+group.getFullPath());
			    
			}
			
		    } // next group
		
		} // next prop
	    
		logger.log(1, "Total available XT = "+(sumxt/3600000.0)+"H from "+xg+" groups, "+
			   "Critical XT = "+(sumct/3600000.0)+"H of which NonBG ="+ (sumnbt/3600000.0)+"H, from "+cg+" groups");
		
	    } // next night
	    
	    pout.close();

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }


    private static void displayEphemeris(Site site, long sn, long en) {

	// Work out sun/moon
	long tt = sn;
	Position sun = Astrometry.getSolarPosition(tt);
	while (tt < en && sun.getAltitude(tt, site) > 0.0) {
	    sun = Astrometry.getSolarPosition(tt);
	    tt += 5*60*1000L;
	}
	// ok sunset
	long sunset = tt;
	Position moon = Astrometry.getLunarPosition(tt);
	double moonalt = moon.getAltitude(tt, site);
	System.err.println("Sunset:     "+sdf.format(new Date(tt))+" Moon "+Position.toDegrees(moonalt, 2));
	 
	// night               
	tt = sunset;
	while (tt < en && sun.getAltitude(tt, site) > Math.toRadians(-18.0)) {
            sun = Astrometry.getSolarPosition(tt);
            tt += 5*60*1000L;
        }
	long startnight = tt;
	moon = Astrometry.getLunarPosition(tt);
        moonalt = moon.getAltitude(tt, site);
        System.err.println("StartNight: "+sdf.format(new Date(tt))+" Moon "+Position.toDegrees(moonalt, 2));
	                       
	// if moon up look for moonset else look for moonrise.
	if (moonalt > 0.0) {
	    tt = sunset;
	    while (tt < en && moon.getAltitude(tt, site) > 0.0) {
		moon = Astrometry.getLunarPosition(tt);
		tt += 5*60*1000L;
	    }
	    if (tt < en) {
		// hit moonset before dawn, otherwise dont care.
		System.err.println("Moonset:    "+sdf.format(new Date(tt)));
	    }
	} else {
	    tt = sunset;
	    while (tt < en && moon.getAltitude(tt, site) < 0.0) {
                moon = Astrometry.getLunarPosition(tt);
                tt += 5*60*1000L;
            }
	    if (tt < en) {
                // hit moonrise before dawn, otherwise dont care.
                System.err.println("Moonrise:   "+sdf.format(new Date(tt)));
            }
	}
	    
	// look for sunrise
	tt = sunset;
	while (tt < en && sun.getAltitude(tt, site) < Math.toRadians(0.5)) {
	    sun = Astrometry.getSolarPosition(tt);
            tt += 5*60*1000L;
	}
	long sunrise = tt;
		        
	// search backwards for end of night            
	tt = sunrise;
	while (tt > sn && sun.getAltitude(tt, site) > Math.toRadians(-18.0)) {
            sun = Astrometry.getSolarPosition(tt);
            tt -= 5*60*1000L;
        }
	moon = Astrometry.getLunarPosition(tt);
        moonalt = moon.getAltitude(tt, site);
	System.err.println("EndNight:   "+sdf.format(new Date(tt))+" Moon "+Position.toDegrees(moonalt, 2));
	long endnight = tt;

	tt = sunrise;
	moon = Astrometry.getLunarPosition(tt);
        moonalt = moon.getAltitude(tt, site);
        System.err.println("Sunrise:    "+sdf.format(new Date(tt))+" Moon "+Position.toDegrees(moonalt, 2));
	
	long lon = endnight-startnight;
	long los = sunrise-sunset;

	System.err.println("Length of dark night:         "+(df.format((double)lon/3600000.0))+"H");
	System.err.println("Length of night inc twilight: "+(df.format((double)los/3600000.0))+"H");
	
	
    }

    private static String displayTargetEphemeris(Site site, long sn, long en, Position target) {
	// is the bugger up or down at start of night

// 	long tt = sn;
// 	double elev = target.getAltitude(tt, site);
// 	if (elev >  Math.toRadians(20.0)) {
// 	    // target up show UP then set time below dome
// 	    while (tt < en && elev > Math.toRadians(20.0)) {
// 		elev = target.getAltitude(tt, site);
// 		tt += 5*60*1000L;
// 	    }
//  	    // display UP plus setting time
// 	    return "Risen, Sets  "+sdf.format(new Date(tt));
	    
// 	} else {
// 	    // target down show DOWN then rise time
// 	    while (tt < en && elev < Math.toRadians(20.0)) {
//                 elev = target.getAltitude(tt, site);
//                 tt += 5*60*1000L;
//             }
// 	    // display UP plus rising time above dome
// 	    return "Set,   Rises "+sdf.format(new Date(tt));
		   
// 	}

	// alternative is to show a block chart with up/down symbol of some sort...

	//do sunset/sunrise period
	long tt = sn;
	Position sun = Astrometry.getSolarPosition(tt);
	while (tt < en && sun.getAltitude(tt, site) > 0.0) {
	    sun = Astrometry.getSolarPosition(tt);
	    tt += 5*60*1000L;
	}
	// ok sunset
	long sunset = tt;
	// look for sunrise
	tt = sunset;
	while (tt < en && sun.getAltitude(tt, site) < 0.0) {
	    sun = Astrometry.getSolarPosition(tt);
            tt += 5*60*1000L;
	}
	long sunrise = tt;

	StringBuffer buff = new StringBuffer();
	tt = sunset;
	double elev = target.getAltitude(tt, site);
	while (tt < sunrise) {
	    // within tt, tt+1 hour we count amount of uptime
	    long tt2 = tt;
	    int cc = 0;
	    while (tt2 < tt+60*60*1000L) {
		elev = target.getAltitude(tt2, site);
		if (elev > Math.toRadians(20.0))
		    cc++;
		tt2 += 6*60*1000L;
	    }
	    // at the end we have a count of 6 minute blocks - upto 10 of these
	    if (cc == 0)
		buff.append("_");
	    else if
		(cc == 10)
		buff.append("*");
	    else
		buff.append(""+cc);

	    tt += 60*60*1000L; // 1 hour block

	}

	return buff.toString();
    }

}
