#!/usr/bin/env bash

# ==============================================================================
# SICOVE - Notificator Service Installer (Systemd)
# ==============================================================================
# Este script automatiza la instalación del microservicio MailNotificator como un
# servicio del sistema (systemd) en entornos de desarrollo/producción Linux.
# ==============================================================================

# Colores ANSI para salida visual premium
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # Sin color
BOLD='\033[1m'

# Configuración por defecto
SERVICE_NAME="notificator-service"
APP_DIR="/opt/sai_ms/notificator"
JAR_NAME="AutomatedNotification.jar"
SYSTEMD_PATH="/etc/systemd/system/${SERVICE_NAME}.service"
APP_USER="saidev"
APP_GROUP="saidev"
JAVA_HOME="/opt/jdk-25.0.3"
JAVA_PATH="${JAVA_HOME}/bin/java"

echo -e "${CYAN}${BOLD}======================================================================${NC}"
echo -e "${CYAN}${BOLD}      INSTALADOR DE SERVICIO LINUX - NOTIFICATOR SERVICE              ${NC}"
echo -e "${CYAN}${BOLD}======================================================================${NC}\n"

# 1. Verificar permisos de root
if [ "$EUID" -ne 0 ]; then
    echo -e "${RED}❌ Error: Este script debe ejecutarse como root (sudo).${NC}"
    echo -e "Ejecute: ${BOLD}sudo ./install-notificator-service.sh${NC}\n"
    exit 1
fi

# 2. Verificar la presencia de Java en la ruta configurada
echo -e "${BLUE}${BOLD}[1/7] Verificando entorno Java en ${JAVA_HOME}...${NC}"
if [ -x "$JAVA_PATH" ]; then
    JAVA_VERSION=$("$JAVA_PATH" -version 2>&1 | head -n 1)
    echo -e "${GREEN}✓ Java detectado en:${NC} $JAVA_PATH"
    echo -e "${GREEN}✓ Versión:${NC} $JAVA_VERSION"
else
    echo -e "${RED}❌ Error: No se encontró un ejecutable de Java en $JAVA_PATH${NC}"
    echo -e "Por favor, asegúrese de instalar JDK 25 en '${JAVA_HOME}' antes de continuar.\n"
    exit 1
fi

# 3. Crear usuario y grupo de sistema para el servicio
echo -e "\n${BLUE}${BOLD}[2/7] Creando usuario de sistema '${APP_USER}'...${NC}"
if getent passwd "$APP_USER" >/dev/null 2>&1; then
    echo -e "${YELLOW}⚠️ El usuario '${APP_USER}' ya existe. Continuando...${NC}"
else
    groupadd -r "$APP_GROUP" 2>/dev/null
    useradd -r -s /bin/false -g "$APP_GROUP" "$APP_USER"
    echo -e "${GREEN}✓ Usuario y grupo '${APP_USER}:${APP_GROUP}' creados exitosamente.${NC}"
fi

# 4. Crear directorios de la aplicación
echo -e "\n${BLUE}${BOLD}[3/7] Creando directorios en '${APP_DIR}'...${NC}"
mkdir -p "$APP_DIR"
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Directorio '${APP_DIR}' listo.${NC}"
else
    echo -e "${RED}❌ Error al crear el directorio de aplicación.${NC}\n"
    exit 1
fi

# 5. Comprobar o solicitar el archivo JAR
echo -e "\n${BLUE}${BOLD}[4/7] Configurando archivo JAR...${NC}"
if [ -f "./target/AutomatedNotification-0.0.1-SNAPSHOT.jar" ]; then
    echo -e "${YELLOW}ℹ️ Detectado JAR local en target. Copiando a ${APP_DIR}...${NC}"
    cp "./target/AutomatedNotification-0.0.1-SNAPSHOT.jar" "${APP_DIR}/${JAR_NAME}"
elif [ -f "./AutomatedNotification-0.0.1-SNAPSHOT.jar" ]; then
    echo -e "${YELLOW}ℹ️ Detectado JAR local. Copiando a ${APP_DIR}...${NC}"
    cp "./AutomatedNotification-0.0.1-SNAPSHOT.jar" "${APP_DIR}/${JAR_NAME}"
elif [ -f "${APP_DIR}/${JAR_NAME}" ]; then
    echo -e "${GREEN}✓ JAR '${JAR_NAME}' ya existe en el destino.${NC}"
else
    echo -e "${YELLOW}⚠️ No se encontró el archivo JAR en el directorio actual.${NC}"
    echo -e "Por favor, asegúrese de compilar primero con: ${BOLD}mvn clean package${NC}"
    echo -e "Y copie el archivo JAR resultante a: ${BOLD}${APP_DIR}/${JAR_NAME}${NC}"
    # Crear un placeholder para que no falle la creación del servicio
    touch "${APP_DIR}/${JAR_NAME}"
fi

# 6. Crear archivo de inicialización SQL externo para la base de datos de desarrollo
echo -e "\n${BLUE}${BOLD}[5/7] Configurando inicialización de BD de desarrollo (data-dev.sql)...${NC}"
DATA_DEV_SQL="${APP_DIR}/data-dev.sql"
if [ ! -f "$DATA_DEV_SQL" ]; then
    cat << EOF > "$DATA_DEV_SQL"
-- ==============================================================================
# Configuración SMTP para entorno de Desarrollo (Develop)
# Modifica estos valores con los del servidor SMTP real de la institución.
# ==============================================================================
DELETE FROM smtp_configuration;

INSERT INTO smtp_configuration (id, host, port, username, password, protocol, active, auth_enabled, start_tls_enabled)
VALUES (1, '10.0.1.61', 25, '', '', 'smtp', true, false, false);
EOF
    echo -e "${GREEN}✓ Archivo plantilla '${DATA_DEV_SQL}' creado.${NC}"
    echo -e "${YELLOW}⚠️ RECUERDA: Modifica este archivo con los datos del servidor SMTP real de la institución.${NC}"
else
    echo -e "${GREEN}✓ Archivo de datos '${DATA_DEV_SQL}' ya existe. Conservando configuración actual.${NC}"
fi

# 7. Crear archivo de configuración para variables de entorno y JVM options
echo -e "\n${BLUE}${BOLD}[6/7] Generando archivo de configuración en '${APP_DIR}/${SERVICE_NAME}.conf'...${NC}"
cat << EOF > "${APP_DIR}/${SERVICE_NAME}.conf"
# ==============================================================================
# Configuración del servicio Notificator (MailNotificator)
# ==============================================================================

# Opciones de la Máquina Virtual de Java (JVM)
JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

# Puerto y perfil activo de Spring Boot (0 = aleatorio/random)
SPRING_PROFILES_ACTIVE=dev
SERVER_PORT=0

# URL del servidor Eureka
EUREKA_URL=http://localhost:8761/eureka

# Ruta al archivo SQL externo para omitir el data.sql por defecto en Git
SPRING_SQL_INIT_DATA_LOCATIONS=file:${DATA_DEV_SQL}
EOF
echo -e "${GREEN}✓ Archivo de configuración generado.${NC}"

# 8. Crear el archivo de servicio Systemd
echo -e "\n${BLUE}${BOLD}[7/7] Generando archivo de servicio systemd en '${SYSTEMD_PATH}'...${NC}"
cat << EOF > "$SYSTEMD_PATH"
[Unit]
Description=SAI SICOVE Notificator Service
After=syslog.target network.target

[Service]
User=${APP_USER}
Group=${APP_GROUP}
Type=simple
WorkingDirectory=${APP_DIR}

# Definir JAVA_HOME y variables de entorno
Environment="JAVA_HOME=${JAVA_HOME}"
# Cargar las variables del archivo .conf
EnvironmentFile=${APP_DIR}/${SERVICE_NAME}.conf

# Comando de ejecución
ExecStart=${JAVA_PATH} \$JAVA_OPTS -Dspring.profiles.active=\${SPRING_PROFILES_ACTIVE} -Dserver.port=\${SERVER_PORT} -Deureka.client.service-url.defaultZone=\${EUREKA_URL} -Dspring.sql.init.data-locations=\${SPRING_SQL_INIT_DATA_LOCATIONS} -jar ${APP_DIR}/${JAR_NAME}

# Configuración de reinicios y límites
Restart=always
RestartSec=10
SuccessExitStatus=143
LimitNOFILE=65536

# Logs de salida
StandardOutput=syslog
StandardError=syslog
SyslogIdentifier=${SERVICE_NAME}

[Install]
WantedBy=multi-user.target
EOF
echo -e "${GREEN}✓ Archivo de servicio systemd generado.${NC}"

# Ajustar permisos de directorios y archivos de logs
echo -e "\n${BLUE}${BOLD}Ajustando permisos...${NC}"
chown -R "$APP_USER":"$APP_GROUP" "$APP_DIR"
chmod 640 "${APP_DIR}/${SERVICE_NAME}.conf"
chmod 640 "$DATA_DEV_SQL"
chmod 750 "$APP_DIR"
if [ -f "${APP_DIR}/${JAR_NAME}" ]; then
    chmod 644 "${APP_DIR}/${JAR_NAME}"
fi

# Asegurar permisos de logs
mkdir -p /var/log/services
chown -R "$APP_USER":"$APP_GROUP" /var/log/services

# Recargar systemd daemon y habilitar servicio
echo -e "${YELLOW}ℹ️ Recargando daemon de systemd...${NC}"
systemctl daemon-reload
systemctl enable "$SERVICE_NAME" >/dev/null 2>&1

echo -e "\n${GREEN}${BOLD}======================================================================${NC}"
echo -e "${GREEN}${BOLD}        ¡PROCESO DE INSTALACIÓN COMPLETADO CON ÉXITO!                 ${NC}"
echo -e "${GREEN}${BOLD}======================================================================${NC}"
echo -e "Para finalizar y ejecutar el servicio Notificator:"
echo -e " 1. Copie su archivo JAR compilado a: ${BOLD}${APP_DIR}/${JAR_NAME}${NC}"
echo -e " 2. Modifique la configuración SMTP en el servidor de desarrollo en:"
echo -e "    ${BOLD}${DATA_DEV_SQL}${NC}"
echo -e " 3. Asegure la propiedad del JAR: ${BOLD}chown ${APP_USER}:${APP_GROUP} ${APP_DIR}/${JAR_NAME}${NC}"
echo -e " 4. Inicie el servicio: ${BOLD}systemctl start ${SERVICE_NAME}${NC}"
echo -e " 5. Verifique el estado: ${BOLD}systemctl status ${SERVICE_NAME}${NC}"
echo -e " 6. Revise los logs en vivo: ${BOLD}journalctl -u ${SERVICE_NAME} -f -n 100${NC}"
echo -e "    O en el archivo de logs: ${BOLD}tail -f /var/log/services/Eureka.log (o el configurado)${NC}\n"
