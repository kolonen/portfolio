package portfolio

import java.io.{FileWriter, BufferedWriter}

import org.joda.time.{LocalDate, Days}

import scala.concurrent.{Await, ExecutionContext}
import ExecutionContext.Implicits.global
import scala.language.postfixOps
import scala.concurrent._
import scala.concurrent.duration._




/**
 * Created by kolonen on 4.11.2015.
 */
object test {

  def main(args: Array[String]): Unit = {

    /*val  db = new Database
    val quotes = QuoteReader.readQuotes("data/pohjola.csv", "POH1S")
    quotes.foreach(q => println(q))
    db.saveQuotes(quotes)
  */
    //val rates = FxRateReader.readQuotes("data/sek.csv", "SEK")
    //db.saveFxRates(rates)
    //val rates = db.getFxRates("SEK", new LocalDate("2014-10-10"), new LocalDate("2014-10-12"))
    //println(rates)


    val d =  new LocalDate("2014-11-03")
    val pvs = Portfolio.getPortfolioValueSeries(d)
    //val pvs2 = pvs.reverse

    val w: BufferedWriter = new BufferedWriter(new FileWriter("data/output.csv"))
    pvs.foreach(p => {
      w.write(p._1+","+p._2)
      w.newLine()
    })
    w.flush()
    w.close()
  }

  def main5(args: Array[String]): Unit = {
    val  db = new Database
    val quotes = QuoteReader.readQuotes("data/sanoma.csv", "SAA1V")
    quotes.foreach(q => println(q))
    db.saveQuotes(quotes)
  }

  def main2(args: Array[String]): Unit = {
    val  db = new Database
    //val quotes = db.getQuotes(Seq("NDA1V"), new LocalDate("2001-10-10"), new LocalDate("2001-10-12"))
    //quotes.foreach(q => println(q))

  }

  def main1(args: Array[String]) = {
    val p = Portfolio.getPortfolioSeries(LocalDate.now())
    p.foreach(h => println(h))

  //  val p2 = Portfolio.getPortfolio(DateTime.now.minusYears(3))
  //  p2.holdings.values.foreach(h => println(h))

  //  val i = Portfolio.getInvestment(DateTime.now)
  //  println(i)
  }
  def main4(args: Array[String]) = {
    val d1 = new LocalDate("2001-10-10")
    val d2 = new LocalDate("2001-10-12")
    val pv = Portfolio.getPortfolioValueSeries(LocalDate.now)
    println(pv.head)
  }
}
