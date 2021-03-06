(ns ^{:doc "Support for a simple i18n scheme."
      :author "Armando Blancas, Arnout Roemers"}
  crustimoney.i18n)


;;; Language definitions.

(def ^{:doc "English language - the default."}
  lang-en
  {:expected-eof          "expected EOF"
   :char-terminal         "character"
   :string-terminal       "string"
   :regex-terminal        "a character sequence that matches"
   :expected-terminal     "expected %s '%s'"
   :invalid-parsing-expr  "An instance of %s is not a valid parsing expression."})

(def ^{:doc "Dutch language."}
  lang-nl
  {:expected-eof          "verwachtte einde van tekst"
   :char-terminal         "teken"
   :string-terminal       "tekenreeks"
   :regex-terminal        "een tekenreeks dat overeenkomt met"
   :expected-terminal     "verwachtte %s '%s'"
   :invalid-parsing-expr  "Een instantie van %s is niet een geldige expressie."})


;;; Core functions.

(def ^:private text (atom lang-en))


(defn i18n-merge
  "Merges m into the text map for customization."
  [m] (swap! text merge m))


(defn i18n
  "Gets or formats the value for the supplied key."
  ([k] (k (deref text)))
  ([k & more] (apply format (i18n k) more)))


(defn di18n
  "Returns a Delay instance with the (formatted) value for the supplied key.
   Useful in (def)'ed expressions that evaluate too soon."
  ([k] (delay (k (deref text))))
  ([k & more] (delay (apply format (i18n k) more))))