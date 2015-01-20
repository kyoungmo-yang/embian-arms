docker stop embian_arms
docker rm embian_arms
docker rmi embian/arms

docker build -t embian/arms .


docker run -d --name embian_arms -p 80:80 -p 8080:8080 -p 8083:8083 -p 8086:8086 -p 9200:9200 -p 9300:9300 -p 5672:5672 -p 15672:15672 HOST="128.134.142.231" -e DEFAULT_USER="arms" -e DEFAULT_PASS="embian1001" --volumes-from embian_arms_data embian/arms
