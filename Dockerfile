FROM centos:centos8

WORKDIR /wd

RUN cd /etc/yum.repos.d/ && sed -i 's/mirrorlist/#mirrorlist/g' /etc/yum.repos.d/CentOS-* && sed -i 's|#baseurl=http://mirror.centos.org|baseurl=http://vault.centos.org|g' /etc/yum.repos.d/CentOS-* && yum update -y
RUN yum install -y java-17-openjdk-devel && yum install -y unzip

COPY ./server/build/distributions/jifa.zip .
RUN unzip jifa.zip && rm jifa.zip

CMD ["/wd/jifa/bin/jifa"]
