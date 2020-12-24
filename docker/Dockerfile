FROM openjdk:8-jre-alpine
RUN mkdir /work
ADD spring-petclinic-*.jar /work/spring-petclinic.jar
RUN chmod +x /work/spring-petclinic.jar
RUN echo "##### RUNNING APPLICATION #######"
ENTRYPOINT ["java","-jar","/work/spring-petclinic.jar"]