@ConfigurationProperties(prefix = "participant")
public class OnapParticipantConfig {

    private String participantId;
    private String participantType;
    private String mode; // simulator | real

    private Kafka kafka;
    private Mysql mysql;

    public static class Kafka {
        private String deployTopic;
        private String ackTopic;
    }

    public static class Mysql {
        private String url;
        private String username;
        private String password;
    }
}