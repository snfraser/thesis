#!/usr/bin.gnuplot -persist

set xdata time
set timefmt "%Y-%m-%d %H:%M:%S"

set xlabel "Time [UT]"
set ylabel "Contention [Number of Groups]"

set title "Comparison of contention rates for several runs using single ODB snapshot"

set term postscript enhanced color landscape
set output "bs1_pr.eps"

plot  "b1.dat" using 1:3 with line ti "Run 1", \
      "b2.dat" using 1:3 with line ti "Run 2", \
      "b3.dat" using 1:3 with line ti "Run 3", \
      "b4.dat" using 1:3 with line ti "Run 4", \
      "b5.dat" using 1:3 with line ti "Run 5"


     
