#FROM robotframework/rfdocker
#
#### Uncomment following two lines if having external test libraries:
##COPY --chown=robot:robot requirements.txt .
#RUN pip3 install --no-cache-dir -r requirements.txt
#COPY *.robot /home/robot/atest
FROM  ppodgorsek/robot-framework:3.0.3
COPY requirements.txt .
RUN pip install -r requirements.txt