(ns changepoint.core
  "An implementation of changepoint detection.

  Changepoint detection is useful for finding changes in a time-series
  sequence.  For example, with a series of weather measurements it would
  be possible to find the points in the series with either abrupt changes
  in the preceeding measurements and/or a rogue or errorneous value.

  See http://www.variation.com/cpa/tech/changepoint.html for details."
  (:use
    [clojure.contrib.seq :only [indexed]])
  (:import
    com.ziclix.changepoint.Changepoint))

(defn mean
  "Find the mean of the sequence."
  [coll]
  (when (seq coll)
    (float (/ (reduce + coll) (count coll)))))

(defn argmax
  "Find the index of the max value in the sequence."
  [coll]
  (first (reduce (fn [x y] (max-key second x y)) (indexed coll))))

(defn cusum
  "Find the cusum of a time series."
  [coll]
  (loop [coll (seq coll)
         acc (vector 0)
         prior 0
         avg (mean coll)]
    (if (empty? coll)
      acc
      (let [v (+ prior (- (first coll) avg))]
        (recur (rest coll) (conj acc v) v avg)))))

(defn bootstrap
  "A bootstrap analysis consists of performing a large number of bootstraps
   and counting the number of bootstraps for which S0(diff) is less than
   S(diff)."
  [coll iterations]
  (let [diff (fn [x] (let [y (cusum x)] (- (reduce max y) (reduce min y))))
        sdiff (diff coll)]
    (reduce +
      (repeatedly iterations
        #(if (< (diff (shuffle coll)) sdiff) 1 0)))))

(defn changepoint-seq
  "Changepoint detection implemented in Clojure.

  Slightly less performant than `changepoint` but easier to modify.

  user=> (time (dotimes [i 10] (changepoint-seq small 0.90 (* (inc i) 10))))
  \"Elapsed time: 36.587 msecs\"

  The return value is an ordered set of indexes at which changepoints occurred."
  ([coll confidence]
    (changepoint-seq coll confidence 1000))
  ([coll confidence iterations]
    (let [v (apply conj (vector-of :double) coll)
          q (fn q [coll confidence iterations offset]
              (let [b (bootstrap coll iterations)
                    p (float (/ b iterations))]
                (when (> p confidence)
                  (let [mx (argmax (cusum coll))]
                    (lazy-seq
                      (cons
                        (+ mx offset)
                        (when (and (> mx 0) (< mx (count coll)))
                          (lazy-cat
                            (q (subvec coll 0 mx) confidence iterations offset)
                            (q (subvec coll mx) confidence iterations (dec (+ mx offset)))))))))))]
      (apply sorted-set (q v confidence iterations 0)))))

(defn changepoint
  "Changepoint detection implemented in Java.

  Slightly more performant than `changepoint-seq`.

  user=> (time (dotimes [i 10] (changepoint small 0.90 (* (inc i) 10))))
  \"Elapsed time: 6.245 msecs\"

  The return value is an ordered set of indexes at which changepoints occurred."
  ([coll confidence]
    (changepoint coll confidence 1000))
  ([coll confidence iterations]
    (let [data (into-array Double/TYPE coll)
          xs (Changepoint/changepoint data confidence iterations)]
      (apply sorted-set xs))))
