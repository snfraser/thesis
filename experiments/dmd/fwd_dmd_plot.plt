#!/usr/bin/gnuplot -persist
#    
# set terminal x11 

set terminal postscript enhanced eps color landscape "Helvetica" 14
set output "../plots/fwd_dmd_20071016_32day_leadtimes.eps"

set xdata time
set timefmt "%Y-%m-%d %H:%M:%S"
set format x "%H:%M"

set xlabel "Time [UT]"
set ylabel "Demand"
set title "Forward demand forecasts: Comparison of forecast lead times"

plot 	"../data/fwd_dmd_20071016_32day_p1.dat" using 1:3 ti "1 day" with lines, \
	"../data/fwd_dmd_20071016_32day_p2.dat" using 1:3 ti "2 day" with lines, \
	"../data/fwd_dmd_20071016_32day_p4.dat" using 1:3 ti "4 day" with lines, \
	"../data/fwd_dmd_20071016_32day_p8.dat" using 1:3 ti "8 day" with lines, \
	"../data/fwd_dmd_20071016_32day_p16.dat" using 1:3 ti "16 day" with lines, \
	"../data/fwd_dmd_20071016_32day_p31.dat" using 1:3 ti "31 day" with lines

#    EOF
