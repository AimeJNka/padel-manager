DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'app_user') THEN
    CREATE USER app_user WITH PASSWORD 'padel_password';
END IF;
  IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'app_readonly') THEN
    CREATE USER app_readonly WITH PASSWORD 'readonly_password';
END IF;
END
$$;