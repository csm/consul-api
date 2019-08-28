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
   :ReplicationType (field (s/enum :legacy :policies :tokens)
                           {:description "The type of replication that is currently in use."})
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
                        :Labels {s/Str s/Str}}]
                  {:description "A list of gauges which store one value that is updated as time goes on, such as the amount of memory allocated"})
   :Points (field [s/Any]
                  {:description "A list of point metrics, which each store a series of points under a given name"})
   :Counters (field [{:Name s/Str
                          :Count s/Int
                          :Sum s/Int
                          :Min s/Int
                          :Max s/Int
                          :Mean s/Int
                          :Stddev s/Int
                          :Labels {s/Str s/Str}}]
                    {:description "A list of counters, which store info about a metric that is incremented over time such as the number of requests to an HTTP endpoint."})
   :Samples (field [{:Name s/Str
                         :Count s/Int
                         :Sum Double
                         :Min Double
                         :Max Double
                         :Stddev Double
                         :Labels {s/Str s/Str}}]
                   {:description "A list of samples, which store info about the amount of time spent on an operation, such as the time taken to serve a request to a specific http endpoint."})})

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

(s/defschema TaggedAddresses
  {(s/optional-key :lan) {:address s/Str
                          (s/optional-key :port) s/Int}
   (s/optional-key :wan) {:address s/Str
                          (s/optional-key :port) s/Int}})

(s/defschema AgentService
  {:ID s/Str
   :Service s/Str
   :Tags [s/Str]
   :TaggedAddresses TaggedAddresses
   :Meta {s/Str s/Str}
   :Port s/Int
   :Address s/Str
   :EnableTagOverride s/Bool
   :Weights {s/Str s/Int}})

(s/defschema AgentServices
  {s/Str AgentService})

(s/defschema Proxy
  {:DestinationServiceName s/Str
   :DestinationServiceID s/Str
   :LocalServiceAddress s/Str
   :LocalServicePort s/Int
   :Config {s/Str s/Str}
   :Upstreams [{:DestinationType s/Str
                :DestinationName s/Str
                :LocalBindPort s/Int}]})

(s/defschema AgentServiceConfiguration
  {:Kind s/Str
   :ID s/Str
   :Service s/Str
   :Tags (s/maybe [s/Str]) ; todo just a guess
   :Meta (s/maybe {s/Str s/Str})
   :Port s/Int
   :TaggedAddresses TaggedAddresses
   :Weights {s/Str s/Int}
   :EnableTagOverride s/Bool
   :ContentHash s/Str
   (s/optional-key :Proxy) (s/maybe Proxy)})

(s/defschema AgentServicesHealth
  {s/Str [AgentService]})

(s/defschema Connect
  {(s/optional-key :Native) (field s/Bool {:description "Specifies whether this service supports the Connect protocol natively. If this is true, then Connect proxies, DNS queries, etc. will be able to service discover this service."})
   (s/optional-key :Proxy) (field Proxy {:description "Deprecated Specifies that a managed Connect proxy should be started for this service instance, and optionally provides configuration for the proxy. The format is as documented in Managed Proxy Deprecation."})
   (s/optional-key :SidecarService) (field {s/Str s/Any} {:description "Specifies an optional nested service definition to register. For more information see Sidecar Service Registration."})})

(s/defschema AgentRegisterRequest
  {:Name (field s/Str {:description "Specifies the logical name of the service. Many service instances may share the same logical service name."})
   (s/optional-key :ID) (field s/Str {:description "Specifies a unique ID for this service. This must be unique per agent. This defaults to the Name parameter if not provided."})
   (s/optional-key :Tags) (field [s/Str] {:description "Specifies a list of tags to assign to the service. These tags can be used for later filtering and are exposed via the APIs."})
   (s/optional-key :Address) (field s/Str {:description "Specifies the address of the service. If not provided, the agent's address is used as the address for the service during DNS queries."})
   (s/optional-key :TaggedAddresses) (field TaggedAddresses {:description "Specifies a map of explicit LAN and WAN addresses for the service instance. Both the address and port can be specified within the map values."})
   (s/optional-key :Meta) (field {s/Str s/Str} {:description "Specifies arbitrary KV metadata linked to the service instance."})
   (s/optional-key :Port) (field s/Int {:description "Specifies the port of the service."})
   (s/optional-key :Kind) (field s/Str {:description "The kind of service. Defaults to \"\" which is a typical Consul service. This value may also be \"connect-proxy\" for services that are Connect-capable proxies representing another service or \"mesh-gateway\" for instances of a mesh gateway"})
   (s/optional-key :Proxy) (field Proxy {:description "From 1.2.3 on, specifies the configuration for a Connect proxy instance. This is only valid if Kind == \"connect-proxy\" or Kind == \"mesh-gateway\". See the Proxy documentation for full details."})
   (s/optional-key :Connect) (field Connect {:description "Specifies the configuration for Connect. See the Connect Structure section below for supported fields."})
   (s/optional-key :Check) (field AgentCheck {:description "Specifies a check. Please see the check documentation for more information about the accepted fields. If you don't provide a name or id for the check then they will be generated. To provide a custom id and/or name set the CheckID and/or Name field."})
   (s/optional-key :Checks) (field [AgentCheck] {:description "Specifies a list of checks. Please see the check documentation for more information about the accepted fields. If you don't provide a name or id for the check then they will be generated. To provide a custom id and/or name set the CheckID and/or Name field. The automatically generated Name and CheckID depend on the position of the check within the array, so even though the behavior is deterministic, it is recommended for all checks to either let consul set the CheckID by leaving the field empty/omitting it or to provide a unique value."})
   (s/optional-key :EnableTagOverride) (field s/Bool {:description "Specifies to disable the anti-entropy feature for this service's tags. If EnableTagOverride is set to true then external agents can update this service in the catalog and modify the tags. Subsequent local sync operations by this agent will ignore the updated tags. For instance, if an external agent modified both the tags and the port for this service and EnableTagOverride was set to true then after the next sync cycle the service's port would revert to the original value but the tags would maintain the updated value. As a counter example, if an external agent modified both the tags and port for this service and EnableTagOverride was set to false then after the next sync cycle the service's port and the tags would revert to the original value and all modifications would be lost."})
   (s/optional-key :Weights) (field {s/Str s/Int} {:description " Specifies weights for the service. Please see the service documentation for more information about weights. If this field is not provided weights will default to {\"Passing\": 1, \"Warning\": 1}.\n\nIt is important to note that this applies only to the locally registered service. If you have multiple nodes all registering the same service their EnableTagOverride configuration and all other service configuration items are independent of one another. Updating the tags for the service registered on one node is independent of the same service (by name) registered on another node. If EnableTagOverride is not specified the default value is false. See anti-entropy syncs for more info."})})

(s/defschema ConnectAuthorizeRequest
  {:Target (field s/Str {:description "The name of the service that is being requested."})
   :ClientCertURI (field s/Str {:description "The unique identifier for the requesting client. This is currently the URI SAN from the TLS client certificate."})
   :ClientCertSerial (field s/Str {:description "The colon-hex-encoded serial number for the requesting client cert. This is used to check against revocation lists."})})

(s/defschema ConnectAuthorizeResponse
  {:Authorized s/Bool
   :Reason s/Str})

(s/defschema CertificateAuthorityRoot
  {:ID s/Str
   :Name s/Str
   :SerialNumber s/Int
   :SigningKeyID s/Str
   :NotBefore s/Inst
   :NotAfter s/Inst
   :RootCerts s/Str
   :IntermediateCerts (s/maybe [s/Str]) ; FIXME what is this field really?
   :Active s/Bool
   :CreateIndex s/Int
   :ModifyIndex s/Int})

(s/defschema CertificateAuthorityRoots
  {:ActiveRootID s/Str
   :Roots [CertificateAuthorityRoot]})

(s/defschema ServiceLeafCertificate
  {:SerialNumber (field s/Str {:description "Monotonically increasing 64-bit serial number representing all certificates issued by this Consul cluster."})
   :CertPEM (field s/Str {:description "The PEM-encoded certificate."})
   :PrivateKeyPEM (field s/Str {:description "The PEM-encoded private key for this certificate."})
   :Service (field s/Str {:description "The name of the service that this certificate identifies."})})

; catalog API

(s/defschema CatalogRegisterRequest
  {(s/optional-key :ID) (field s/Uuid {:description "An optional UUID to assign to the node."})
   :Node (field s/Str {:description "Specifies the node ID to register."})
   :Address (field s/Str {:description "Specifies the address to register."})
   (s/optional-key :Datacenter) (field s/Str {:description "Specifies the datacenter, which defaults to the agent's datacenter if not provided."})
   (s/optional-key :TaggedAddresses) (field TaggedAddresses {:description "Specifies the tagged addresses."})
   (s/optional-key :NodeMeta) (field {s/Str s/Str} {:description "Specifies arbitrary KV metadata pairs for filtering purposes."})
   (s/optional-key :Service) (field AgentService {:description "Specifies to register a service. If ID is not provided, it will be defaulted to the value of the Service.Service property. Only one service with a given ID may be present per node. The service Tags, Address, Meta, and Port fields are all optional. For more information about these fields and the implications of setting them, see the Service - Agent API page as registering services differs between using this or the Services Agent endpoint."})
   (s/optional-key :Check) (field AgentCheck {:description "Specifies to register a check. The register API manipulates the health check entry in the Catalog, but it does not setup the script, TTL, or HTTP check to monitor the node's health. To truly enable a new health check, the check must either be provided in agent configuration or set via the agent endpoint.

   The CheckID can be omitted and will default to the value of Name. As with Service.ID, the CheckID must be unique on this node. Notes is an opaque field that is meant to hold human-readable text. If a ServiceID is provided that matches the ID of a service on that node, the check is treated as a service level health check, instead of a node level health check. The Status must be one of passing, warning, or critical.

   The Definition field can be provided with details for a TCP or HTTP health check. For more information, see the Health Checks page."})
   (s/optional-key :Checks) (field [AgentCheck] {:description "Specifies to register a check. The register API manipulates the health check entry in the Catalog, but it does not setup the script, TTL, or HTTP check to monitor the node's health. To truly enable a new health check, the check must either be provided in agent configuration or set via the agent endpoint.

   The CheckID can be omitted and will default to the value of Name. As with Service.ID, the CheckID must be unique on this node. Notes is an opaque field that is meant to hold human-readable text. If a ServiceID is provided that matches the ID of a service on that node, the check is treated as a service level health check, instead of a node level health check. The Status must be one of passing, warning, or critical.

   The Definition field can be provided with details for a TCP or HTTP health check. For more information, see the Health Checks page."})
   (s/optional-key :SkipNodeUpdate) (field s/Bool {:description "Specifies whether to skip updating the node's information in the registration. This is useful in the case where only a health check or service entry on a node needs to be updated or when a register request is intended to update a service entry or health check. In both use cases, node information will not be overwritten, if the node is already registered. Note, if the parameter is enabled for a node that doesn't exist, it will still be created."})})

(s/defschema CatalogDeregisterRequest
  {:Node (field s/Str {:description "Specifies the ID of the node. If no other values are provided, this node, all its services, and all its checks are removed."})
   (s/optional-key :Datacenter) (field s/Str {:description "Specifies the datacenter, which defaults to the agent's datacenter if not provided."})
   (s/optional-key :CheckID) (field s/Str {:description "Specifies the ID of the check to remove."})
   (s/optional-key :ServiceID) (field s/Str {:description "Specifies the ID of the service to remove. The service and all associated checks will be removed."})})

(s/defschema CatalogNode
  {:ID s/Uuid
   :Node s/Str
   :Address s/Str
   :Datacenter s/Str
   (s/optional-key :TaggedAddresses) TaggedAddresses
   (s/optional-key :Meta) {s/Str s/Str}})

(s/defschema CatalogServiceNode
  {:Address (field s/Str {:description "is the IP address of the Consul node on which the service is registered."})
   :Datacenter (field s/Str {:description "is the data center of the Consul node on which the service is registered."})
   (s/optional-key :TaggedAddresses) (field TaggedAddresses {:description "is the list of explicit LAN and WAN IP addresses for the agent"})
   (s/optional-key :NodeMeta) (field {s/Str s/Str} {:description "is a list of user-defined metadata key/value pairs for the node"})
   :CreateIndex (field s/Int {:description "is an internal index value representing when the service was created"})
   :ModifyIndex (field s/Int {:description "is the last index that modified the service"})
   :Node (field s/Str {:description "is the name of the Consul node on which the service is registered"})
   (s/optional-key :ServiceAddress) (field s/Str {:description "is the IP address of the service host — if empty, node address should be used"})
   :ServiceEnableTagOverride (field s/Bool {:description "indicates whether service tags can be overridden on this service"})
   :ServiceID (field s/Str {:description "is a unique service instance identifier"})
   :ServiceName (field s/Str {:description "is the name of the service"})
   (s/optional-key :ServiceMeta) (field {s/Str s/Str} {:description "is a list of user-defined metadata key/value pairs for the service"})
   :ServicePort (field s/Int {:description "is the port number of the service"})
   (s/optional-key :ServiceTags) (field [s/Str] {:description "is a list of tags for the service"})
   (s/optional-key :ServiceTaggedAddresses) (field TaggedAddresses {:description "is the map of explicit LAN and WAN addresses for the service instance. This includes both the address as well as the port."})
   :ServiceKind (field s/Str {:description "is the kind of service, usually \"\". See the Agent registration API for more information."})
   (s/optional-key :ServiceProxy) (field Proxy {:description "is the proxy config as specified in Connect Proxies."})
   (s/optional-key :ServiceConnect) (field Connect {:description "are the Connect settings. The value of this struct is equivalent to the Connect field for service registration."})})

(s/defschema CatalogService
  {:ID s/Str
   :Service s/Str
   (s/optional-key :Tags) (s/maybe [s/Str])
   (s/optional-key :TaggedAddresses) TaggedAddresses
   (s/optional-key :Meta) {s/Str s/Str}
   :Port s/Int})