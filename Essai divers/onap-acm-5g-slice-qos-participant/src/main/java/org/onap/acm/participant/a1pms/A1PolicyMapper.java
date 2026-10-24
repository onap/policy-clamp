public final class A1PolicyMapper {

    private A1PolicyMapper() {}

    public static A1Policy fromAutomationComposition(
            ElementContext context) {

        Map<String, Object> props = context.getProperties();

        return A1Policy.builder()
            .policyId(context.getElementId())
            .snssai((String) props.get("snssai"))
            .qos(
                new QoSProfile(
                    (Integer) props.get("maxBitrate"),
                    (Integer) props.get("latency"),
                    (Integer) props.get("priority")
                )
            )
            .build();
    }
}
