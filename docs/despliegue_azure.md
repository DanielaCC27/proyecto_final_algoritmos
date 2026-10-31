# Desplegar la aplicación en Azure

La aplicación es un solo proceso Java dentro de una imagen Docker (`Dockerfile`). Lee el puerto de la variable de entorno `PORT` (por defecto 8080), así que sirve en cualquier servicio de contenedores. La forma más corta en Azure es **Azure Container Apps**: Azure construye la imagen desde el `Dockerfile` en la nube, sin instalar Docker en tu equipo.

## Qué necesitas

- Una cuenta de Azure con suscripción activa (por ejemplo *Azure for Students*, que se activa con el correo institucional `@uqvirtual.edu.co`).
- Azure CLI instalado: https://learn.microsoft.com/cli/azure/install-azure-cli-windows
- El proyecto en tu equipo (carpeta `proyecto-final`, la que tiene el `Dockerfile`).

## Pasos (PowerShell o la terminal de VS Code)

```powershell
# 1. Entrar a tu cuenta (abre el navegador)
az login

# 2. Instalar la extensión de Container Apps y registrar los proveedores (solo la primera vez)
az extension add --name containerapp --upgrade
az provider register --namespace Microsoft.App
az provider register --namespace Microsoft.OperationalInsights

# 3. Desde la carpeta proyecto-final: construir y publicar con un solo comando
cd Documentos\proyecto-final
az containerapp up `
  --name bibliometria-uq `
  --resource-group rg-bibliometria `
  --location eastus `
  --source . `
  --ingress external `
  --target-port 8080
```

Al terminar, el comando imprime la URL pública, algo como `https://bibliometria-uq.<region>.azurecontainerapps.io`. Esa es la dirección que va en el informe, en la presentación y en el README.

Notas:
- El nombre (`--name`) debe ir en minúsculas y no repetirse en la región; si Azure lo rechaza, cambia el nombre.
- Si `eastus` no está permitido en tu suscripción de estudiante, prueba `eastus2`, `centralus` o `westus3`.
- Con `--source .` Azure crea por ti el grupo de recursos, el registro de contenedores y el entorno.

## Probar

1. Abre la URL en el navegador: debe cargar la página con las tres pestañas (similitud, agrupamiento y búsqueda).
2. Prueba un servicio directamente: `<URL>/api/articulos` devuelve los 20 artículos en JSON.

## Actualizar después de un cambio

El despliegue real usa la imagen que publica GitHub Actions en ghcr.io (ver el README, sección *Despliegue en Azure*). Tras subir los cambios a `main` y esperar la marca verde en *Actions*:

```powershell
az containerapp update --name bibliometria-uq --resource-group rg-bibliometria `
  --image ghcr.io/danielacc27/proyecto_final_algoritmos:latest --revision-suffix v2
```

Usa un sufijo distinto en cada actualización (`v3`, `v4`, …).

## Ver errores

```powershell
az containerapp logs show --name bibliometria-uq --resource-group rg-bibliometria --follow
```

## Costos y cómo apagarlo

Container Apps cobra por uso y tiene una cuota mensual gratuita, pero revisa tu saldo en el portal de Azure. Cuando termines la sustentación puedes borrar todo con:

```powershell
az group delete --name rg-bibliometria --yes
```

## Alternativa desde el portal (sin comandos)

En https://portal.azure.com: *Crear un recurso > Container Apps > Crear*, elige *Implementar desde un repositorio de GitHub*, selecciona este repositorio, la rama principal y la ruta del `Dockerfile`; en *Ingreso* activa *Aceptar tráfico desde cualquier lugar* y puerto de destino `8080`.
