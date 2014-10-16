#!/usr/bin/gnuplot 

set title "Variation of Q_{RN} with relative scoring weight w_{trans}"
set xlabel "Relative weight w_{trans}"
set ylabel "Q_{RN}"
set key graph 0.3,0.4
set xrange [-0.05:1.0]
#set yrange [1.25:1.32]
#set label 1 "ENV = E_{FP}" at graph 0.2,0.9
#set label 2 "SEL = BEST"   at graph 0.2,0.8

set output "cs1_dw1a2_rn.eps"
set term postscript enhanced color landscape lw 1

plot \
"cs1_dw1_rn.dat" using 1:2 with point pt 5  ti "ENV = E_{FP}, SEL = BEST", \
29.6 lt 1 ti "ENV = E_{FP}, SEL = RAND", \
"cs1_dw2_rn.dat" using 1:2 with point pt 6 ti "ENV = E_{FX}, SEL = BEST", \
27.8 lt 3 ti "ENV = E_{FX}, SEL = RAND"
