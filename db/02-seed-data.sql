-- ============================================================
-- Seed Data: Jargon Dictionary + Sample Entities
-- Replace with your real domain data
-- ============================================================

-- Jargon Dictionary (abstract — swap short_form/full_form for your domain)
INSERT INTO jargon_dictionary (short_form, full_form, description, domain, example_usage) VALUES
('SLA',  'Service Level Agreement',      'A contract defining expected service quality, availability, and performance metrics between provider and consumer.', 'Operations',   'The SLA requires 99.9% uptime per calendar month.'),
('KPI',  'Key Performance Indicator',    'A measurable value demonstrating how effectively an objective is being achieved.', 'Business',     'Monthly revenue growth is a KPI tracked by leadership.'),
('ETL',  'Extract Transform Load',       'A pipeline that moves data from source systems to a destination after transformation.', 'Data',         'The ETL pipeline runs nightly to refresh the reporting database.'),
('RBAC', 'Role-Based Access Control',    'A security model where access rights are assigned based on user roles rather than individuals.', 'Security',     'RBAC ensures only admins can delete records.'),
('DR',   'Disaster Recovery',            'A documented strategy and process to restore systems and data after a catastrophic failure.', 'Operations',   'The DR plan requires RTO of 4 hours and RPO of 1 hour.'),
('RTO',  'Recovery Time Objective',      'Maximum acceptable time to restore a system after a disruption.', 'Operations',   'Our RTO is 4 hours for tier-1 services.'),
('RPO',  'Recovery Point Objective',     'Maximum acceptable amount of data loss measured in time.', 'Operations',   'RPO of 1 hour means we back up every hour.'),
('MTTR', 'Mean Time To Recover',         'Average time to restore a service after a failure.', 'Operations',   'Our MTTR for P1 incidents is under 2 hours.'),
('MTTF', 'Mean Time To Failure',         'Average time a system operates before failing.', 'Engineering',  'High MTTF indicates reliable hardware.'),
('CI',   'Continuous Integration',       'Practice of automatically building and testing code changes frequently.', 'Engineering',  'CI pipelines run on every pull request.'),
('CD',   'Continuous Delivery',          'Automated process of releasing validated code changes to production safely.', 'Engineering',  'CD ensures every merge to main is deployable.'),
('API',  'Application Programming Interface', 'A set of defined rules enabling software components to communicate.', 'Engineering',  'The API exposes endpoints for data retrieval.'),
('SDK',  'Software Development Kit',     'A collection of tools, libraries, and documentation for building software.', 'Engineering',  'The GCP SDK simplifies calling cloud services.'),
('IAM',  'Identity and Access Management', 'Framework for managing digital identities and controlling resource access.', 'Security',     'IAM roles define who can deploy to Cloud Run.'),
('MCP',  'Model Context Protocol',       'An open protocol enabling AI models to call external tools and data sources in a standardized way.', 'AI',          'MCP Server exposes database tools to Gemini.'),
('RAG',  'Retrieval Augmented Generation','AI pattern combining retrieval of relevant documents with language model generation for grounded answers.', 'AI',  'RAG prevents hallucination by grounding answers in real data.'),
('LLM',  'Large Language Model',         'A deep learning model trained on large text datasets capable of generating and understanding human language.', 'AI',  'Gemini is the LLM powering our chatbot.'),
('GKE',  'Google Kubernetes Engine',     'Google Cloud managed Kubernetes service for running containerized workloads.', 'Infrastructure', 'Our microservices run on GKE.'),
('VPC',  'Virtual Private Cloud',        'Isolated virtual network within a cloud provider for secure resource communication.', 'Infrastructure', 'All Cloud Run services connect via VPC.'),
('P1',   'Priority 1 Incident',          'Highest severity incident causing complete service outage or critical data loss.', 'Operations',   'P1 incidents require all-hands response within 15 minutes.')
ON CONFLICT (short_form) DO NOTHING;

-- Sample Entities (abstract — replace with your domain objects)
INSERT INTO entities (code, name, category, status, metadata) VALUES
('ENT-001', 'Alpha Service',    'SERVICE',    'ACTIVE',   '{"tier": 1, "owner": "team-alpha"}'),
('ENT-002', 'Beta Platform',    'PLATFORM',   'ACTIVE',   '{"tier": 2, "owner": "team-beta"}'),
('ENT-003', 'Gamma Component',  'COMPONENT',  'ACTIVE',   '{"tier": 1, "owner": "team-gamma"}'),
('ENT-004', 'Delta Module',     'MODULE',     'INACTIVE', '{"tier": 3, "owner": "team-delta"}'),
('ENT-005', 'Epsilon Gateway',  'GATEWAY',    'ACTIVE',   '{"tier": 1, "owner": "team-epsilon"}')
ON CONFLICT (code) DO NOTHING;

-- Sample Metrics
INSERT INTO entity_metrics (entity_id, metric_name, metric_value, measured_at, period) VALUES
(1, 'uptime_percent',      99.95, NOW() - INTERVAL '1 day',  'DAILY'),
(1, 'response_time_ms',    145,   NOW() - INTERVAL '1 day',  'DAILY'),
(1, 'error_rate_percent',  0.05,  NOW() - INTERVAL '1 day',  'DAILY'),
(2, 'uptime_percent',      98.50, NOW() - INTERVAL '1 day',  'DAILY'),
(2, 'response_time_ms',    320,   NOW() - INTERVAL '1 day',  'DAILY'),
(3, 'uptime_percent',      99.99, NOW() - INTERVAL '1 day',  'DAILY'),
(5, 'uptime_percent',      99.80, NOW() - INTERVAL '1 day',  'DAILY'),
(5, 'response_time_ms',    89,    NOW() - INTERVAL '1 day',  'DAILY');

-- Sample Events
INSERT INTO events (entity_id, event_type, severity, description, status, occurred_at) VALUES
(2, 'SLA_BREACH',    'CRITICAL', 'Uptime dropped below 99% SLA threshold for ENT-002',     'OPEN',     NOW() - INTERVAL '2 hours'),
(1, 'HIGH_LATENCY',  'WARNING',  'Response time exceeded 500ms threshold for ENT-001',     'RESOLVED', NOW() - INTERVAL '6 hours'),
(5, 'DEGRADED',      'WARNING',  'Partial outage on ENT-005 gateway — 3 of 5 nodes down',  'OPEN',     NOW() - INTERVAL '30 minutes');
