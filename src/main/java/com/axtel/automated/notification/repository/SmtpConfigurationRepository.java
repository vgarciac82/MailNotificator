package com.axtel.automated.notification.repository;

import com.axtel.automated.notification.entity.SmtpConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SmtpConfigurationRepository extends JpaRepository<SmtpConfiguration, Long> {

    /**
     * Busca la configuración activa del servidor SMTP.
     * En caso de existir más de una, podemos limitarlo al "top 1".
     */
    Optional<SmtpConfiguration> findFirstByActiveTrue();
}
