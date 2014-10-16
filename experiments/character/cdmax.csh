#!/bin/csh

set file = $1

if (-e cdmax.dat) then
 /bin/rm -fv cdmax.dat
endif
touch cdmax.dat

foreach file (cdprofile_*.dat)

    set sd   = `cat $file | grep "SDAT"    | awk '{print $2}'`
    set max  = `cat $file | grep "C_D_MAX" | awk '{print $2}'`
    set cmax = `cat $file | grep "C_CD_MAX" | awk '{print $2}'`

    echo $sd $max $cmax >>&! cdmax.dat
end
#set output "cdplot_${cdat}.eps"
#set terminal postscript enhanced color landscape

gnuplot <<`EOF
set output "cdmax.eps"
set terminal postscript enhanced color landscape
set xdata time
set timefmt "%Y-%m-%d"
set xlabel "Date"
set ylabel "Demand C_D"
set title "Demand peak"
set format x "%m/%d"

plot "cdmax.dat" using 1:2 ti "Max total demand" with point, \
     "cdmax.dat" using 1:3 ti "Max Crit demand" with point


EOF


