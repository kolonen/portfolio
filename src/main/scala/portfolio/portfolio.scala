package portfolio

import org.joda.time.{LocalDate, Days}

case class Asset(instrument: String, quantity: Int, assetType: Option[String])
case class Portfolio(holdings: Map[String, Asset], date: LocalDate)
case class Balance(date: LocalDate, investment: Double, cash: Double)
case class Position(instrument: String, opened: LocalDate, closed: Option[LocalDate])

class MissingQuotesException(message: String = null) extends java.lang.Exception(message)


object Portfolio {

  val db = new Database
  val BaseCurrency = "EUR"

  def getPositions(instrument: String, sps: List[Portfolio]) = {

    var closedPositions = List[Position]()
    var openPositions = Map[String, Position]()

    for (p <- sps) {
      if(p.holdings.contains(instrument) && !openPositions.contains(instrument)) {
        openPositions = openPositions + (instrument -> Position(instrument, p.date, None))
      } else if( !p.holdings.contains(instrument) && openPositions.contains(instrument)) {
        closedPositions = openPositions.get(instrument).get.copy(closed = Some(p.date)) :: closedPositions
        openPositions = openPositions - instrument
      }
    }
    closedPositions ++ openPositions.values.toList
  }

  /**
   * Calculates portfolio value series from day one to date.
   *
   * @param date
   * @return
   */
  def getPortfolioValueSeries(date: LocalDate) = {

    val sparseSeries = getPortfolioSeries(date)
    val denseSeries = toDenseSeries(getPortfolioSeries(date), date)
    val days = generateDateRange(denseSeries.head.date, denseSeries.last.date)
    val instruments = denseSeries.flatMap(p => p.holdings.map(h => h._2.instrument)).distinct
    val positions = instruments.map( i => getPositions(i, sparseSeries)).flatten
    val quotes = positions.map( p => db.getQuotes(p.instrument, p.opened, p.closed.getOrElse(days.last))).flatten.map(q => ((q.date, q.instrument), q)).toMap

    val pm = denseSeries.map(p => (p.date, p)).toMap
    days.map(d => (d, getPortfolioValue(pm(d), quotes)))
  }

  /**
   *
   * @param p
   * @param quotes
   * @param maxMissingQuotes
   * @return
   */
  def getPortfolioValue(p: Portfolio, quotes: Map[(LocalDate, String), Quote], maxMissingQuotes: Int = 5) = {
    def getQuote(quotes: Map[(LocalDate, String), Quote], date: LocalDate, instrument: String) = {
      var q = quotes.get(date, instrument)
      var c = 1
      while(!q.isDefined && c <= maxMissingQuotes) {
        q = quotes.get(date.minusDays(c), instrument)
        c = c + 1
      }
      if(q.isEmpty) throw new MissingQuotesException()
      q.get
    }
    p.holdings.map(h => getQuote(quotes, p.date, h._1).baseCurrencyClose * h._2.quantity).sum
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
}