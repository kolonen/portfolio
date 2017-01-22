
import org.joda.time.{LocalDate, DateTime}
import org.scalatest.{Matchers, FunSpec}
import portfolio.{Quote, FxRate, Portfolio}

/**
 * Created by kolonen on 4.12.2015.
 */
class PortfolioSpec extends FunSpec with Matchers{

/*
  describe("Quote utilities") {
    it("gennerates correct date rage") {
      val d1 = new LocalDate("2014-01-01")
      val d2 = new LocalDate("2014-01-10")
      println(Portfolio.generateDateRange(d1,d2))
    }

    it("fills in missing fx rates correctly ") {
      val rates = List(
        FxRate(new LocalDate("2014-01-01"), "SEK", 9.1),
        FxRate(new LocalDate("2014-01-04"), "SEK", 9.2),
        FxRate(new LocalDate("2014-01-05"), "SEK", 9.101),
        FxRate(new LocalDate("2014-01-07"), "SEK", 9.00))

      val expexted = List(
        FxRate(new LocalDate("2014-01-01"), "SEK", 9.1),
        FxRate(new LocalDate("2014-01-02"), "SEK", 9.1),
        FxRate(new LocalDate("2014-01-03"), "SEK", 9.1),
        FxRate(new LocalDate("2014-01-04"), "SEK", 9.2),
        FxRate(new LocalDate("2014-01-05"), "SEK", 9.101),
        FxRate(new LocalDate("2014-01-06"), "SEK", 9.101),
        FxRate(new LocalDate("2014-01-07"), "SEK", 9.00))

      Portfolio.fillFxRates(rates) should contain theSameElementsInOrderAs(expexted)
    }
  }
*/
}
