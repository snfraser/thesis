#!/usr/bin/gnuplot -persist
#    
# set terminal x11 

set terminal postscript enhanced eps color landscape "Helvetica" 14
set output "../plots/fwd_dmd_20071016_32day_av.eps"

set xlabel "Forecast lead time [days]"
set ylabel "Average demand"
set title "Variation of forecast average demand with forecast lead time"
set nokey

plot 	"../data/fwd_dmd_20071016_32day_av.dat" using 1:2  with linespoints pt 5


#    EOF
