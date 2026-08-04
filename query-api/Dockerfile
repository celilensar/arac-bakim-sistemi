# =========================================================================
#  query-api  ·  iki asamali (multi-stage) Docker image
# =========================================================================
# Neden iki asama?
#   - 1. asama (build): Maven + JDK ile jar'i uretir. Buyuk (~600MB), sadece
#     derleme icin gerekli; nihai image'a girmez.
#   - 2. asama (runtime): sadece JRE + uretilen jar. Kucuk ve guvenli
#     (Maven, kaynak kod, derleyici son image'da yok).
#   Sonuc: kucuk, temiz, hizli baslayan bir calisma image'i.
# =========================================================================

# ---- 1) BUILD ASAMASI ---------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Once sadece pom.xml'i kopyalayip bagimliliklari indiriyoruz.
# Docker katman cache: pom degismedikce bu katman yeniden calismaz,
# sadece kod degisince derleme tekrarlanir => cok daha hizli build.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Simdi kaynak kodu kopyala ve paketle (testleri deploy'da atliyoruz).
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# ---- 2) RUNTIME ASAMASI -------------------------------------------------
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Guvenlik: root yerine yetkisiz kullanici ile calistir.
RUN useradd -r -u 1001 spring
USER spring

# Build asamasindan sadece uretilen jar'i al.
COPY --from=build /app/target/*.jar app.jar

# App Runner konteynerin bu portu dinlemesini bekler.
EXPOSE 8080

# JVM'i konteyner-farkindaligiyla baslat (bellek limitine saygi duyar).
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
