-- Recommended read-only account for Codex / MCP access.
-- Replace the password before executing in your local MySQL.

CREATE USER IF NOT EXISTS 'kb_mcp_ro'@'localhost' IDENTIFIED BY 'change_me';
CREATE USER IF NOT EXISTS 'kb_mcp_ro'@'127.0.0.1' IDENTIFIED BY 'change_me';

GRANT SELECT, SHOW VIEW ON kb_agent.* TO 'kb_mcp_ro'@'localhost';
GRANT SELECT, SHOW VIEW ON kb_agent.* TO 'kb_mcp_ro'@'127.0.0.1';

FLUSH PRIVILEGES;
