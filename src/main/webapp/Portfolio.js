var React = require('react')
var PageHeader = require('react-bootstrap').PageHeader
var PerformanceChart = require("./PerformanceChart.js")
var _ = require('lodash')
var $ = require('jquery')

module.exports = React.createClass({
    getInitialState : function() {
        return {
          dateRange: ["1999-01-01"],
          values: [0],
          cash: [0],
          investment: [0]
        }
    },
    componentDidMount : function() {
      $.get("http://localhost:8080/portfolio/values?to=2014-03-01", function(r) {
            this.setState({
              dateRange: _.map(r, function(v) { return v.date }),
              values: _.map(r, function(v) { return parseFloat(v.value) })
            })
                    console.log(this.state.dateRange)
                    console.log(this.state.values)
      }.bind(this))

      $.get("http://localhost:8080/portfolio/balanceSeries?to=2014-03-01", function(r) {
        this.setState({
            cash: _.map(r, function(b) { return parseFloat(b.cash) }),
            investment: _.map(r, function(b) { return parseFloat(b.investment) }),
          })
        console.log(this.state.investment)
        console.log(this.state.cash)
      }.bind(this))
    },
    render : function() {
      return (
        <div>
		      <div className="container">
	          <PageHeader>Portfolio</PageHeader>
	          <div>
	            <PerformanceChart values = { this.state.values } dateRange = { this.state.dateRange } cash = { this.state.cash } investment = {this.state.investment}/>
	          </div>
          </div>
      </div>)
   }
})
