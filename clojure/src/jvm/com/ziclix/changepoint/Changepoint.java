package com.ziclix.changepoint;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Random;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.ArrayDeque;

/**
 * An implementation of changepoint detection.
 *
 * http://www.variation.com/cpa/tech/changepoint.html
 */
public class Changepoint {

  private static class Point {
    public int offset;
    public double[] data;

    public Point(double[] data, int offset) {
      this.data = data;
      this.offset = offset;
    }

    @Override
    public String toString() {
      return String.format(
        "offset: %d, data: %s", offset, Arrays.toString(data));
    }
  }

  private static final Random random = new Random();

  /**
   * Computes the mean of an array.
   */
  public static double mean(double[] v) {
    double x = 0.0;
    for (int k = 0; k < v.length; k++) {
      x += v[k];
    }
    return x / (double) v.length;
  }

  /**
   * Return the max value.
   */
  public static double max(double[] v) {
    double x = v[0];
    for (int i = 1; i < v.length; i++) {
      x = v[i] > x ? v[i] : x;
    }
    return x;
  }

  /**
   * Return the min value.
   */
  public static double min(double[] v) {
    double x = v[0];
    for (int i = 1; i < v.length; i++) {
      x = v[i] < x ? v[i] : x;
    }
    return x;
  }

  /**
   * Return the min (element 0) and max (element 1) values.
   */
  public static double[] minmax(double[] v) {
    double[] x = new double[] { v[0], v[0] };
    for (int i = 1; i < v.length; i++) {
      final double y = v[i];
      x[0] = y < x[0] ? y : x[0];
      x[1] = y > x[1] ? y : x[1];
    }
    return x;
  }

  /**
   * Return the index of the max value.
   */
  public static int argmax(double[] v) {
    int x = 0;
    for (int i = 1; i < v.length; i++) {
      x = v[i] > v[x] ? i : x;
    }
    return x;
  }

  /**
   * Return the index of the min value.
   */
  public static int argmin(double[] v) {
    int x = 0;
    for (int i = 1; i < v.length; i++) {
      x = v[i] < v[x] ? i : x;
    }
    return x;
  }

  /**
   * Shuffle the array in-place.
   *
   * http://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle
   */
  public static double[] shuffle(double[] array) {
    return shuffle(array, new Random());
  }

  /**
   * Shuffle the array in-place.
   *
   * http://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle
   */
  public static double[] shuffle(double[] array, Random random) {
    for (int i = array.length; i > 1; i--) {
        int j = random.nextInt(i);
        double tmp = array[j];
        array[j] = array[i - 1];
        array[i - 1] = tmp;
    }
    return array;
  }

  /**
   * The difference between the max and min value in a sequence.
   */
  public static double maxdiff(double[] v) {
    double[] minmax = minmax(v);
    return minmax[1] - minmax[0];
  }

  /**
   * The cumulative sum of a sequence.
   *
   * See http://en.wikipedia.org/wiki/CUSUM for more details.
   */
  public static double[] cusum(double[] v) {
    double mean = mean(v);
    double[] m = new double[v.length + 1];
    m[0] = 0;
    for (int i = 0; i < v.length; i++) {
      m[i + 1] = m[i] + (v[i] - mean);
    }
    return m;
  }

  /**
   * A bootstrap analysis consists of performing a large number of bootstraps
   * and counting the number of bootstraps for which S0(diff) is less than
   * S(diff).
   */
  public static int bootstrap(double[] data, int iterations) {
    double[] cusum = cusum(data);
    double sdiff = maxdiff(cusum);

    int n = 0;
    for (int i = 0; i < iterations; i++) {
      double[] array = Arrays.copyOf(data, data.length);
      double diff = maxdiff(cusum(shuffle(array, random)));
      if (diff < sdiff) {
        n += 1;
      }
    }
    return n;
  }

  /**
   * A change-point analysis is performed on a series of time ordered data in
   * order to detect whether any changes have occurred.  It determines the
   * number of changes and estimates the time of each change.
   *
   * @return an array of indexes at which changes occurred
   */
  public static int[] changepoint(double[] data, double confidence) {
    return changepoint(data, confidence, 1000);
  }

  /**
   * A change-point analysis is performed on a series of time ordered data in
   * order to detect whether any changes have occurred.  It determines the
   * number of changes and estimates the time of each change.
   *
   * @param n the number of iterations
   * @return an array of indexes at which changes occurred
   */
  public static int[] changepoint(double[] data, double confidence, int n) {
    if (confidence <= 0.0 && confidence > 1.0) {
      throw new IllegalArgumentException(
        "confidence must be > 0.0 and <= 1.0");
    }

    int q = 0;
    TreeSet<Integer> xs = new TreeSet<Integer>();
    ArrayDeque<Point> deque = new ArrayDeque<Point>();
    deque.push(new Point(data, 0));
    while (!deque.isEmpty()) {
      q++;
      Point p = deque.pop();
      float b = (float) bootstrap(p.data, n);
      if ((b / n ) > confidence) {
        int mx = argmax(cusum(p.data));
        if (xs.add(mx + p.offset)) {
          if (mx > 0 && mx < p.data.length) {
            deque.push(new Point(
              Arrays.copyOfRange(p.data, 0, mx), p.offset));
            deque.push(new Point(
              Arrays.copyOfRange(p.data, mx, p.data.length), mx + p.offset - 1));
          }
        }
      }
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug(String.format(
        "data length: %d, confidence: %.2f, iterations: %d, paths: %d",
        data.length, confidence, n, q));
    }

    int i = 0;
    int[] v = new int[xs.size()];
    for (Integer x: xs) {
      v[i++] = x.intValue();
    }
    return v;
  }

  private static final Log LOG = LogFactory.getFactory().getLog(Changepoint.class);
}
