# core-benchmarking
Scripts for provisioning and benchmarking clamp core performance


## Installation

### OSX/Mac

* brew install scala@2.12

## Running Tests

```bash
$ cd benchmarking
$ mvn gatling:test -DgatlingSimulationClass=clampcore.{simulation-class-name}
```

**Simulation Classes**
- `InitiateWorkflowSimulation` - Tests workflow creation, service request creation and service status polling APIs
- `CreateServiceRequestSimulation` - Tests service request creation API