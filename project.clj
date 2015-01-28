(defproject lein-githooks "0.1.0"
  :description "Leiningen plugin for managing git client hooks"
  :url "http://www.github.com/curvelogic/lein-githooks"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :eval-in-leiningen true

  :profiles {:dev {:githooks {:pre-push ["lein test"]}}})
