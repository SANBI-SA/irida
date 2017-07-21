FROM ubuntu:14.04
MAINTAINER Zipho Mashologu "zipho@sanbi.ac.za"

# Update system packages
RUN apt-get update -y --fix-missing
RUN apt-get install --yes --no-install-recommends python-software-properties \
                                                  software-properties-common

RUN apt-add-repository --yes ppa:webupd8team/java

# Update sources
RUN apt-get update -y

# DB  - mysql and to accept the license for the Java installer.
RUN apt-get install -y debconf-utils apparmor
RUN echo 'mysql-server mysql-server/root_password password password123' | sudo debconf-set-selections
RUN echo 'mysql-server mysql-server/root_password_again password password123' | sudo debconf-set-selections
RUN apt-get install --yes --no-install-recommends mysql-server npm

# Restart mysql to persist the new password
RUN /etc/init.d/mysql restart

# Install app dependencies
RUN apt-get install --yes --no-install-recommends maven git mercurial 
RUN echo debconf shared/accepted-oracle-license-v1-1 select true | sudo debconf-set-selections
RUN echo debconf shared/accepted-oracle-license-v1-1 seen true | sudo debconf-set-selections
RUN apt-get install --yes --no-install-recommends oracle-java8-installer


# Installing dependencies
RUN mkdir /opt/irida
ADD lib /opt/irida/lib
ADD .gitlab-ci.yml /opt/irida/
ADD .gitmodules /opt/irida/
ADD .git /opt/irida/.git
ADD src/main/webapp/package.json /opt/irida/

#WORKDIR /opt/irida
#RUN bash lib/install-libs.sh

COPY config/irida.conf.sample /etc/irida/irida.conf 

#RUN echo "grant all privileges on irida_db.* to 'irida_user'@'localhost' identified by 'irida_passwd';" | mysql --user=root --password=password123
#RUN echo "create database irida_db;" | mysql --user=root --password=password123

#WORKDIR src/main/webapp/
#RUN npm install
#RUN gulp

#  Mark folders as imported from the host.
#VOLUME ["/opt/irida/", "./", "/var/lib/docker"]

# Expose port 80 (webserver), 21 (FTP server), 8080 (Proxy)
EXPOSE :80
EXPOSE :21
EXPOSE :8080

#echo '{ "allow_root": true }' > /root/.bowerrc
# ./run.sh --create-db
# sudo apt-get install nodejs-legacy
# npm install gulp