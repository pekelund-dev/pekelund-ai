-- ============================================================
-- V2: Seed reference data for the ITII demo environment
-- ============================================================

-- ── Services ──────────────────────────────────────────────────────────────────
INSERT INTO services (name, description, team, dependencies, critical) VALUES
  ('api-gateway',          'Main entry point for all client traffic; handles routing, rate-limiting and auth', 'Platform Team',    NULL,                                             TRUE),
  ('payment-service',      'Processes card payments and manages payment state machines',                      'Payments Team',    'api-gateway,order-service,fraud-service',         TRUE),
  ('order-service',        'Manages order creation, updates and lifecycle events',                            'Commerce Team',    'api-gateway,inventory-service,payment-service',   TRUE),
  ('user-service',         'Handles user accounts, authentication and session management',                    'Platform Team',    'api-gateway',                                    TRUE),
  ('inventory-service',    'Tracks product stock levels and reservations',                                    'Warehouse Team',   'api-gateway',                                    FALSE),
  ('notification-service', 'Sends transactional email and SMS notifications',                                 'Platform Team',    'user-service',                                   FALSE),
  ('fraud-service',        'Real-time fraud scoring for payment transactions',                                'Security Team',    'payment-service',                                TRUE),
  ('reporting-service',    'Generates business reports and analytics dashboards',                             'Analytics Team',   'order-service,inventory-service',                FALSE);

-- ── Runbooks ──────────────────────────────────────────────────────────────────
INSERT INTO runbooks (title, description, service_name, content, tags) VALUES
(
  'Payment Service: Database Connection Exhaustion',
  'Steps for diagnosing and resolving connection pool exhaustion in payment-service',
  'payment-service',
  '## Symptoms
- Requests returning 503 with "Unable to acquire JDBC Connection"
- `p95_latency_ms` spikes above 5000ms
- `error_rate_per_min` above 50

## Root Cause
The connection pool is exhausted. This typically occurs when:
1. A long-running transaction holds a connection
2. A downstream database is slow, causing connections to back up
3. A deployment leaked connections (missing `finally` block / try-with-resources)

## Diagnosis Steps
1. Run `search_logs` for `payment-service` at ERROR level looking for "Unable to acquire JDBC Connection"
2. Check `get_service_metrics` for `cpu_percent` and `p95_latency_ms` — high latency with normal CPU suggests DB wait
3. Query `get_incident_history` for similar `DATABASE` category incidents

## Resolution
1. Restart the payment-service pod to release connections (short-term)
2. Increase `hikari.maximum-pool-size` if DB can sustain more connections
3. Identify slow queries using the DB slow-query log and add missing indexes
4. Review recent deployments for resource leaks',
  'connection-pool,database,jdbc,payment,503,timeout'
),
(
  'Order Service: Out-of-Memory (OOM) Kill',
  'How to diagnose and recover from JVM heap exhaustion in order-service',
  'order-service',
  '## Symptoms
- Container restarts with OOM kill signal
- `memory_percent` sustained above 90%
- `java.lang.OutOfMemoryError: Java heap space` in logs

## Root Cause Candidates
1. Memory leak in a new deployment (most common)
2. Unusual traffic spike causing object retention
3. Large batch job holding objects in memory

## Diagnosis Steps
1. Check `get_service_metrics` for `memory_percent` trend over last 30 minutes
2. Check `search_logs` for OutOfMemoryError or GC overhead
3. Review recent deployments via `get_incident_history` for deployment-correlated OOM events

## Resolution
1. **Immediate**: Restart the pod — it will be re-scheduled by Kubernetes
2. **Short-term**: Increase JVM heap (`-Xmx`) in deployment config
3. **Long-term**: Capture a heap dump on next occurrence, run MAT analysis to find the leak
4. Rollback the most recent deployment if correlated',
  'oom,memory,heap,java,jvm,order,restart'
),
(
  'API Gateway: High Latency / Traffic Spike',
  'Handling latency spikes and traffic overload at the API gateway layer',
  'api-gateway',
  '## Symptoms
- `p95_latency_ms` > 2000ms for the gateway
- Increased `error_rate_per_min` across multiple downstream services
- CPU spikes with no corresponding DB slowness

## Root Cause Candidates
1. Legitimate traffic spike (marketing campaign, viral event)
2. DDoS or bot traffic
3. Misconfigured rate limits allowing excessive traffic through

## Diagnosis Steps
1. Check `get_service_metrics` for `cpu_percent` and `error_rate_per_min`
2. Inspect `search_logs` for rate-limit errors or unusual user-agent patterns
3. Compare with `get_incident_history` for similar traffic events

## Resolution
1. Enable circuit breaker on gateway to shed load from overwhelmed services
2. Add or tighten rate limiting rules for the affected paths
3. Scale out downstream services if legitimate traffic
4. If DDoS: engage CDN/WAF team (Security Team escalation)',
  'latency,traffic-spike,gateway,rate-limit,ddos,circuit-breaker'
),
(
  'General: Database Failover Procedure',
  'Platform-wide runbook for primary PostgreSQL failover',
  NULL,
  '## When to Use
Use this runbook when the primary database is unresponsive and automatic failover
has NOT triggered, or when a manual failover is required for maintenance.

## Steps
1. Confirm the primary is down: `ping DB_PRIMARY_HOST` and check CloudWatch/Datadog
2. Identify the current replica with lowest replication lag
3. Promote replica to primary:
   ```
   pg_ctl promote -D /var/lib/postgresql/data
   ```
4. Update the DB_URL environment variable in all affected services
5. Perform rolling restart of payment-service, order-service, user-service
6. Verify connectivity with a test query
7. Open incident for post-mortem

## Post-Failover
- Monitor replication for the new replica setup
- Notify DBA team and schedule a window to restore HA configuration',
  'database,failover,postgresql,disaster-recovery,platform'
),
(
  'Deployment Rollback Procedure',
  'How to safely roll back a service deployment that caused an incident',
  NULL,
  '## When to Use
When a new deployment correlates with an incident start time (check `get_incident_history`
for recent deployments, or compare incident `created_at` with deployment timestamps).

## Steps
1. Identify the previous stable image tag from the CI/CD system
2. Update the Kubernetes deployment:
   ```
   kubectl set image deployment/<service-name> \
     <container>=<registry>/<service-name>:<previous-tag>
   kubectl rollout status deployment/<service-name>
   ```
3. Alternatively use `kubectl rollout undo`:
   ```
   kubectl rollout undo deployment/<service-name>
   ```
4. Monitor error rate and latency — should return to baseline within 2 minutes
5. Document what the offending change was in the incident notes

## Prevention
- Enforce canary deployments or blue/green for critical services
- Add automated smoke tests that block promotion if error rate rises',
  'rollback,deployment,kubernetes,kubectl,release'
);

-- ── Simulated Logs (realistic failure scenario for demo) ──────────────────────
-- payment-service: connection pool exhaustion incident (most recent first)
INSERT INTO simulated_logs (service_name, level, message, metadata, timestamp) VALUES
  ('payment-service', 'ERROR', 'Unable to acquire JDBC Connection; nested exception is org.hibernate.exception.JDBCConnectionException',
   '{"requestId":"req-7a1b","userId":"user-9923","endpoint":"/api/payments/process"}',
   NOW() - INTERVAL '2 minutes'),
  ('payment-service', 'ERROR', 'Unable to acquire JDBC Connection; nested exception is org.hibernate.exception.JDBCConnectionException',
   '{"requestId":"req-7a1c","userId":"user-8812","endpoint":"/api/payments/process"}',
   NOW() - INTERVAL '3 minutes'),
  ('payment-service', 'WARN',  'HikariPool-1 - Connection is not available, request timed out after 30000ms',
   '{"pool":"HikariPool-1","timeout":30000}',
   NOW() - INTERVAL '4 minutes'),
  ('payment-service', 'WARN',  'HikariPool-1 - Connection is not available, request timed out after 30000ms',
   '{"pool":"HikariPool-1","timeout":30000}',
   NOW() - INTERVAL '5 minutes'),
  ('payment-service', 'INFO',  'Processing payment for order-id=ORD-55321, amount=249.99 SEK',
   '{"orderId":"ORD-55321","amount":249.99,"currency":"SEK"}',
   NOW() - INTERVAL '10 minutes'),
  ('payment-service', 'INFO',  'Payment authorised successfully for order-id=ORD-55318',
   '{"orderId":"ORD-55318","authCode":"AUTH-7ABC","durationMs":312}',
   NOW() - INTERVAL '15 minutes'),
  ('payment-service', 'ERROR', 'Fraud service timeout after 5000ms — defaulting to ALLOW',
   '{"fraudServiceUrl":"http://fraud-service/score","timeoutMs":5000}',
   NOW() - INTERVAL '20 minutes'),

-- order-service: normal operations with occasional warnings
  ('order-service',   'INFO',  'Order ORD-55321 created for user-9923, total 249.99 SEK',
   '{"orderId":"ORD-55321","userId":"user-9923","items":3}',
   NOW() - INTERVAL '10 minutes'),
  ('order-service',   'WARN',  'Inventory reservation slow: 2300ms for product SKU-1042',
   '{"sku":"SKU-1042","durationMs":2300}',
   NOW() - INTERVAL '12 minutes'),
  ('order-service',   'INFO',  'Order ORD-55320 dispatched, tracking ID TRK-90012',
   '{"orderId":"ORD-55320","trackingId":"TRK-90012"}',
   NOW() - INTERVAL '18 minutes'),

-- api-gateway: traffic logs
  ('api-gateway',     'WARN',  'Rate limit approaching for client-ip 203.0.113.42: 85% of quota',
   '{"clientIp":"203.0.113.42","quotaUsedPct":85}',
   NOW() - INTERVAL '3 minutes'),
  ('api-gateway',     'INFO',  'Upstream payment-service returning 503 — circuit breaker HALF_OPEN',
   '{"upstream":"payment-service","status":503,"circuitBreaker":"HALF_OPEN"}',
   NOW() - INTERVAL '2 minutes'),
  ('api-gateway',     'ERROR', 'Circuit breaker OPEN for payment-service — all requests failing fast',
   '{"upstream":"payment-service","circuitBreaker":"OPEN","failureRate":0.72}',
   NOW() - INTERVAL '1 minute'),

-- fraud-service: slow response degradation
  ('fraud-service',   'WARN',  'Scoring model response time degraded: p95=4200ms (threshold=2000ms)',
   '{"p95Ms":4200,"thresholdMs":2000}',
   NOW() - INTERVAL '25 minutes'),
  ('fraud-service',   'WARN',  'Scoring model response time degraded: p95=4800ms',
   '{"p95Ms":4800}',
   NOW() - INTERVAL '22 minutes'),
  ('fraud-service',   'ERROR', 'ML model inference timeout — falling back to rule-based scoring',
   '{"modelEndpoint":"ml-model-v3","timeoutMs":5000,"fallback":"rule-based"}',
   NOW() - INTERVAL '20 minutes'),

-- user-service: normal
  ('user-service',    'INFO',  'User user-9923 authenticated successfully via OAuth2',
   '{"userId":"user-9923","provider":"oauth2","durationMs":45}',
   NOW() - INTERVAL '11 minutes'),
  ('user-service',    'INFO',  'Session refreshed for user-8812',
   '{"userId":"user-8812","durationMs":12}',
   NOW() - INTERVAL '8 minutes');

-- ── Simulated Metrics ─────────────────────────────────────────────────────────
-- payment-service: showing degradation pattern
INSERT INTO simulated_metrics (service_name, metric_name, value, unit, timestamp) VALUES
  ('payment-service', 'cpu_percent',        45.2,  '%',      NOW() - INTERVAL '1 minute'),
  ('payment-service', 'memory_percent',     62.1,  '%',      NOW() - INTERVAL '1 minute'),
  ('payment-service', 'error_rate_per_min', 87.0,  'req/min',NOW() - INTERVAL '1 minute'),
  ('payment-service', 'p95_latency_ms',     8200,  'ms',     NOW() - INTERVAL '1 minute'),
  ('payment-service', 'cpu_percent',        44.8,  '%',      NOW() - INTERVAL '5 minutes'),
  ('payment-service', 'memory_percent',     61.5,  '%',      NOW() - INTERVAL '5 minutes'),
  ('payment-service', 'error_rate_per_min', 72.0,  'req/min',NOW() - INTERVAL '5 minutes'),
  ('payment-service', 'p95_latency_ms',     6500,  'ms',     NOW() - INTERVAL '5 minutes'),
  ('payment-service', 'cpu_percent',        43.1,  '%',      NOW() - INTERVAL '10 minutes'),
  ('payment-service', 'memory_percent',     60.0,  '%',      NOW() - INTERVAL '10 minutes'),
  ('payment-service', 'error_rate_per_min', 3.2,   'req/min',NOW() - INTERVAL '10 minutes'),
  ('payment-service', 'p95_latency_ms',     310,   'ms',     NOW() - INTERVAL '10 minutes'),

-- api-gateway: normal then degraded
  ('api-gateway',     'cpu_percent',        28.4,  '%',      NOW() - INTERVAL '1 minute'),
  ('api-gateway',     'memory_percent',     41.2,  '%',      NOW() - INTERVAL '1 minute'),
  ('api-gateway',     'error_rate_per_min', 62.0,  'req/min',NOW() - INTERVAL '1 minute'),
  ('api-gateway',     'p95_latency_ms',     3100,  'ms',     NOW() - INTERVAL '1 minute'),
  ('api-gateway',     'error_rate_per_min', 1.1,   'req/min',NOW() - INTERVAL '15 minutes'),
  ('api-gateway',     'p95_latency_ms',     120,   'ms',     NOW() - INTERVAL '15 minutes'),

-- fraud-service: CPU spike (model loaded into memory)
  ('fraud-service',   'cpu_percent',        91.0,  '%',      NOW() - INTERVAL '1 minute'),
  ('fraud-service',   'memory_percent',     88.5,  '%',      NOW() - INTERVAL '1 minute'),
  ('fraud-service',   'p95_latency_ms',     5200,  'ms',     NOW() - INTERVAL '1 minute'),
  ('fraud-service',   'cpu_percent',        55.0,  '%',      NOW() - INTERVAL '20 minutes'),
  ('fraud-service',   'memory_percent',     72.0,  '%',      NOW() - INTERVAL '20 minutes'),
  ('fraud-service',   'p95_latency_ms',     420,   'ms',     NOW() - INTERVAL '20 minutes'),

-- order-service: healthy
  ('order-service',   'cpu_percent',        22.1,  '%',      NOW() - INTERVAL '1 minute'),
  ('order-service',   'memory_percent',     48.3,  '%',      NOW() - INTERVAL '1 minute'),
  ('order-service',   'error_rate_per_min', 0.8,   'req/min',NOW() - INTERVAL '1 minute'),
  ('order-service',   'p95_latency_ms',     380,   'ms',     NOW() - INTERVAL '1 minute'),

-- user-service: healthy
  ('user-service',    'cpu_percent',        18.5,  '%',      NOW() - INTERVAL '1 minute'),
  ('user-service',    'memory_percent',     35.7,  '%',      NOW() - INTERVAL '1 minute'),
  ('user-service',    'error_rate_per_min', 0.2,   'req/min',NOW() - INTERVAL '1 minute'),
  ('user-service',    'p95_latency_ms',     65,    'ms',     NOW() - INTERVAL '1 minute');

-- ── Historical Incidents (resolved, for AI learning context) ──────────────────
INSERT INTO incidents (title, description, severity, status, category, affected_services,
                        reported_by, resolution, root_cause, ai_summary, resolved_at) VALUES
(
  'Payment Service DB Connection Pool Exhausted (Previous)',
  'payment-service started returning 503 errors at 14:22. Error logs showed HikariCP connection timeout.',
  'HIGH', 'RESOLVED', 'DATABASE', 'payment-service,api-gateway',
  'on-call-eng-1',
  'Restarted payment-service pod to flush connections. Increased pool size from 10 to 20. Added long-running transaction timeout.',
  'A batch reconciliation job was holding 8 connections open for >10 minutes, starving the pool.',
  'The payment-service suffered database connection pool exhaustion caused by a long-running batch reconciliation job.',
  NOW() - INTERVAL '7 days'
),
(
  'Fraud Service Memory Leak After v2.4.1 Deployment',
  'Memory usage of fraud-service climbed steadily after deploying v2.4.1 at 09:00. OOM kill at 11:30.',
  'HIGH', 'RESOLVED', 'MEMORY', 'fraud-service,payment-service',
  'on-call-eng-2',
  'Rolled back to fraud-service v2.4.0. Root cause identified as ML model not releasing tensor buffers. Fixed in v2.4.2.',
  'New ML model version in v2.4.1 retained PyTorch tensor objects after inference due to missing .detach() call.',
  'Deployment of fraud-service v2.4.1 introduced a memory leak in the ML inference pipeline.',
  NOW() - INTERVAL '14 days'
),
(
  'API Gateway Rate Limiter Misconfiguration',
  'Sudden drop in successful requests across all services at 16:00. Gateway was returning 429 for all traffic.',
  'CRITICAL', 'RESOLVED', 'DEPLOYMENT', 'api-gateway',
  'on-call-eng-1',
  'Rolled back gateway configuration. Rate limit thresholds were set 1000x too low in the new config.',
  'Infrastructure engineer mistakenly applied the staging rate-limit config (requests/hour instead of requests/second) to production.',
  'Critical configuration error: production gateway used staging rate limits, blocking virtually all traffic.',
  NOW() - INTERVAL '30 days'
);
