#!/usr/bin/gnuplot 

set title "Convergence of Q_{TD}"
set xlabel "Simulation runs"
set ylabel "Q_{TD}"
set yrange [3.2:4.2]
set label 1 "ENV = E_{FP}" at graph 0.1,0.9
set label 2 "SEL = RAND" at graph 0.1,0.8
set output "cs1_rs1_td.eps"
set term postscript enhanced color landscape
plot "cs1_rs1_td.dat" using 12:15 with line ti "Simulation values", 3.29 lt 3 ti "Final estimate"
