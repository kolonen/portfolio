package portfolio

import java.text.DecimalFormat

import org.joda.time.format.DateTimeFormat
import org.joda.time.LocalDate
import org.scalatra._
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.JsonDSL._

class PortfolioApi extends ScalatraServlet {

  val dtf = DateTimeFormat.forPattern("yyyy-MM-dd")
  val df = new DecimalFormat("#.00")
  def formatDate(d: LocalDate) = d.toString(dtf)
  def formatMoney(d: Double) = df.format(d)
  def jsonResponse(r: JValue) = compact(render(r))

  get("/portfolio/cash") {

  }

  get("/portfolio/balance") {
    val to = new LocalDate(params("to"))
    val p = Portfolio.getBalanceSeries(to)
    jsonResponse(p.map(v => ("date" -> formatDate(v.date))~("cash" -> formatMoney(v.cash))~("investment" -> formatMoney(v.investment))))
  }

  get("/portfolio/values") {
    val to = new LocalDate(params("to"))
    val p = Portfolio.getPortfolioValueSeries(to)
    jsonResponse(p.map(v => ("date" -> formatDate(v._1))~("value" -> formatMoney(v._2))))
  }

  get("/portfolio") {
  }
}

case class PortfolioValue(date: LocalDate, value: Double)
case class ValuesResponse(values: List[PortfolioValue])
