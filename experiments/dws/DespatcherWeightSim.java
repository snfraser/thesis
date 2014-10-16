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

/** Runs a basic test of the simulator.*/
public class DespatcherWeightSim implements TimeSignalGenerator, SimulationEventListener {

    private Site site;

    private ScheduleSimulator sim;

    private Phase2Model phase2;

    private BasicScoringModel bsm;

    private BasicHistoryModel bhm;

    private BasicChargeAccountingModel bcam;

    private BasicAccountingModel bam;

    private BasicMutableEnvironmentPredictor bep;

    private BasicExecutionTimingModel bxtm;
    
    private BasicStochasticExecutionTimingModel bsem;

    private BasicTimingConstraintWindowCalculator btcwc;

    private BasicMutableWeatherModel bwm;

    /** The simulation time - provided to scheduler/simulatotr via TimeModel.*/
    private long time;

    private LogProxy logger;

    // SQM
    private int    ngs   = 0;
    private int    nfgs  = 0;

    // Metric tallies
    private double pqm    = 0.0;
    private double oaqm   = 0.0;
    private double tdqm   = 0.0;
    private double rnqm   = 0.0;
    private double pxoaqm = 0.0;

    //private double foaqm = 0.0;
    //private double fpqm = 0.0;
    
    private long sumxt  = 0L;

    //private PopulationStatistics ohstat;
    private PopulationStatistics oastat; // [oa] optimal airmass
    private PopulationStatistics pstat;  // [px] exec weighted priority
    private PopulationStatistics tdstat; // [td] target demand
    private PopulationStatistics xtstat; // [x]  total execution
    private PopulationStatistics rnstat; // [rn] urgency (1/rn)

    private PopulationStatistics pxoastat; // [px X oa]  optimal airmass with priority
    // private PopulationStatistics ;


    //private OptimalHeightUtilityCalculator ohuc;
    private PriorityUtilityCalculator        puc;
    private OptimalAirmassUtilityCalculator  oauc;
    private TargetDemandUtilityCalculator    tdc;
    private RemainingNightsUtilityCalculator rnuc;
    private OptimalAirmassPriorityUtilityCalculator pxoauc;

    public DespatcherWeightSim(Site site,
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

	Logger dlogger = LogManager.getLogger("SIM");
	logger = new LogProxy("SIM", "", dlogger);
		
	oauc = new OptimalAirmassUtilityCalculator(site, btcwc, bxtm, 5*60*1000L);
	puc  = new PriorityUtilityCalculator(bxtm);
	tdc  = new TargetDemandUtilityCalculator(btcwc, bxtm);
	rnuc = new RemainingNightsUtilityCalculator(btcwc);
	pxoauc = new OptimalAirmassPriorityUtilityCalculator(site, btcwc, bxtm, 5*60*1000L);
	
	oastat   = new PopulationStatistics();
	pstat    = new PopulationStatistics();
	tdstat   = new PopulationStatistics();
	xtstat   = new PopulationStatistics();
	rnstat   = new PopulationStatistics();
	pxoastat = new PopulationStatistics();

	//foastat = new PopulationStatistics();
        //fpstat  = new PopulationStatistics();
	
    }

  
    /** Setup test using supplied config.*/
    public static void main(String args[]) {
	
	try {

	    CommandTokenizer parser = new CommandTokenizer("--");
	    parser.parse(args);
	    ConfigurationProperties config = parser.getMap();

	    //Logger slogger = LogManager.getLogger("SIM");
	    //slogger.setLogLevel(config.getIntValue("log-level", 3));
	    //ConsoleLogHandler console = new ConsoleLogHandler(new SimulationLogFormatter());
	    //console.setlogLevel(Logging.OFF);
	    //slogger.addHandler(console);

	    Logger dlogger = LogManager.getLogger("SIM");
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
            BasicSelectionHeuristic bsel = new BasicSelectionHeuristic();

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
	    ScheduleSimulator sim = new ScheduleSimulator(site, brs, bxtm, bsel, bep, bwm);
	   
	    // Time synch NOT NEEDED?	   
	  

	    // Setup test controller...
	    DespatcherWeightSim test = new DespatcherWeightSim(site, sim, phase2, bam, bsm, bxtm, bsem, btcwc, history, bep, bwm, bcam, start);
	    
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
	
	// Here we vary the BSM weights between runs - the main aim is to increase
	// the relative weighting of w_trans to all the others.
	
	//	double dw = 0.06;
			
	//	int nw = 30; // number of increments in w.

	int nn = 20;

	// Runs, each pass increment the value of BSM Transit weight.
	int    nw = 15;
	double wt = 0.0;
	for (int iw = 0; iw < nw; iw++) {
	        
	    // these are the only weights
	    wt = (double)iw/(double)(nw - 1);
	    bsm.wgtTrans    = wt;
	    bsm.wgtPriority = 1.0 - wt;

	    logger.method("run").cat("DAT").log(1, "Start run: "+iw+"WT= "+bsm.wgtTrans);
	    logger.cat("");
	 
	    oastat.clear();
	    pstat.clear();
	    tdstat.clear();
	    xtstat.clear();
	    rnstat.clear();
	    pxoastat.clear();

	    //foastat.clear();
            //fpstat.clear();

	    for (int is = 0; is < nn; is++) {

		logger.method("run").log(2, "Attempt to load history info from Phase2...");
		bhm.loadHistory(phase2, start);
		logger.method("run").log(2, "Attempt to load account info from Phase2...");
		bam.loadAccounts(phase2, start);
		
		ngs  = 0;   // count number of groups scheduled this run.
		nfgs = 0;   // count number flexible groups this run.

		oaqm = 0.0; // record oh metric this run.
		pqm  = 0.0; // record p  metric this run.
		tdqm = 0.0; // record td metric this run.
		rnqm = 0.0; // record rn metric this run.
		sumxt = 0L; // record total exec this run.
		pxoaqm = 0.0; // record px X oa metric this run.
		//foaqm = 0.0;
		//fpqm  = 0.0;

		time = start;
		// Start the simulator framework...
		logger.method("run").log(2, "Starting the simulator framework...");
		
		sim.runSimulation(this, start, end, this);
	
		// update results for pass [is] of run [iw]
	
		oastat.addSample(oaqm/ngs);
		pstat.addSample(pqm/(double)night);
		tdstat.addSample(tdqm);
		xtstat.addSample((double)sumxt/(double)night);
		rnstat.addSample(rnqm);
		pxoastat.addSample(pxoaqm/(double)night);

		//foastat.addSample(foaqm/nfgs);
                //fpstat.addSample(fpqm/(double)night);

	// 	logger.cat("SDAT").log(1,"Run: "+is+" oastat= "+oastat);
// 		logger.cat("SDAT").log(1,"Run: "+is+" tdstat= "+tdstat);
// 		logger.cat("SDAT").log(1,"Run: "+is+" pstat=  "+pstat);
// 		logger.cat("SDAT").log(1,"Run: "+is+" xtstat= "+xtstat);
// 		logger.cat("SDAT").log(1,"Run: "+is+" rnstat= "+rnstat);

// 		logger.cat("SDAT").log(1,"Run: "+is+" foastat= "+foastat);
// 		logger.cat("SDAT").log(1,"Run: "+is+" fpstat=  "+fpstat);

		logger.cat("");
	    } // next run at fixed wt
	    
	    // print results for nn passes at (wt).
	    logger.cat("OA_DAT").log(1, ""+wt+" "+oastat);
	    logger.cat("PX_DAT").log(1, ""+wt+" "+pstat);
	    logger.cat("TD_DAT").log(1, ""+wt+" "+tdstat);
	    logger.cat("X_DAT").log(1, ""+wt+" "+xtstat);
	    logger.cat("RN_DAT").log(1,""+wt+" "+rnstat);
	    logger.cat("PXOA_DAT").log(1,""+wt+" "+pxoastat);

	    //logger.cat("FOHADAT").log(1, ""+wt+" "+foastat);
            //logger.cat("FPADAT").log(1, ""+wt+" "+fpstat);


	} // next wt value

    }

    public long getTime() { return time;}
    
    /** Handle time signal request.*/
    public void awaitTimingSignal(TimeSignalListener tsl, long t) {
	// this shouldnot be called
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

	double tdhm = tdc.getUtility(metric.group, time, env, hist);
	tdqm += tdhm;

	double rnhm = rnuc.getUtility(metric.group, time, env, hist);
	rnqm += rnhm;

	double pxoahm = pxoauc.getUtility(metric.group, time, env, hist);
	pxoaqm += pxoahm;

	sumxt += exec;
	ngs++;

	logger.log(2,"Received notification of group selection: Time= "+
		   ScheduleSimulator.sdf.format(new Date(time))+
		   " Utility: [OA]: "+oahm+
		   " Utility: [P}: "+phm+
		   " Utility  [TD]: "+tdhm+
		   " GroupMetrics = "+metric+
		   " Updating history...");
	
	bhm.updateHistory(metric.group, time + exec); 
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

    } 
    
}
