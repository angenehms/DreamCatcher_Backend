FROM eclipse-temurin:17-jdk-alpine

# 스프링 부트가 실행될 때 사용하는 임시 폴더(/tmp)를 호스트와 연결해 둡
# 내장 톰캣(Tomcat)이 실행될 때 임시 파일을 만드는데, 이걸 컨테이너 내부가 아니라 호스트 메모리나 디스크를 활용하게 해서 속도를 높이고 컨테이너 무게를 줄이기 위함
VOLUME /tmp

ARG JAR_FILE=build/libs/*SNAPSHOT.jar

COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java","-jar","/app.jar"]