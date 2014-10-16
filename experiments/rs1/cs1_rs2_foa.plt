#!/usr/bin/gnuplot 

set title "Convergence of Q_{OA} Flexible"
set xlabel "Simulation runs"
set ylabel "Q_{OA} Flexible"
#set yrange [1.25:1.32]
set label 1 "ENV = E_{FX}" at graph 0.1,0.5
set label 2 "SEL = RAND" at graph 0.1,0.4
set output "cs1_rs2_foa.eps"
set term postscript enhanced color landscape
plot "cs1_rs2_foa.dat" using 12:15 with line ti "Simulation values", 0.809 lt 3 ti "Final estimate"
