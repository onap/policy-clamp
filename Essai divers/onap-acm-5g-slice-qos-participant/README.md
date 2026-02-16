Sections :
- Overview
- Architecture
- Supported Participants
- Kafka Topics
- A1PMS Integration
- Database Model
- Local Development


Commandes à effectuer:

docker-compose up -d
mvn clean spring-boot:run
Send deploy message:
kafka-console-producer \
  --bootstrap-server localhost:9092 \
  --topic automationcomposition-deploy
