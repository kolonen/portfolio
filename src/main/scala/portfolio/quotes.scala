package portfolio

import java.io.FileReader
import org.joda.time.{LocalDate, DateTime}

import scala.collection.JavaConversions._
import org.apache.commons.csv.{CSVRecord, CSVFormat}

case class Quote(instrument: String, date: LocalDate, open: Option[Double], high: Option[Double], low: Option[Double], close: Double, volume: Option[Int])
/**
 * Created by kolonen on 11.11.2015.
 */
object QuoteReader{

  def readQuotes(file: String, instrument: String) = {
    val in = new FileReader(file)
    val records = CSVFormat.EXCEL.withHeader().parse(in).toList

    records.map(r =>
      Quote(instrument,
        new LocalDate(r.get("Date")),
        getOption(r, "Open").map(_.toDouble),
        getOption(r, "High").map(_.toDouble),
        getOption(r, "Low").map(_.toDouble),
        r.get("Close").toDouble,
        getOption(r, "Volume").map(_.toInt))
    )
  }
  def getOption(r: CSVRecord, column: String) = if (r.isMapped(column) && r.get(column).nonEmpty) Some(r.get(column)) else None
}
