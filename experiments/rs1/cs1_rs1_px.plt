#!/usr/bin/gnuplot 

set title "Convergence of Q_{PX}"
set xlabel "Simulation runs"
set ylabel "Q_{PX}"
set yrange [1.25:1.32]
set label 1 "ENV = E_{FP}" at graph 0.1,0.9
set label 2 "SEL = RAND" at graph 0.1,0.8
set output "cs1_rs1_px.eps"
set term postscript enhanced color landscape
plot "cs1_rs1_px.dat" using 12:15 with line ti "Simulation values", 1.2896 lt 3 ti "Final estimate"
