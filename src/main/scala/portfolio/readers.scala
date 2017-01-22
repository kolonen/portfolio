package portfolio

import java.io.FileReader
import java.text.SimpleDateFormat
import org.joda.time.LocalDate
import scala.collection.JavaConversions._
import org.apache.commons.csv.{CSVRecord, CSVFormat}

object Readers {

  def readEvents(file: String, eventsSource: String) = {
    val in = new FileReader(file)
    val df = new java.text.DecimalFormat
    val dfs = df.getDecimalFormatSymbols
    dfs.setGroupingSeparator(' ')
    dfs.setDecimalSeparator(',')

    val records = CSVFormat.newFormat(';').withHeader().parse(in).toList

    records.map(r => Event(
      extId = r.get("Id").toInt,
      source = eventsSource,
      tradeDate = new LocalDate(r.get("Kauppapäivä")),
      eventType = r.get("Tapahtumatyyppi"),
      instrument = r.get("Arvopaperi"),
      quantity = r.get("Määrä").toInt,
      amount = r.get("Summa").toDouble,
      price = Some(r.get("Kurssi").toDouble),
      currency = r.get("Valuutta"),
      curRate = r.get("Valuuttakurssi").toDouble,
      profit = r.get("Tulos").toDouble))
  }

  def readFxRates(file: String, currency: String) = {
    val df = new SimpleDateFormat("dd.MM.yyyy")
    val in = new FileReader(file)
    val records = CSVFormat.EXCEL.withHeader().parse(in).toList
    records.map(r => FxRate(date = new LocalDate(df.parse(r.get("Date"))), currency = currency, rate = r.get("Average").toDouble))
  }

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
        getOption(r, "Volume").map(_.toInt)
        ,"EUR",
        None)

    )
  }
  def getOption(r: CSVRecord, column: String) = if (r.isMapped(column) && r.get(column).nonEmpty) Some(r.get(column)) else None
}
