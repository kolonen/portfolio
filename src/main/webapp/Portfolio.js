var React = require('react')
var PageHeader = require('react-bootstrap').PageHeader
var PortfolioChart = require("./PerformanceChart.js")
var _ = require('lodash')
var $ = require('jquery')

module.exports = React.createClass({
    getInitialState : function() {
        return {
          values : [{ date : "1999-01-01", value : 0 }],
          balances : [{ date : "1999-01-01", cash : 0, investment: 12589.88 }]
        }
    },
    componentDidMount : function() {
      $.get("http://localhost:8080/portfolio/values?to=2014-03-01", function(r) {
            this.setState({ values  : r })
            console.log(this.state.values)
      }.bind(this))
      $.get("http://localhost:8080/portfolio/balanceSeries?to=2014-03-01", function(r) {
        this.setState({ balances : r })
        console.log(this.state.balances)
      }.bind(this))
    },
    render : function() {
      return (
        <div>
		      <div className="container">
	          <PageHeader>Portfolio</PageHeader>
	          <div>
	            <PerformanceChart values = { this.state.values } balances = { this.state.balances }/>
	          </div>
          </div>
      </div>)
   }
})
