package portfolio

import org.joda.time.DateTime

/**
 * Created by kolonen on 6.12.2015.
 */
class portfolioDeprecated {


  val db = new Database
  val BaseCurrency = "EUR"

  /* Returns portfolio contents at given date
   */
  def getPortfolio(date: DateTime) = {

    def trade(holdings: Map[String, Asset], t: Event) =
      if (!holdings.contains(t.instrument) ) holdings + ((t.instrument, Asset(t.instrument, t.quantity, None)))
      else if (t.eventType.equals("MYYNTI")) {
        val remaining = holdings(t.instrument).quantity - t.quantity
        if(remaining > 0) holdings + ((t.instrument, Asset(t.instrument, remaining, None)))
        else holdings - t.instrument
      }
      else holdings + ((t.instrument, Asset(t.instrument, holdings(t.instrument).quantity + t.quantity, None)))

    Portfolio(db.getTrades(date).foldLeft[Map[String, Asset]](Map[String, Asset]())((acc, e) => trade(acc,e)), date)
  }

}
