(ns reference-client.common-errors
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [aero.core :as aero]
            [clojure.java.io :as io]))

(def config
  (aero/read-config (io/resource "config.edn")))

(def base-url (get-in config [:api :base-url]))
(def api-key (get-in config [:api :api-key]))

;; =============================================================================
;; Authentication Errors
;; =============================================================================

(defn test-wrong-api-key
  "Tests authentication failure with an invalid API key."
  []
  (http/post (str base-url "/api/v1/accounts")
             {:headers {"Authorization" "APIKey wrong-key-12345"
                        "Content-Type" "application/json"}
              :body (json/generate-string {:currency "USD"})
              :throw-exceptions false
              :as :json}))

;; Response:
;; {:status 403
;;  :reason-phrase "Forbidden"
;;  :body "{\"message\":\"Forbidden\"}"}

;; =============================================================================
;; Validation Errors
;; =============================================================================

(defn test-missing-currency
  "Tests validation error when currency field is missing."
  []
  (http/post (str base-url "/api/v1/accounts")
             {:headers {"Authorization" (str "APIKey " api-key)
                        "Content-Type" "application/json"}
              :body (json/generate-string {})
              :throw-exceptions false
              :as :json}))

;; Response:
;; {:status 400
;;  :reason-phrase "Bad Request"
;;  :body "{\"message\": \"Missing required field: currency\"}"}

(defn test-unsupported-currency
  "Tests validation error with unsupported currency codes.
  Tested with: JPY (Japanese Yen), CAD (Canadian Dollar), XXX (invalid).
  API treats all non-USD/EUR/GBP currencies the same way."
  [currency-code]
  (http/post (str base-url "/api/v1/accounts")
             {:headers {"Authorization" (str "APIKey " api-key)
                        "Content-Type" "application/json"}
              :body (json/generate-string {:currency currency-code})
              :throw-exceptions false
              :as :json}))

;; Response (same for JPY, CAD, XXX, or any non-supported currency):
;; {:status 400
;;  :reason-phrase "Bad Request"
;;  :body "{\"message\": \"Invalid currency. Allowed values: USD, EUR, GBP\"}"}

(defn test-invalid-amount-format
  "Tests validation error with malformed payment amount.
  NOTE: API does NOT validate amount format - accepts non-numeric values!"
  [account-id]
  (http/post (str base-url "/api/v1/accounts/" account-id "/payments")
             {:headers {"Authorization" (str "APIKey " api-key)
                        "Content-Type" "application/json"}
              :body (json/generate-string {:amount {:value "not-a-number"
                                                    :currency "USD"}})
              :throw-exceptions false
              :as :json}))

;; Response:
;; {:status 200
;;  :reason-phrase "OK"
;;  :body {:id "50a56456-bc23-7b3d-b354-c08763547fd2"
;;         :type "payment"
;;         :amount {:value "not-a-number", :currency "USD"}
;;         :status "pending"}}
;;
;; IMPORTANT: The API accepts non-numeric amount values without validation!
;; This may be a bug or intentional design decision. Document this behavior.

(defn test-missing-amount
  "Tests validation error when amount is missing from payment."
  [account-id]
  (http/post (str base-url "/api/v1/accounts/" account-id "/payments")
             {:headers {"Authorization" (str "APIKey " api-key)
                        "Content-Type" "application/json"}
              :body (json/generate-string {})
              :throw-exceptions false
              :as :json}))

;; Response:
;; {:status 400
;;  :reason-phrase "Bad Request"
;;  :body "{\"message\": \"Missing required field: amount.value\"}"}

;; =============================================================================
;; Rate Limiting Tests
;; =============================================================================

(defn create-test-accounts
  "Creates n test accounts sequentially and returns their IDs."
  [n]
  (println (str "Creating " n " test accounts..."))
  (let [accounts (for [i (range n)]
                   (do
                     (when (zero? (mod i 5))
                       (println (str "  Created " i " accounts...")))
                     (try
                       (let [response (http/post (str base-url "/api/v1/accounts")
                                                 {:headers {"Authorization" (str "APIKey " api-key)
                                                            "Content-Type" "application/json"}
                                                  :body (json/generate-string {:currency "USD"})
                                                  :throw-exceptions false
                                                  :as :json})]
                         {:id (get-in response [:body :id])
                          :status (:status response)})
                       (catch Exception e
                         {:error (.getMessage e)}))))]
    (println (str "Created " (count (filter :id accounts)) " accounts successfully"))
    (filter :id accounts)))

(defn save-test-accounts!
  "Saves test account IDs to resources/test_accounts.edn."
  [accounts]
  (let [data {:accounts (mapv :id accounts)
              :created-at (str (java.time.Instant/now))
              :count (count accounts)}]
    (spit "resources/test_accounts.edn" (pr-str data))
    (println (str "Saved " (count accounts) " account IDs to resources/test_accounts.edn"))
    data))

(defn load-test-accounts
  "Loads test account IDs from resources/test_accounts.edn."
  []
  (when (.exists (io/file "resources/test_accounts.edn"))
    (read-string (slurp "resources/test_accounts.edn"))))

(defn blast-server
  "Attempts to trigger rate limiting with n concurrent requests.
  Uses a mix of different API operations (get balance, list transactions, create payments)."
  [n test-accounts]
  (println (str "\nBlasting server with " n " concurrent requests..."))
  (let [start-time (System/currentTimeMillis)
        account-ids (:accounts test-accounts)
        ;; Create a mix of different request types
        requests (take n (cycle
                          [(fn [i] ;; Get balance
                             {:type :get-balance
                              :account-id (nth account-ids (mod i (count account-ids)))
                              :request-num i})
                           (fn [i] ;; List transactions
                             {:type :list-transactions
                              :account-id (nth account-ids (mod i (count account-ids)))
                              :request-num i})
                           (fn [i] ;; Create payment
                             {:type :create-payment
                              :account-id (nth account-ids (mod i (count account-ids)))
                              :request-num i})]))
        ;; Execute all requests concurrently using futures
        futures (doall
                 (map-indexed
                  (fn [i req-fn]
                    (future
                      (try
                        (let [req (req-fn i)
                              response (case (:type req)
                                         :get-balance
                                         (http/get (str base-url "/api/v1/accounts/" (:account-id req) "/balance")
                                                   {:headers {"Authorization" (str "APIKey " api-key)}
                                                    :throw-exceptions false})

                                         :list-transactions
                                         (http/get (str base-url "/api/v1/accounts/" (:account-id req) "/transactions")
                                                   {:headers {"Authorization" (str "APIKey " api-key)}
                                                    :throw-exceptions false})

                                         :create-payment
                                         (http/post (str base-url "/api/v1/accounts/" (:account-id req) "/payments")
                                                    {:headers {"Authorization" (str "APIKey " api-key)
                                                               "Content-Type" "application/json"}
                                                     :body (json/generate-string {:amount {:value "1.00" :currency "USD"}})
                                                     :throw-exceptions false}))]
                          (merge req {:status (:status response)
                                      :time (System/currentTimeMillis)}))
                        (catch Exception e
                          {:request-num i
                           :error (.getMessage e)
                           :time (System/currentTimeMillis)}))))
                  requests))
        ;; Wait for all futures to complete
        results (doall (map deref futures))
        end-time (System/currentTimeMillis)]

    ;; Analyze results
    (let [duration-ms (- end-time start-time)
          rate-limited (filter #(= 429 (:status %)) results)
          successful (filter #(and (:status %) (< (:status %) 400)) results)
          errors (filter #(or (:error %) (>= (:status % 0) 400)) results)]

      (println (str "Completed in " duration-ms "ms"))
      (println (str "  Successful: " (count successful)))
      (println (str "  Rate limited (429): " (count rate-limited)))
      (println (str "  Other errors: " (count (remove #(= 429 (:status %)) errors))))

      {:total-requests n
       :duration-ms duration-ms
       :requests-per-second (/ n (/ duration-ms 1000.0))
       :successful (count successful)
       :rate-limited (count rate-limited)
       :errors (count errors)
       :rate-limited? (pos? (count rate-limited))
       :sample-rate-limit-response (first rate-limited)
       :results results})))

;; Rate Limiting Test Results (2026-01-04):
;;
;; Tested exponentially increasing concurrent request loads:
;; - 5 concurrent requests:    ✓ All successful (2408ms)
;; - 10 concurrent requests:   ✓ All successful (1428ms)
;; - 20 concurrent requests:   ✓ All successful (1506ms)
;; - 50 concurrent requests:   ✓ All successful (1473ms)
;; - 100 concurrent requests:  ✓ All successful (2388ms)
;; - 1000 concurrent requests: ✓ All successful (13342ms, ~75 req/sec)
;; - 5000 concurrent requests: ⏱ Client timeout after 120s
;;
;; Findings:
;; - NO rate limiting (429) responses observed up to 1000 concurrent requests
;; - API handled 1000 simultaneous requests across mixed operations (balance, transactions, payments)
;; - API Gateway appears to have very high or no rate limits configured
;; - Performance remained consistent up to 100 concurrent requests (~1.5s)
;; - At 1000 requests, throughput was ~75 requests/second
;;
;; Documentation Guidance:
;; - For typical use cases, the API handles concurrent requests robustly
;; - Up to 1000 concurrent requests tested successfully
;; - For applications requiring >1000 concurrent requests or sustained high throughput,
;;   contact the engineering team to discuss your specific needs
;; - No explicit rate limiting documentation found - assume generous limits for this environment
