package portfolio

import org.joda.time.DateTime

case class Asset(instrument: String, quantity: Int, assetType: Option[String])
case class Portfolio(holdings: Map[String, Asset], date: DateTime)

case class Balance(investment: Double, cash: Double)

/**
 * Created by kolonen on 7.11.2015.
 */
object Portfolio {

  val db = new Database

  /* Returns the portfolio contents at given date
   */
  def getPortfolio(date: DateTime) = {

    def trade(holdings: Map[String, Asset], t: Event) =
      if (!holdings.contains(t.instrument) ) holdings + ((t.instrument, Asset(t.instrument, t.quantity, None)))
      else if (t.eventType.equals("MYYNTI")) {
        val remaining = holdings(t.instrument).quantity - t.quantity
        if(remaining > 0) holdings + ((t.instrument, Asset(t.instrument, remaining, None)))
        else holdings - t.instrument
      }
      else holdings+((t.instrument, Asset(t.instrument, holdings(t.instrument).quantity + t.quantity, None)))

    Portfolio(db.getTrades(date).foldLeft[Map[String, Asset]](Map[String, Asset]())((acc, e) => trade(acc,e)), date)
  }

  /* Returns the current cash in the portfolio, representing the amount of
   * money got from closed positions that is not spent to open positions afterwards.
   */
  def getCash(date: DateTime) =
    db.getCashFlowEvents(date).foldLeft[Double](0.0)((cash, event) => Math.max(0, cash + event.amount))


  /* Calculates the total investment tied to portfolio on given date. Tied investment is buys - sells - dividends.
   * Calculation is done event by event, even though the intermediate values are not used at this point.
   */
  def getInvestment(date: DateTime) = {
    def calculate(b: Balance, e: Event) = {
      val cashAfter = b.cash + e.amount
      val investment = if ( cashAfter < 0.0) b.investment + Math.abs(cashAfter) else b.investment
      Balance(investment, Math.max(0, cashAfter))
    }
    db.getCashFlowEvents(date).foldLeft[Balance](Balance(0.0, 0.0)) ((balance, event) => calculate(balance, event))
  }
}
