Entanglement
==============

> "Spooky action at a distance"

What if an atom could take its values from another atom?

<img src="https://raw.githubusercontent.com/Frozenlock/entanglement/master/quantum-entanglement1.png"
	alt="Entanglement"/>

Same data, different representation.


```clj
 ;; Those two atoms **SHARE THE SAME DATA**
 atom-a
 #<Atom: {:first-name Mike, :last-name Moran, :age 22, :sex M}>

 atom-b
 #<Atom: [Mike 22]>

 ;; Being the same data, you can reset one...

 (reset! atom-b ["Bob" 35])
 ["Bob" 35]
  
 ;; and the other will reflect this change!
  
 atom-a
 #<Atom: {:first-name "Bob", :last-name "Moran", :age 35, :sex "M"}>
```

You can now have an abstaction layer between your state (atom) and
your program.


Usage
-----

Add this to your project dependencies:

[![Clojars Project](http://clojars.org/org.clojars.frozenlock/entanglement/latest-version.svg)](http://clojars.org/org.clojars.frozenlock/entanglement)


In your namespace declaration: `(:require [entanglement.core :refer [entangle]])`.

To re-create `atom-a` and `atom-b` from the example above:

```clj
;; Let's create the reference atom
(def atom-a (atom {:first-name "Mike" :last-name "Moran" :age 22 :sex "M"}))

;; And now we make an atom taking values from the first atom:
(def atom-b (entangle atom-a
                      ;; getter function
                      (fn [s-a]
                        [(:first-name s-a) (:age s-a)])
                      ;; setter function
                      (fn [s-a new-value]
                        (assoc s-a
                          :first-name (first new-value)
                          :age (last new-value)))
                      ;;optional validator
                      :validator
                      (fn [new-value]
                        (assert (-> new-value first string?) "First value should be a string")
                        (assert (-> new-value last number?) "Second value should be a number"))))
```						

License
-------

Copyright (c) 2015 Frozenlock

Distributed under the Eclipse Public License, the same as Clojure.
