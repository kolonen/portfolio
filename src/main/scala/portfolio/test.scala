package portfolio

import org.joda.time.DateTime

import scala.concurrent.{Await, ExecutionContext}
import ExecutionContext.Implicits.global
import scala.language.postfixOps
import scala.concurrent._
import scala.concurrent.duration._


/**
 * Created by kolonen on 4.11.2015.
 */
object test {

  def main(args: Array[String]) = {

    val p = Portfolio.getPortfolio(DateTime.now)
    p.holdings.values.foreach(h => println(h))

  }

}
