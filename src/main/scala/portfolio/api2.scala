package portfolio

import org.scalatra.ScalatraServlet
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.JsonDSL._

/*
 * Created by kolonen on 27.12.2015.
 */
class Portfolio2 extends ScalatraServlet {


  def test() = parse(""" { "numbers" : [1, 2, 3, 4] } """)

}
