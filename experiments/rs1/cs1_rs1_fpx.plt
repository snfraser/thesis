#!/usr/bin/gnuplot -persist

set title "Convergence of Q_{PX} Flexible"
set xlabel "Simulation runs"
set ylabel "Q_{PX} Flexible"
#set yrange [1.25:1.32]
set label 1 "ENV = E_{FP}" at graph 0.1,0.4
set label 2 "SEL = RAND" at graph 0.1,0.3
set output "cs1_rs1_fpx.eps"
set term postscript enhanced color landscape
plot "cs1_rs1_fpx.dat" using 12:15 with line ti "Simulation values", 0.1676 lt 3 ti "Final estimate"
