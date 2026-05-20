@Service
public class A1PolicyClient {

    public void applyPolicy(Map<String, Object> params) {
        // POST policy to A1PMS
    }
}
Automotive handler:
if (!simulator) {
    a1PolicyClient.applyPolicy(params.getParameters());
}
