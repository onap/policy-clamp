onap-vnf-onboarding-tools/
├── README.md
├── docs/
│   ├── architecture.md
│   ├── onboarding-workflow.mdarchitecture.md
│   └── diagrams/
│       └── high-level-architecture.png
├── ci-cd/
│   ├── github-actions/
│   │   └── vnf-validation.yml
│   ├── gitlab-ci/
│   │   └── .gitlab-ci.yml
│   └── jenkins/
│       └── Jenkinsfile
├── validation/
│   ├── schema/
│   │   └── onap-vnf-schema.yaml
│   ├── rules/
│   │   └── onap-best-practices.yaml
│   └── validate.py
├── testing/
│   ├── lifecycle/
│   │   └── test_vnf_lifecycle.py
│   └── integration/
│       └── test_onap_deploy.py
├── examples/
│   └── sample-vnf/
│       ├── descriptors/
│       ├── images/
│       └── README.md
├── scripts/
│   └── package-vnf.sh
├── LICENSE
└── CONTRIBUTING.md
