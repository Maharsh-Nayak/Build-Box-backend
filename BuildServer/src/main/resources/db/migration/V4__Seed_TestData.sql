INSERT INTO users (email, name) VALUES ('test@example.com', 'Test User') ON CONFLICT (email) DO NOTHING;
INSERT INTO projects (name, slug, repo_url, user_id, base_path) 
SELECT 'Test Fullstack App', 'test-fullstack-app', 'https://github.com/Build-Box/test-node-app', id, 'Backend'
FROM users WHERE email = 'test@example.com'
ON CONFLICT (slug) DO NOTHING;
