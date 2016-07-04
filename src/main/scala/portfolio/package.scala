import org.joda.time.{Days, LocalDate}

/**
 * Created by kolonen on 3.7.2016.
 */
package object portfolio {

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
