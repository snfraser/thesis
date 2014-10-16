#!/usr/bin/gnuplot -persist

set title "Convergence of Q_{PX} Flexible"
set xlabel "Simulation runs"
set ylabel "Q_{PX} Flexible"
set yrange [0.25:0.42]
set label 1 "ENV = E_{FX}" at graph 0.1,0.4
set label 2 "SEL = RAND" at graph 0.1,0.3
set key graph 0.9,0.5
set output "cs1_rs2_fpx.eps"
set term postscript enhanced color landscape
plot "cs1_rs2_fpx.dat" using 12:15 with line ti "Simulation values", 0.4124 lt 3 ti "Final estimate"
