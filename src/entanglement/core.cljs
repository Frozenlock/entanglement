(ns entanglement.core)

(deftype Entangled [source-atom meta validator watches
                    getter setter derefer]
  Object
  (equiv [this other]
    (-equiv this other))
  
  IAtom

  IEquiv
  (-equiv [o other] (identical? o other))

  IDeref
  (-deref [this]
    (let [derefer (or derefer (fn [_ a g]
                                (g (-deref a))))]
      (derefer this source-atom getter)))

  IMeta
  (-meta [_] meta)

  
  ;; Every watch is added to the source atom. This way, every time a
  ;; source atom is modified, it will ripple thought all the entangled
  ;; children.

  ;; Might cause memory leaks?
   IWatchable
  (-add-watch [this key f]
    (add-watch source-atom [this key]
               (fn [_ _ oldval newval]
                 (when-not (= oldval newval) ;; the getter fn can be
                                             ;; expensive, better to
                                             ;; avoid it if we can.
                   (let [old (getter oldval)
                         new (getter newval)]
                     ;; We don't test for new equality here because
                     ;; any watch function can be called even if
                     ;; oldval and newval are identical. As per
                     ;; 'add-watch' doc: 'Whenever the reference's
                     ;; state *might* have been changed (...)'.
                     (f key this old new)))))
               this)
  
  (-remove-watch [this key]
    (remove-watch source-atom [this key])
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


(defn entangle
  "Return an atom which applies custom getter and setter to the
  source-atom for every lookup/update/watches.
  
  getter:              (fn [derefed-source-atom] ...)
  setter [optional]:   (fn [derefed-source-atom new-value]...)

  derefer [optional]:  (fn [this(new-atom) source-atom getter] ....)
  
  When creating delicate entanglement (when the datastructure between
  the source atom and the new atom are quite different), it is
  suggested to provide a validator function.

  The validator and meta arguments act the same way as for normal
  atoms.

  A 'read-only' atom can be created simply by passing nil as the
  setter argument. Any attempt to modify directly the returned atom
  will result in an error."
  ([source-atom getter] (entangle source-atom getter nil))
  ([source-atom getter setter & {:keys [meta validator derefer]}]
   (assert (satisfies? IAtom source-atom) "Only atoms can be entangled.")
   (Entangled. source-atom meta validator nil getter setter derefer)))


;;; Simple cursor implementation using entanglement

(defn cursor
  "Create a cursor. Behaves like a normal atom for the value at the
  specified path."
  [a path]  
  (if-not (seq path) ;; if the path is emtpy, just return the atom...
    a
    (entangle a
              #(get-in % path)
              #(assoc-in %1 path %2))))

