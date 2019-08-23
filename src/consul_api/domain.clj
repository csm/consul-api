(ns consul-api.domain
  (:require [schema.core :as s]
            [ring.swagger.json-schema :refer [field]]))

; ACL API

(s/defschema ACLRulePolicy
  {:policy (s/enum :read :write :deny :list)})

(s/defschema ACLRules
  {(s/optional-key :key_prefix) {s/Str ACLRulePolicy}
   (s/optional-key :key) {s/Str ACLRulePolicy}
   (s/optional-key :operator) (s/enum :read :write :deny :list)})

(s/defschema ACLPolicy
  {:ID s/Uuid
   :Name s/Str
   (s/optional-key :Description) s/Str
   (s/optional-key :Rules) ACLRules
   (s/optional-key :Datacenters) [s/Str]})

(s/defschema ACLBootstrapResponse
  {(s/optional-key :ID) (field s/Uuid {:description "Deprecated, equal to SecretID."})
   :AccessorID s/Uuid
   :SecretID s/Uuid
   :Description s/Str
   :Policies [ACLPolicy]
   :Local s/Bool
   :CreateTime s/Inst
   :Hash s/Str
   :CreateIndex s/Int
   :ModifyIndex s/Int})

(s/defschema ACLReplicationStatus
  {:Enabled (field s/Bool {:description "Reports whether ACL replication is enabled for the datacenter"})
   :Running (field s/Bool {:description "Reports whether the ACL replication process is running. The process may take approximately 60 seconds to begin running after a leader election occurs."})
   :SourceDatacenter (field s/Str {:description "The authoritative ACL datacenter that ACLs are being replicated from and will match the primary_datacenter configuration"})
   :ReplicationType (field (s/enum :legacy :policies :tokens
                               {:description "The type of replication that is currently in use."}))
   :ReplicatedIndex (field s/Int {:description "The last index that was successfully replicated. Which data the replicated index refers to depends on the replication type. For legacy replication this can be compared with the value of the X-Consul-Index header returned by the /v1/acl/list endpoint to determine if the replication process has gotten all available ACLs. When in either tokens or policies mode, this index can be compared with the value of the X-Consul-Index header returned by the /v1/acl/polcies endpoint to determine if the policy replication process has gotten all available ACL policies. Note that ACL replication is rate limited so the indexes may lag behind the primary datacenter."})
   :ReplicatedTokenIndex (field s/Int {:description "The last token index that was successfully replicated. This index can be compared with the value of the X-Consul-Index header returned by the /v1/acl/tokens endpoint to determine if the replication process has gotten all available ACL tokens. Note that ACL replication is rate limited so the indexes may lag behind the primary datacenter."})
   :LastSuccess (field s/Inst {:description "The UTC time of the last successful sync operation. Since ACL replication is done with a blocking query, this may not update for up to 5 minutes if there have been no ACL changes to replicate. A zero value of \"0001-01-01T00:00:00Z\" will be present if no sync has been successful."})
   :LastError (field s/Inst {:description "The UTC time of the last error encountered during a sync operation. If this time is later than LastSuccess, you can assume the replication process is not in a good state. A zero value of \"0001-01-01T00:00:00Z\" will be present if no sync has resulted in an error."})})

(s/defschema ACLLoginRequest
  {:AuthMethod (field s/Str {:description "The name of the auth method to use for login."})
   :BearerToken (field s/Str {:description "The bearer token to present to the auth method during login for authentication purposes. For the Kubernetes auth method this is a Service Account Token (JWT)."})
   (s/optional-key :Meta) (field {s/Str s/Str} {:description "Specifies arbitrary KV metadata linked to the token. Can be useful to track origins."})})

(s/defschema ACLRole
  {:ID s/Uuid
   :Name s/Str
   (s/optional-key :Description) s/Str})

(s/defschema ACLServiceIdentity
  {:ServiceName s/Str
   (s/optional-key :Datacenters) [s/Str]})

(s/defschema ACLLoginResponse
  {:AccessorID s/Uuid
   :SecretID s/Uuid
   :Description s/Str
   :Roles [ACLRole]
   :ServiceIdentities [{:ServiceName s/Str}]
   :Local s/Bool
   :AuthMethod s/Str
   :CreateTime s/Inst
   :Hash s/Str
   :CreateIndex s/Int
   :ModifyIndex s/Int})

(s/defschema ACLTokenRequest
  {})

;; Agent API

(s/defschema AgentMember
  {:Name s/Str
   :Addr s/Str
   :Port s/Int
   :Tags {s/Str s/Str}
   :Status s/Int
   :ProtocolMin s/Int
   :ProtocolMax s/Int
   :ProtocolCur s/Int
   :DelegateMin s/Int
   :DelegateMax s/Int
   :DelegateCur s/Int})

(s/defschema AgentConfiguration
  {:Config {:Datacenter s/Str
            :NodeName s/Str
            :NodeID s/Uuid
            :Server s/Bool
            :Revision s/Str
            :Version s/Str}
   :DebugConfig s/Any
   :Coord {:Adjustment s/Int
           :Error Double
           :Vec [s/Int]}
   :Member AgentMember
   :Meta {s/Str s/Str}})

(s/defschema AgentMetrics
  {:Timestamp (field s/Inst {:description "The timestamp of the interval for the displayed metrics. Metrics are aggregated on a ten second interval, so this value (along with the displayed metrics) will change every ten seconds."})
   :Gauges (field [{:Name s/Str
                        :Value s/Int
                        :Labels {s/Str s/Str}
                      {:description "A list of gauges which store one value that is updated as time goes on, such as the amount of memory allocated"}}])
   :Points (field [s/Any
                      {:description "A list of point metrics, which each store a series of points under a given name"}])
   :Counters (field [{:Name s/Str
                          :Count s/Int
                          :Sum s/Int
                          :Min s/Int
                          :Max s/Int
                          :Mean s/Int
                          :Stddev s/Int
                          :Labels {s/Str s/Str}
                        {:description "A list of counters, which store info about a metric that is incremented over time such as the number of requests to an HTTP endpoint."}}])
   :Samples (field [{:Name s/Str
                         :Count s/Int
                         :Sum Double
                         :Min Double
                         :Max Double
                         :Stddev Double
                         :Labels {s/Str s/Str}
                       {:description "A list of samples, which store info about the amount of time spent on an operation, such as the time taken to serve a request to a specific http endpoint."}}])})

(s/defschema AgentCheck
  {:Node s/Str
   :CheckID s/Str
   :Name s/Str
   :Status s/Str
   :Notes s/Str
   :Output s/Str
   :ServiceID s/Str
   :ServiceName s/Str
   :ServiceTags [s/Str]})

(s/defschema AgentChecks
  {s/Str AgentCheck})

(s/defschema RegisterCheckRequest
  {:Name (field s/Str {:description "Specifies the name of the check"})
   (s/optional-key :ID) (field s/Str {:description "Specifies a unique ID for this check on the node. This defaults to the \"Name\" parameter, but it may be necessary to provide an ID for uniqueness."})
   (s/optional-key :Interval) (field s/Str {:description "Specifies the frequency at which to run this check. This is required for HTTP and TCP checks."})
   (s/optional-key :Notes) (field s/Str {:description "Specifies arbitrary information for humans. This is not used by Consul internally."})
   (s/optional-key :DeregisterCriticalServiceAfter) (field s/Str {:description "Specifies that checks associated with a service should deregister after this time. This is specified as a time duration with suffix like \"10m\". If a check is in the critical state for more than this configured value, then its associated service (and all of its associated checks) will automatically be deregistered. The minimum timeout is 1 minute, and the process that reaps critical services runs every 30 seconds, so it may take slightly longer than the configured timeout to trigger the deregistration. This should generally be configured with a timeout that's much, much longer than any expected recoverable outage for the given service."})
   (s/optional-key :Args) (field [s/Str] {:description "Specifies command arguments to run to update the status of the check. Prior to Consul 1.0, checks used a single Script field to define the command to run, and would always run in a shell. In Consul 1.0, the Args array was added so that checks can be run without a shell. The Script field is deprecated, and you should include the shell in the Args to run under a shell, eg. \"args\": [\"sh\", \"-c\", \"...\"]."})
   (s/optional-key :AliasNode) (field s/Str {:description "Specifies the ID of the node for an alias check. If no service is specified, the check will alias the health of the node. If a service is specified, the check will alias the specified service on this particular node."})
   (s/optional-key :AliasService) (field s/Str {:description "Specifies the ID of a service for an alias check. If the service is not registered with the same agent, AliasNode must also be specified. Note this is the service ID and not the service name (though they are very often the same)."})
   (s/optional-key :DockerContainerID) (field s/Str {:description "Specifies that the check is a Docker check, and Consul will evaluate the script every Interval in the given container using the specified Shell. Note that Shell is currently only supported for Docker checks."})
   (s/optional-key :GRPC) (field s/Str {:description "Specifies a gRPC check's endpoint that supports the standard gRPC health checking protocol. The state of the check will be updated at the given Interval by probing the configured endpoint."})
   (s/optional-key :GRPCUseTLS) (field s/Bool {:description "Specifies whether to use TLS for this gRPC health check. If TLS is enabled, then by default, a valid TLS certificate is expected. Certificate verification can be turned off by setting TLSSkipVerify to true."})
   (s/optional-key :HTTP) (field s/Str {:description "Specifies an HTTP check to perform a GET request against the value of HTTP (expected to be a URL) every Interval. If the response is any 2xx code, the check is passing. If the response is 429 Too Many Requests, the check is warning. Otherwise, the check is critical. HTTP checks also support SSL. By default, a valid SSL certificate is expected. Certificate verification can be controlled using the TLSSkipVerify."})
   (s/optional-key :Method) (field s/Str {:description "Specifies a different HTTP method to be used for an HTTP check. When no value is specified, GET is used."})
   (s/optional-key :Header) (field {s/Str [s/Str]} {:description "pecifies a set of headers that should be set for HTTP checks. Each header can have multiple values."})
   (s/optional-key :Timeout) (field s/Str {:description "Specifies a timeout for outgoing connections in the case of a Script, HTTP, TCP, or gRPC check. Can be specified in the form of \"10s\" or \"5m\" (i.e., 10 seconds or 5 minutes, respectively)."})
   (s/optional-key :OutputMaxSize) (field s/Int {:description "Allow to put a maximum size of text for the given check. This value must be greater than 0, by default, the value is 4k. The value can be further limited for all checks of a given agent using the check_output_max_size flag in the agent."})
   (s/optional-key :TLSSkipVerify) (field s/Bool {:description "Specifies if the certificate for an HTTPS check should not be verified."})
   (s/optional-key :TCP) (field s/Str {:description "Specifies a TCP to connect against the value of TCP (expected to be an IP or hostname plus port combination) every Interval. If the connection attempt is successful, the check is passing. If the connection attempt is unsuccessful, the check is critical. In the case of a hostname that resolves to both IPv4 and IPv6 addresses, an attempt will be made to both addresses, and the first successful connection attempt will result in a successful check."})
   (s/optional-key :TTL) (field s/Str {:description "Specifies this is a TTL check, and the TTL endpoint must be used periodically to update the state of the check."})
   (s/optional-key :ServiceID) (field s/Str {:description "Specifies the ID of a service to associate the registered check with an existing service provided by the agent."})
   (s/optional-key :Status) (field s/Str {:description "Specifies the initial status of the health check."})})