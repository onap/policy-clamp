This module contains forked code that was originally in the policy/models repo.

WARNING
In some cases, the original package structure must be maintained as Participant Intermediary library exposes these.
In particular, the following classes/packages must NOT be modified to avoid breaking participant implementations:
- org.onap.policy.models.base.PfModelException
- org.onap.policy.models.base.PfModelRuntimeException
- org.onap.policy.models.tosca.authorative.concepts.*
