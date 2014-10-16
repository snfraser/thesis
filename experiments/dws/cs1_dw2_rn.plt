#!/usr/bin/gnuplot 

set title "Variation of Q_{RN} with relative scoring weight w_{trans}"
set xlabel "Relative weight w_{trans}"
set ylabel "Q_{RN}"
set key graph 0.9,0.1
set xrange [-0.05:1.0]
#set yrange [1.25:1.32]
set label 1 "ENV = E_{FX}" at graph 0.1,0.3
#set label 2 "SEL = BEST"   at graph 0.1,0.2

set output "cs1_dw2_rn.eps"
set term postscript enhanced color landscape
plot "cs1_dw2_rn.dat" using 1:($2-$5):3:4:($2+$5) with candlestick lt 1 ti "SEL = BEST",\
27.8 lt 3 ti "SEL = RAND"
 

