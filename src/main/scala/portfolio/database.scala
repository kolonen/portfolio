package portfolio

import javax.sql.DataSource
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import scalikejdbc._
import org.joda.time.{LocalDate}

case class Event(eventId: Int, extId: Int, source: String, tradeDate: LocalDate, eventType: String,
                 instrument: String, quantity: Int, amount: Double, price: Option[Double], currency: String, curRate: Double, profit: Double)

object Event {
  def apply(rs: WrappedResultSet) = new Event(
    rs.int("event_id"),
    rs.int("ext_id"),
    rs.string("source"),
    new LocalDate(rs.date("trade_date")),
    rs.string("event_type"),
    rs.string("instrument"),
    rs.int("quantity"),
    rs.double("amount"),
    rs.doubleOpt("price"),
    rs.string("currency"),
    rs.double("cur_rate"),
    rs.double("profit"))
}

case class Quote(instrument: String, date: LocalDate, open: Option[Double] = None, high: Option[Double] = None,
                 low: Option[Double] = None, close: Double, volume: Option[Int] = None, currency: String, fxRate: Option[Double] = None) {

  val baseCurrencyClose = close/fxRate.getOrElse(1.0);

}

object Quote {
  def apply(rs: WrappedResultSet) = new Quote(
    rs.string("instrument"),
    new LocalDate(rs.date("date")),
    rs.doubleOpt("open"),
    rs.doubleOpt("high"),
    rs.doubleOpt("low"),
    rs.double("close"),
    rs.intOpt("volume"),
    rs.string("currency"),
    rs.doubleOpt("fx_rate")
  )
}

case class FxRate(date: LocalDate, currency: String, average: Double)

object FxRate {
  def apply(rs: WrappedResultSet) = new FxRate(
    new LocalDate(rs.date("date")),
    rs.string("currency"),
    rs.double("average")
  )
}

class Database {

  val dataSource: DataSource = {
    val config = new HikariConfig()
    config.setJdbcUrl("jdbc:mysql://localhost:3306/portfolio")
    config.setUsername("root")
    config.setPassword("")
    val ds = new HikariDataSource(config)
    ds
  }
  ConnectionPool.singleton(new DataSourceConnectionPool(dataSource))

  def getEvents =  DB readOnly { implicit session =>
    sql"""SELECT event_id, ext_id, source, trade_date, event_type, instrument, quantity, amount, price, currency, cur_rate, profit
          FROM event
          ORDER BY trade_date, event_type"""
    .map(rs => Event(rs))
    .list
    .apply()
  }

  def getTrades(until: LocalDate) = DB readOnly { implicit session =>
    sql"""SELECT event_id, ext_id, source, trade_date, event_type, instrument, quantity, amount, price, currency, cur_rate, profit
          FROM event
          WHERE event_type IN ('OSTO', 'MYYNTI')
          AND trade_date <= ${until}
          ORDER BY trade_date, event_type"""
      .map(rs => Event(rs))
      .list
      .apply()
  }

  def getCashFlowEvents(until: LocalDate) = DB readOnly { implicit session =>
    sql"""SELECT event_id, ext_id, source, trade_date, event_type, instrument, quantity, amount, price, currency, cur_rate, profit
          FROM event
          WHERE event_type IN ('OSTO', 'MYYNTI', 'OSINKO')
          AND trade_date <= ${until}
          ORDER BY trade_date, event_type"""
      .map(rs => Event(rs)).list.apply
  }
  def saveQuotes(quotes: List[Quote]) = DB autoCommit  { implicit session =>
    quotes.foreach(q =>
      sql"""INSERT INTO quote (instrument, date, open, high, low, close, volume)
            VALUES (${q.instrument}, ${q.date}, ${q.open}, ${q.high}, ${q.low}, ${q.close}, ${q.volume})""".execute().apply
    )}

  def getQuotes(instrument: String, from: LocalDate, to: LocalDate) = DB readOnly { implicit session =>
    val b =
    sql"""SELECT q.instrument, q.date, q.open, q.high, q.low, q.close, q.volume, q.currency, fx.average as fx_rate
          FROM quote q
          LEFT JOIN fx_rate fx ON (fx.date = q.date AND fx.currency = q.currency)
          WHERE q.date >= ${from}
          AND q.date <= ${to}
          AND q.instrument = ${instrument}
          ORDER BY q.date""".map(rs => Quote(rs)).list.apply

    println(b.headOption)
    b
  }

  def getQuotes2(instruments: Seq[String], from: LocalDate, to: LocalDate) = DB readOnly { implicit session =>
    sql"""SELECT instrument, date, open, high, low, close, volume, currency
          FROM quote
          WHERE date >= ${from}
          AND date <= ${to}
          AND instrument IN (${instruments})"""
    .map(rs => Quote(rs)).list.apply
  }

  def saveFxRates(fxRates: Seq[FxRate]) = DB autoCommit { implicit session =>
    fxRates.foreach(fx =>
      sql"""INSERT INTO fx_rate (currency, date, average) VALUES(${fx.currency}, ${fx.date}, ${fx.average})""".execute().apply
    )
  }

  def getFxRates(currency: String, from: LocalDate, to: LocalDate) = DB readOnly { implicit session =>
      sql"""SELECT date, currency, average
          FROM fx_rate
          WHERE currency = $currency
          AND date >= ${from}
          AND date <= ${to}
       """.map(rs => FxRate(rs)).list.apply

  }
}
