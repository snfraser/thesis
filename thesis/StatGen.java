import java.io.*;
import java.util.*;

public class StatGen {

    private BufferedReader bin;

    private PrintWriter pout;

    public StatGen(BufferedReader bin, PrintWriter pout) {
	this.bin = bin;
	this.pout = pout;
    }

    private void run() throws Exception {

	String line = null;

	while (true) {

	    // skip empty lines
	    while ((line = bin.readLine()) != null) {
		if (! line.equals(""))
		    break;
	
	    }

	    if (line.startsWith("END"))
		return;
		
	    String figno = line;		 
	    String label = bin.readLine();
	    String title = bin.readLine();

	    System.err.println("Processing: "+title);
	    
	    // X
	    line = bin.readLine();
	    StringTokenizer st = new StringTokenizer(line);
	    st.nextToken();
	    String xvar =  st.nextToken();

	    // Y
	    line = bin.readLine();
	    st = new StringTokenizer(line);
	    st.nextToken();
	    String yvar =  st.nextToken();

	    // ncomp
	    line = bin.readLine();
	    st = new StringTokenizer(line);
	    st.nextToken();
	    int nc = Integer.parseInt(st.nextToken());
	    System.err.println("Num comp: "+nc);

	    // npoints X
	    line = bin.readLine();
	    st = new StringTokenizer(line);
	    st.nextToken();
	    int np = Integer.parseInt(st.nextToken());
	    double[] x = new double[np];
	    for (int ip = 0; ip < np; ip++) {
		x[ip] = Double.parseDouble(st.nextToken());
	    }
	    System.err.println("Num points: "+np);

	    // baseline values
	    double[] yb = new double[np];	
	    double[] dyb = new double[np];
	    line = bin.readLine();
	    st = new StringTokenizer(line);
	    st.nextToken();
	    String basename =  st.nextToken();
	    System.err.println("Basename: "+basename+" have "+st.countTokens()+" tokens left");
	    for (int ip = 0; ip < np; ip++) {
		yb[ip]  = Double.parseDouble(st.nextToken());
		dyb[ip] = Double.parseDouble(st.nextToken());
	    }
	    System.err.println("Base values: "+yb);
	    pout.println("\\clearpage");
	    pout.println("\\begin{landscape}");
	  
	    pout.println("\\begin{table}[h]");
	    pout.println("\\begin{center}");
	    StringBuffer llb = new StringBuffer("l");
	    for (int i = 0; i < np; i++) {
		llb.append("l");
	    }
	    pout.println("\\begin{tabular}{"+llb+"}");
	    pout.println("\\toprule");
	    // title


	    pout.println("\\multicolumn{"+(np+1)+"}{c}{Values of "+yvar+" versus "+xvar+"}\\\\");
	    pout.println("\\midrule");
	    pout.print(xvar);
	    for (int i = 0; i < np; i++) {
		pout.print(" & "+x[i]);
	    }
	    pout.println("\\\\");
	    pout.println("\\midrule");

	    pout.print(basename);
	    for (int ip = 0; ip < np; ip++) {
		pout.printf(" &  %4.2f ($\\pm$%4.2f)", yb[ip], dyb[ip]);
	    }
	    pout.println("\\\\");
	    pout.println("\\midrule");

	    String complist = "";
	    // each comparisons values
	    for (int ic = 1; ic < nc; ic++) {
		double[] y  = new double[np];	
		double[] dy = new double[np];
		line = bin.readLine();
		st = new StringTokenizer(line);
		st.nextToken();   
		String compname =  st.nextToken();
		for (int ip = 0; ip < np; ip++) {
		    y[ip]  = Double.parseDouble(st.nextToken());
		    dy[ip] = Double.parseDouble(st.nextToken());
		}
		System.err.println("Comp "+ic+" values: "+y);
		if (ic != 1)
		    complist+=","+compname;
		else
		     complist+=compname;

		pout.print(compname);
		for (int ip = 0; ip < np; ip++) {
		    pout.printf(" &  %4.2f ($\\pm$%4.2f)", y[ip], dy[ip]);
		}
		pout.println("\\\\");

		// work out diff and std from base
		pout.print("$\\Delta_{90}$("+compname+","+basename+")");
		for (int ip = 0; ip < np; ip++) {
		    double diff = y[ip]-yb[ip];
		    double dstd = Math.sqrt(dyb[ip]*dyb[ip] + dy[ip]*dy[ip]);
		    
		    double imp = 100.0*diff/yb[ip];
		    double hi = 100.0*(diff+dstd*1.28)/yb[ip];
		    double lo = 100.0*(diff-dstd*1.28)/yb[ip];
		    double hilo = 0.5*(hi-lo);
		    pout.printf(" & %4.2f\\%% ($\\pm$%4.2f\\%%)", imp, hilo);
		}
		pout.println("\\\\");
	    }

	    pout.println("\\bottomrule");
	    pout.println("\\end{tabular}");    
	    pout.println("\\end{center}");
	    pout.println("\\caption[Comparison of "+yvar+" versus "+xvar+" for "+complist+" relative to "+basename+".]"+
			 "{Comparison of schedulers ("+complist+") relative to baseline ("+
			 basename+") based on results displayed in "+figno+
			 ". Measurements show values of "+yvar+" and its relative improvement against selected values of "+xvar+"}");
	    pout.println("\\label{b:"+label+"}");  
	    pout.println("\\end{table}");
	
	    pout.println("\\end{landscape}");

	    pout.println();
	    pout.println();

	} //next block 

    } 


    public static void main(String args[]) {

	try {

	    BufferedReader bin = new BufferedReader(new FileReader(args[0]));
	    PrintWriter pout = new PrintWriter(new FileWriter(args[1]));
	    StatGen s = new StatGen(bin, pout);

	    s.run();

	    pout.close();

	} catch (Exception e) {
	    e.printStackTrace();
	}

    }



}
