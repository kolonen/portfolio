package portfolio

import org.joda.time.{LocalDate, Days, DateTime}

case class Asset(instrument: String, quantity: Int, assetType: Option[String])
case class Portfolio(holdings: Map[String, Asset], date: DateTime)

case class Balance(investment: Double, cash: Double)

/**
 * Created by kolonen on 7.11.2015.
 */
object Portfolio {

  val db = new Database

  /*
   * Returns a sparse series of different portfolios (contents) from date one to given date.
   */
  def getPortfolioSeries(date: DateTime) = {
    def trade(p: Portfolio, t: Event) = {
      val h = if (!p.holdings.contains(t.instrument)) p.holdings + ((t.instrument, Asset(t.instrument, t.quantity, None)))
      else if (t.eventType.equals("MYYNTI")) {
        val remaining = p.holdings(t.instrument).quantity - t.quantity
        if (remaining > 0) p.holdings + ((t.instrument, Asset(t.instrument, remaining, None)))
        else p.holdings - t.instrument
      }
      else p.holdings + ((t.instrument, Asset(t.instrument, p.holdings(t.instrument).quantity + t.quantity, None)))
      Portfolio(h, t.tradeDate)
    }
    val trades = db.getTrades(date)
    val t = trades.head
    val dayOnePortfolio = Portfolio(Map(t.instrument -> Asset(t.instrument, t.quantity, None)), t.tradeDate)
    trades.tail.scanLeft(dayOnePortfolio) ((acc, e) => trade(acc,e))
  }

  def toDenseSeries(ps: List[Portfolio], to: DateTime) = {
    val from = ps.head.date
    val days = generateDateRange(from, to)
    var current = ps.head
    var rest = ps.tail
    days.map(d => {
      if(rest.isEmpty || rest.head.date.isAfter(d)) current.copy(date = d)
      else {
        current = rest.head
        rest = rest.tail
        current.copy(date = d)
      }
    })
  }

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

  /* Returns current cash in the portfolio, representing the amount of
   * money got from closed positions that is not spent to open positions afterwards.
   */
  def getCash(date: DateTime) =
    db.getCashFlowEvents(date).foldLeft[Double](0.0)((cash, event) => Math.max(0, cash + event.amount))


  /* Calculates total investment tied to portfolio on given date. Tied investment is buys - sells - dividends.
   * Calculation is done event by event, even though intermediate values are not used at this point.
   */
  def getInvestment(date: DateTime) = {
    def calculate(b: Balance, e: Event) = {
      val cashAfter = b.cash + e.amount
      val investment = if ( cashAfter < 0.0) b.investment + Math.abs(cashAfter) else b.investment
      Balance(investment, Math.max(0, cashAfter))
    }
    db.getCashFlowEvents(date).foldLeft[Balance](Balance(0.0, 0.0)) ((balance, event) => calculate(balance, event))
  }

  def getPortfolioValueSeries(date: DateTime) = {
    val ps1 = getPortfolioSeries(date)
    println(ps1.last)
    val ps = toDenseSeries(ps1, date)
    val l = ps.last
    println(l)
    val days = generateDateRange(ps.head.date, ps.last.date)
    val instruments = ps.flatMap(p => p.holdings.map(h => h._2.instrument)).distinct
    val quotes = {
      val q = for(i <- instruments) yield (prepareQuotes(db.getQuotes(i, days.head.toLocalDate, days.last.toLocalDate)))
      q.reduceLeft(_++_)
    }
    val pm = ps.map(p => (p.date.toLocalDate, p)).toMap
    days.map(d => (d.toLocalDate, getPortfolioValue(pm(d.toLocalDate), quotes)))
  }

  def getPortfolioValue(p: Portfolio, quotes: Map[(LocalDate, String), Double]) = {
    println(p.holdings + ": "+p.holdings.map(h => quotes((p.date.toLocalDate, h._2.instrument)) * h._2.quantity).sum)
    p.holdings.map(h => quotes((p.date.toLocalDate, h._2.instrument)) * h._2.quantity).sum
  }

  /**
   * Fills the gaps from weekends and bank holidays in quote list and puts them to a map
   *
   * @param quotes
   */
  def prepareQuotes(quotes: List[Quote]) = {
    val instrument = quotes.head.instrument
    val m = quotes.map(q => ((q.date, q.instrument), q.baseCurrencyClose)).toMap
    val resultMap = scala.collection.mutable.Map[(LocalDate, String), Double]()
    val h = quotes.head
    val l = quotes.last
    println(l+ " "+h)
    val days = generateDateRange(quotes.head.date.toDateTimeAtStartOfDay, quotes.last.date.toDateTimeAtStartOfDay)
    val d1 = days.last
    println(d1)
    var prevQuote = quotes.head.close
    days.foreach(d => {
      val q = m.get((d.toLocalDate, instrument))
      if(q.isDefined) {
        prevQuote = q.get
        resultMap.put((d.toLocalDate, instrument), q.get)
      }
      else resultMap.put((d.toLocalDate, instrument), prevQuote)
    })
    println(resultMap.get((days.last.toLocalDate, instrument)))
    resultMap.toMap
  }

  def generateDateRange(from: DateTime, to: DateTime) =
    for(d <- List.range(0, Days.daysBetween(from, to).getDays+1)) yield (from.plusDays(d))
}
