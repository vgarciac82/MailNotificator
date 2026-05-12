-- Configuración por defecto o stub para iniciar el servicio
INSERT INTO smtp_configuration (host, port, username, password, protocol, active, auth_enabled, start_tls_enabled)
VALUES ('localhost', 25, '', '', 'smtp', true, false, false);
