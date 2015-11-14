package portfolio

import javax.sql.DataSource
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import scalikejdbc._
import org.joda.time.DateTime

case class Event(eventId: Int, extId: Int, source: String, tradeDate: DateTime, eventType: String,
                 instrument: String, quantity: Int, amount: Double, price: Option[Double], currency: String, curRate: Double, profit: Double)

object Event {
  def apply(rs: WrappedResultSet) = new Event(
    rs.int("event_id"),
    rs.int("ext_id"),
    rs.string("source"),
    new DateTime(rs.date("trade_date")),
    rs.string("event_type"),
    rs.string("instrument"),
    rs.int("quantity"),
    rs.double("amount"),
    rs.doubleOpt("price"),
    rs.string("currency"),
    rs.double("cur_rate"),
    rs.double("profit"))
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
          ORDER BY trade_date"""
    .map(rs => Event(rs))
    .list
    .apply()
  }

  def getTrades(until: DateTime) = DB readOnly { implicit session =>
    sql"""SELECT event_id, ext_id, source, trade_date, event_type, instrument, quantity, amount, price, currency, cur_rate, profit
          FROM event
          WHERE event_type IN ('OSTO', 'MYYNTI')
          AND trade_date <= ${until}
          ORDER BY trade_date"""
      .map(rs => Event(rs))
      .list
      .apply()
  }

  def getCashFlowEvents(until: DateTime) = DB readOnly { implicit session =>
    sql"""SELECT event_id, ext_id, source, trade_date, event_type, instrument, quantity, amount, price, currency, cur_rate, profit
          FROM event
          WHERE event_type IN ('OSTO', 'MYYNTI', 'OSINKO')
          AND trade_date <= ${until}
          ORDER BY trade_date"""
      .map(rs => Event(rs))
      .list
      .apply()
  }
  def saveQuotes(quotes: List[Quote]) = DB autoCommit  { implicit session =>
    quotes.foreach(q => {
      println(q)
      sql"""INSERT INTO quote (instrument, date, open, high, low, close, volume)
          VALUES (${q.instrument}, ${q.date}, ${q.open}, ${q.high}, ${q.low}, ${q.close}, ${q.volume})""".execute().apply()
    }
    )
  }
}
