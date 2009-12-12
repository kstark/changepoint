
import numpy
import unittest
from changepoint import changepoint

class ChangepointTest(unittest.TestCase):

    def test_changepoint(self):
        data = numpy.array([
             10.7, 13.0, 11.4, 11.5, 12.5, 14.1, 14.8, 14.1, 12.6, 16.0, 11.7, 10.6,
             10.0, 11.4,  7.9,  9.5,  8.0, 11.8, 10.5, 11.2,  9.2, 10.1, 10.4, 10.5
        ])
        cps = sorted(set(changepoint(data)))
        self.assertEquals([11.7, 10.6], list(data[cps]))

if __name__ == "__main__":
    unittest.main()
