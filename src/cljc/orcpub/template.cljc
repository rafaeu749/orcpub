(ns orcpub.template
  (:require [clojure.spec :as spec]
            [orcpub.modifiers :as modifiers]))

(spec/def ::name string?)
(spec/def ::key keyword?)
(spec/def ::min (spec/nilable (spec/int-in 0 100)))
(spec/def ::max (spec/nilable (spec/int-in 1 100)))
(spec/def ::attribute (spec/keys :req [::name ::key]))
(spec/def ::attributes (spec/+ ::attribute))
(spec/def ::derived-value (spec/or :func (spec/fspec :args (spec/cat :entity map?))
                                   :keyword keyword?))
(spec/def ::derived-attribute (spec/keys :req [::name ::key ::derived-value]))
(spec/def ::derived-attributes (spec/+ ::derived-attribute))
(spec/def ::modifiers (spec/+ ::modifiers/modifier))
(spec/def ::option (spec/keys :req [::name ::key]
                              :opt [::modifiers ::selections]))
(spec/def ::options (spec/+ ::option))
(spec/def ::selection (spec/keys :req [::name ::key ::options]
                                 :opt [::min ::max]))
(spec/def ::selections (spec/* ::selection))
(spec/def ::template (spec/keys :opt [::attributes ::derived-attributes ::selections]))

(spec/def ::modifier-map-value (spec/or :modifiers ::modifiers
                                        :modifier-map ::modifier-map))
(spec/def ::modifier-map-entry (spec/tuple keyword? ::modifier-map-value))
(spec/def ::modifier-map (spec/map-of keyword? ::modifier-map-value))

(defn name-to-kw [name]
  (-> name
      clojure.string/lower-case
      (clojure.string/replace #"\W" "-")
      keyword))

(defn selection
  ([name options]
   (selection name options 1 1))
  ([name options min max &[sequential?]]
   {::name name
    ::key (name-to-kw name)
    ::options options
    ::min min
    ::max max
    ::sequential? (boolean sequential?)}))

(defn selection? [name options]
  (selection name options 0 1))

(defn selection+ [name options]
  (selection name options 1 nil))

(defn sequential-selection [name options]
  (selection name options 1 nil true))

(defn option [name & [selections modifiers]]
  (cond-> {::name name
           ::key (name-to-kw name)}
    selections (assoc ::selections selections)
    modifiers (assoc ::modifiers modifiers)))

(declare make-modifier-map-from-selections)

(defn make-modifier-map-entry-from-option [option]
  [(::key option)
   (let [modifiers option
         selections (::selections option)]
     (if selections
       (merge (make-modifier-map-from-selections (::selections option)) modifiers)
       modifiers))])

(spec/fdef
 make-modifier-map-entry-from-option
 :args ::option
 :ret ::modifier-map-entry)

(defn make-modifier-map-entry-from-selection [selection]
  [(::key selection)
   (into (select-keys selection [::min ::max])
         (map make-modifier-map-entry-from-option (::options selection)))])

(spec/fdef
 make-modifier-map-entry-from-selection
 :args ::selection
 :ret ::modifier-map-entry)

(defn make-modifier-map-from-selections [selections]
  (into {} (map make-modifier-map-entry-from-selection selections)))

(spec/fdef
 make-modifier-map-entry-from-selections
 :args ::selections
 :ret ::modifier-map)

(defn make-modifier-map [template]
  (make-modifier-map-from-selections (::selections template)))

(spec/fdef
 make-modifier-map
 :args ::template
 :ret ::modifier-map)

(spec/fdef make-modifier-map
           :args (spec/cat :template ::template)
           :ret ::modifier-map)