package clampcore

import io.gatling.core.Predef._
import io.gatling.core.feeder.Feeder
import io.gatling.http.Predef._
import org.slf4j.{Logger, LoggerFactory}

import java.util.UUID
import scala.concurrent.duration._
import scala.language.postfixOps

class CreateServiceRequestSimulation extends Simulation {

  var logger: Logger = LoggerFactory.getLogger("SimulationLogger")

  var workflowName = ""

  val workflowDefinition =
    """
      {
        "name": "${workflow_name}",
        "description": "a benchmarking flow with only http sync services",
        "steps": [
          {
            "name": "benchmarking step one",
            "mode": "HTTP",
            "val": {
              "method": "GET",
              "url": "http://api-server:8083/api/step1"
            }
          }
        ]
      }
      """

  val baseHttp = http
    .baseUrl("http://localhost:8080")
    .header("no-cache", "no-cache")
    .contentTypeHeader("application/json")
    .userAgentHeader("PostmanRuntime/7.26.8")
    .acceptHeader("*/*")
    .connectionHeader("keep-alive")

  val uuidfeeder: Feeder[String] = Iterator.continually(Map("workflow_name" -> UUID.randomUUID().toString))

  def createWorkflow() = {
    exec(http("create_workflow")
      .post("/workflow")
      .body(StringBody(workflowDefinition)).asJson
      .check(jsonPath("$.name").saveAs("workflowName"))
    )
  }

  def createServiceRequest() = {
    http("execute_workflow")
      .post("/serviceRequest/${workflow_name}")
      .check(status.is(200))
  }

  var createWorkflowScenario = scenario("Create workflow scenario")
    .feed(uuidfeeder)
    .exec(createWorkflow())
    .exec(session => {
      workflowName = session("workflowName").as[String]
      session
    })

  var createPollServiceRequestScenario = scenario("Create service request scenario")
    .exec(_.set("workflow_name", workflowName))
    .exec(createServiceRequest())

  setUp(
    List(
      createWorkflowScenario.inject(
        atOnceUsers(1)
      )
      .protocols(baseHttp),

      createPollServiceRequestScenario.inject(
        nothingFor(1 second),
        rampUsersPerSec(1).to(500).during(1 minute)
      )
      .protocols(baseHttp)
    )
  )
  .maxDuration(5 minutes)
}