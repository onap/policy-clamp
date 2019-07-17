source init_sg.sh

while [ $? -ne 0 ]; do
   sleep 10
   source init_sg.sh
done &

/bin/bash -c "source /usr/local/bin/docker-entrypoint.sh;"
