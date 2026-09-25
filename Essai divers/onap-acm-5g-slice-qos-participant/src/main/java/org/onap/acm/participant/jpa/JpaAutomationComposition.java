@Entity
@Table(name = "automation_composition")
public class JpaAutomationComposition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private UUID automationCompositionId;
    private UUID elementId;

    @Enumerated(EnumType.STRING)
    private ParticipantType participantType;

    @Enumerated(EnumType.STRING)
    private DeployState deployState;

    private String message;
}
