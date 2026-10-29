# Imagen para desplegar la aplicacion web (Render, Railway, Fly.io o cualquier servidor con Docker)
FROM eclipse-temurin:21-jdk AS compilacion
WORKDIR /app
COPY src src
COPY lib lib
RUN mkdir bin && javac -encoding UTF-8 -cp lib/pdfbox-app-3.0.3.jar -d bin $(find src -name "*.java")

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=compilacion /app/bin bin
COPY datos datos
COPY web web
ENV PORT=8080
EXPOSE 8080
CMD ["java", "-cp", "bin", "web.ServidorWeb"]
