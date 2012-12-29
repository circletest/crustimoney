(ns pegparser.internal.core)

(defrecord State
  [rules remainder pos current as-terminal errors errors-pos])

(defn succes
  [content new-state]
  {:succes
    {:content content
     :new-state new-state}})

(defn error
  [content {:keys [pos errors errors-pos] :as state}]
  (let [[errors errors-pos] (cond (= pos errors-pos) [(conj errors content) errors-pos]
                                  (> pos errors-pos) [#{content} pos]
                                  :else [errors errors-pos])]
    {:errors errors
     :errors-pos errors-pos}))

(declare parse-terminal)
(declare parse-nonterminal)

(defn parse-vector
  [vect {:keys [current as-terminal] :as state}]
  (loop [vect vect
         new-state state
         result [nil nil]]
    (let [v (first vect)]
      (if (or (nil? v) (= v /))
        (succes (if (second result)
                  (if (empty? (first result))
                    (second result)
                    (into [] (cons (first result) (second result))))
                  (first result))
                new-state)
        (let [[parse-fn combine-fn] (cond
                (vector? v) [parse-vector
                             #(if as-terminal
                                [nil (str (second result) %)]
                                (if (vector? %)
                                  [(first result) (into [] (concat % (second result)))]
                                  [(merge (first result) %) (second result)]))]
                (keyword? v) [parse-nonterminal
                              #(if as-terminal
                                 [{} (str (second result) %)]
                                 (if (= current v)
                                   [(first result) (if (vector? %) % (if (empty? %) [] (vector %)))]
                                   [(assoc (first result) v %) (second result)]))]
                :else [parse-terminal (fn [pr] result)])]
          (let [parse-result (parse-fn v new-state)]
            (if-let [succes (:succes parse-result)]
              (recur (rest vect) (:new-state succes) (combine-fn (:content succes)))
              (let [next-choice (drop-while #(not (= / %)) vect)]
                (if (empty? next-choice)
                  parse-result
                  (recur (rest next-choice) (merge state parse-result) [nil nil]))))))))))

(defn regex? [v]
  (instance? java.util.regex.Pattern v))

(defn parse-terminal
  [expression {:keys [remainder pos] :as state}]
  (let [[result match-type-str] (cond
          (char? expression) [(when (= expression (first remainder)) (str expression)) "character"]
          (string? expression) [(when (.startsWith remainder expression) expression) "string"]
          (regex? expression) [(re-find (re-pattern (str "^" (.pattern expression))) remainder)
                               "a character sequence that matches"]
          :else (throw (Exception. (format "An instance of %s is not a valid parsing expression."
                                           (class expression)))))]
    (if result
      (succes result (assoc state :remainder (subs remainder (count result))
                                  :pos (+ pos (count result))))
      (error (format "expected %s '%s'" match-type-str expression) state))))

(defn parse-nonterminal
  [nonterminal {:keys [rules as-terminal] :as state}]
  (let [expression (or (rules nonterminal)
                       (rules (keyword (str (name nonterminal) \-))))
        as-terminal (or as-terminal (rules (keyword (str (name nonterminal) \-))))]
    (if (vector? expression)
      (parse-vector expression (assoc state :current nonterminal
                                            :as-terminal as-terminal))
      (parse-terminal expression state))))