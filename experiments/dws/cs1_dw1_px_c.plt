#!/usr/bin/gnuplot 

set title "Variation of Q_{PX} with relative scoring weight w_{trans}"
set xlabel "Relative weight w_{trans}"
set ylabel "Q_{PX}"
set key graph 0.9,0.3
set xrange [-0.05:1.0]
#set yrange [1.25:1.32]
#set label 1 "ENV = E_{FP}" at graph 0.2,0.9
set label 2 "SEL = BEST"   at graph 0.2,0.3

set output "cs1_dw1_px_c.eps"
set term postscript enhanced color landscape lw 1

plot \
"cs1_dw1_px.dat" using 1:2 with point  pt 5 lt 1  ti "ENV = E_{FP}, All ", \
"cs1_dw1_fpx.dat" using 1:2 with point pt 5 lt 2  ti "ENV = E_{FP}, Flex", \
"cs1_dw2_px.dat" using 1:2 with point  pt 2 lt 1  ti "ENV = E_{FX}, All ", \
"cs1_dw2_fpx.dat" using 1:2 with point pt 2 lt 2  ti "ENV = E_{FX}, Flex"

