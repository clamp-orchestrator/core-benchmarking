# core-benchmarking
Scripts for provisioning and benchmarking clamp core performance


## Installation

### OSX/Mac

* brew install scala@2.12

## Running Clamp
Clamp and its dependencies can be run with the following Docker Compose command

```bash
$ docker-compose -d up
```

## Running Tests

```bash
$ mvn gatling:test -DgatlingSimulationClass=clampcore.{simulation-class-name} -D{arg-name}={arg-value}
```

**Simulation Classes**
- `InitiateWorkflowSimulation` - Tests workflow creation, service request creation and service status polling APIs
- `CreateServiceRequestSimulation` - Tests service request creation API

**Example**
```bash
$ mvn gatling:test -DgatlingSimulationClass=clampcore.CreateServiceRequestSimulation -DmaxRPS=500 -DdurationSeconds=120 -DmaxDurationSeconds=300
```