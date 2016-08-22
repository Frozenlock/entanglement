Entanglement
==============

> "Spooky action at a distance" -Einstein

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
                      ;; optional setter function
                      (fn [s-a new-value]
                        (assoc s-a
                          :first-name (first new-value)
                          :age (last new-value)))
                      ;;optional validator
                      :validator
                      (fn [new-value]
                        (assert (-> new-value first string?) "First value should be a string")
                        (assert (-> new-value last number?) "Second value should be a number"))
					  ;;optional identifier to avoid duplicate watches (see docstring)
					  :identifier
					  :my-entangled-atom))
```						

Rationale
-------

An atom is a nice state abstraction. You have this 'thing' that holds
data and with which you must thread carefully. It has a limited set of
functions to play with it (`deref`,`reset!`, `swap!`,
`add-watch`/`remove-watch`). Simple, yet functional.

The problem is that the atom does not necessarily match the simplest
code architecture.

If you build your code to reflect how the data is stored in the atom,
you are adding complexity. The more detached the atom structure is
from the code logic, the more you have to juggle the data around. It
also means that your code becomes intertwined with this particular
atom.

Before you know it, your entire code base is dependant on one or more
atoms having a particular structure. So long for reusable functions.

`Entanglement` proproses to create atoms from other atoms and linking
the data together. It lets you build an 'interface' that presents
the data like you want it. Build your code in the simplest way
possible, assuming the atom will match what you want.

```clj
;; what was...
(swap! my-atom update-in [:some :path :that :might :be :quite :deep] my-fn)

;; ...can now become

(swap! my-atom my-fn)
```

"Wait, this looks a lot like cursors?"

Right, because it is! In fact, cursors are a subset of entangled atoms.
Here is an implement of cursors with `entanglement`:
```clj
(defn cursor
  "Create a cursor. Behaves like a normal atom for the value at the
  specified path."
  [a path]  
  (if-not (seq path) ;; if the path is emtpy, just return the atom...
    a
    (entangle a
              #(get-in % path)
              #(assoc-in %1 path %2)
              :identifier [::cursor a path])))
```
			  
(Cursors are such a common case that we already provide the `cursor`
function in `entanglement.core`.)

Cursors, lenses, wraps... they are all symptomatic of the need to
detach atom data structure from the code.

Every atom made with `entanglement` is 100% opaque. From the functions
point of view, it's just like any other atom.


Warning
-------

Watches are not added to entangled atoms, but rather to the source
atoms. This means that one should be careful when adding watches
because *they won't be automatically GCed* even if the entangled atoms
are.


License
-------

Copyright (c) 2015 Frozenlock

Distributed under the Eclipse Public License, the same as Clojure.
