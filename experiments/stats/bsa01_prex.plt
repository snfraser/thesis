#!/usr/bin.gnuplot -persist

set xdata time
set timefmt "%Y-%m-%d %H:%M:%S"

set xlabel "Time [UT]"
set ylabel "Contention [Number of Groups]"

set title "Comparison of contention rates for several runs using ODB snapshot 2007-11-12"

set term postscript enhanced color landscape
set output "bsa01_prex.eps"

set label 2 "X_{n/s}" at graph 0.05, 0.75

plot  "bsa0_pr_r1.dat" using 1:3 with line ti "E_{FP}", \
      "bsa1_ex_r1.dat" using 1:3 with line ti "E_{FX}"



     
