(defproject lein-githooks "0.1.1-SNAPSHOT"
  :description "Leiningen plugin for managing git client hooks"
  :url "http://github.com/gmorpheme/lein-githooks"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :eval-in-leiningen true

  :signing {:gpg-key "github@gmorpheme.net"}
  
  :profiles {:dev {:plugins [[lein-githooks "0.1.0"]]
                   :githooks {:pre-push ["lein test"]}}})
