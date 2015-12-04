package portfolio

import java.io.FileReader
import java.text.SimpleDateFormat
import org.joda.time.LocalDate
import scala.collection.JavaConversions._
import org.apache.commons.csv.CSVFormat

/**
 * Created by kolonen on 28.11.2015.
 */
object FxRateReader {

  val df = new SimpleDateFormat("dd.MM.yyyy")

  def readQuotes(file: String, currency: String) = {
    val in = new FileReader(file)
    val records = CSVFormat.EXCEL.withHeader().parse(in).toList
    records.map(r => FxRate(new LocalDate(df.parse(r.get("Date"))), currency, r.get("Average").toDouble))
  }
}
