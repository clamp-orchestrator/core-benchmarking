package clampcore

import io.gatling.core.Predef._
import io.gatling.core.feeder.Feeder
import io.gatling.http.Predef._

import java.util.UUID
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.Random

class InitiateWorkflowSimulation extends Simulation {

  val workflowDefinition = """
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
          },
          {
            "name": "benchmarking step two",
            "mode": "HTTP",
            "transform": false,
            "val": {
              "method": "POST",
              "url": "http://api-server:8083/api/step2"
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

  val triggerWorkflowScenario = scenario("Trigger workflow scenario")
    .feed(uuidfeeder)
    .exec(http("create_workflow")
      .post("/workflow")
      .body(StringBody(workflowDefinition)).asJson
    )
    .exec(http("execute_workflow")
      .post("/serviceRequest/${workflow_name}")
      .check(status.is(200))
      .check(jsonPath("$.pollUrl").saveAs("pollUrl"))
    )
    .exec(session => session.set("${pollUrl}","IN PROGRESS"))
    .asLongAs(session => session("${pollUrl}").as[String] != "COMPLETED") {
      exec(http("check_workflow_status")
        .get("${pollUrl}")
        .check(status.is(200))
        .check(jsonPath("$.status").saveAs("${pollUrl}"))
      )
    }

  setUp(triggerWorkflowScenario
    .inject(
      atOnceUsers(10),
      rampUsers(200) during (30 seconds))
    .protocols(baseHttp))
}
