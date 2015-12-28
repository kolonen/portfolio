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
  val BaseCurrency = "EUR"

  /**
   *
   * @param date
   * @return
   */
  def getPortfolioValueSeries(date: DateTime) = {

    def getPortfolioValue(p: Portfolio, quotes: Map[(LocalDate, String), Double]) =
      p.holdings.map(h => quotes((p.date.toLocalDate, h._2.instrument)) * h._2.quantity).sum

    val ps = toDenseSeries(getPortfolioSeries(date), date)
    val days = generateDateRange(ps.head.date, ps.last.date)
    val instruments = ps.flatMap(p => p.holdings.map(h => h._2.instrument)).distinct
    val quotes =
      {
        for(i <- instruments) yield {
          val q = db.getQuotes(i, days.head.toLocalDate, days.last.toLocalDate)
          val fxRates =
            if(!q.head.currency.equals(BaseCurrency))
              Some(fillFxRates(db.getFxRates(q.head.currency, q.head.date, q.last.date), Some(date)))
            else None
          prepareQuotes(db.getQuotes(i, days.head.toLocalDate, days.last.toLocalDate), fxRates, Some(date))
        }
      }.reduceLeft(_++_)

    val pm = ps.map(p => (p.date.toLocalDate, p)).toMap
    days.map(d => (d.toLocalDate, getPortfolioValue(pm(d.toLocalDate), quotes)))

  }

  /**
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

  /**
   * Calculates total investment tied to portfolio on given date. Tied investment is buys - sells - dividends.
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

  /**
   * Returns current cash in the portfolio, representing the amount of
   * money got from closed positions that is not spent to open positions afterwards.
   */
  def getCash(date: DateTime) =
    db.getCashFlowEvents(date).foldLeft[Double](0.0)((cash, event) => Math.max(0, cash + event.amount))

  /**
   * Adds missing quotes for dates between quotes.first.date and quotes.last.date. If a quote is missing, previous value is used.
   *
   * Converts values to base currency if not originally.
   *
   * @param quotes ordered list of quotes per day
   * @param fxRates ordered list of fx rates per day. for the same time line as quotes.
   * @return a map with date, instrument pair as key and days (closing) quote as value
   */
  def prepareQuotes(quotes: List[Quote], fxRates: Option[List[FxRate]], to: Option[DateTime] = None) = {
    val m =
      if(fxRates.isDefined) quotes.zip(fxRates.get).map(i => ((i._1.date, i._1.instrument), i._1.close/i._2.average)).toMap
      else quotes.map(q => ((q.date, q.instrument), q.close)).toMap

    val instrument = quotes.head.instrument
    val resultMap = scala.collection.mutable.Map[(LocalDate, String), Double]()
    val days = generateDateRange(quotes.head.date.toDateTimeAtStartOfDay, to.getOrElse(quotes.last.date.toDateTimeAtStartOfDay))
    var prevQuote = quotes.head.close
    days.foreach(d => {
      val q = m.get((d.toLocalDate, instrument))
      if(q.isDefined) {
        prevQuote = q.get
        resultMap.put((d.toLocalDate, instrument), q.get)
      }
      else resultMap.put((d.toLocalDate, instrument), prevQuote)
    })
    resultMap.toMap
  }

  /**
   * Adds rates for days which are not present between first and last element in the list. Previous rate is
   * used for missing days.
   *
   * @param rates a list of rates ordered by date
   * @return an ordered list rates which contains rates for all days
   */
  def fillFxRates(rates: List[FxRate], to: Option[DateTime] = None) = {
    val currency = rates.head.currency
    val rateMap = rates.map(r => (r.date, r.average)).toMap
    val days = generateDateRange(rates.head.date.toDateTimeAtStartOfDay, to.getOrElse(rates.last.date.toDateTimeAtStartOfDay))
    days.foldLeft[(List[FxRate], Double)] ((List(), rates.head.average)) ((acc, x) => {
      val rate = rateMap.get(x.toLocalDate)
      if(rate.isDefined)
        (acc._1:+FxRate(x.toLocalDate, currency, rate.get), rate.get)
      else
        (acc._1:+FxRate(x.toLocalDate, currency, acc._2), acc._2)
    })._1
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

  def generateDateRange(from: DateTime, to: DateTime) =
    for(d <- List.range(0, Days.daysBetween(from, to).getDays+1)) yield (from.plusDays(d))

}
