# https://github.com/elastic/elasticsearch-docker
FROM docker.elastic.co/elasticsearch/elasticsearch:6.6.2

COPY config/sg/ config/sg/
COPY config/ config/
COPY bin/ bin/
#RUN chmod +x bin/init_sg.sh
COPY my-entrypoint.sh /usr/local/bin/my-entrypoint.sh
RUN chmod a+x /usr/local/bin/my-entrypoint.sh
# Search Guard plugin
RUN elasticsearch-plugin install --batch com.floragunn:search-guard-6:6.6.2-25.1 \
	&& chmod +x plugins/search-guard-6/tools/*.sh \
	&& chown -R elasticsearch config/sg/ \
	&& chmod -R go= config/sg/

# Add your elasticsearch plugins setup here
# Example: RUN elasticsearch-plugin install analysis-icu

#  This custom entrypoint script is used instead of 
# the original's /usr/local/bin/docker-entrypoint.sh
ENTRYPOINT ["bash","-c","my-entrypoint.sh"]