#!/bin/csh

set file = $1

if (-e cdtrend.dat) then
 /bin/rm -fv cdtrend.dat
endif
touch cdtrend.dat

foreach file (cdprofile_*.dat)

    set sd   = `cat $file | grep "SDAT"    | awk '{print $2}'`
    set av   = `cat $file | grep "C_D_AV"  | awk '{printf("%2.2f",$2)}'`
    set cav  = `cat $file | grep "C_CD_AV" | awk '{printf("%2.2f",$2)}'`
  
    echo $sd $av $cav  >>&! cdtrend.dat
end
#set output "cdplot_${cdat}.eps"
#set terminal postscript enhanced color landscape

gnuplot << EOF
set output "cdtrend.eps"
set terminal postscript enhanced color landscape
set xdata time
set timefmt "%Y-%m-%d"
set xlabel "Date"
set ylabel "Demand C_D"
set title "Average demand trend C_D"
set format x "%m/%d"

plot "cdtrend.dat" using 1:2 ti "Total demand" with point pt 5, \
     "cdtrend.dat" using 1:3 ti "Critical demand" with point pt 5

EOF
#set terminal fig landscape color fontsize 8
#set output "profiles/see_profile_${b1}.fig"

