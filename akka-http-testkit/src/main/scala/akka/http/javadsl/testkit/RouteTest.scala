/*
 * Copyright (C) 2009-2014 Typesafe Inc. <http://www.typesafe.com>
 */

package akka.http.javadsl.testkit

import scala.annotation.varargs
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import akka.stream.ActorMaterializer
import akka.http.scaladsl.server
import akka.http.javadsl.model.HttpRequest
import akka.http.javadsl.server.{ AllDirectives, Route, Directives }
import akka.http.impl.util.JavaMapping.Implicits._
import akka.http.impl.server.RouteImplementation
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.{ RouteResult, RoutingSettings, Route ⇒ ScalaRoute }
import akka.actor.ActorSystem
import akka.event.NoLogging
import akka.http.impl.util._

abstract class RouteTest extends AllDirectives {
  implicit def system: ActorSystem
  implicit def materializer: ActorMaterializer
  implicit def executionContext: ExecutionContext = system.dispatcher

  protected def awaitDuration: FiniteDuration = 500.millis

  def runRoute(route: Route, request: HttpRequest): TestResponse =
    runScalaRoute(ScalaRoute.seal(RouteImplementation(route)), request)
  def runRouteUnSealed(route: Route, request: HttpRequest): TestResponse =
    runScalaRoute(RouteImplementation(route), request)

  private def runScalaRoute(scalaRoute: ScalaRoute, request: HttpRequest): TestResponse = {
    val result = scalaRoute(new server.RequestContextImpl(request.asScala, NoLogging, RoutingSettings(system)))

    result.awaitResult(awaitDuration) match {
      case RouteResult.Complete(response) ⇒ createTestResponse(response)
    }
  }

  /**
   * Wraps a list of route alternatives with testing support.
   */
  @varargs
  def testRoute(first: Route, others: Route*): TestRoute =
    new TestRoute {
      val underlying: Route = Directives.route(first, others: _*)

      def run(request: HttpRequest): TestResponse = runRoute(underlying, request)
    }

  protected def createTestResponse(response: HttpResponse): TestResponse
}