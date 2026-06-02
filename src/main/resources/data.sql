-- Configuración por defecto o stub para iniciar el servicio
INSERT INTO smtp_configuration (host, port, username, password, protocol, active, auth_enabled, start_tls_enabled)
VALUES ('10.0.1.61', 25, '', '', 'smtp', true, false, false);
