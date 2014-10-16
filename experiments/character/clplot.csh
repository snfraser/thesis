#!/bin/csh

# Extract load values for plotting from multiple files

foreach file (cdprofile*.dat)
    # Append load values to load data

    echo "Extracting from $file "

    set ln = `cat $file | grep "C_LN" | awk '{print $2}'`
    set cl = `cat $file | grep "C_CL" | awk '{print $2}'`
    set ll = `cat $file | grep "C_L " | awk '{print $2}'`
    set pl = `cat $file | grep "C_PL" | awk '{print $2}'`
    set ul = `cat $file | grep "C_UL" | awk '{print $2}'`
    
    set sd = `cat $file  | grep "SDATE" | awk '{print $2}'`

    echo "DATA:" $sd $ln $cl $ll $pl $ul
    echo $sd $ln $cl $ll $pl $ul >>& cload.dat
 
end
