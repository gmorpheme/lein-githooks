(ns ^{:doc "Plugin to manage git hooks."}
  leiningen.githooks
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clojure.data :refer (diff)]
            [clojure.java.shell :as shell]
            [leiningen.core.eval :refer (sh)]
            [leiningen.core.main :refer (info abort)]))

(defn git-root-directory
  "Locate root of git repo."
  []
  (string/trim (:out (shell/sh "git" "rev-parse" "--show-toplevel"))))

(defn hook-directory
  "Locate top-level git directory."
  [project]
  (when-let [dir (git-root-directory)]
    (io/file dir ".git" "hooks")))

(defn project-directory-path
  "Locate project.clj directory relative to git-root"
  [project]
  (when-let [dir (git-root-directory)]
    (let [git (.getCanonicalPath (io/file dir))
          lein (.getCanonicalPath (io/file (:root project)))
          relative-path (subs lein (count git))]
      (str "." relative-path))))

(defn hooks-defined
  "List the hooks that are currently defined (in .git/hooks)."
  [project]
  (->> (file-seq (hook-directory project))
       (filter #(.isFile %))
       (map #(.getName %))
       (remove #(.endsWith % ".sample"))))

(defn hooks-required
  "Retrieves the hooks required in the project map"
  [project]
  (->> (keys (:githooks project))
       (remove #{:auto-install})
       (map name)))

(defn hook-file
  "Return hook-file for given hook-type."
  [project hook-type]
  (io/file (hook-directory project) (name hook-type)))

(def cookie #".*@@lein-githooks.*")

(def hook-template "#!/bin/bash
# @@lein-githooks
# Auto-generated by lein-githooks. Do not make manual changes.
#
cd %s
lein githooks run %s
")

(defn assert-ownership
  "Abort if the hook doesn't look like one of ours..."
  [project hook-type]
  (when (not (re-find cookie (slurp (hook-file project hook-type))))
    (abort "Hook " hook-type " was not created by lein-githooks. Aborting.")))

(defn ensure-executable
  "Ensure that a file is executable."
  [hook-file]
  (.setExecutable hook-file true))

(defn delete-hook
  "Remove an existing hook."
  [project hook-type]
  {:pre [(string? hook-type)]}
  (assert-ownership project hook-type)
  (info "Deleting hook: " hook-type)
  (let [f (hook-file project hook-type)]
    (.delete f)))

(defn create-hook
  "Create hook file for given hook-type."
  [project hook-type]
  {:pre [(string? hook-type)]}
  (info "Creating hook: " hook-type)
  (let [f (hook-file project hook-type)
        content (format hook-template
                        (project-directory-path project)
                        (name hook-type))]
    (spit f content)
    (ensure-executable f)))

(defn update-hook
  "Update an existing hook to use lein-githooks. Should refuse to
  alter a hook that is not owned by lein-githooks."
  [project hook-type]
  {:pre [(string? hook-type)]}
  (assert-ownership project hook-type)
  (info "Updating hook: " hook-type)
  (let [f (hook-file project hook-type)
        content (format hook-template
                        (project-directory-path project)
                        (name hook-type))]
    (spit f content)
    (ensure-executable f)))

(defn install 
  "Updates the .git/hooks directory to contain hooks for all and only
  the hooks defined in project.clj. Will refuse to update or remove
  hooks that were not generated by lein-githooks."
  [project]
  (let [existing-hooks (hooks-defined project)
        required-hooks (hooks-required project)
        [deletes creates updates] (map (partial keep identity)
                                       (diff existing-hooks required-hooks))]
    (doseq [hook updates] (update-hook project hook))
    (doseq [hook creates] (create-hook project hook))
    (doseq [hook deletes] (delete-hook project hook))))

(defn clean
  "Removes the hooks specified in project.clj from .git/hooks
  directory. Should not generally be required. Will fail for hooks 
  not created by lein-githooks."
  [project]
  (let [project-hooks (hooks-required project)]
    (doseq [hook project-hooks] (delete-hook project hook))))

(defn run
  "Run a hook by name."
  [project hook]
  {:pre [(keyword? hook)]}
  (info "Running" (name hook) "hook.")
  (let [cmds (get-in project [:githooks hook])]
    (loop [[cmd & cmds] cmds]
      (when (seq cmd)
        (let [exit-code (apply sh (string/split cmd #"\W+"))]
          (if (zero? exit-code)
            (recur cmds)
            (abort hook "hook failed.")))))))

(defn githooks
  "`lein githooks install` will install the hooks required by
  project.clj, creating, updating and deleting hooks as appropriate.

  `lein githooks clean` will remove the hooks named in project.clj.

  `lein githooks run pre-commit` (for example) will run the pre-commit
  hooks. "
  [project & [command hook]]

  (info (resolve 'lein-githooks.plugin/hooks))
  
  (case command
    "install" (install project)
    "clean" (clean project)
    "run" (run project (keyword hook))
    (abort "Unknown subcommand")))

(defn auto-install
  "Leiningen hook to call auto-install. No-op if :auto-install is false."
  [func & [project [task & _] :as args]]
  (when (and (get-in project [:githooks :auto-install])
             (not= task "githooks"))
    (install project))
  (apply func args))
