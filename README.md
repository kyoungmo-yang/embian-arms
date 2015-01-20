Embian ARMS(API Response Monitoring System)
=====================
Embian ARMS is a Log-Based API Monitoring System, it is consist of [influxdb](http://influxdb.com/), [elasticsearch](http://www.elasticsearch.org/), cep-engine, dashboard, [rabbitmq](https://www.rabbitmq.com/), [memcached](http://memcached.org/), [fluentd](http://fluentd.org) and [nginx](http://nginx.org/).

Building image
-----

To create the image `yjj0309/embian-arms`, execute the following command on embian-arms-docker folder:

    docker build -t yjj0309/arms-fluentd-docker:last .

Or download automated build from public [Docker Hub Registry](https://registry.hub.docker.com/u/yjj0309/embian-arms-docker/):

    docker pull yjj0309/embian-arms-docker


Running embian/arms image
--------------------------
1. Create and run a container for storing data of influxdb and elasticsearch:

    docker run -i -t --name embian_arms_data -v /var/influxdb -v /var/elasticsearch busybox /bin/sh

2. Run embian/arms image
  (You can changes enviroments such as DEFAULT_USER, DEFAULT_PASS and HOST_NAME)

    docker run -d --name embian_arms -p 80:80 -p 8080:8080 -p 8083:8083 -p 8086:8086 -p 9200:9200 -p 9300:9300 -p 5672:5672 -p 15672:15672 -e DEFAULT_USER="arms_user" -e DEFAULT_PASS="arms_pass" -e HOST_NAME="`echo $(/bin/ip route get 8.8.8.8 | /usr/bin/head -1 | /usr/bin/cut -d' ' -f8)`" --volumes-from embian_arms_data yjj0309/arms-fluentd-docker:last


After few seconds, open http://<HOST_NAME> to see the dashboard web page.

Pushing test logs to Embian ARMS
--------------------------
We build a sample docker image for testing Embian ARMS.
See [arms/fluentd](https://registry.hub.docker.com/u/yjj0309/arms-fluentd-docker/).
