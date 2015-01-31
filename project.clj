(defproject org.clojars.frozenlock/entanglement "0.0.2"
  :description "Spooky action at a distance (between atoms)"
  :url "https://github.com/Frozenlock/entanglement"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0-alpha4"]
                 [org.clojure/clojurescript "0.0-2356"]]
  :plugins [[lein-cljsbuild "1.0.3"]
            [com.cemerick/clojurescript.test "0.3.3"]]
  :profiles {:test {:cljsbuild
                    {:builds
                     {:client {:source-paths ^:replace
                               ["test" "src"]}}}}}
  :source-paths ["src"]
  :cljsbuild
  {:builds
   {:client {:source-paths ["src"]
             :compiler
             {
              :output-dir "target/client"
              :output-to "target/cljs-client.js"
              :pretty-print true}}}})
