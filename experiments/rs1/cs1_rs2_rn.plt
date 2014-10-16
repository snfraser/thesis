#!/usr/bin/gnuplot 

set title "Convergence of Q_{RN}"
set xlabel "Simulation runs"
set ylabel "Q_{RN}"
#set yrange [28:32]
set label 1 "ENV = E_{FX}" at graph 0.1,0.9
set label 2 "SEL = RAND" at graph 0.1,0.8
set output "cs1_rs2_rn.eps"
set term postscript enhanced color landscape
plot "cs1_rs2_rn.dat" using 12:15 with lines ti "Simulation values", 27.83 lt 3 ti "Final estimate"
