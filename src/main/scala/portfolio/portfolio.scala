package portfolio

import org.joda.time.DateTime

case class Asset(instrument: String, quantity: Int, assetType: Option[String])
case class Portfolio(holdings: Map[String, Asset], date: DateTime)

/**
 * Created by kolonen on 7.11.2015.
 */
object Portfolio {

  val db = new Database

  /**
   * returns the portfolio contents at given date
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

    Portfolio(db.getTrades
      .filter(e => e.tradeDate.isBefore(date) || e.tradeDate.equals(date))
      .foldLeft[Map[String, Asset]](Map[String, Asset]())((acc, e) => trade(acc,e)), date)
  }
}
