package clampcore

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import scala.concurrent.duration._

class InitiateWorkflowSimulation extends Simulation {

  val baseHttp = http
    .baseUrl("http://localhost:8080")
    .header("no-cache", "no-cache")
    .userAgentHeader("Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0")
    .acceptEncodingHeader("gzip, gzip, deflate, br")
    .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .connectionHeader("keep-alive")

  val triggerWorkflowScenario = scenario("Trigger workflow scenario")
    .exec(http("create_workflow")
      .post("/workflow")
      .body(RawFileBody("workflow_definition.json")).asJson
    )

  setUp(triggerWorkflowScenario.inject(atOnceUsers(1)).protocols(baseHttp))
}
