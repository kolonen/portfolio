package portfolio

import org.joda.time.{LocalDate, Days}

case class Asset(instrument: String, quantity: Int, assetType: Option[String])
case class Portfolio(holdings: Map[String, Asset], date: LocalDate)
case class Balance(date: LocalDate, investment: Double, cash: Double)

class MissingQuotesException(message: String = null) extends java.lang.Exception(message)
/**
 * Created by kolonen on 7.11.2015.
 */
object Portfolio {

  val db = new Database
  val BaseCurrency = "EUR"

  /**
   * Calculates portfolio value series from day one to date. 
   *
   * @param date
   * @return
   */
  def getPortfolioValueSeries(date: LocalDate) = {

    def getPortfolioValue(p: Portfolio, quotes: Map[(LocalDate, String), Double]) =
      p.holdings.map(h => quotes((p.date, h._2.instrument)) * h._2.quantity).sum

    val ps = toDenseSeries(getPortfolioSeries(date), date)
    val days = generateDateRange(ps.head.date, ps.last.date)
    val instruments = ps.flatMap(p => p.holdings.map(h => h._2.instrument)).distinct
    val quotes =
      {
        for(i <- instruments) yield {
          //TODO get quotes only for the time the instrument is in the portfolio
          val q = db.getQuotes(i, days.head, days.last)
          val fxRates =
            if(!q.head.currency.equals(BaseCurrency))
              Some(fillFxRates(db.getFxRates(q.head.currency, q.head.date, q.last.date), Some(date)))
            else None
          prepareQuotes(db.getQuotes(i, days.head, days.last), fxRates, Some(date))
        }
      }.reduceLeft(_++_)

    val pm = ps.map(p => (p.date, p)).toMap
    days.map(d => (d, getPortfolioValue(pm(d), quotes)))

  }

  /**
   * Returns a sparse series of different portfolios (contents) from date one to given date.
   *
   * Portfolio for a date is the closing portfolio of that date, i.e. the portfolio after all the buys and sells on that day.
   *
   */
  def getPortfolioSeries(date: LocalDate) = {
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
    val portfolioSeries = trades.tail.scanLeft(dayOnePortfolio) ((acc, e) => trade(acc,e))
    
    val p = portfolioSeries.tail.foldLeft[(List[Portfolio], Portfolio)] ((List(), portfolioSeries.head)) ( (acc, p) => {
      if (p.date.isAfter(acc._2.date)) (acc._1 :+ acc._2, p)
      else (acc._1, p)
    })
    p._1 :+ p._2
  }

  /**
   * Calculates total investment tied to portfolio on given date. Tied investment is buys - sells - dividends.
   * Calculation is done event by event, even though intermediate values are not used at this point.
   */
  def getInvestment(date: LocalDate) = {
    def calculate(b: Balance, e: Event) = {
      val cashAfter = b.cash + e.amount
      val investment = if ( cashAfter < 0.0) b.investment + Math.abs(cashAfter) else b.investment
      Balance(e.tradeDate, investment, Math.max(0, cashAfter))
    }
    val events = db.getCashFlowEvents(date)
    events.foldLeft[Balance](Balance(events.head.tradeDate.minusDays(1), 0.0, 0.0)) ((balance, event) => calculate(balance, event))
  }

  def getBalanceSeries(date: LocalDate) = {
    def calculate(b: Balance, e: Event) = {
      val cashAfter = b.cash + e.amount
      val investment = if ( cashAfter < 0.0) b.investment + Math.abs(cashAfter) else b.investment
      Balance(e.tradeDate, investment, Math.max(0, cashAfter))
    }
    val events = db.getCashFlowEvents(date)
    val balances = events.scanLeft[Balance, List[Balance]](Balance(events.head.tradeDate, 0.0, 0.0)) ((balance, event) => calculate(balance, event))
    fillBalances(balances, Some(date))
  }

  /**
   * Returns current cash in the portfolio, representing the amount of
   * money got from closed positions that is not spent to open positions afterwards.
   */
  def getCash(date: LocalDate) =
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
  def prepareQuotes(quotes: List[Quote], fxRates: Option[List[FxRate]], to: Option[LocalDate] = None) = {
    val m =
      if(fxRates.isDefined) quotes.zip(fxRates.get).map(i => ((i._1.date, i._1.instrument), i._1.close/i._2.average)).toMap
      else quotes.map(q => ((q.date, q.instrument), q.close)).toMap

    val instrument = quotes.head.instrument
    val resultMap = scala.collection.mutable.Map[(LocalDate, String), Double]()
    val days = generateDateRange(quotes.head.date, to.getOrElse(quotes.last.date))
    var prevQuote = quotes.head.close
    var missingStreak = 0
    val maxStreak = 5
    days.foreach(d => {
      val q = m.get((d, instrument))
      if(q.isDefined) {
        prevQuote = q.get
        resultMap.put((d, instrument), q.get)
        missingStreak = 0
      }
      else {
        resultMap.put((d, instrument), prevQuote)
        missingStreak +=1
      }
      if(missingStreak > maxStreak)
        throw new MissingQuotesException(s"Too many missing quotes for $instrument between ${d.minusDays(maxStreak)} and $d")
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
  def fillFxRates(rates: List[FxRate], to: Option[LocalDate] = None) = {
    val currency = rates.head.currency
    val rateMap = rates.map(r => (r.date, r.average)).toMap
    val days = generateDateRange(rates.head.date, to.getOrElse(rates.last.date))
    days.foldLeft[(List[FxRate], Double)] ((List(), rates.head.average)) ((acc, x) => {
      val rate = rateMap.get(x)
      if(rate.isDefined)
        (acc._1:+FxRate(x, currency, rate.get), rate.get)
      else
        (acc._1:+FxRate(x, currency, acc._2), acc._2)
    })._1
  }

  def fillBalances(balances: List[Balance], to: Option[LocalDate] = None) = {
    val balanceMap = balances.map(r => (r.date, r)).toMap
    val days = generateDateRange(balances.head.date, to.getOrElse(balances.last.date))
    days.foldLeft[(List[Balance], Balance)] ((List(), balances.head)) ((acc, x) => {
      val balance = balanceMap.get(x)
      if(balance.isDefined)
        (acc._1:+balance.get.copy(date = x), balance.get)
      else
        (acc._1:+Balance(x, acc._2.investment, acc._2.cash), acc._2)
    })._1
  }

  def toDenseSeries(ps: List[Portfolio], to: LocalDate) = {
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

  def generateDateRange(from: LocalDate, to: LocalDate) =
    for(d <- List.range(0, Days.daysBetween(from, to).getDays+1)) yield (from.plusDays(d))

}
