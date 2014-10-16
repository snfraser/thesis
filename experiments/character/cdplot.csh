#!/bin/csh

set file = $1

set sd   = `cat $file | grep "SDAT"    | awk '{print $2}'`
set ed   = `cat $file | grep "EDAT"    | awk '{print $2}'`
set av   = `cat $file | grep "C_D_AV"  | awk '{printf("%2.2f",$2)}'`
set cav  = `cat $file | grep "C_CD_AV" | awk '{printf("%2.2f",$2)}'`
set pav  = `cat $file | grep "C_PD_AV" | awk '{printf("%2.2f",$2)}'`
set max  = `cat $file | grep "C_D_MAX" | awk '{print $2}'`
set cmax = `cat $file | grep "C_CD_MAX" | awk '{print $2}'`
set pmax = `cat $file | grep "C_PD_MAX" | awk '{print $2}'`

set cdat = `date -d "${sd}" -u "+%Y%m%d"`

set filed = ${file}_c
set filecd = ${file}_cd
set filepd = ${file}_pd
echo "Output to $filed and $filecd and $filepd "

cat $file | grep "C_D" > $filed
cat $file | grep "C_CD" > $filecd
cat $file | grep "C_PD" > $filepd

echo "Plotting from $sd to $ed Av=$av CAV=$cav Max=$max Cmax=$cmax "

gnuplot << EOF
set output "cdplot_${cdat}.eps"
set terminal postscript enhanced color landscape
set xdata time
set timefmt "%Y-%m-%d %H:%M:%S"
set xlabel "Time [UT]"
set ylabel "Demand C_D"
set title "Demand profile (${sd} - ${ed})"
set format x "%Hh"
set yrange [0:10]

set label 1 "Avg(all) = ${av}"  at graph 0.8,0.7
set label 2 "Avg(crit)= ${cav}" at graph 0.8,0.65
set label 3 "Avg(p/w) = ${pav}" at graph 0.8,0.6

plot "${filed}" using 2:4 ti "Total demand" with line, \
     "${filecd}" using 2:4 ti "Critical demand" with line, \
     "${filepd}" using 2:(5*\$4) ti "Priority demand" with line

EOF
#set terminal fig landscape color fontsize 8
#set output "profiles/see_profile_${b1}.fig"

