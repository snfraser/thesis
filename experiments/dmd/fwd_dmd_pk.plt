#!/usr/bin/gnuplot -persist
#    
# set terminal x11 

set terminal postscript enhanced eps color landscape "Helvetica" 14
set output "../plots/fwd_dmd_20071016_32day_pk.eps"

set xlabel "Forecast lead time [days]"
set ylabel "Peak demand"
set title "Variation of forecast peak demand with forecast lead time"
set nokey

plot 	"../data/fwd_dmd_20071016_32day_pk.dat" using 1:2  with linespoints pt 5


#    EOF
