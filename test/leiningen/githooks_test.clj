(ns leiningen.githooks-test
  (:require [clojure.test :refer :all]
            [leiningen.githooks :as g]))

(deftest accepts-hooks-as-ours
  (is (re-find g/cookie g/hook-template)))

(deftest project-relative-path
  (with-redefs [g/git-root-directory (fn [] "/gitroot")]
    (is (= (g/project-directory-path {:root "/gitroot/server/"})
           "./server"))))
