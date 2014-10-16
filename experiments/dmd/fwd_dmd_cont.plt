#!/usr/bin/gnuplot -persist
#    
# set terminal x11 

set terminal postscript enhanced eps color landscape "Helvetica" 14
set output "../plots/fwd_dmd_20071016_32day_cont.eps"

set xdata time
set timefmt "%Y-%m-%d %H:%M:%S"

set xlabel "Date/time [UT]"
set ylabel "Contention"

set title "Contention during demand forecast simulation"
set nokey

plot 	"../data/fwd_dmd_20071016_32day_cont.dat" using 1:3  with lines


#    EOF
