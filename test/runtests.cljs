(ns runtests
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test run-tests testing)])
  (:require [testcursor]
            [cemerick.cljs.test :as t]))

(enable-console-print!)

(def test-results (atom nil))

(defn ^:export run-all-tests []
  (println "-----------------------------------------")
  (try
    (reset! test-results (t/run-all-tests))
    (catch js/Object e
      (do
        (println "Testrun failed\n" e "\n" (.-stack e))
        (reset! test-results {:error e}))))
  (println "-----------------------------------------"))

