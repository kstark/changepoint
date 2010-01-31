(ns changepoint
  (:gen-class
   :main false
   :methods [#^{:static true} [changepoint [java.util.List float int] java.util.List]]))

(defn mean
  [coll]
  (float (/ (reduce + coll) (count coll))))

(defn enumerate
  "Returns a lazy sequence of [index item] pairs until the
  coll is exhausted."
  ([coll] (enumerate 0 coll))
  ([n coll]
    (lazy-seq
     (when-let [s (seq coll)]
      (cons [n (first s)] (enumerate (inc n) (rest s)))))))

(defn argmax
  [coll]
  (first (reduce (fn [x y] (max-key second x y)) (enumerate coll))))

(defn shuffle
  "Return a random permutation of coll."
  [coll]
  (let [l (java.util.ArrayList. coll)]
    (java.util.Collections/shuffle l)
    (seq l)))

(defn cusum
  ([coll]
    (cusum coll [0] 0 (if (empty? coll) nil (mean coll))))

  ([coll acc prior avg]
    (if (nil? (first coll))
      acc
      (let [v (+ prior (- (first coll) avg))]
      (recur (next coll) (concat acc [v]) v avg)))))

(defn bootstrap
  [coll iterations]
  (defn diff [x] (let [b (cusum x)] (- (reduce max b) (reduce min b))))
  (let [sdiff (diff coll)]
    (reduce + (take iterations (repeatedly #(if (< (diff (shuffle coll)) sdiff) 1 0))))))

(defn changepoint
  ([coll confidence iterations]
  (changepoint coll confidence iterations 0))

  ([coll confidence iterations offset]
  (let [p (float (* 100.0 (/ (bootstrap coll iterations) iterations)))]
    (if (> p confidence)
      (let [mx (argmax (cusum coll))]
      (apply concat [(+ mx offset)] [(changepoint (take mx coll) confidence iterations offset) (changepoint (drop mx coll) confidence iterations (dec offset))]))))))
