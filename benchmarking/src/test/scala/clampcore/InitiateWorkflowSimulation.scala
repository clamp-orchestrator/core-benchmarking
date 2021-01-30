package clampcore

import io.gatling.core.Predef._
import io.gatling.core.feeder.Feeder
import io.gatling.http.Predef._
import org.slf4j.{Logger, LoggerFactory}

import java.util.UUID
import scala.concurrent.duration._
import scala.language.postfixOps

class InitiateWorkflowSimulation extends Simulation {

  var logger: Logger = LoggerFactory.getLogger("SimulationLogger")
  val CONSTANT_RPS = Integer.getInteger("constantRPS", 200).toDouble
  val TEST_DURATION = Integer.getInteger("durationSeconds", 1800).toInt
  val MAX_DURATION = Integer.getInteger("durationMaxSeconds", 1800).toInt

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

  before {
    println("Running simulation with " + CONSTANT_RPS + " req/sec for " + TEST_DURATION + " seconds. For a maximum of " + MAX_DURATION + " seconds")
  }

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
    )
  }

  def extractServiceRequestPollUrl() = {
    http("execute_workflow")
      .post("/serviceRequest/${workflow_name}")
      .check(status.is(200))
      .check(jsonPath("$.pollUrl").saveAs("pollUrl"))
  }

  def pollAndSaveServiceRequestStatus() = {
    exec(http("check_workflow_status")
      .get("${pollUrl}")
      .check(status.is(200))
      .check(jsonPath("$.status").saveAs("status"))
      .check(jsonPath("$.total_time_in_ms").saveAs("totalTimeTakenMs"))
      .check(jsonPath("$.steps[*].time_taken").findAll.optional.saveAs("timeTakenEachStepMs"))
    )
  }

  def computeAndLogOrchestrationTime() = {
    exec(
      session => {
        val totalStepTime = session("timeTakenEachStepMs").as[Seq[Any]].map(_.toString.toInt).sum
        val overAllTime = session("totalTimeTakenMs").as[Int]
        val orchestrationTime = overAllTime - totalStepTime
        val pollUrl = session("pollUrl").as[String]
        logger.info(pollUrl + "," + orchestrationTime)
        session
      }
    )
  }

  val triggerWorkflowScenario = scenario("Trigger workflow scenario")
    .feed(uuidfeeder)
    .exec(createWorkflow())
    .exec(extractServiceRequestPollUrl())
    .exec(session => session.set("status", "IN PROGRESS"))
    .asLongAs(session => session("status").as[String] != "COMPLETED") {
      pollAndSaveServiceRequestStatus()
    }
    .exec(computeAndLogOrchestrationTime())

  setUp(triggerWorkflowScenario
    .inject(
      constantUsersPerSec(CONSTANT_RPS).during(TEST_DURATION.seconds)
    )
    .protocols(baseHttp))
    .maxDuration(MAX_DURATION seconds)
}
