DELETE FROM smtp_configuration;
INSERT INTO smtp_configuration (
        id,
        host,
        port,
        username,
        password,
        protocol,
        active,
        auth_enabled,
        start_tls_enabled
    )
VALUES (
        1,
        '10.254.253.1',
        2525,
        'sai',
        'tP4WjrhzGK',
        'smtp',
        true,
        true,
        true
    );