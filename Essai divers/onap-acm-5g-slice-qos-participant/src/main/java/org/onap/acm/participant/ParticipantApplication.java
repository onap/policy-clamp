@SpringBootApplication
@EnableConfigurationProperties(OnapParticipantConfig.class)
@EnableJpaRepositories
public class ParticipantApplication {
    public static void main(String[] args) {
        SpringApplication.run(ParticipantApplication.class, args);
    }
}
