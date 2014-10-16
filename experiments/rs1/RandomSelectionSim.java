import ngat.oss.simulation.*;
import ngat.oss.simulation.metrics.*;

import ngat.util.*;
import ngat.util.logging.*;
import ngat.astrometry.*;
import ngat.phase2.*;
import ngat.icm.*;

import java.io.*;
import java.rmi.*;
import java.util.*;

/** Runs a test of the simulator using a despatcher with a random selection heuristic - 
 * results are independant of any scoring mechanism used.
 */
public class RandomSelectionSim implements TimeSignalGenerator, SimulationEventListener {

    private Site site;

    private ScheduleSimulator sim;

    private Phase2Model phase2;

    private BasicScoringModel bsm;

    private BasicHistoryModel bhm;

    private BasicChargeAccountingModel bcam;

    private BasicAccountingModel bam;

    private BasicMutableEnvironmentPredictor bep;

    private BasicMutableWeatherModel bwm;

    private BasicExecutionTimingModel bxtm;
    
    private BasicStochasticExecutionTimingModel bsem;

    private BasicTimingConstraintWindowCalculator btcwc;

    /** The simulation time - provided to scheduler/simulatotr via TimeModel.*/
    private long time;

    private LogProxy logger;

    // SQM
    private int    ngs   = 0;
    private int    nfgs  = 0;
 
    // Metric tallies
    private double pqm  = 0.0;
    private double oaqm = 0.0;
    private double tdqm = 0.0;
    private double rnqm = 0.0;
    private double pxoaqm = 0.0;

    private long sumxt  = 0L;

    //private PopulationStatistics ohstat;
    private PopulationStatistics oastat;
    private PopulationStatistics pstat;
    private PopulationStatistics tdstat;
    private PopulationStatistics xtstat;
    private PopulationStatistics rnstat;
    private PopulationStatistics pxoastat; // [px X oa]  optimal airmass with priority

    //private OptimalHeightUtilityCalculator ohuc;
    private PriorityUtilityCalculator puc;
    private OptimalAirmassUtilityCalculator oauc;
    private TargetDemandUtilityCalculator tdc;
    private RemainingNightsUtilityCalculator rnuc;
    private OptimalAirmassPriorityUtilityCalculator pxoauc;

    public RandomSelectionSim(Site site,
			      ScheduleSimulator sim,
			      Phase2Model phase2,
			      BasicAccountingModel bam,
			      BasicScoringModel bsm,
			      BasicExecutionTimingModel bxtm,
			      BasicStochasticExecutionTimingModel bsem,
			      BasicTimingConstraintWindowCalculator btcwc,
			      BasicHistoryModel bhm,
			      BasicMutableEnvironmentPredictor bep,	
			      BasicMutableWeatherModel bwm,
			      BasicChargeAccountingModel bcam,
			      long time) {	
	this.site = site;
	this.sim = sim;
	this.phase2 = phase2;
	this.bam = bam;
	this.bsm = bsm;
	this.bxtm = bxtm;
	this.bsem = bsem;
	this.btcwc = btcwc;
	this.bhm = bhm;
	this.bep = bep;	
	this.bwm = bwm;
	this.bcam = bcam;
	this .time = time;

	Logger dlogger = LogManager.getLogger("RSS");
	logger = new LogProxy("RSS", "", dlogger);
		
	oauc = new OptimalAirmassUtilityCalculator(site, btcwc, bxtm, 5*60*1000L);
	puc  = new PriorityUtilityCalculator(bxtm);
	tdc  = new TargetDemandUtilityCalculator(btcwc, bxtm);
	rnuc = new RemainingNightsUtilityCalculator(btcwc);
	pxoauc = new OptimalAirmassPriorityUtilityCalculator(site, btcwc, bxtm, 5*60*1000L);

	oastat = new PopulationStatistics();
	pstat  = new PopulationStatistics();
	tdstat = new PopulationStatistics();
	xtstat = new PopulationStatistics();
	rnstat = new PopulationStatistics();
	pxoastat = new PopulationStatistics();

	//	foastat = new PopulationStatistics();
        //fpstat  = new PopulationStatistics();
	
    }

  
    /** Setup test using supplied config.*/
    public static void main(String args[]) {

	try {

	    CommandTokenizer parser = new CommandTokenizer("--");
	    parser.parse(args);
	    ConfigurationProperties config = parser.getMap();

	    Logger dlogger = LogManager.getLogger("RSS");
	    dlogger.setLogLevel(config.getIntValue("log-level", 3));
	    LogHandler con = new ConsoleLogHandler(new SimulationLogFormatter());
	    con.setLogLevel(config.getIntValue("log-level", 3));
	    dlogger.addHandler(con);

	    Site site = new Site(config.getProperty("site"), 
				 Math.toRadians(config.getDoubleValue("lat")), 
				 Math.toRadians(config.getDoubleValue("long")));
	    
	    long start = (ScheduleSimulator.sdf.parse(config.getProperty("start"))).getTime();
            long end   = (ScheduleSimulator.sdf.parse(config.getProperty("end"))).getTime();

	    // Instruments.
	    InstrumentRegistry instruments = (InstrumentRegistry)Naming.lookup("rmi://localhost/InstrumentRegistry");
	  
	    // Exec model
	    File bxf = new File(config.getProperty("exec")); // exec model properties
            BasicExecutionTimingModel bxtm = new BasicExecutionTimingModel(site, instruments);
	    PropertiesConfigurator.use(bxf).configure(bxtm);

	    // Allow 2 day for this test...          
	    bxtm.setExternalTimeConstraint(end+30*24*3600*1000L, "End of simulation");

	    // Stochastic wrapper.
	    File bsef = new File(config.getProperty("sexm")); // stochastic exec model properties
	    BasicStochasticExecutionTimingModel bsem = new BasicStochasticExecutionTimingModel(bxtm);
	    PropertiesConfigurator.use(bsef).configure(bsem);

	    // TC window calculator.
	    BasicTimingConstraintWindowCalculator btcwc = new BasicTimingConstraintWindowCalculator(bxtm, 5*60*1000L);

	    // Charging
	    File bcaf = new File(config.getProperty("cost")); // charge model properties
	    BasicChargeAccountingModel bcam = new BasicChargeAccountingModel();
	    PropertiesConfigurator.use(bcaf).configure(bcam);

	    // Scoring model
	    File smf = new File(config.getProperty("score")); // scoring properties
            BasicScoringModel bsm = new BasicScoringModel(site);
            PropertiesConfigurator.use(smf).configure(bsm);

	    // Selector
	    int seed = (int)(54321.0*(Math.random()+1.0));
            RandomSelectionHeuristic rsel = new RandomSelectionHeuristic(seed);

	    // Env prediction
	    BasicMutableEnvironmentPredictor bep = new BasicMutableEnvironmentPredictor();

	    // set the initial seeing, if we choose random then the SimApp will modify
	    // the BEP seeing while it runs.
	    int seeing = Group.POOR;	 
	    String seestr = config.getProperty("seeing");
            if ("poor".equals(seestr))
                seeing = Group.POOR;
            else if
                ("aver".equals(seestr))
                seeing = Group.AVERAGE;
            else if
                ("ex".equals(seestr))
                seeing = Group.EXCELLENT;
	    
	    bep.setSeeing(seeing);
	    bep.setPhotom(true);
	    
	    //   // RTC
	    // 	    RemainingTimeCalculator rtc = new DummyRemainingTimeCalculator();

	    // Weather - always good.
	    BasicMutableWeatherModel bwm = new BasicMutableWeatherModel();
	    bwm.setGood(true);
	    
	    // ODB
	    String root = config.getProperty("root");
	 
	    Phase2ModelProvider provider = (Phase2ModelProvider)Naming.
		lookup("rmi://localhost/"+root+"_Phase2ModelProvider");
	    Phase2Model phase2 = provider.getPhase2Model();

	    // History
            BasicHistoryModel history = new BasicHistoryModel();
	    //history.loadHistory(phase2, start);

	    BasicAccountingModel bam = new  BasicAccountingModel();
	    //bam.loadAccounts(phase2, start);

	    // Rank sequencing...
	    BasicRankSequencer brs = new BasicRankSequencer(phase2, history, bam, bxtm, bsm);

	    // TODO?  Despatcher
	    //   BasicDespatcher despatcher = new BasicDespatcher(phase2, history, bxtm, bsm, bsel, rtc);

	    // setup simulator...
	    ScheduleSimulator sim = new ScheduleSimulator(site, brs, bxtm, rsel, bep, bwm);
	   
	    // Time synch NOT NEEDED?	   
	  

	    // Setup test controller...
	    RandomSelectionSim test = new RandomSelectionSim(site, sim, phase2, bam, bsm, bxtm, bsem, btcwc, history, bep, bwm, bcam, start);
	    
	    test.run(start, end);
	    
	} catch (Exception e) {
	    e.printStackTrace();
	    return;
	}

    }

    public void run(long start, long end) throws Exception {
            
	logger.log(1, "Start sim run...");

	// work out the night length
	long night = 0L;
	long astro = 0L;
	long t = start;
	while (t < end) {

	    Position sun = Astrometry.getSolarPosition(t);
	    double sunElev = sun.getAltitude(time, site);
	    if (sunElev < 0.0)
		night += 5*60*1000L;  
	    if (sunElev < Math.toRadians(-18.0))
		astro += 5*60*1000L;
	    t += 5*60*1000L;
	}

	    logger.cat("");
	 
	    oastat.clear();
	    pstat.clear();
	    tdstat.clear();
	    xtstat.clear();
	    rnstat.clear();
	    pxoastat.clear();

	    //	    foastat.clear();
	    // fpstat.clear();

	    // do a load of sim runs.
	    for (int is = 0; is < 100; is++) {
		
		logger.method("run").log(2, "Attempt to load history info from Phase2...");
		bhm.loadHistory(phase2, start);
		logger.method("run").log(2, "Attempt to load account info from Phase2...");
		bam.loadAccounts(phase2, start);
		
		ngs  = 0;   // count number of groups scheduled this run.
		nfgs = 0;   // count number of flex groups scheduled this run.
		oaqm = 0.0; // record oh metric this run.
		pqm  = 0.0; // record p  metric this run.
		tdqm = 0.0; // record td metric this run.
		rnqm = 0.0; // record rn metric this run.
		pxoaqm = 0.0; // record px X oa metric this run.
		sumxt = 0L; // record total exec this run.
	
		time = start;
		// Start the simulator framework...
		logger.method("run").log(1, "Starting the simulator framework...run "+is);
		
		sim.runSimulation(this, start, end, this);
	
		// update results for pass [is] of run [iw]
	
		oastat.addSample(oaqm/ngs);
		pstat.addSample(pqm/(double)night);
		tdstat.addSample(tdqm);
		xtstat.addSample((double)sumxt/(double)night);
		rnstat.addSample(rnqm);
		pxoastat.addSample(pxoaqm/(double)night);
	
		//	foastat.addSample(foaqm/nfgs);
		//  fpstat.addSample(fpqm/(double)night);

	// 	logger.cat("OA_DAT").log(1, ""+wt+" "+oastat);
// 		logger.cat("PX_DAT").log(1, ""+wt+" "+pstat);
// 		logger.cat("TD_DAT").log(1, ""+wt+" "+tdstat);
// 		logger.cat("X_DAT").log(1, ""+wt+" "+xtstat);
// 		logger.cat("RN_DAT").log(1,""+wt+" "+rnstat);
// 		logger.cat("PXOA_DAT").log(1,""+wt+" "+pxoastat);
		// 		logger.cat("SDAT").log(1,"Run: "+is+" foastat= "+foastat);
		// 		logger.cat("SDAT").log(1,"Run: "+is+" fpstat=  "+fpstat);

		logger.cat("");
	    } // next run at fixed wt
	    
	    // print results for nn passes at (wt).
	    logger.cat("OA_DAT").log(1,  " "+oastat);
	    logger.cat("PX_DAT").log(1,  " "+pstat);
	    logger.cat("TD_DAT").log(1,  " "+tdstat);
	    logger.cat("X_DAT").log(1,   " "+xtstat);
	    logger.cat("RN_DAT").log(1,  " "+rnstat);
	    logger.cat("PXOA_DAT").log(1," "+pxoastat);

    }

    public long getTime() { return time;}
    
    /** Handle time signal request.*/
    public void awaitTimingSignal(TimeSignalListener tsl, long t) {

	time = t;
	tsl.timingSignal(t);
   
    }

    /** Handle notification of a simulation group selection event.
     * We need to step the time model forwards and notify sim of this time step.
     */
    public void groupSelected(Metric metric) {
	logger.method("groupSelected(Metric, t)");

	// Update the execution history for time using calculated exec time
	long exec = bsem.getExecTime(metric.group);

	ExecutionStatistics hist = bhm.getExecutionStatistics(metric.group);

	// Update the accounting using calculated charge (under current env !)
	EnvironmentSnapshot env = bep.predictEnvironment(time+exec);
	double xhours = bcam.calculateCost(metric.group, env)/3600000.0;

	// SQMs
	double oahm = oauc.getUtility(metric.group, time, env, hist);
	oaqm += oahm;
	
	double phm = puc.getUtility(metric.group, time, env, hist);
	pqm  += phm;

	double pxoahm = pxoauc.getUtility(metric.group, time, env, hist);
	pxoaqm += pxoahm;
	
	double tdhm = tdc.getUtility(metric.group, time, env, hist);
	tdqm += tdhm;

	double rnhm = rnuc.getUtility(metric.group, time, env, hist);
	rnqm += rnhm;

	sumxt += exec;
	ngs++;

	logger.log(2,"Received notification of group selection: Time= "+
		   ScheduleSimulator.sdf.format(new Date(time))+
		   " Utility: [OA]: "+oahm+
		   " Utility: [P}: "+phm+
		   " Utility  [TD]: "+tdhm+
		   " GroupMetrics = "+metric+
		   " Updating history...");
	
	bhm.updateHistory(metric.group, time+exec); 
	logger.log(2,"Updating group history for using sexec "+(exec/1000)+"s, completing at: "+
		   ScheduleSimulator.sdf.format(new Date(time+exec)));
	
	metric.accounts.getAccount(AccountingModel.ACCOUNT_TOTAL).debit(xhours);
	logger.log(2,"Updating proposal accounts with costing: "+xhours);
	
	// time step forward.
	time += exec;
	sim.timingSignal(time);

    }
    
    /** Handle notification of contention stats.*/
    public void contentionResults(int contention) {
	logger.method("contentionResults(t,con)");
	logger.cat("CONT").log(2,"Received contention results: Time= "+
		   ScheduleSimulator.sdf.format(new Date(time))+" C = "+contention);	
    }
    
    /** Notification that simulation has completed.*/
    public void simulationCompleted() {

 	logger.method("simCompleted()").log(2,"Received notification that simulation has completed");
// 	logger.cat("DAT").log(1, "SQM results: [OH]: "+sqm+" out of a total possible of: "+ngs+" Eff="+(100.0*sqm/(double)ngs)+"% [P]: "+pqm);
// 	logger.cat("XDAT").log(1, ""+ngs+" "+sqm+" "+(100.0*sqm/(double)ngs)+" "+pqm);
// 	// ng oh_qm oh_eff p_qm (p_eff)
// 	logger.cat("");
    } 
    
}
