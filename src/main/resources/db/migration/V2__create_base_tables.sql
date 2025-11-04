-- Members 테이블
CREATE TABLE members (
    id NUMBER PRIMARY KEY,
    username VARCHAR2(50) NOT NULL UNIQUE,
    password VARCHAR2(255) NOT NULL,
    role VARCHAR2(20) NOT NULL,
    email VARCHAR2(100) NOT NULL,
    company_name VARCHAR2(100),
    position VARCHAR2(25),
    mobile_number VARCHAR2(20),
    office_phone VARCHAR2(20),
    name VARCHAR2(50) NOT NULL,
    note VARCHAR2(255),
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    is_deleted NUMBER(1) DEFAULT 0 NOT NULL
);

-- Agents 테이블
CREATE TABLE agents (
    id NUMBER PRIMARY KEY,
    agent_key VARCHAR2(36) NOT NULL UNIQUE,
    agent_name VARCHAR2(100) NOT NULL,
    description VARCHAR2(255),
    agent_status VARCHAR2(20) NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    is_deleted NUMBER(1) DEFAULT 0 NOT NULL
);

-- Containers 테이블
CREATE TABLE containers (
    id NUMBER PRIMARY KEY,
    agent_id NUMBER NOT NULL,
    container_hash VARCHAR2(64) NOT NULL,
    state VARCHAR2(20) NOT NULL,
    name VARCHAR2(50) NOT NULL,
    cpu_quota NUMBER NOT NULL,
    cpu_period NUMBER NOT NULL,
    cpu_limit_cores NUMBER(6,2),
    online_cpus NUMBER NOT NULL,
    mem_limit NUMBER NOT NULL,
    oom_kills NUMBER NOT NULL,
    image_name VARCHAR2(255),
    image_size NUMBER,
    storage_limit NUMBER NOT NULL,
    metrics_initialized NUMBER(1) DEFAULT 0 NOT NULL,
    last_oom_killed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    is_deleted NUMBER(1) DEFAULT 0 NOT NULL,
    CONSTRAINT FK_CONTAINER_AGENT FOREIGN KEY (agent_id) REFERENCES agents(id)
);

-- Widget Templates 테이블
CREATE TABLE widget_templates (
    id NUMBER PRIMARY KEY,
    type VARCHAR2(50) NOT NULL UNIQUE,
    name VARCHAR2(100) NOT NULL,
    description VARCHAR2(255),
    default_span NUMBER NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL
);

-- Dashboards 테이블
CREATE TABLE dashboards (
    id NUMBER PRIMARY KEY,
    member_id NUMBER NOT NULL,
    name VARCHAR2(255) NOT NULL,
    is_default NUMBER(1) NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    is_deleted NUMBER(1) DEFAULT 0 NOT NULL,
    CONSTRAINT FK_DASHBOARD_MEMBER FOREIGN KEY (member_id) REFERENCES members(id)
);

-- Dashboard Widget 테이블
CREATE TABLE dashboard_widget (
    id NUMBER PRIMARY KEY,
    dashboard_id NUMBER NOT NULL,
    widget_template_id NUMBER NOT NULL,
    grid_position NUMBER NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    is_deleted NUMBER(1) DEFAULT 0 NOT NULL,
    CONSTRAINT FK_DASHBOARD_WIDGET_DASHBOARD FOREIGN KEY (dashboard_id) REFERENCES dashboards(id),
    CONSTRAINT FK_DASHBOARD_WIDGET_TEMPLATE FOREIGN KEY (widget_template_id) REFERENCES widget_templates(id),
    CONSTRAINT UK_DASHBOARD_POSITION UNIQUE (dashboard_id, grid_position)
);

-- Alert Rules 테이블
CREATE TABLE alert_rules (
    id NUMBER PRIMARY KEY,
    member_id NUMBER NOT NULL,
    rule_name VARCHAR2(100) NOT NULL,
    metric_type VARCHAR2(20) NOT NULL,
    is_enabled NUMBER(1) NOT NULL,
    info_threshold NUMBER(5,2),
    warning_threshold NUMBER(5,2),
    high_threshold NUMBER(5,2),
    critical_threshold NUMBER(5,2),
    cooldown_seconds NUMBER NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    is_deleted NUMBER(1) DEFAULT 0 NOT NULL,
    CONSTRAINT FK_ALERT_RULE_MEMBER FOREIGN KEY (member_id) REFERENCES members(id)
);

-- Alerts 테이블
CREATE TABLE alerts (
    id NUMBER PRIMARY KEY,
    rule_id NUMBER NOT NULL,
    member_id NUMBER NOT NULL,
    container_id NUMBER,
    message VARCHAR2(255) NOT NULL,
    metric_type VARCHAR2(255) NOT NULL,
    metric_value NUMBER(6,2) NOT NULL,
    is_read NUMBER(1) DEFAULT 0 NOT NULL,
    alert_level VARCHAR2(20),
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    collected_at TIMESTAMP,
    is_deleted NUMBER(1) DEFAULT 0 NOT NULL,
    CONSTRAINT FK_ALERT_RULE FOREIGN KEY (rule_id) REFERENCES alert_rules(id),
    CONSTRAINT FK_ALERT_MEMBER FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT FK_ALERT_CONTAINER FOREIGN KEY (container_id) REFERENCES containers(id)
);

CREATE INDEX IDX_ALERT_IS_READ ON alerts(is_read);
CREATE INDEX IDX_ALERT_MEMBER_IS_READ ON alerts(member_id, is_read);

-- Favorites 테이블
CREATE TABLE favorites (
    id NUMBER PRIMARY KEY,
    member_id NUMBER NOT NULL,
    container_id NUMBER NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT FK_FAVORITE_MEMBER FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT FK_FAVORITE_CONTAINER FOREIGN KEY (container_id) REFERENCES containers(id),
    CONSTRAINT UK_MEMBER_CONTAINER UNIQUE (member_id, container_id)
);