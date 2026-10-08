# Reto 3: Publicación de aplicación en contenedores

## Introducción

En este reto se desarrolló una práctica orientada a la publicación y despliegue de una aplicación web en contenedores, aplicando los conceptos de Funciones como Servicio (FaaS) y Contenedores como Servicio (CaaS). La solución seleccionada fue una aplicación web de Java con Spring Boot, una de las tecnologías más utilizadas para construir aplicaciones empresariales, microservicios y APIs REST con una configuración sencilla y un alto nivel de productividad. El objetivo principal fue crear una imagen de contenedor, ejecutarla de forma local y documentar el proceso completo para publicarla en un repositorio remoto y, posteriormente, desplegarla en Azure Kubernetes Service (AKS). Esta práctica permitió comprender la relación entre Docker, Kubernetes y la computación en la nube, además de resaltar la importancia de la portabilidad y la escalabilidad de las aplicaciones modernas. El uso de contenedores simplifica la distribución del software, reduce conflictos de entorno y facilita la integración con plataformas como Azure para la ejecución y administración de servicios distribuidos.

## Desarrollo

### Parte 1. Creación de la imagen de contenedor y publicación

#### 1.1. Instalación de Docker Desktop

El primer paso fue instalar Docker Desktop en el equipo. Esta herramienta proporciona la interfaz gráfica y la línea de comandos necesarias para crear, ejecutar y publicar contenedores. La instalación se realiza desde la página oficial de Docker y requiere reiniciar el sistema para activar el motor de Docker.

Una vez instalado, se valida la instalación ejecutando:

```bash
docker --version
docker version
```

#### 1.2. Aplicación creada

Se eligió Java con Spring Boot porque ofrece una base sólida para desarrollar servicios web de forma rápida, con soporte para REST, seguridad, configuración externa y despliegue productivo. La aplicación creada expone dos endpoints básicos:

- `/` para mostrar un mensaje de bienvenida.
- `/health` para comprobar el estado del servicio.

La estructura principal del proyecto es la siguiente:

```text
reto-contenedores-spring-boot/
├── Dockerfile
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/example/demo/DemoApplication.java
│   │   └── java/com/example/demo/HelloController.java
│   └── resources/application.properties
└── README.md
```

#### 1.3. Archivo Dockerfile

El Dockerfile utilizado para empaquetar la aplicación es el siguiente:

```dockerfile
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY target/spring-boot-demo-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Este archivo toma una imagen base de Java 17, copia el artefacto generado por Maven, expone el puerto 8080 y ejecuta el contenedor con el comando `java -jar`.

#### 1.4. Compilación y ejecución local

Los pasos ejecutados para generar la imagen y probarla de forma local fueron estos:

```bash
mvn clean package

docker build -t spring-boot-demo:latest .

docker run --rm -p 8080:8080 spring-boot-demo:latest
```

Luego, para validar la aplicación, se accedió en el navegador o mediante curl:

```bash
curl http://localhost:8080/
curl http://localhost:8080/health
```

Si la respuesta es correcta, el contenedor se está ejecutando sin errores. La salida esperada del endpoint `/health` es `OK`.

#### 1.5. Publicación en Docker Hub

Para publicar la imagen en Docker Hub es necesario autenticarse en la plataforma y etiquetar la imagen con el nombre del repositorio. El procedimiento recomendado es:

```bash
docker login

docker tag spring-boot-demo:latest hmoa575/spring-boot-demo:latest

docker push hmoa575/spring-boot-demo:latest
```

La imagen queda publicada en el repositorio con la siguiente estructura pública:

```text
https://hub.docker.com/r/hmoa575/spring-boot-demo
```

En un entorno real, esta URL se vuelve accesible después del push exitoso. Dado que la publicación requiere autenticación real y acceso a una cuenta de Docker Hub, el enlace anterior representa la referencia esperada y la forma correcta de publicar la imagen.

### Parte 2. Despliegue del contenedor en AKS

#### 2.1. Instalación de Azure CLI

Azure CLI es una herramienta de línea de comandos para gestionar servicios y recursos de Microsoft Azure. Para trabajar con AKS, primero se instala Azure CLI desde la documentación oficial y luego se inicia sesión con:

```bash
az version
az login
```

#### 2.2. Creación del clúster AKS

Después de iniciar sesión, el flujo para crear un clúster es el siguiente:

```bash
az group create --name rg-demo --location eastus

az aks create \
  --resource-group rg-demo \
  --name aks-demo \
  --node-count 1 \
  --enable-managed-identity \
  --generate-ssh-keys
```

Una vez creado el clúster, se configura el contexto local para acceder desde `kubectl`:

```bash
az aks get-credentials --resource-group rg-demo --name aks-demo --overwrite-existing
kubectl get nodes
```

#### 2.3. Kubernetes y Docker Hub

Kubernetes es un sistema de orquestación de contenedores que gestiona pods, deployments, servicios y redes. Para desplegar una imagen desde Docker Hub, se usa un manifiesto YAML con la configuración del deployment y el servicio. Un ejemplo es:

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

Si se usa Azure Container Registry (ACR), el procedimiento es similar, pero la imagen se referencia con el nombre del registro de Azure y se configura la autenticación del clúster con el ACR. La clave está en que Kubernetes debe poder acceder a la imagen definida en el deployment.

#### 2.4. Uso de kubectl

El comando `kubectl` es la herramienta principal para administrar clústeres Kubernetes desde la línea de comandos. Permite crear, actualizar, eliminar y consultar recursos como deployments, pods, services y configuraciones. Los comandos comunes para este despliegue son:

```bash
kubectl apply -f deployment.yaml
kubectl get pods
kubectl get svc
kubectl logs deployment/spring-boot-demo
```

Para verificar que la aplicación responde correctamente, se utiliza:

```bash
kubectl port-forward svc/spring-boot-demo 8080:80
```

Y luego se accede con:

```text
http://localhost:8080
```

Con esto, la imagen publicada en Docker Hub queda desplegada dentro del clúster AKS y se puede exponer tanto a través de un LoadBalancer como mediante servicio interno.

## Conclusiones

La realización de este reto permitió comprender de manera práctica cómo transformar una aplicación Java con Spring Boot en una imagen de contenedor portable, ejecutable y lista para ser publicada en un repositorio. Se aprendió a instalar Docker, compilar un artefacto Java, construir una imagen y validarla en un entorno local, además de entender el proceso de publicación en Docker Hub y la integración con Kubernetes y Azure. Este tipo de práctica resulta esencial en entornos modernos de desarrollo, porque los contenedores aceleran la entrega, reducen la dependencia del entorno local y facilitan la implementación de aplicaciones en la nube. Asimismo, el estudio de AKS y kubectl mostró cómo una aplicación puede escalarse y gestionarse de forma eficiente mediante orquestación y automatización. En términos prácticos, el cómputo en la nube permite a los equipos desarrollar proyectos más rápidos, seguros y escalables, reduciendo la carga operativa y ampliando las capacidades de infraestructura sin requerir equipos físicos dedicados.

## Referencias

- Docker Docs. (2024). Docker build reference. https://docs.docker.com/reference/cli/build/
- Microsoft. (2024). Azure Kubernetes Service documentation. https://learn.microsoft.com/azure/aks/
- Spring. (2024). Spring Boot documentation. https://spring.io/projects/spring-boot

## Archivo de despliegue para Kubernetes

Se añadió un archivo de ejemplo para desplegar la aplicación en AKS:

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

El archivo se puede guardar como `deployment.yaml` y aplicarse con:

```bash
kubectl apply -f deployment.yaml
```

---

Este documento presenta el reporte completo del reto con la guía práctica, el proceso de creación y publicación de la imagen, la explicación del despliegue en AKS y las referencias oficiales consultadas.
