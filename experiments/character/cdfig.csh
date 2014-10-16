#!/bin/csh

set ii = 0
foreach file (cdprofile*.dat)

    set sd   = `cat $file | grep "SDAT"    | awk '{print $2}'`
    set ed   = `cat $file | grep "EDAT"    | awk '{print $2}'`

    set cdat = `date -d "${sd}" -u "+%Y%m%d"`

    set fig = "fig:cd_${cdat}"

    set eps = "figures/cdplot_${cdat}.eps"
       
    if ($ii == 0) then
	echo "\clearpage"
	echo "\begin{figure}[h]"
	echo "\begin{center}"
    endif
    echo "\subfigure[Demand profiles for night ${sd} - ${ed}]{"
    echo "   \label{${fig}}"
    echo "   \includegraphics[scale=0.25, angle=-90]{${eps}}"
    echo " }"

    if ($ii == 5) then
	echo "\end{center}"
	echo "\end{figure}"
	set ii = 0
    else
	@ ii++
    endif

end
