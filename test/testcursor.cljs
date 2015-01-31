(ns testcursor
  (:require-macros [cemerick.cljs.test
                    :refer (is deftest with-test run-tests testing)])
  (:require [cemerick.cljs.test :as t]
            [entanglement.core :as ent]))

;; Here we test if the cursors act as a normal atom

(deftest values
  (let [test-atom (atom {:a {:b {:c {:d 1}}}})
        test-cursor (ent/cursor test-atom [:a :b :c :d])
        test-cursor2 (ent/cursor test-atom [])] ;; nasty edge case

    (is (= cljs.core/Atom (type test-cursor2)))
    
    ;; get the initial values
    (is (= (get-in @test-atom [:a :b :c :d])
           @test-cursor))

    (is (= (get-in @test-atom [])
           @test-cursor2))
    
    ;; now we update the cursor with a reset    
    (reset! test-cursor 2)
    (is (= @test-cursor 2))
    (is (= (get-in @test-atom [:a :b :c :d]) 2))    
   
    (reset! test-cursor2 3)
    (is (= @test-cursor2 3))
    (is (= @test-atom 3))
    (reset! test-atom {:a {:b {:c {:d 1}}}}) ;; restore test-atom

    ;; swap
    (reset! test-cursor {}) ;; empty map
    (swap! test-cursor assoc :z 3)
    (is (= @test-cursor {:z 3}))
    (is (= (get-in @test-atom [:a :b :c :d])
           {:z 3}))

    (reset! test-cursor2 {}) ;; empty map
    (swap! test-cursor2 assoc :z 3)
    (is (= @test-cursor2 {:z 3}))
    (is (= (get-in @test-atom [])
           {:z 3}))))


(deftest atom-behaviors
  (let [test-atom (atom {:a {:b {:c {:d 1}}}})
        test-cursor (ent/cursor test-atom [:a :b :c :d])
        witness (atom nil)]
    ;; per the description, reset! should return the new values
    (is (= {}
           (reset! test-cursor {})))
    
    ;; per the description, swap! should return the new values
    (is (= {:z [1 2 3]}
           (swap! test-cursor assoc :z [1 2 3])))

    ;; watches should behave like with a normal atom
    (reset! test-cursor "old")
    (add-watch test-cursor :w #(reset! witness {:key %1 :ref %2 :old %3 :new %4}))
    (reset! test-cursor "new") ;; this should trigger the watch function
    (is (= (:key @witness) :w))
    (is (= (:ref @witness) test-cursor))
    (is (= (:old @witness) "old"))
    (is (= (:new @witness) "new"))
    ;; can we remove the watch?
    (remove-watch test-cursor :w)
    (reset! test-cursor "removed")
    (is (= (:new @witness) "new")) ;; shouldn't have changed
    ))
