#!/usr/bin.gnuplot -persist

set xdata time
set timefmt "%Y-%m-%d %H:%M:%S"

set xlabel "Time [UT]"
set ylabel "Contention [Number of Groups]"

set title "Comparison of contention rates for several runs using ODB snapshot 2007-11-12"

set term postscript enhanced color landscape
set output "bsa1_ex.eps"

set label 1 "E_{FX}" at graph 0.05,0.8
set label 2 "X_{n/s}" at graph 0.05, 0.75

plot  "bsa1_ex_r1.dat" using 1:3 with line ti "Run 1", \
      "bsa1_ex_r2.dat" using 1:3 with line ti "Run 2", \
      "bsa1_ex_r3.dat" using 1:3 with line ti "Run 3", \
      "bsa1_ex_r4.dat" using 1:3 with line ti "Run 4", \
      "bsa1_ex_r5.dat" using 1:3 with line ti "Run 5"



     
