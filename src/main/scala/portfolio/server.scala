package portfolio

import javax.servlet.ServletContext

import org.eclipse.jetty.servlet.DefaultServlet
import org.eclipse.jetty.webapp.WebAppContext
import org.scalatra.servlet.ScalatraListener
import org.eclipse.jetty.server.Server

import org.scalatra.LifeCycle
/**
 * Created by kolonen on 22.12.2015.
 */
object server {

  def main(args: Array[String]) {

    val server = new Server(8080)
    val context = new WebAppContext()
    context setContextPath "/"
    context.setInitParameter(ScalatraListener.LifeCycleKey, "portfolio.ScalatraBootstrap")
    context.addEventListener(new ScalatraListener)
    context.addServlet(classOf[DefaultServlet], "/")
    server.setHandler(context)
    server.start
    server.join
  }
}

class ScalatraBootstrap extends LifeCycle {
  override def init(context: ServletContext) {
    context.mount(new PortfolioApi, "/*")
  }
}

