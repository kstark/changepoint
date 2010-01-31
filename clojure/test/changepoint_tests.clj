(ns changepoint-tests
  (:use clojure.test)
  (:use changepoint))

(def data [
  10.7 13.0 11.4 11.5 12.5 14.1 14.8 14.1
  12.6 16.0 11.7 10.6 10.0 11.4 7.9 9.5
  8.0 11.8 10.5 11.2 9.2 10.1 10.4 10.5])

(deftest- Changepoint
  (is (= #{10 11} (set (changepoint data 90.0 1000)))))
