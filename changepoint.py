
"""
The implementation borrows heavily from the algorithm documented at
http://www.variation.com/cpa/tech/changepoint.html.  A more detailed
paper is available at http://www.inference.phy.cam.ac.uk/rpa23/papers/rpa-changepoint.pdf.

The basic idea is to find abrupt variations in the generative parameters
of a data sequence.
"""
from __future__ import division

import numpy

def cusum(data, data_avg_series):
    m = numpy.zeros(len(data) + 1, dtype=float)
    m[1:] = data.cumsum() - data_avg_series
    return m

def bootstrap(data, iterations):
    data_avg_series = numpy.average(data) * numpy.arange(1, len(data) + 1)
    c = cusum(data, data_avg_series)
    sdiff = c.max() - c.min()

    n = 0
    data_shuffled = numpy.array(data)
    for i in range(iterations):
        numpy.random.shuffle(data_shuffled)
        b = cusum(data_shuffled, data_avg_series)
        bdiff = b.max() - b.min()
        n += int(bdiff < sdiff)
    return c, n

def changepoint(data, confidence=95., iterations=1000):
    stack = [(data, 0)]
    while stack:
        data, offset = stack.pop()
        if offset < 0:
            continue
        c, x = bootstrap(data, iterations)
        p = (x/iterations) * 100.0
        if p > confidence:
            mx = numpy.abs(c).argmax()
            yield mx + offset
            stack.append((data[:mx], offset))
            stack.append((data[mx:], offset+mx-1))
