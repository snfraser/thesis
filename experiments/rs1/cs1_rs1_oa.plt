#!/usr/bin/gnuplot 

set title "Convergence of Q_{OA}"
set xlabel "Simulation runs"
set ylabel "Q_{OA}"
#set yrange [1.25:1.32]
set label 1 "ENV = E_{FP}" at graph 0.1,0.9
set label 2 "SEL = RAND" at graph 0.1,0.8
set output "cs1_rs1_oa.eps"
set term postscript enhanced color landscape
plot "cs1_rs1_oa.dat" using 12:15 with line ti "Simulation values", 0.8345 lt 3 ti "Final estimate"
