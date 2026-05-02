DO $$
BEGIN
  EXECUTE format('GRANT CONNECT ON DATABASE %I TO app_user', current_database());
  EXECUTE format('GRANT CONNECT ON DATABASE %I TO app_readonly', current_database());
END$$;

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO app_user;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO app_readonly;

GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO app_user;