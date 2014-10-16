#!/usr/bin/gnuplot 

set title "Variation of Q_{PX} with relative scoring weight"
set xlabel "Relative weight w_{trans}"
set ylabel "Q_{PX}"
set key graph 0.9,0.2
set xrange [-0.05:1.0]
#set yrange [1.25:1.32]
set label 1 "ENV = E_{FP}" at graph 0.1,0.3
set label 2 "SEL = BEST"   at graph 0.1,0.2
set label 3 "SCHED=D(W)"   at graph 0.1,0.1
#set output "cs1_dw1_px.eps"
#set term postscript enhanced color landscape
plot \
 "cs1_dw1_px.dat" using 1:($2-$5):3:4:($2+$5) with candlestick lt 3 ti "All best",\
 1.289 lt 3 ti "All Random",\
 "cs1_dw1_fpx.dat" using 1:($2-$5):3:4:($2+$5) with candlestick lt 4 ti "Flex best",\
 0.167 lt 4 ti "Flex random"

