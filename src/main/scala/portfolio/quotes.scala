package portfolio

import java.io.FileReader
import org.joda.time.{LocalDate, DateTime}

import scala.collection.JavaConversions._
import org.apache.commons.csv.CSVFormat

case class Quote(instrument: String, date: LocalDate, open: Double, high: Double, low: Double, close: Double, volume: Int)
/**
 * Created by kolonen on 11.11.2015.
 */
object QuoteReader{

  def readQuotes(file: String, instrument: String) = {
    val in = new FileReader(file)
    val records = CSVFormat.EXCEL.withHeader().parse(in).toList

    records.map(r => Quote(instrument, new LocalDate(r.get("Date")), r.get("Open").toDouble, r.get("High").toDouble, r.get("Low").toDouble, r.get("Close").toDouble, r.get("Volume").toInt))
  }
}
