#!/usr/bin/gnuplot -p

data = "FixedGenerationTermination.dat"

set terminal svg size 700,400 enhanced fname "Times Roman" fsize 11
set output "FixedGenerationTermination.svg"

set grid

set logscale x
set autoscale y
set ylabel "Fitness"
set xlabel "Generation" font ",12"
set bmargin
set origin 0.0, 0.0
set tmargin 1
set format x "10^{%L}"
set format y " %2.1f"

plot data using 1:(0.001*$16):(0.001*$18):(0.001*$19):(0.001*$17) with candlesticks lt rgb "red" notitle axes x1y1 whiskerbars, \
     data using 1:(0.001*$15):(0.001*$15):(0.001*$15):(0.001*$15) with candlesticks lt rgb "red" lw 2 notitle axes x1y1, \
     data using 1:(0.001*$11) with lines notitle lt rgb "red" axes x1y1
