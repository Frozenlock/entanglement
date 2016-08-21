(ns entanglement.core)

(deftype Entangled [source-atom meta validator watches
                    getter setter derefer identifier]
  Object
  (equiv [this other]
    (-equiv this other))
  
  IAtom

  IEquiv
  (-equiv [this other]
    (and (instance? Entangled other)
         (or (= identifier (.-identifier other))
             (= identical? this other))
         (= source-atom (.-source-atom other))))

  IDeref
  (-deref [this]
    (let [derefer (or derefer (fn [_ a g]
                                (g (-deref a))))]
      (derefer this source-atom getter)))

  IMeta
  (-meta [_] meta)

  IWithMeta
  (-with-meta [o meta]
    (Entangled. source-atom meta validator watches
                getter setter derefer identifier))
  
  ;; Every watch is added to the source atom. This way, every time a
  ;; source atom is modified, it will ripple thought all the entangled
  ;; children.

  ;; Might cause memory leaks? (If we add-watch an entangled atom, it is
  ;; now referenced in the source atom, meaning it shouldn't be
  ;; GCed...)
   IWatchable
  (-add-watch [this key f]
    (add-watch source-atom [(or identifier this) key]
               (fn [_ _ oldval newval]
                 (when-not (= oldval newval) ;; the getter fn can be
                                             ;; expensive, better to
                                             ;; avoid it if we can.
                   (let [old (getter oldval)
                         new (getter newval)]
                     (when-not (= old new)
                       (f key this old new))))))
               this)
  
  (-remove-watch [this key]
    (remove-watch source-atom [(or identifier this) key])
    this)


  ;; possibility of creating a read-only atom by omitting a setter
  ;; function
  
  IReset
  (-reset! [a new-value]
    (if setter
      (let [validate (.-validator a)]
        (when-not (nil? validate)
          (assert (validate new-value) "Validator rejected reference state"))
        (do (swap! source-atom setter new-value)
            new-value))
      (throw (js/Error. "Read-only: no setter provided for this atom."))))

  ISwap
  (-swap! [a f]
    (reset! a (f (-deref a))))
  (-swap! [a f x]
    (reset! a (f (-deref a) x)))
  (-swap! [a f x y]
    (reset! a (f (-deref a) x y)))
  (-swap! [a f x y more]
    (reset! a (apply f (-deref a) x y more)))

  IPrintWithWriter
  (-pr-writer [a writer opts]
    (-write writer "#<Atom: ")
    (pr-writer (-deref a) writer opts) ;; the current value
    (-write writer ">"))

  IHash
  (-hash [this] (goog/getUid this)))



;;;;; Main API

(defn entangle
  "Return an atom which applies custom getter and setter to the
  source-atom for every lookup/update/watches.
  
  getter:              (fn [derefed-source-atom] ...)
  setter [optional]:   (fn [derefed-source-atom new-value]...)

  derefer [optional]:  (fn [this(new-atom) source-atom getter] ....)

  identifier [optional]: :some-id -- more info below
  
  When creating delicate entanglement (when the datastructure between
  the source atom and the new atom are quite different), it is
  suggested to provide a validator function.

  The validator and meta arguments act the same way as for normal
  atoms.

  A 'read-only' atom can be created simply by omitting or passing nil
  as the setter argument. Any attempt to modify directly the returned
  atom will result in an error.
  
  Because we can't test for equality between functions (getter and
  setter), basic entangled atoms can't test for equality. This is
  especially important when adding watches, as they need to test for
  equality for potential duplicates. To avoid this problem, it's
  possible to provide a 'identifier' field which contains the object
  on which the equality should be tested."
  ([source-atom getter] (entangle source-atom getter nil))
  ([source-atom getter setter & {:keys [meta validator derefer identifier]}]
   (assert (satisfies? IAtom source-atom) "Only atoms can be entangled.")
   (Entangled. source-atom meta validator nil getter setter derefer identifier)))


;;; Simple cursor implementation using entanglement

(defn cursor
  "Create a cursor. Behaves like a normal atom for the value at the
  specified path."
  [a path]  
  (if-not (seq path) ;; if the path is emtpy, just return the atom...
    a
    (entangle a
              #(get-in % path)
              #(assoc-in %1 path %2)
              :identifier [::cursor a path]))) ;; <- identifier to test for equality

