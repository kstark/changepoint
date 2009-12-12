"""
http://www.variation.com/cpa/tech/changepoint.html
"""
from __future__ import division

import numpy

def cusum(data):
    avg = numpy.average(data)
    m = numpy.zeros(len(data) + 1, dtype=float)
    m[0] = 0
    for i in range(len(data)):
        m[i+1] = m[i] + (data[i] - avg)
    return m

def bootstrap(data, iterations):
    c = cusum(data)
    sdiff = c.max() - c.min()

    def shuffled(x):
        y = numpy.array(x)
        numpy.random.shuffle(y)
        return y

    n = 0
    for i in range(iterations):
        b = cusum(shuffled(data))
        bdiff = b.max() - b.min()
        n += (1 if bdiff < sdiff else 0)
    return n

def changepoint(data, confidence=90, iterations=1000, offset=0):
    x = bootstrap(data, iterations)
    p = (x/iterations) * 100.0
    if p > confidence:
        c = cusum(data)
        mx = c.argmax()
        yield mx + offset
        for x in changepoint(data[:mx], confidence, iterations, offset):
            yield x
        for x in changepoint(data[mx:], confidence, iterations, offset+mx-1):
            yield x
