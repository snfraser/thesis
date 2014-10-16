#!/bin/csh

set TRK = $1

cat cont2c_1.log | grep "Utility: \[RN\]" | awk '{print $18,$19,$22,$25,$29}' > conts2_1.dat

cat cont2c_1.log | grep "Utility: \[RN\]" | grep "$TRK" | awk '{print $18,$19,$22,$25,$29}' > conts2_trk.dat



# Sort the groups into selection count order
cat conts2_1.dat | sort -k 5 | awk '{print $5}' | uniq -c | sort -k 1 -n > c_ord.dat

# The most selected group is last entry in c_ord.dat
set most = `cat c_ord.dat | tail -1 | awk '{print $2}'`
cat cont2c_1.log | grep "Utility: \[RN\]" | grep "$most" | awk '{print $18,$19,$22,$25,$29}' > conts2_big.dat

gnuplot -persist << EOF
set xdata time
set timefmt x "%Y-%m-%d %H:%M:%S"
set format x "%d-%b"
set title "Winning group YT metric" 0.000000,0.000000  font ""
set xlabel "Date"
set ylabel "YT Metric"
set yrange [ -0.200000 : 1.50000 ] noreverse nowriteback
plot "conts2_1.dat" using 1:4 with point pt 5 ti "YT", \
     "conts2_big.dat" using 1:4 with point lt 3 pt 4  ti "${most}", \
     "conts2_trk.dat" using 1:4 with point lt 2 pt 4  ti "${TRK}"
EOF
gnuplot << EOF
set terminal postscript enhanced color landscape
set output "yt_ytrack.eps"
set xdata time
set timefmt x "%Y-%m-%d %H:%M:%S"
set format x "%d-%b"
set title "Winning group YT metric" 0.000000,0.000000  font ""
set xlabel "Date"
set ylabel "YT Metric"
set yrange [ -0.200000 : 1.50000 ] noreverse nowriteback
plot "conts2_1.dat" using 1:4 with point pt 5 ti "YT", \
     "conts2_big.dat" using 1:4 with point lt 3 pt 4  ti "${most}", \
     "conts2_trk.dat" using 1:4 with point lt 2 pt 4  ti "${TRK}"
EOF
