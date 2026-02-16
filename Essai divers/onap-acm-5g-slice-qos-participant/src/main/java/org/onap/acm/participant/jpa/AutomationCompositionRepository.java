public interface AutomationCompositionRepository
        extends JpaRepository<JpaAutomationComposition, Long> {

    Optional<JpaAutomationComposition>
        findByAutomationCompositionIdAndElementId(UUID acId, UUID elementId);
}

