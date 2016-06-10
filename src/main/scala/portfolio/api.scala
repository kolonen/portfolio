package portfolio

import java.text.DecimalFormat

import org.joda.time.format.DateTimeFormat
import org.joda.time.LocalDate
import org.scalatra._
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.JsonDSL._

case class Figure(date: LocalDate, value: Double, cash: Double, investment: Double)

class PortfolioApi extends ScalatraServlet {

  val dtf = DateTimeFormat.forPattern("yyyy-MM-dd")
  val df = new DecimalFormat("#.00")
  def formatDate(d: LocalDate) = d.toString(dtf)
  def formatMoney(d: Double) = df.format(d)
  def jsonResponse(r: JValue) = compact(render(r))

  get("/portfolio/balanceSeries") {
    val to = new LocalDate(params("to"))
    val p = Portfolio.getBalanceSeries(to)
    contentType = "application/json"
    jsonResponse(p.map(v => ("date" -> formatDate(v.date))~("cash" -> formatMoney(v.cash))~("investment" -> formatMoney(v.investment))))
  }

  get("/portfolio/values") {
    val to = new LocalDate(params("to"))
    val p = Portfolio.getPortfolioValueSeries(to)
    contentType = "application/json"
    jsonResponse(p.map(v => ("date" -> formatDate(v._1))~("value" -> formatMoney(v._2))))
  }

  get("/portfolio") {
    val to = new LocalDate(params("to"))
    val values = Portfolio.getPortfolioValueSeries(to)
    val balances = Portfolio.getBalanceSeries(to)
    contentType = "application/json"
    val figures = values.zip(balances).map(f => Figure(f._1._1, f._1._2, f._2.cash, f._2.investment))
    jsonResponse(figures.map(v => ("date" -> formatDate(v.date))~("value" -> formatMoney(v.value))~("cash" -> formatMoney(v.cash))~("investment" -> formatMoney(v.investment))))
  }
}
