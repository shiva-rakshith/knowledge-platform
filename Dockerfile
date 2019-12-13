FROM openjdk:8-jre-alpine
RUN adduser -u 1001 -h /home/sunbird/ -D sunbird \
    && mkdir -p /home/sunbird \
    && chown -R sunbird:sunbird /home/sunbird
USER sunbird
# This assume that the content-service dist is unzipped.
COPY --chown=sunbird ./learning-api/content-service-1.0-SNAPSHOT /home/sunbird/content-service-1.0-SNAPSHOT
COPY --chown=sunbird ./schemas /home/sunbird/content-service-1.0-SNAPSHOT/schemas
WORKDIR /home/sunbird/
CMD java  -cp '/home/sunbird/content-service-1.0-SNAPSHOT/lib/*' play.core.server.ProdServerStart /home/sunbird/content-service-1.0-SNAPSHOT
