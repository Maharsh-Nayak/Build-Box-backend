CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       email VARCHAR(255) UNIQUE NOT NULL,
                       name VARCHAR(255),
                       avatar_url TEXT,
                       password_hash TEXT,
                       provider VARCHAR(50),
                       provider_id VARCHAR(255),
                       email_verified BOOLEAN DEFAULT FALSE,
                       created_at TIMESTAMP DEFAULT NOW(),
                       updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE teams (
                       id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(255) NOT NULL,
                       owner_id BIGINT REFERENCES users(id),
                       created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE team_members (
                              id BIGSERIAL PRIMARY KEY,
                              team_id BIGINT REFERENCES teams(id) ON DELETE CASCADE,
                              user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
                              role VARCHAR(20),
                              created_at TIMESTAMP DEFAULT NOW(),
                              UNIQUE(team_id, user_id)
);

CREATE TABLE projects (
                          id BIGSERIAL PRIMARY KEY,
                          name VARCHAR(255) NOT NULL,
                          slug VARCHAR(255) UNIQUE NOT NULL,
                          user_id BIGINT REFERENCES users(id),
                          team_id BIGINT REFERENCES teams(id),
                          repo_url TEXT,
                          created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE deployments (
                             id BIGSERIAL PRIMARY KEY,
                             project_id BIGINT REFERENCES projects(id) ON DELETE CASCADE,
                             version INT DEFAULT 1,
                             status VARCHAR(20) DEFAULT 'QUEUED',
                             deployment_url TEXT,
                             commit_id VARCHAR(255),
                             commit_message TEXT,
                             branch VARCHAR(255),
                             created_at TIMESTAMP DEFAULT NOW(),
                             completed_at TIMESTAMP
);

CREATE TABLE domains (
                         id BIGSERIAL PRIMARY KEY,
                         project_id BIGINT REFERENCES projects(id) ON DELETE CASCADE,
                         domain VARCHAR(255) UNIQUE NOT NULL,
                         verified BOOLEAN DEFAULT FALSE,
                         created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE env_variables (
                               id BIGSERIAL PRIMARY KEY,
                               project_id BIGINT REFERENCES projects(id) ON DELETE CASCADE,
                               name VARCHAR(255) NOT NULL,
                               value TEXT NOT NULL,
                               environment VARCHAR(20),
                               created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE build_logs (
                            id BIGSERIAL PRIMARY KEY,
                            deployment_id BIGINT REFERENCES deployments(id) ON DELETE CASCADE,
                            log TEXT NOT NULL,
                            timestamp TIMESTAMP DEFAULT NOW()
);

CREATE TABLE activity (
                          id BIGSERIAL PRIMARY KEY,
                          user_id BIGINT REFERENCES users(id),
                          project_id BIGINT REFERENCES projects(id),
                          action TEXT NOT NULL,
                          created_at TIMESTAMP DEFAULT NOW()
);
