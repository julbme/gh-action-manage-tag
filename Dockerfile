FROM eclipse-temurin:17-jre

ENV JAVA_TOOL_OPTIONS=""

ARG SCM_URL=https://github.com/julbme/gh-action-manage-branch
ARG ARTIFACT_ID=gh-action-manage-tag
ARG VERSION=1.0.0-SNAPSHOT

WORKDIR /app

RUN curl -s -L -o /app/app.jar "${SCM_URL}/releases/download/v${VERSION}/${ARTIFACT_ID}-${VERSION}-shaded.jar"

CMD ["sh", "-c", "java ${JAVA_TOOL_OPTIONS} -jar /app/app.jar"]
