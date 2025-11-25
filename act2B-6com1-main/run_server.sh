#!/bin/bash
set -e  # Detiene la ejecución si ocurre un error

# Cambia al directorio del proyecto si es necesario
# cd "/ruta/a/tu/proyecto"

echo "=== Ejecutando mvn clean ==="
if ! mvn clean; then
    echo "[ERROR] mvn clean falló."
    exit 1
fi

echo "=== Ejecutando mvn test ==="
if ! mvn test; then
    echo "[ERROR] mvn test falló."
    exit 1
fi

echo "=== Ejecutando mvn exec:java ==="
if ! mvn exec:java; then
    echo "[ERROR] mvn exec:java falló."
    exit 1
fi

echo
echo "================================"
echo "Proceso finalizado. La consola permanece abierta."
echo "Presiona Ctrl+C para salir manualmente."
echo "================================"

# Mantener la consola abierta (solo si ejecutas desde doble clic en GUI)
# read -p "Presiona Enter para salir..."

# Uso:
# chmod +x run_maven.sh
# ./run_maven.sh