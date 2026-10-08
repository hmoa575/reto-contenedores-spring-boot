# Reto 3: Publicación de aplicación en contenedores

## 1. Introducción

Este reto tiene como objetivo consolidar el conocimiento sobre contenedores y servicios en la nube aplicando dos conceptos clave: la creación de una imagen de contenedor para una aplicación web y la investigación del proceso necesario para desplegar esa imagen en Azure Kubernetes Service (AKS). La solución elegida fue una aplicación web de Java con Spring Boot, un framework ampliamente utilizado para desarrollar servicios REST, microservicios y backends empresariales con estructura clara y eficiente. El enfoque se centra en demostrar cómo un proyecto Java puede ser empaquetado en un contenedor portable, ejecutado de manera local y preparado para ser publicado en un registro de contenedores como Docker Hub o Azure Container Registry. Además, se analiza el procedimiento para desplegarlo en AKS mediante Azure CLI, Kubernetes y kubectl, mostrando la integración entre contenedores, orquestación y computación en la nube. Esta práctica refleja un enfoque moderno de entrega continua y despliegue escalable, alineado con las arquitecturas actuales de software basado en servicios y automatización.

## 2. Desarrollo

### Parte 1. Práctica de creación de imagen y publicación

#### 2.1. Elección de la aplicación

Se seleccionó una aplicación web de Java con Spring Boot porque permite crear servicios simples y robustos, con configuración reducida, despliegue ágil y compatibilidad directa con contenedores. En este caso, la aplicación expone una ruta raíz para responder con un mensaje de salud y una ruta /health para verificar el estado del servicio. Esta estructura facilita la validación local y la comprobación del funcionamiento del contenedor.

#### 2.2. Instalación de Docker Desktop

El primer paso consiste en instalar Docker Desktop en el equipo. Esta herramienta ofrece una interfaz gráfica para gestionar imágenes, contenedores, redes y volúmenes, y además incluye la CLI de Docker para ejecutar comandos desde la línea de comandos. La instalación debe hacerse desde la página oficial de Docker, siguiendo el asistente de instalación según el sistema operativo del equipo. Una vez finalizada, es necesario confirmar que el servicio del motor de Docker esté activo ejecutando:

```bash
docker --version
docker version
```

#### 2.3. Creación de la aplicación Spring Boot

Se creó un proyecto Maven con Spring Boot 3 y Java 17. La aplicación contiene un controlador con dos rutas:

- `/` devuelve un mensaje indicando que la aplicación está funcionando.
- `/health` devuelve un estado de salud para validación operativa.

Los archivos principales del proyecto creados en este repositorio son:

- `pom.xml`
- `src/main/java/com/example/demo/DemoApplication.java`
- `src/main/java/com/example/demo/HelloController.java`
- `src/main/resources/application.properties`

#### 2.4. Dockerfile

El contenedor se construye con un `Dockerfile` que se encarga de empaquetar la aplicación Java en una imagen ejecutable. El contenido del Dockerfile para esta solución es el siguiente:

```dockerfile
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY target/spring-boot-demo-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Este archivo usa una imagen base ligera de Java 17, copia el artefacto generado por Maven, expone el puerto 8080 y ejecuta la aplicación con el comando `java -jar`.

#### 2.5. Construcción de la imagen local

Para generar la imagen se ejecuta el siguiente proceso:

```bash
mvn clean package

docker build -t spring-boot-demo:latest .
```

Posteriormente, se valida la ejecución local del contenedor:

```bash
docker run --rm -p 8080:8080 spring-boot-demo:latest
```

La aplicación queda disponible en el puerto 8080 y puede validarse con:

```bash
curl http://localhost:8080/
curl http://localhost:8080/health
```

Si la respuesta es correcta, la imagen ha sido creada y ejecutada localmente con éxito.

#### 2.6. Publicación en Docker Hub

Para publicar la imagen en Docker Hub, se requiere que el usuario tenga una cuenta activa y que el repositorio sea público o privado según el caso. El proceso recomendado es:

```bash
docker login

docker tag spring-boot-demo:latest hmoa575/spring-boot-demo:latest

docker push hmoa575/spring-boot-demo:latest
```

La imagen queda asociada a un repositorio de Docker Hub siguiendo el formato:

```text
https://hub.docker.com/r/hmoa575/spring-boot-demo
```

Importante: la publicación real requiere autenticación en Docker Hub y acceso desde el equipo local. En este entorno de trabajo no se ejecutó el push real, pero la URL anterior representa la estructura y la finalidad esperada de la publicación.

### Parte 2. Guía para despliegue de la imagen en AKS

#### 2.7. Azure CLI, AKS y Kubernetes

Azure Kubernetes Service (AKS) es un servicio gestionado por Microsoft Azure para orquestar contenedores con Kubernetes. Para trabajar con él, es necesario instalar Azure CLI en el equipo. La instalación puede realizarse con el instalador oficial de Microsoft o mediante un paquete del sistema operativo. La documentación oficial de Microsoft indica que el flujo habitual es:

```bash
az version
az login
```

A continuación se crea un recurso de grupo y un clúster de AKS:

```bash
az group create --name rg-demo --location eastus
az aks create \
  --resource-group rg-demo \
  --name aks-demo \
  --node-count 1 \
  --enable-managed-identity \
  --generate-ssh-keys
```

Luego se configura el contexto local para acceder al clúster:

```bash
az aks get-credentials --resource-group rg-demo --name aks-demo --overwrite-existing
kubectl get nodes
```

#### 2.8. Kubernetes y Docker Hub

Kubernetes necesita un manifiesto de despliegue para definir cómo se ejecutará el contenedor. Se usa un archivo `deployment.yaml` con la imagen publicada en Docker Hub. Un ejemplo típico es:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: spring-boot-demo
spec:
  replicas: 2
  selector:
    matchLabels:
      app: spring-boot-demo
  template:
    metadata:
      labels:
        app: spring-boot-demo
    spec:
      containers:
        - name: spring-boot-demo
          image: docker.io/hmoa575/spring-boot-demo:latest
          ports:
            - containerPort: 8080
---
apiVersion: v1
kind: Service
metadata:
  name: spring-boot-demo
spec:
  type: LoadBalancer
  selector:
    app: spring-boot-demo
  ports:
    - port: 80
      targetPort: 8080
```

La imagen puede apuntar a Docker Hub o, si se usa Azure, a Azure Container Registry (ACR). En el caso de ACR, se suele autenticar el clúster con el registro y se usa la referencia completa del nombre del repositorio. La idea central es que Kubernetes recupere la imagen definida en el manifiesto y cree los pods necesarios.

#### 2.9. Uso de kubectl

El comando `kubectl` es la herramienta principal para interactuar con clústeres Kubernetes desde la línea de comandos. Su función es aplicar, inspeccionar y administrar recursos del clúster, como deployments, pods, servicios y configuraciones. La secuencia de despliegue sería:

```bash
kubectl apply -f deployment.yaml
kubectl get pods
kubectl get svc
kubectl logs deployment/spring-boot-demo
```

Para comprobar el acceso, se puede usar:

```bash
kubectl port-forward svc/spring-boot-demo 8080:80
```

Luego se accede a la URL local:

```text
http://localhost:8080
```

Con esto se valida que el contenedor originado desde Docker Hub o ACR se está ejecutando en AKS sin problemas.

## 3. Conclusiones

La realización de este reto permitió comprender de manera práctica cómo una aplicación Java con Spring Boot puede transformarse en un artefacto portable y ejecutable en un contenedor, además de visualizar el flujo completo desde la creación de la imagen hasta su publicación y despliegue. Se reforzó la comprensión del ciclo de vida de un contenedor: preparación del artefacto, creación del Dockerfile, construcción de la imagen, validación local y publicación en un registro. Asimismo, se analizó el papel de AKS y Kubernetes en la orquestación de contenedores a escala, destacando que Azure CLI y kubectl permiten gestionar clústeres y recursos de manera eficiente desde la línea de comandos. Este conocimiento resulta fundamental para proyectos actuales basados en microservicios, despliegues automatizados y arquitecturas nativas en la nube. En la práctica, la computación en la nube y la virtualización ligera permiten acelerar el desarrollo, mejorar la portabilidad y reducir la complejidad de la infraestructura, brindando mayor agilidad y escalabilidad a los equipos de ingeniería de software.

## 4. Referencias

AAPA (s.f.). Azure Kubernetes Service (AKS). Microsoft Learn. https://learn.microsoft.com/azure/aks/

Docker Docs. (2024). Docker build reference. Docker Documentation. https://docs.docker.com/reference/cli/build/

Spring. (2024). Spring Boot. Spring. https://spring.io/projects/spring-boot

## 5. Código fuente del proyecto

El proyecto base para esta práctica se encuentra en este repositorio y puede usarse como referencia para crear y validar la imagen Docker localmente. Los comandos de ejecución son:

```bash
mvn clean package

docker build -t spring-boot-demo:latest .
docker run --rm -p 8080:8080 spring-boot-demo:latest
```

La aplicación responde en:

```text
http://localhost:8080/
http://localhost:8080/health
```

## 6. Nota de publicación

La publicación a Docker Hub requiere una cuenta real del usuario y un `docker login` previo. El enlace mostrado en esta guía es un ejemplo de la estructura de publicación esperada para el repositorio de la imagen; el push real requiere autenticación local.

La documentación oficial del proveedor o del producto puede variar según la fecha de consulta; por ello, se recomienda verificar siempre la referencia más reciente al momento de desplegar en entornos reales.

---

El ejemplo anterior se integra como reporte académico del reto y está documentado de acuerdo con la práctica descrita por la asignatura.


















































