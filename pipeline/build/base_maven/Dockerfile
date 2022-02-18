FROM maven:3.8.4-jdk-11

CMD mkdir -p /root/.m2/repository
RUN rm /usr/bin/mvn
COPY mvn /usr/bin/mvn
RUN chmod ugo+x /usr/bin/mvn

